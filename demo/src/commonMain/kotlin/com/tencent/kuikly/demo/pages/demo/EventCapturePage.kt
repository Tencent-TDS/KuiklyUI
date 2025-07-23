/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.CaptureRule
import com.tencent.kuikly.core.base.attr.CaptureRuleDirection
import com.tencent.kuikly.core.base.attr.CaptureRuleType
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.Slider
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.abs
import kotlin.math.max

@Page("capture", supportInLocal = true)
internal class EventCapturePage : BasePager() {

    private class SlidePageItem {
        lateinit var bgColor : Color
        lateinit var title : String
    }

    private var captureRule by observable<CaptureRule?>(null)
    private lateinit var viewRef: ViewRef<*>
    private var viewRect by observable(Pair(0f, 0f))
    private var dragging = false

    // private var translateLeft by observable(0f)
    private var touchStart: Pair<Float, Float>? = null
    private var slop = 1f
    private var pageItemList = arrayListOf<SlidePageItem>()
    private var lastX = 0f
    private var lastTs = 0L
    private var lastV = 0f

    override fun created() {
        super.created()
        slop = pageData.params.optDouble("slop", 1.0).toFloat()

        pageItemList.add(SlidePageItem().also { item ->
            item.bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            item.title = "第一页"
        })
        pageItemList.add(SlidePageItem().also { item ->
            item.bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            item.title = "第二页"
        })
        pageItemList.add(SlidePageItem().also { item ->
            item.bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            item.title = "第三页"
        })
        pageItemList.add(SlidePageItem().also { item ->
            item.bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            item.title = "最后一页"
        })
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }
            Button {
                attr {
                    width(100f)
                    height(40f)
                    titleAttr {
                        text("reset")
                    }
                    highlightBackgroundColor(Color(0x33ffeeee))
                    borderRadius(20f)
                    border(Border(1f, BorderStyle.SOLID, Color.BLUE))
                }
                event {
                    click {
                        ctx.viewRef.view?.getViewAttr()?.animateTo(Animation.easeInOut(.25f)) {
                            transform(Translate.DEFAULT)
                        }
                    }
                }
            }
            View {
                ref { ctx.viewRef = it }
                attr {
                    absolutePositionAllZero()
                    capture(ctx.captureRule)
                }
                event {
                    pan {
                        val view = getView() ?: return@pan
                        when (it.state) {
                            "start" -> {
                                KLog.i("capture", "pan start x=${it.pageX}")
                                if (it.pageX < 100f) {
                                    ctx.touchStart = Pair(it.pageX, it.pageY)
                                } else {
                                    ctx.touchStart = null
                                }
                            }

                            "move" -> {
                                KLog.i(
                                    "capture",
                                    "move x=${it.pageX} y=${it.pageY} start=${ctx.touchStart} drag=${ctx.dragging}"
                                )
                                val touchStart = ctx.touchStart ?: return@pan
                                val layoutWidth = view.frame.width
                                if (!ctx.dragging) {
                                    val diffX = it.pageX - touchStart.first
                                    val diffY = abs(it.pageY - touchStart.second)
                                    if (diffX.square() + diffY.square() > ctx.slop.square()) {
                                        if (diffX > diffY) {
                                            KLog.i("capture", "accept")
                                            ctx.dragging = true
                                        } else {
                                            KLog.i("capture", "reject")
                                            ctx.touchStart = null
                                            return@pan
                                        }
                                    } else {
                                        KLog.i("capture", "continue")
                                        return@pan
                                    }
                                }
                                if (ctx.dragging) {
                                    val now = DateTime.currentTimestamp()
                                    if (ctx.lastTs > 0L) {
                                        ctx.lastV = (it.pageX - ctx.lastX) / max(1L, now - ctx.lastTs)
                                    }
                                    ctx.lastX = it.pageX
                                    ctx.lastTs = now
                                    val translateLeft = max(0f, (it.pageX - touchStart.first) / layoutWidth)
                                    KLog.i("capture", "dragging translateLeft=${translateLeft}")
                                    view.getViewAttr().transform(Translate(translateLeft, 0f))
                                }
                            }

                            "end" -> {
                                KLog.i("capture", "end v=${ctx.lastV}")
                                val touchStart = ctx.touchStart ?: return@pan
                                val layoutWidth = view.frame.width
                                ctx.dragging = false
                                val translateLeft = if (ctx.lastV > ctx.slop || (it.pageX - touchStart.first) / layoutWidth > .5f) 1f else 0f
                                view.getViewAttr().animateTo(Animation.easeInOut(.25f)) {
                                    transform(Translate(translateLeft, 0f))
                                }
                                ctx.touchStart = null
                                ctx.lastTs = 0L
                                ctx.lastV = 0f
                            }

                            else -> {}
                        }
                    }
                }
                NavBar {
                    attr {
                        title = "title"
                    }
                }
                PageList {
                    attr {
                        flexDirectionRow()
                        size(ctx.viewRect.first, 100f)
                        pageItemWidth(ctx.viewRect.first)
                        pageItemHeight(100f)
                        defaultPageIndex(1)
                        bouncesEnable(false)
                        showScrollerIndicator(false)
                    }
                    ctx.pageItemList.forEach { item ->
                        View {
                            attr {
                                backgroundColor(item.bgColor)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text(item.title)
                                    fontSize(20f)
                                    color(Color.BLACK)
                                }
                            }
                        }
                    }
                }
                View {
                    attr {
                        height(100f)
                        backgroundColor(Color.WHITE)
                        allCenter()
                    }
                    event {
                        pan {
                            KLog.i("capture", "inner pan $it")
                        }
                    }
                    Text {
                        attr { text("listen pan") }
                    }
                }
                View {
                    attr {
                        flex(1f)
                    }
                    event {
                        layoutFrameDidChange {
                            ctx.viewRect = Pair(it.width, it.height)
                        }
                    }
                    PageList {
                        attr {
                            flexDirectionColumn()
                            size(ctx.viewRect.first, ctx.viewRect.second)
                            pageItemWidth(ctx.viewRect.first)
                            pageItemHeight(ctx.viewRect.second)
                            defaultPageIndex(1)
                        }
                        View {
                            attr { backgroundColor(0xffffeeee) }
                            View {
                                attr {
                                    flexDirectionRow()
                                    padding(5f)
                                }
                                View {
                                    attr {
                                        size(20f, 20f)
                                        borderRadius(10f)
                                        border(Border(2f, BorderStyle.SOLID, Color.GRAY))
                                    }
                                    View {
                                        attr {
                                            visibility(ctx.captureRule != null)
                                            absolutePosition(4f, 4f, 4f, 4f)
                                            borderRadius(10f)
                                            backgroundColor(Color.BLACK)
                                        }
                                    }
                                }
                                Text {
                                    attr {
                                        text("拦截")
                                        marginLeft(5f)
                                        height(20f)
                                        lineHeight(20f)
                                    }
                                }
                                event {
                                    click {
                                        if (ctx.captureRule == null) {
                                            ctx.captureRule = CaptureRule(CaptureRuleType.PAN, Frame(0f, 0f, 50f, ctx.pageData.pageViewHeight), CaptureRuleDirection.TO_RIGHT)
                                        } else {
                                            ctx.captureRule = null
                                        }
                                    }
                                }
                            }
                        }
                        View {
                            attr { backgroundColor(0xffeeffee); allCenter() }
                            View {
                                attr {
                                    size(100f, 100f)
                                    backgroundColor(Color.WHITE)
                                }
                                event {
                                    pan {
                                        KLog.i("capture", "inner pan $it")
                                    }
                                }
                                Text {
                                    attr { text("listen pan") }
                                }
                            }
                        }
                        View {
                            attr { backgroundColor(0xffeeeeff) }
                        }
                    }
                    Slider {
                        attr {
                            absolutePosition(left = 0f, bottom = pagerData.safeAreaInsets.bottom)
                            size(ctx.viewRect.first, 50f)
                            currentProgress(0.5f)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private fun Float.square() = this * this
        private inline fun Attr.animateTo(animation: Animation, block: Attr.() -> Unit) {
            Attr.StyleConst.ANIMATION with animation.toString()
            block()
            Attr.StyleConst.ANIMATION with ""
        }
    }

}

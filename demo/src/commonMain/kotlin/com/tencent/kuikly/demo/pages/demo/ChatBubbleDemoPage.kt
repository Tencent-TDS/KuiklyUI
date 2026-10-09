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

package com.tencent.kuikly.demo.pages.demo.abc

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Anchor
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.demo.base.NavBar

private fun ViewContainer<*, *>.ChatBubble(init: ChatBubbleView.() -> Unit) {
    addChild(ChatBubbleView(), init)
}

private open class ChatBubbleView : ComposeView<ComposeAttr, ComposeEvent>() {
    var messageText: String by observable("")

    override fun createAttr() = ComposeAttr()
    override fun createEvent() = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    flexDirectionRow()
                    alignItemsCenter()
                }
                View {
                    attr {
                        size(40f, 40f)
                        borderRadius(20f)
                        allCenter()
                        backgroundColor(Color(0xFF0A84FF))
                    }
                    Text {
                        attr {
                            text("K")
                            fontSize(16f)
                            color(Color.WHITE)
                        }
                    }
                }
                View {
                    attr {
                        flex(1f)
                        margin(left = 10f)
                        padding(all = 10f)
                        borderRadius(10f)
                        backgroundColor(Color(0xFFEFEFF4))
                    }
                    Text {
                        attr {
                            text(ctx.messageText)
                            fontSize(15f)
                            lineHeight(20f)
                            color(Color(0xFF1C1C1E))
                        }
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.ActionButton(init: ActionButtonView.() -> Unit) {
    addChild(ActionButtonView(), init)
}

private open class ActionButtonView : ComposeView<ComposeAttr, ComposeEvent>() {
    var title: String by observable("")
    var buttonColor: Color = Color.BLUE
    var icon: String by observable("")

    override fun createAttr() = ComposeAttr()
    override fun createEvent() = ComposeEvent()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flex(1f)
                    allCenter()
                    backgroundColor(ctx.buttonColor)
                }
                Text {
                    attr {
                        text(ctx.icon)
                        fontSize(18f)
                        color(Color.WHITE)
                    }
                }
                Text {
                    attr {
                        text(ctx.title)
                        marginTop(2f)
                        fontSize(12f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}

private const val BIG_SCALE = 1f
private const val SMALL_SCALE = 1f / 3f

internal class SwipeActionCardData(scope: PagerScope) {
    var bgColor = Color.BLACK
    var title = ""
    var transformScale by scope.observable(BIG_SCALE)
    var translateX by scope.observable(0f)
    var zPosition by scope.observable(0)
}

internal fun PagerScope.SwipeActionCardData() = SwipeActionCardData(this)

@Page("ChatBubbleDemoPage")
internal class ChatBubbleDemoPage : BasePager() {
    private var cardDataList by observableList<SwipeActionCardData>()
    private var isFavorited by observable(false)
    private var toastText by observable("")
    private var viewWidth by observable(0f)

    private fun slotWidth(): Float =
        if (viewWidth > 0f) viewWidth else pagerData.pageViewWidth

    override fun created() {
        super.created()

        cardDataList.add(SwipeActionCardData().apply {
            bgColor = Color(0xFFF7F7F7)
            title = "帮忙看下这个 MR"
        })
        cardDataList.add(SwipeActionCardData().apply {
            bgColor = Color.TRANSPARENT
            title = ""
        })
    }

    /**
     * offset=0：第一个占满（scale 1，translate 0），第二个在屏外。
     * offset=2/3*W（最大滚动距离）：第一个显示 2/3，第二个以 1/2 scale 完整露出（视觉宽 1/3）。
     *
     * 内容总宽 = W + 2/3·W = 5/3·W，最大 offset 正好 2/3·W，不会滚过头。
     * item1 视差跟随：物理左移 offsetX，translate 右拉 progress/3·W，净位移 -0.5·offsetX；
     * item2 物理宽 2/3·W，scale 0.5 → 视觉宽 1/3·W，translate 贴住 item1 右缘，不漏缝。
     */
    private fun updateSwipeRevealState(offsetX: Float, slotWidth: Float) {
        if (slotWidth <= 0f || cardDataList.isEmpty()) {
            return
        }
        val maxOffset = 2f * slotWidth / 3f
        val progress = (offsetX / maxOffset).coerceIn(0f, 1f)

        cardDataList.forEachIndexed { index, item ->
            when (index) {
                0 -> {
                    item.transformScale = 1f
                    item.translateX = progress * (1f / 3f)
                    item.zPosition = 30
                }
                else -> {
                    item.transformScale = 0.5f
                    item.translateX = progress * 0.5f
                    item.zPosition = 20
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        val galleryH = 100f

        updateSwipeRevealState(offsetX = 0f, slotWidth = ctx.slotWidth())

        return {
            NavBar {
                attr {
                    title = "聊天左滑操作"
                }
            }
            vif({ ctx.toastText.isNotEmpty() }) {
                Text {
                    attr {
                        text(ctx.toastText)
                        margin(left = 16f, right = 16f, top = 8f)
                        fontSize(13f)
                        color(Color(0xFF8A8A8E))
                    }
                }
            }
            View {
                attr {
                    marginTop(16f)
                    margin(left = 16f, right = 16f)
                    height(72f)
                    flexDirectionRow()
                    borderRadius(12f)
                    overflow(true)
                    backgroundColor(Color.WHITE)
                }
                event {
                    layoutFrameDidChange { frame ->
                        if (frame.width > 0f && ctx.viewWidth != frame.width) {
                            ctx.viewWidth = frame.width
                            ctx.updateSwipeRevealState(offsetX = 0f, slotWidth = ctx.slotWidth())
                        }
                    }
                }
                PageList {
                    attr {
                        flex(1f)
                        height(72f)
                        flexDirectionRow()
                        bouncesEnable(false)
                        overflow(false)
                        showScrollerIndicator(false)
                        backgroundColor(Color.TRANSPARENT)
                    }
                    event {
                        scroll {
                            ctx.updateSwipeRevealState(offsetX = it.offsetX, slotWidth = ctx.slotWidth())
                        }
                    }

                    ctx.cardDataList.forEachIndexed { index, item ->
                        View {
                            attr {
                                width(if (index == 0) ctx.slotWidth() else ctx.slotWidth() * 2f / 3f)
                                height(72f)
                                zIndex(item.zPosition)
                                transform(
                                    scale = Scale(item.transformScale, 1f),
                                    translate = Translate(item.translateX, 0f),
                                    anchor = Anchor(0f, 0.5f)
                                )
                                backgroundColor(item.bgColor)
                            }
                            vif({ index == 0 }) {
                                ChatBubble {
                                    attr {
                                        flex(1f)
                                        margin(left = 14f, right = 14f)
                                    }
                                    messageText = item.title
                                }
                            }
                            vif({ index != 0 }) {
                                View {
                                    attr {
                                        flex(1f)
                                        flexDirectionRow()
                                    }
                                    ActionButton {
                                        attr {
                                            flex(1f)
                                            height(72f)
                                        }
                                        title = if (ctx.isFavorited) "已收藏" else "收藏"
                                        icon = if (ctx.isFavorited) "★" else "☆"
                                        buttonColor = Color(0xFF30D158)
                                        event {
                                            click {
                                                ctx.isFavorited = !ctx.isFavorited
                                                ctx.toastText = if (ctx.isFavorited) "已收藏" else "已取消收藏"
                                            }
                                        }
                                    }
                                    ActionButton {
                                        attr {
                                            flex(1f)
                                            height(72f)
                                        }
                                        title = "删除"
                                        buttonColor = Color(0xFFFF3B30)
                                        icon = "✕"
                                        event {
                                            click {
                                                ctx.toastText = "已删除"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

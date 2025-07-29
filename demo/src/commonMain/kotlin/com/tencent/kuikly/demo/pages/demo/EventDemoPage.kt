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
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.event.PanGestureParams
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("EventDemoPage")
internal class EventDemoPage : BasePager() {
    var bgHeight: Float by observable(195f)
    var bgOriginHeight: Float = 0f
    lateinit var list: MutableList<GoodsData>
    val globalData = GlobalData()
    var rebuildList: Int by observable(0)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "EventDemo"
                }
            }
            View {
                attr {
                    flex(1f)

                }
                Image {
                    attr {
                        absolutePosition(top = 0f, left = 0f, right = 0f)
                        height(ctx.bgHeight)
                        backgroundColor(Color.GREEN)
                        src("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")

                    }
                }
                vbind({ ctx.rebuildList }) {
                    List {
                        attr {
                            flex(1f)
                        }

                        event {
                            scroll {
                                KLog.i("EventDemoPage", it.toString())

                                val offsetY = it.offsetY

                                if (offsetY < 0) {
                                    ctx.bgHeight = (ctx.bgOriginHeight + -offsetY).toFloat()
                                } else {
                                    ctx.bgHeight = ctx.bgOriginHeight
                                }
                            }
                        }

                        View {
                            attr {
                                height(ctx.bgOriginHeight)
                            }
                        }

                        ctx.list.forEachIndexed { index, goodsData ->
                            GoodsCard {
                                ref {
                                    goodsData.viewRef = it
                                }
                                attr {
                                    data = goodsData
                                    itemIndex = index
                                    transform(Translate(0f, goodsData.translatePercentY))
                                    zIndex(ctx.globalData.zIndex)
                                }
                                attr {
                                    transform(Translate(0f, goodsData.animationTranslatePercentY))
                                    animation(
                                        Animation.easeInOut(0.3f),
                                        goodsData.animationTranslatePercentY
                                    )
                                }

                                event {

                                    animationCompletion {
                                        if (ctx.globalData.targetAnimationCompletionItem == goodsData) {

                                            var newList = mutableListOf<GoodsData>()
                                            ctx.list.forEachIndexed { index, goodsData3 ->
                                                val goodsDataItem = GoodsData()
                                                goodsDataItem.bgColor = goodsData3.bgColor
                                                newList.add(goodsDataItem)
                                            }
                                            ctx.list = newList
                                            ctx.rebuildList++
                                            KLog.i("EventDemoPage", "rebuildList = rebuildList + 1")
                                        }
                                    }

                                    editBtnPan {
                                        val params = it as PanGestureParams
                                        //   Utils.logToNative(pagerId, jsonData.toString())
                                        val state = params.state
                                        //   val x = jsonData.optDouble("x").toFloat()
                                        val y = params.pageY
                                        if (state == "start") {
                                            ctx.globalData.locationYOnPageWhenBegin = y
                                            ctx.globalData.zIndex = ++ctx.globalData.zIndex
                                        }
                                        var offsetY = y - ctx.globalData.locationYOnPageWhenBegin
                                        goodsData.translatePercentY =
                                            offsetY / this@GoodsCard.cardHeight
                                        var beginIndex = index
                                        var layoutFrame =
                                            goodsData.viewRef.view!!.flexNode.layoutFrame
                                        var beginIndexCenterY = layoutFrame.midY() + offsetY  // 中点
                                        ctx.list.forEachIndexed { index2, goodsData2 ->
                                            if (goodsData2 != goodsData) {
                                                // for each
                                                var goodsData2Frame =
                                                    goodsData2.viewRef.view!!.flexNode.layoutFrame
                                                if (index2 < index) { //
                                                    //  Utils.logToNative(pagerId, "beginIndexCenterY" + beginIndexCenterY + "goodsData2Frame" + goodsData2Frame.maxY())
                                                    // 上方
                                                    if (beginIndexCenterY < goodsData2Frame.maxY()) {
                                                        if (goodsData2.animationTranslatePercentY != 1f) {
                                                            ctx.globalData.lastMovedIndex = index2
                                                            ctx.globalData.preTargetPercentageY =
                                                                (goodsData2Frame.midY() - layoutFrame.midY()) / this@GoodsCard.cardHeight
                                                            goodsData2.animationTranslatePercentY =
                                                                1f
                                                        }
                                                    } else {
                                                        if (goodsData2.animationTranslatePercentY != 0f) {
                                                            ctx.globalData.lastMovedIndex = -1
                                                            goodsData2.animationTranslatePercentY =
                                                                0f
                                                        }
                                                    }
                                                } else { // 下方
                                                    if (beginIndexCenterY > goodsData2Frame.minY()) {
                                                        if (goodsData2.animationTranslatePercentY != -1f) {
                                                            ctx.globalData.lastMovedIndex = index2
                                                            ctx.globalData.preTargetPercentageY =
                                                                (goodsData2Frame.midY() - layoutFrame.midY()) / this@GoodsCard.cardHeight
                                                            goodsData2.animationTranslatePercentY =
                                                                -1f
                                                        }
                                                    } else {
                                                        if (goodsData2.animationTranslatePercentY != 0f) {
                                                            ctx.globalData.lastMovedIndex = -1
                                                            goodsData2.animationTranslatePercentY =
                                                                0f
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 更新其他goodsData的卡片

                                        if (state == "end") {
                                            if (ctx.globalData.lastMovedIndex != -1) {
                                                goodsData.animationTranslatePercentY =
                                                    ctx.globalData.preTargetPercentageY
                                                val movedItem =
                                                    ctx.list[ctx.globalData.lastMovedIndex]
                                                ctx.list.remove(goodsData)
                                                val insertIndex = ctx.list.indexOf(movedItem)
                                                if (movedItem.animationTranslatePercentY == 1f) {
                                                    ctx.list.add(insertIndex, goodsData)
                                                } else {
                                                    ctx.list.add(insertIndex + 1, goodsData)
                                                }
                                            } else {
                                                goodsData.animationTranslatePercentY =
                                                    0.01f // 触发diff
                                                // goodsData.animationTranslatePercentY = 0f
                                            }
                                            ctx.globalData.targetAnimationCompletionItem = goodsData
                                            ctx.globalData.lastMovedIndex = -1

                                        }

                                    }
                                }
                            }
                        }

                        View {
                            attr {
                                height(600f)
                            }
                        }

                    }

                }

            }

        }
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun created() {
        super.created()
        bgHeight = pageData.pageViewWidth * (533f / 800f)
        bgOriginHeight = bgHeight

        val list = mutableListOf<GoodsData>()
        this.list = list
        apply {
            val data = GoodsData()
            data.bgColor = Color.YELLOW
            list.add(data)
        }
        apply {
            val data = GoodsData()
            data.bgColor = Color.BLUE
            list.add(data)
        }
        apply {
            val data = GoodsData()
            data.bgColor = Color.RED
            list.add(data)
        }
        apply {
            val data = GoodsData()
            data.bgColor = Color.GREEN
            list.add(data)
        }
        apply {
            val data = GoodsData()
            data.bgColor = Color.RED
            list.add(data)
        }
        apply {
            val data = GoodsData()
            data.bgColor = Color.BLUE
            list.add(data)
        }

    }

}

internal class GoodsData : BaseObject() {
    lateinit var bgColor: Color
    var translatePercentY: Float by observable(0f)
    var animationTranslatePercentY: Float by observable(0f)
    lateinit var viewRef: ViewRef<LiveGoodsCard>
}

internal class GlobalData : BaseObject() {
    var zIndex = 0
    var locationYOnPageWhenBegin = 0f
    var lastMovedIndex = -1
    var preTargetPercentageY = 0f
    var targetAnimationCompletionItem: Any? = null
}

internal class LiveGoodsCardAttr : ComposeAttr() {
    lateinit var outData: GlobalData
    lateinit var data: GoodsData
    var itemIndex: Int = 0
}

internal class LiveGoodsEvent : ComposeEvent() {

    fun editBtnPan(handlerFn: EventHandlerFn) {
        registerEvent(EDIT_BTN_PAN, handlerFn)
    }

    companion object {
        const val EDIT_BTN_PAN = "editBtnPan"
    }
}

internal class LiveGoodsCard : ComposeView<LiveGoodsCardAttr, LiveGoodsEvent>() {
    var cardHeight = 100f
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                height(ctx.cardHeight)
                backgroundColor(ctx.attr.data.bgColor)
                flexDirectionRow()
            }
            View {
                attr {
                    size(80f, 80f)
                    margin(10f)
                    allCenter()
                    backgroundColor(Color.WHITE)
                }
                Text {
                    attr {
                        color(Color.BLACK)
                        fontSize(25f)
                        text(ctx.attr.itemIndex.toString())
                    }

                }
            }

            View {
                attr {
                    flex(1f)
                }
            }
            View {
                attr {
                    allCenter()
                    size(100f, 100f)
                }

                View {
                    attr {
                        size(50f, 30f)
                        backgroundColor(Color.BLACK)
                    }

                    event {
                        pan {
                            KLog.i("EventDemoPage", it.toString())
                            this@LiveGoodsCard.event.onFireEvent(LiveGoodsEvent.EDIT_BTN_PAN, it)
                        }
                    }

                }
            }
        }
    }

    override fun createAttr(): LiveGoodsCardAttr {
        return LiveGoodsCardAttr()
    }

    override fun createEvent(): LiveGoodsEvent {
        return LiveGoodsEvent()
    }

    //

}

internal fun ViewContainer<*, *>.GoodsCard(init: LiveGoodsCard.() -> Unit) {
    addChild(LiveGoodsCard(), init)
}

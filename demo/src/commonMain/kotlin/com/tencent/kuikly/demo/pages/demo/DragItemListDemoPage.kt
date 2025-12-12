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
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.event.PanGestureParams
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import kotlin.math.roundToInt

/**
 * 可托拽调整Item顺序的列表Demo
*/
@Page("DragItemListDemoPage")
internal class DragItemListDemoPage : BasePager() {
    var list by observableList<DragItemData>()
    var globalData = ItemData()
    lateinit var listRef: ViewRef<ListView<*, *>>

    companion object {
        const val TAG = "DragItemListDemoPage"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            View {
                attr {
                    flex(1f)
                }
                List {
                    ref {
                        ctx.listRef = it
                    }
                    attr {
                        flex(1f)
                    }
                    vforIndex({ctx.list}) { cardData, index, _ ->
                        DragItemCard {
                            ref {
                                cardData.viewRef = it
                            }
                            attr {
                                keepAlive(true)
                                data = cardData
                                itemIndex = cardData.text.toInt()
                                transform(Translate(0f, cardData.translatePercentY))
                                zIndex(cardData.index)
                            }
                            attr {
                                transform(Translate(0f, cardData.animationTranslatePercentY))
                                animation(
                                    Animation.easeInOut(cardData.animationTime),
                                    cardData.animationTranslatePercentY
                                )
                            }

                            event {
                                editBtnPan {
                                    val params = it as PanGestureParams
                                    if (it.pageY < 30f) {
                                        val listView = ctx.listRef.view!!
                                        val currentOffset = listView.contentView!!.offsetY
                                        val targetOffset = currentOffset - 30f

                                        // 只有当前不在顶部时才滚动（offsetY > 0 表示还能往上滚）
                                        if (targetOffset > 0f) {
                                            listView.setContentOffset(
                                                offsetX = 0f,
                                                offsetY = targetOffset,
                                                animated = false,
                                            )
                                        } else {
                                            listView.setContentOffset(
                                                offsetX = 0f,
                                                offsetY = 0f,
                                                animated = false
                                            )
                                        }
                                    }

                                    if (it.pageY > ctx.listRef.view!!.flexNode.layoutFrame.height - 30f) {
                                        val listView = ctx.listRef.view!!
                                        val currentOffset = listView.contentView!!.offsetY
                                        val contentHeight = listView.contentView!!.flexNode.layoutFrame.height
                                        val viewportHeight = listView.flexNode.layoutFrame.height
                                        val maxOffset = contentHeight - viewportHeight
                                        val targetOffset = currentOffset + 30f
                                        // 只有未到底部时才滚动
                                        if (targetOffset < maxOffset) {
                                            listView.setContentOffset(
                                                offsetX = 0f,
                                                offsetY = targetOffset,
                                                animated = false
                                            )
                                        } else {
                                            listView.setContentOffset(
                                                offsetX = 0f,
                                                offsetY = maxOffset,
                                                animated = false
                                            )
                                        }
                                    }

                                    val state = params.state
                                    val y = params.pageY
                                    if (state == "start") {
                                        val temp = ctx.listRef.view!!.contentView!!.offsetY
                                        ctx.globalData.contentViewOffset = temp
                                        ctx.globalData.locationYOnPageWhenBegin = y
                                        cardData.index = 1
                                        ctx.list.forEach {
                                            it.animationTime = 0.3f
                                            it.translatePercentY = 0f
                                            it.animationTranslatePercentY = 0f
                                        }
                                    }

                                    val currentOffset = ctx.listRef.view!!.contentView!!.offsetY
                                    val scrollDelta = currentOffset - ctx.globalData.contentViewOffset
                                    val offsetY = (y - ctx.globalData.locationYOnPageWhenBegin) + scrollDelta
                                    cardData.translatePercentY = offsetY / this@DragItemCard.cardHeight
                                    val currentIndex = ctx.list.indexOf(cardData)
                                    var layoutFrame =
                                        cardData.viewRef.view!!.flexNode.layoutFrame
                                    var beginIndexCenterY = layoutFrame.midY() + offsetY  // 中点
                                    ctx.list.forEachIndexed { index2, cardData2 ->
                                        if (cardData2 != cardData) {
                                            var goodsData2Frame =
                                                cardData2.viewRef.view!!.flexNode.layoutFrame
                                            if (index2 < currentIndex) { //
                                                if (beginIndexCenterY < goodsData2Frame.maxY()) {
                                                    if (cardData2.animationTranslatePercentY != 1f) {
                                                        ctx.globalData.lastMovedIndex = index2
                                                        ctx.globalData.preTargetPercentageY =
                                                            (goodsData2Frame.midY() - layoutFrame.midY()) / this@DragItemCard.cardHeight
                                                        cardData2.animationTranslatePercentY =
                                                            1f
                                                    }
                                                } else {
                                                    if (cardData2.animationTranslatePercentY != 0f) {
                                                        ctx.globalData.lastMovedIndex = -1
                                                        cardData2.animationTranslatePercentY =
                                                            0f
                                                    }
                                                }
                                            } else {
                                                if (beginIndexCenterY > goodsData2Frame.minY()) {
                                                    if (cardData2.animationTranslatePercentY != -1f) {
                                                        ctx.globalData.lastMovedIndex = index2
                                                        ctx.globalData.preTargetPercentageY =
                                                            (goodsData2Frame.midY() - layoutFrame.midY()) / this@DragItemCard.cardHeight
                                                        cardData2.animationTranslatePercentY =
                                                            -1f
                                                    }
                                                } else {
                                                    if (cardData2.animationTranslatePercentY != 0f) {
                                                        ctx.globalData.lastMovedIndex = -1
                                                        cardData2.animationTranslatePercentY =
                                                            0f
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (state == "end") {
                                        ctx.list.forEach {
                                            it.animationTime = 0f
                                        }
                                        val fromIndex = ctx.list.indexOf(cardData)
                                        var targetIndex = (fromIndex + (offsetY / this@DragItemCard.cardHeight).roundToInt())
                                        targetIndex =
                                            targetIndex.coerceIn(0, ctx.list.size - 1)
                                        KLog.d(TAG, "fromIndex $fromIndex targetIndex $targetIndex")
                                        if (fromIndex != targetIndex) {
                                            ctx.list.removeAt(fromIndex)
                                            ctx.list.add(targetIndex, cardData)
                                        }
                                        cardData.translatePercentY = 0f
                                        cardData.animationTranslatePercentY = 0f
                                        ctx.list.forEach {
                                            it.translatePercentY = 0f
                                            it.animationTranslatePercentY = 0f
                                            it.animationTime = 0f
                                            it.index = 0
                                        }
                                        ctx.globalData = ItemData()
                                    }
                                }
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
        for (i in 0..50) {
            val data = ItemData()
            data.text = i.toString()
            data.bgColor = if (i % 3 == 0) Color.RED else if (i % 3 == 1) Color.BLUE else Color.GREEN
            list.add(data)
        }
    }
}

// helper fun for refactoring
private fun PagerScope.ItemData() = DragItemData(this)

internal class DragItemData(scope: PagerScope) : BaseObject() {
    var text = ""
    var index by scope.observable(0)
    lateinit var bgColor: Color
    var translatePercentY: Float by scope.observable(0f)
    var animationTranslatePercentY: Float by scope.observable(0f)
    var animationTime: Float by scope.observable(0f)
    lateinit var viewRef: ViewRef<DragItemCardView>
    var contentViewOffset = 0f
    var lastMovedIndex = -1
    var locationYOnPageWhenBegin = 0f
    var preTargetPercentageY = 0f
}


internal class DragItemCardAttr : ComposeAttr() {
    lateinit var data: DragItemData
    var itemIndex: Int = 0
}

internal class DragItemCardEvent : ComposeEvent() {

    fun editBtnPan(handlerFn: EventHandlerFn) {
        registerEvent(EDIT_BTN_PAN, handlerFn)
    }

    companion object {
        const val EDIT_BTN_PAN = "editBtnPan"
    }
}

internal class DragItemCardView : ComposeView<DragItemCardAttr, DragItemCardEvent>() {
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
                            this@DragItemCardView.event.onFireEvent(LiveGoodsEvent.EDIT_BTN_PAN, it)
                        }
                    }

                }
            }
        }
    }

    override fun createAttr(): DragItemCardAttr {
        return DragItemCardAttr()
    }

    override fun createEvent(): DragItemCardEvent {
        return DragItemCardEvent()
    }
}

internal fun ViewContainer<*, *>.DragItemCard(init: DragItemCardView.() -> Unit) {
    addChild(DragItemCardView(), init)
}
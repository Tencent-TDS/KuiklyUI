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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.getFirstVisiblePosition
import com.tencent.kuikly.core.directives.scrollToPosition
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("vforlazyFrontInsert")
internal class VforLazyFrontInsertDemoPage : BasePager() {

    private lateinit var listRef: ViewRef<ListView<*, *>>
    private val items by observableList<FrontInsertItem>()
    private var frontAdded = 0
    private var pendingFrontInsert = false
    private var insertingFront = false
    private var lastNearTop = false
    private var listScrollSettled = true
    private var frontInsertLockedUntilLeaveTop = false

    override fun created() {
        super.created()
        resetItems()
        scrollToBottom(false)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    backDisable = false
                    title = "VforLazy Front Insert"
                }
            }
            List {
                ref {
                    ctx.listRef = it
                }
                attr {
                    flex(1f)
                    bouncesEnable(false)
                    backgroundColor(Color.WHITE)
                }
                event {
                    dragBegin {
                        ctx.handleListDragBegin()
                    }
                    scroll {
                        ctx.handleListScroll(it.offsetY)
                    }
                    scrollEnd {
                        ctx.handleListScrollEnd(it.offsetY)
                    }
                }
                View {
                    attr {
                        height(TOP_LOADING_HEIGHT)
                        allCenter()
                    }
                    Text {
                        attr {
                            text("触顶自动前插中...")
                            color(Color.GRAY)
                            fontSize(13f)
                        }
                    }
                }
                vforLazy({ ctx.items }, maxLoadItem = LAZY_MAX_LOAD_ITEM) { item, index, count ->
                    View {
                        attr {
                            height(item.height)
                            margin(left = 12f, right = 12f, top = 6f, bottom = 6f)
                            padding(left = 12f, right = 12f)
                            borderRadius(8f)
                            justifyContentCenter()
                            backgroundColor(if (index % 2 == 0) 0xFFF5F5F5 else 0xFFEAEAEA)
                        }
                        Text {
                            attr {
                                text("index=$index id=${item.id}")
                                fontSize(12f)
                                color(Color.GRAY)
                            }
                        }
                        Text {
                            attr {
                                marginTop(4f)
                                text(item.title)
                                fontSize(16f)
                                color(Color.BLACK)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun resetItems() {
        frontAdded = 0
        pendingFrontInsert = false
        insertingFront = false
        lastNearTop = false
        listScrollSettled = true
        frontInsertLockedUntilLeaveTop = false
        items.clear()
        items.addAll(createInitialItems(INITIAL_ITEM_COUNT))
    }

    private fun scrollToBottom(animate: Boolean) {
        addTaskWhenPagerUpdateLayoutFinish {
            listRef.view?.scrollToPosition(items.size, animate = animate)
        }
    }

    private fun handleListDragBegin() {
        listScrollSettled = false
    }

    private fun handleListScroll(offsetY: Float) {
        if (offsetY > TOP_INSERT_TRIGGER_OFFSET) {
            listScrollSettled = false
            if (lastNearTop || frontInsertLockedUntilLeaveTop) {
                lastNearTop = false
                pendingFrontInsert = false
                frontInsertLockedUntilLeaveTop = false
            }
            return
        }
        if (insertingFront) {
            return
        }
        if (frontInsertLockedUntilLeaveTop) {
            lastNearTop = true
            return
        }
        pendingFrontInsert = true
        lastNearTop = true
        if (listScrollSettled) {
            insertFrontItemsKeepingPosition()
        }
    }

    private fun handleListScrollEnd(offsetY: Float) {
        handleListScroll(offsetY)
        listScrollSettled = true
        if (!pendingFrontInsert || insertingFront) {
            return
        }
        insertFrontItemsKeepingPosition()
    }

    private fun insertFrontItemsKeepingPosition() {
        val view = listRef.view ?: return
        pendingFrontInsert = false
        insertingFront = true
        frontInsertLockedUntilLeaveTop = true
        val (index, offset) = getFrontInsertAnchor(view) ?: run {
            insertingFront = false
            return
        }
        val newItems = createFrontItems(FRONT_INSERT_COUNT)
        items.addAll(0, newItems)
        addTaskWhenPagerUpdateLayoutFinish(view) {
            view.scrollToPosition(index + newItems.size, offset, animate = false)
            insertingFront = false
        }
    }

    private fun getFrontInsertAnchor(view: ListView<*, *>): Pair<Int, Float>? {
        val (index, offset) = view.getFirstVisiblePosition()
        if (index < 0) {
            return null
        }
        return if (index == TOP_LOADING_INDEX) {
            Pair(FIRST_ITEM_INDEX, offset - TOP_LOADING_HEIGHT)
        } else {
            Pair(index, offset)
        }
    }

    private fun createInitialItems(count: Int): List<FrontInsertItem> {
        return buildList {
            for (i in 0 until count) {
                add(FrontInsertItem("base-$i", "Base item $i", itemHeightFor(i)))
            }
        }
    }

    private fun createFrontItems(count: Int): List<FrontInsertItem> {
        frontAdded += count
        return buildList {
            for (i in 0 until count) {
                val order = frontAdded - i
                add(FrontInsertItem("front-$order", "Inserted item $order", itemHeightFor(order)))
            }
        }
    }

    private fun itemHeightFor(seed: Int): Float = when (seed % 3) {
        0 -> 56f
        1 -> 72f
        else -> 88f
    }
}

private data class FrontInsertItem(
    val id: String,
    val title: String,
    val height: Float
)

private fun Pager.addTaskWhenPagerUpdateLayoutFinish(view: DeclarativeBaseView<*, *>, task: () -> Unit) {
    addTaskWhenPagerUpdateLayoutFinish {
        if (view.flexNode.isDirty) {
            addTaskWhenPagerUpdateLayoutFinish(view, task)
        } else {
            task()
        }
    }
}

private const val INITIAL_ITEM_COUNT = 200
private const val FRONT_INSERT_COUNT = 50
private const val TOP_LOADING_INDEX = 0
private const val FIRST_ITEM_INDEX = 1
private const val TOP_LOADING_HEIGHT = 40f
private const val TOP_INSERT_TRIGGER_OFFSET = TOP_LOADING_HEIGHT + 320f
private const val LAZY_MAX_LOAD_ITEM = 100

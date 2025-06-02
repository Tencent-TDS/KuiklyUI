/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.foundation.lazy

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.collection.mutableIntListOf
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import com.tencent.kuikly.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.zIndex
import com.tencent.kuikly.compose.views.StickHeader
import com.tencent.kuikly.compose.views.VirtualView

@ExperimentalFoundationApi
internal class LazyListIntervalContent(
    content: LazyListScope.() -> Unit,
) : LazyLayoutIntervalContent<LazyListInterval>(), LazyListScope {
    override val intervals: MutableIntervalList<LazyListInterval> = MutableIntervalList()

    private var _headerIndexes: MutableIntList? = null
    val headerIndexes: IntList
        get() = _headerIndexes ?: emptyIntList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            LazyListInterval(key = key, type = contentType, item = itemContent)
        )
    }

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        intervals.addInterval(
            1,
            LazyListInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() }
            )
        )
    }

    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.(Int) -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableIntListOf().also { _headerIndexes = it }
        headersIndexes.add(intervals.size)
        val headerIndex = intervals.size

        item(key, contentType) {
            StickHeader(content = {
                content(headerIndex)
            })
        }
    }

    override fun stickyHeaderWithMarginTop(
        key: Any?,
        listState: LazyListState,
        hoverMarginTop: Dp,
        content: @Composable LazyItemScope.(Int) -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableIntListOf().also { _headerIndexes = it }
        val hoverMarginTopPx =  with(listState.density) { hoverMarginTop.roundToPx() }
        headersIndexes.add(intervals.size)
        val headerIndex = intervals.size
        item(key, "sticky") {
            val stickyOffset by remember(hoverMarginTopPx, listState) {
                derivedStateOf {
                    val visibleItems = listState.layoutInfo.visibleItemsInfo
                    val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
                    
                    // 查找当前 header 在可见项中的位置
                    val headerItem = visibleItems.firstOrNull { it.contentType == "sticky" }
                    val offset = headerItem?.offset
                    
                    if (headerItem != null) {
                        // header 在可见项中，基于它的偏移计算吸顶效果
                        val currentOffset = offset ?: 0
                        if (currentOffset > hoverMarginTopPx) {
                            // header 还没到达吸顶位置
                            0
                        } else {
                            // header 已经到达吸顶位置
                            (hoverMarginTopPx - currentOffset).toInt()
                        }
                    } else {
                        // header 不在可见项中
                        // 判断 headerIndex 是否小于等于 firstVisibleIndex
                        if (headerIndex <= firstVisibleIndex) {
                            // header 在第一个可见项之前，应该吸顶
                            hoverMarginTopPx.toInt()
                        } else {
                            // header 在可见项之后，不需要吸顶
                            0
                        }
                    }
                }
            }
            
            StickHeader(content = {
                VirtualView(
                    modifier = Modifier
                        .offset {
                            IntOffset(0, stickyOffset)
                        }
                    ) {
                    content(headerIndex)
                }
            },
            modifier = Modifier.zIndex(1000f),
            hoverMarginTop = hoverMarginTop.value)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyListInterval(
    override val key: ((index: Int) -> Any)?,
    override val type: ((index: Int) -> Any?),
    val item: @Composable LazyItemScope.(index: Int) -> Unit
) : LazyLayoutIntervalContent.Interval

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

package com.tencent.kuikly.compose.ui.text

import com.tencent.kuikly.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [MultiParagraph.getBoundingBox] 的纯 Kotlin 回归用例。
 *
 * 核心契约：
 * 1. 同一 [MultiParagraph] 实例里，同 offset 二次读取命中缓存，只触发一次查询。
 * 2. 空路径（[getBoundingBoxFn] 为 null）保持返回零矩形语义，不写缓存、不抛异常。
 */
class MultiParagraphTest {

    @Test
    fun getBoundingBox_sameOffsetTwice_invokesFunctionOnlyOnce() {
        var invokeCount = 0
        val expected = Rect(1f, 2f, 3f, 4f)
        val multiParagraph = MultiParagraph(
            placeholderRects = emptyList(),
            getBoundingBoxFn = { offset ->
                invokeCount++
                Rect(
                    expected.left + offset,
                    expected.top + offset,
                    expected.right + offset,
                    expected.bottom + offset,
                )
            },
        )

        val first = multiParagraph.getBoundingBox(7)
        val second = multiParagraph.getBoundingBox(7)

        assertEquals(Rect(8f, 9f, 10f, 11f), first)
        assertEquals(first, second)
        assertEquals(1, invokeCount)
    }

    @Test
    fun getBoundingBox_nullFunction_returnsZeroRect() {
        val multiParagraph = MultiParagraph(
            initialLineCount = 2,
            placeholderRects = emptyList(),
        )

        val first = multiParagraph.getBoundingBox(3)
        val second = multiParagraph.getBoundingBox(3)

        assertEquals(Rect(0f, 0f, 0f, 0f), first)
        assertEquals(first, second)
    }

    @Test
    fun getBoundingBox_differentOffsets_invokeFunctionPerOffset() {
        var invokeCount = 0
        val multiParagraph = MultiParagraph(
            placeholderRects = emptyList(),
            getBoundingBoxFn = { offset ->
                invokeCount++
                Rect(
                    offset.toFloat(),
                    offset.toFloat(),
                    offset.toFloat() + 1f,
                    offset.toFloat() + 1f,
                )
            },
        )

        val a = multiParagraph.getBoundingBox(3)
        val b = multiParagraph.getBoundingBox(7)
        val aAgain = multiParagraph.getBoundingBox(3)

        assertEquals(Rect(3f, 3f, 4f, 4f), a)
        assertEquals(Rect(7f, 7f, 8f, 8f), b)
        assertEquals(a, aAgain)
        // 两个不同 offset 各触发一次底层查询，同 offset 复读命中缓存不再触发。
        assertEquals(2, invokeCount)
    }
}

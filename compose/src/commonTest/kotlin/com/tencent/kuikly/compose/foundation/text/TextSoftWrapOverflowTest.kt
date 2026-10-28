/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.core.views.RichTextAttr
import com.tencent.kuikly.core.views.TextConst
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `applySoftWrap` 与 `applyOverflow` 作用在同一个 `lineBreakMode` 属性上，
 * 二者按 `applyOverflow` → `applyMaxLines` → `applySoftWrap` 的顺序调用。
 *
 * `softWrap = true` 的语义是「允许按词换行」，不是「放弃显式省略号」。
 * 显式设置的 Ellipsis 必须保留。
 */
class TextSoftWrapOverflowTest {

    private fun attrWith(overflow: TextOverflow, softWrap: Boolean, maxLines: Int): RichTextAttr {
        val attr = RichTextAttr()
        attr.applyOverflow(overflow)
        attr.applyMaxLines(maxLines)
        attr.applySoftWrap(softWrap)
        return attr
    }

    @Test
    fun softWrapKeepsExplicitEllipsis() {
        val attr = attrWith(TextOverflow.Ellipsis, softWrap = true, maxLines = 1)
        assertEquals(
            "tail",
            attr.getProp(TextConst.TEXT_OVERFLOW),
            "softWrap = true 不应覆盖显式设置的 Ellipsis",
        )
    }

    @Test
    fun softWrapDoesNothingWithoutExplicitOverflow() {
        val attr = RichTextAttr()
        attr.applyMaxLines(Int.MAX_VALUE)
        attr.applySoftWrap(true)
        assertEquals(null, attr.getProp(TextConst.TEXT_OVERFLOW))
    }

    @Test
    fun noSoftWrapKeepsTailAndForcesSingleLine() {
        val attr = attrWith(TextOverflow.Ellipsis, softWrap = false, maxLines = 1)
        assertEquals("tail", attr.getProp(TextConst.TEXT_OVERFLOW))
        assertEquals(1, attr.getProp(TextConst.LINES))
    }

    @Test
    fun noSoftWrapSetsClipWhenNoExplicitOverflow() {
        val attr = RichTextAttr()
        attr.applySoftWrap(false)
        assertEquals("clip", attr.getProp(TextConst.TEXT_OVERFLOW))
        assertEquals(1, attr.getProp(TextConst.LINES))
    }

    @Test
    fun clipIsNotClobberedBySoftWrap() {
        val attr = attrWith(TextOverflow.Clip, softWrap = true, maxLines = 1)
        assertEquals("clip", attr.getProp(TextConst.TEXT_OVERFLOW))
    }
}

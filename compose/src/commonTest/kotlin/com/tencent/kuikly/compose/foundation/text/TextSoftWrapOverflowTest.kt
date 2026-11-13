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

    /**
     * `applyOverflow(Clip)` 在属性为空时必须写入 `clip`，不能依赖平台对「未设置」的兜底。
     *
     * iOS `KRConvertUtil.NSLineBreakMode` 与 OHOS `ConvertToTextBreakMode` 对未设置都回退
     * `tail`，跳过写入会让 `TextOverflow.Clip` 在这两个平台上静默变成省略号。
     * 该写入使未显式传 `overflow` 的受限行数文本在 iOS / OHOS 上由省略号变为硬截断，
     * 与 Compose 默认语义一致，由此用例锁定。
     */
    @Test
    fun clipIsWrittenExplicitlyInsteadOfLeftUnset() {
        val attr = RichTextAttr()
        attr.applyOverflow(TextOverflow.Clip)
        assertEquals(
            "clip",
            attr.getProp(TextConst.TEXT_OVERFLOW),
            "clip 必须显式写入，不能依赖平台对未设置的兜底",
        )
    }
}

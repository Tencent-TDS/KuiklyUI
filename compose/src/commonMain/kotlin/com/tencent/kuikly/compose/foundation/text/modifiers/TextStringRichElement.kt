/*
 * Copyright 2023 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.text.modifiers

import com.tencent.kuikly.compose.foundation.text.DefaultMinLines
import com.tencent.kuikly.compose.foundation.text.InlineTextContent
import com.tencent.kuikly.compose.material3.EmptyInlineContent
import com.tencent.kuikly.compose.ui.graphics.ColorProducer
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.style.TextOverflow

/**
 * Modifier element for any Text with [AnnotatedString] or [onTextLayout] parameters
 *
 * This is slower than [TextAnnotatedStringElement]
 */
internal class TextStringRichElement(
    private val text: AnnotatedString,
    private val style: TextStyle,
//    private val fontFamilyResolver: FontFamily.Resolver,
    private val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
    private val color: ColorProducer? = null,
    private val inlineContent: Map<String, InlineTextContent> = EmptyInlineContent
) : ModifierNodeElement<TextStringRichNode>() {

    override fun create(): TextStringRichNode = TextStringRichNode(
        text,
        style,
//        fontFamilyResolver,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        color,
        inlineContent
    )

    override fun update(node: TextStringRichNode) {
        node.doInvalidations(
            textChanged = node.updateText(
                text = text
            ),
            layoutChanged = node.updateLayoutRelatedArgs(
                style = style,
                minLines = minLines,
                maxLines = maxLines,
                softWrap = softWrap,
                inlineContent = inlineContent,
//                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow
            ),
            callbacksChanged = node.updateCallbacks(
                onTextLayout = onTextLayout,
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is TextStringRichElement) return false

        // these three are most likely to actually change
        if (color != other.color) return false
        if (text != other.text) return false /* expensive to check, do it after color */
        if (style != other.style) return false
//        if (placeholders != other.placeholders) return false

        // these are equally unlikely to change
//        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (inlineContent != other.inlineContent) return false
        if (onTextLayout != other.onTextLayout) return false
        if (overflow != other.overflow) return false
        if (softWrap != other.softWrap) return false
        if (maxLines != other.maxLines) return false
        if (minLines != other.minLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + style.hashCode()
//        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + (onTextLayout?.hashCode() ?: 0)
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + inlineContent.hashCode()
//        result = 31 * result + (placeholders?.hashCode() ?: 0)
        result = 31 * result + (color?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

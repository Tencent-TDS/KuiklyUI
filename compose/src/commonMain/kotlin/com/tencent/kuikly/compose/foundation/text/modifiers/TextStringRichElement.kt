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
import com.tencent.kuikly.compose.ui.unit.Density

/**
 * Modifier element for any Text with [AnnotatedString] or [onTextLayout] parameters
 *
 * This is slower than [TextAnnotatedStringElement]
 */
internal class TextStringRichElement(
    private val text: String?,
    private val annotatedText: AnnotatedString?,
    private val style: TextStyle,
    private val density: Density,
//    private val fontFamilyResolver: FontFamily.Resolver,
    private val onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private val overflow: TextOverflow = TextOverflow.Clip,
    private val softWrap: Boolean = true,
    private val maxLines: Int = Int.MAX_VALUE,
    private val minLines: Int = DefaultMinLines,
    private val inlineContent: Map<String, InlineTextContent> = EmptyInlineContent,
    private val fontSizeScale: Float = 1.0f,
    private val fontWeightScale: Float = 1.0f
) : ModifierNodeElement<TextStringRichNode>() {

    override fun create(): TextStringRichNode = TextStringRichNode(
        text,
        annotatedText,
        style,
        density,
//        fontFamilyResolver,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        inlineContent,
        fontSizeScale,
        fontWeightScale
    )

    override fun update(node: TextStringRichNode) {
        val textChanged = node.updateText(
            text = text,
            annotatedText = annotatedText
        )
        val layoutChanged = node.updateLayoutRelatedArgs(
            style = style,
            minLines = minLines,
            maxLines = maxLines,
            softWrap = softWrap,
            inlineContent = inlineContent,
            fontSizeScale = fontSizeScale,
            fontWeightScale = fontWeightScale,
            density = density,
//                fontFamilyResolver = fontFamilyResolver,
            overflow = overflow
        )
        val callbacksChanged = node.updateCallbacks(
            onTextLayout = onTextLayout,
        )
        node.doInvalidations(
            textChanged = textChanged,
            layoutChanged = layoutChanged,
            callbacksChanged = callbacksChanged,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other !is TextStringRichElement) {
            return false
        }

        // these are most likely to actually change
        if (text != other.text) {
            return false /* expensive to check, do it after color */
        }
        if (annotatedText != other.annotatedText) {
            return false
        }
        if (style != other.style) {
            return false
        }
//        if (placeholders != other.placeholders) return false

        // these are equally unlikely to change
//        if (fontFamilyResolver != other.fontFamilyResolver) return false
        if (inlineContent != other.inlineContent) {
            return false
        }
        if (onTextLayout != other.onTextLayout) {
            return false
        }
        if (overflow != other.overflow) {
            return false
        }
        if (softWrap != other.softWrap) {
            return false
        }
        if (maxLines != other.maxLines) {
            return false
        }
        if (minLines != other.minLines) {
            return false
        }
        if (fontSizeScale != other.fontSizeScale) {
            return false
        }
        if (fontWeightScale != other.fontWeightScale) {
            return false
        }
        if (density != other.density) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = (text?.hashCode() ?: 0)
        result = 31 * result + (annotatedText?.hashCode() ?: 0)
        result = 31 * result + style.hashCode()
//        result = 31 * result + fontFamilyResolver.hashCode()
        result = 31 * result + (onTextLayout?.hashCode() ?: 0)
        result = 31 * result + overflow.hashCode()
        result = 31 * result + softWrap.hashCode()
        result = 31 * result + maxLines
        result = 31 * result + minLines
        result = 31 * result + inlineContent.hashCode()
//        result = 31 * result + (placeholders?.hashCode() ?: 0)
        result = 31 * result + fontSizeScale.hashCode()
        result = 31 * result + fontWeightScale.hashCode()
        result = 31 * result + density.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}
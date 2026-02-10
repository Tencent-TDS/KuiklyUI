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

package com.tencent.kuikly.compose.foundation.text

import com.tencent.kuikly.compose.extension.scaleToDensity
import com.tencent.kuikly.compose.material3.EmptyInlineContent
import com.tencent.kuikly.compose.resources.toKuiklyFontFamily
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.LinearGradient
import com.tencent.kuikly.compose.ui.graphics.Shadow
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.isSpecified
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontListFontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.font.GenericFontFamily
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.style.TextIndent
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.views.ISpan
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.fastMutableSetOf
import com.tencent.kuikly.core.views.PlaceholderSpan
import com.tencent.kuikly.core.views.RichTextAttr
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.TextSpan


// Returns platform-specific default font size
private fun TextAttr.defaultFontSize(): Float {
    val pagerData = getPager().pageData
    return when {
        pagerData.isIOS || pagerData.isMacOS || pagerData.isOhOs -> 15f
        pagerData.isAndroid -> 13f
        else -> 13f
    }
}

internal fun TextAttr.applyTextStyle(style: TextStyle, density: Density) {
    // Font properties
    applyFontSize(style.fontSize, density)
    applyFontWeight(style.fontWeight)
    applyFontStyle(style.fontStyle)
    applyFontFamily(style.fontFamily)

    // Layout properties
    applyLetterSpacing(style.letterSpacing, density)
    applyLineHeight(style.lineHeight, density)
    applyTextAlign(style.textAlign)
    applyTextIndent(style.textIndent)
}

// Handles font size with reuse optimization
internal fun TextAttr.applyFontSize(fontSize: TextUnit, density: Density) {
    if (fontSize.isSpecified) {
        val scaledFontSize = this.scaleToDensity(density, fontSize.value)
        if (!(fontSize.value == defaultFontSize() && getProp(TextConst.FONT_SIZE) == null)) {
            fontSize(scaledFontSize)
        }
    } else if (getProp(TextConst.FONT_SIZE) != null) {
        // Reset to default when changed to Unspecified
        fontSize(this.scaleToDensity(density, defaultFontSize()))
    }
}

// Handles letter spacing with reuse optimization
internal fun TextAttr.applyLetterSpacing(letterSpacing: TextUnit, density: Density) {
    if (letterSpacing.isSpecified) {
        val spacing = this.scaleToDensity(density, letterSpacing.value)
        if (!(spacing == 0f && getProp(TextConst.LETTER_SPACING) == null)) {
            letterSpacing(spacing)
        }
    } else if (getProp(TextConst.LETTER_SPACING) != null) {
        // Reset to 0 when changed to Unspecified
        letterSpacing(0f)
    }
}

// Handles line height with reuse optimization
internal fun TextAttr.applyLineHeight(lineHeight: TextUnit, density: Density) {
    if (lineHeight.isSpecified) {
        setProp(TextConst.LINE_HEIGHT, this.scaleToDensity(density, lineHeight.value))
    } else if (getProp(TextConst.LINE_HEIGHT) != null) {
        // Reset to 0 (auto) when changed to Unspecified
        setProp(TextConst.LINE_HEIGHT, 0f)
    }
}

// Handles text and background color with reuse optimization
internal fun TextAttr.applyStyleColor(style: SpanStyle) {
    when {
        style.brush is SolidColor -> {
            val color = (style.brush as SolidColor).value
            applyTextColorOptimized(color)
        }
        style.brush is LinearGradient -> {
            val linearGradient = style.brush as LinearGradient
            backgroundLinearGradient(
                linearGradient.direction,
                *linearGradient.resolveForText().colorStops.toTypedArray()
            )
        }
        else -> {
            if (style.color.isSpecified) {
                applyTextColorOptimized(style.color)
            } else if (getProp(TextConst.TEXT_COLOR) != null) {
                // Reset to black when changed to Unspecified
                applyTextColorOptimized(Color.Black)
            }
        }
    }

    if (style.background.isSpecified) {
        setProp(Attr.StyleConst.BACKGROUND_COLOR, style.background.toKuiklyColor().toString())
    }
}

// Skips setProp when color is black and has never been set
private fun TextAttr.applyTextColorOptimized(color: Color) {
    if (color == Color.Black && getProp(TextConst.TEXT_COLOR) == null) {
        return
    }
    setProp(TextConst.TEXT_COLOR, color.toKuiklyColor().toString())
}

// Handles font style with reuse optimization
internal fun TextAttr.applyFontStyle(fontStyle: FontStyle?) {
    val isDefault = (fontStyle == null || fontStyle == FontStyle.Normal)
    val value = if (fontStyle == FontStyle.Italic) "italic" else "normal"
    
    if (isDefault && getProp(TextConst.FONT_STYLE) == null) {
        return
    }
    setProp(TextConst.FONT_STYLE, value)
}

// Handles font weight with reuse optimization
internal fun TextAttr.applyFontWeight(fontWeight: FontWeight?) {
    val weightValue: String = when (fontWeight) {
        FontWeight.W100 -> "300"
        FontWeight.W200 -> "200"
        FontWeight.W300 -> "300"
        FontWeight.W400, null -> "400"
        FontWeight.W500 -> "500"
        FontWeight.W600 -> "600"
        FontWeight.W700, FontWeight.W800, FontWeight.W900 -> "700"
        else -> "400"
    }
    
    if (weightValue == "400" && getProp(TextConst.FONT_WEIGHT) == null) {
        return
    }
    setProp(TextConst.FONT_WEIGHT, weightValue)
}

internal fun TextAttr.applyFontFamily(fontFamily: FontFamily?) {
    when (fontFamily) {
        is GenericFontFamily -> setProp(TextConst.FONT_FAMILY, fontFamily.name)
        is FontListFontFamily -> setProp(TextConst.FONT_FAMILY, fontFamily.fonts.toKuiklyFontFamily())
        else -> if (this.getProp(TextConst.FONT_FAMILY) != null) {
            setProp(TextConst.FONT_FAMILY, "")
        }
    }
}

// Handles text shadow with reuse optimization
internal fun TextAttr.applyShadow(shadow: Shadow?) {
    val isNoOpShadow = shadow == null ||
        (shadow.color == Color.Black && shadow.offset == Offset.Zero && shadow.blurRadius == 0f)
    
    if (isNoOpShadow && getProp(TextConst.TEXT_SHADOW) == null) {
        return
    }
    
    if (shadow == null) {
        setProp(TextConst.TEXT_SHADOW, "0.0 0.0 0.0 0")
    } else if (shadow.color != Color.Unspecified || shadow.offset != Offset.Zero || shadow.blurRadius > 0) {
        setProp(
            TextConst.TEXT_SHADOW,
            BoxShadow(
                shadow.offset.x,
                shadow.offset.y,
                shadow.blurRadius,
                shadow.color.toKuiklyColor()
            ).toString()
        )
    }
}

internal fun TextAttr.applyTextAlign(textAlign: TextAlign?) {
    // Perf: skip when stays at native default (left) and has never been set
    val align = when (textAlign) {
        TextAlign.Left, TextAlign.Unspecified, null -> com.tencent.kuikly.core.views.TextAlign.LEFT.value
        TextAlign.Center -> com.tencent.kuikly.core.views.TextAlign.CENTER.value
        TextAlign.Right -> com.tencent.kuikly.core.views.TextAlign.RIGHT.value
        else -> com.tencent.kuikly.core.views.TextAlign.LEFT.value
    }
    if (align == com.tencent.kuikly.core.views.TextAlign.LEFT.value &&
        getProp(TextConst.TEXT_ALIGN) == null
    ) {
        return
    }
    setProp(TextConst.TEXT_ALIGN, align)
}

internal fun TextAttr.applyTextIndent(textIndent: TextIndent?) {
    // Perf: skip when stays at native default headIndent = 0 and has never been set
    val value = if (textIndent != null && textIndent.firstLine.isSpecified) {
        textIndent.firstLine.value
    } else {
        0f
    }
    if (value == 0f && getProp(TextConst.HEAD_INDENT) == null) {
        return
    }
    setProp(TextConst.HEAD_INDENT, value)
}


// Handles text decoration with reuse optimization
internal fun TextAttr.applyTextDecoration(decoration: TextDecoration?) {
    val value = when (decoration) {
        TextDecoration.Underline -> "underline"
        TextDecoration.LineThrough -> "line-through"
        else -> "none"
    }
    
    if (value == "none" && getProp(TextConst.TEXT_DECORATION) == null) {
        return
    }
    setProp(TextConst.TEXT_DECORATION, value)
}

internal fun TextAttr.applySoftWrap(softWrap: Boolean) {
    val target = if (softWrap) "wordWrapping" else "clip"

    val current = getProp(TextConst.TEXT_OVERFLOW) as? String
    if (softWrap && (current == null || current == target) && getProp(TextConst.LINES) == null) {
        return
    }

    if (!softWrap) {
        if (current != target) {
            setProp(TextConst.TEXT_OVERFLOW, target)
        }
        if ((getProp(TextConst.LINES) as? Int) != 1) {
            setProp(TextConst.LINES, 1)
        }
    } else {
        if (current != null && current != target) {
            setProp(TextConst.TEXT_OVERFLOW, target)
        }
    }
}

internal fun TextAttr.applyOverflow(overflow: TextOverflow) {
    val mode = when (overflow) {
        TextOverflow.Clip -> "clip"
        TextOverflow.Ellipsis -> "tail"
        else -> "clip"
    }
    if (mode == "clip" && getProp(TextConst.TEXT_OVERFLOW) == null) {
        return
    }
    val current = getProp(TextConst.TEXT_OVERFLOW) as? String
    if (current == mode) return
    setProp(TextConst.TEXT_OVERFLOW, mode)
}

internal fun TextAttr.applyMaxLines(maxLines: Int) {
    if (maxLines == Int.MAX_VALUE && getProp(TextConst.LINES) == null) {
        return
    }
    val current = getProp(TextConst.LINES) as? Int
    if (current == maxLines) return
    setProp(TextConst.LINES, maxLines)
}

internal fun RichTextAttr.applyAnnotatedString(
    annoText: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = EmptyInlineContent,
    density: Density
) {
    val spans = fastArrayListOf<ISpan>()

    // Collect all style change positions
    val positions = fastMutableSetOf<Int>()
    positions.add(0)
    positions.add(annoText.text.length)

    // Collect SpanStyle positions
    annoText.spanStyles.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    // Collect ParagraphStyle positions
    annoText.paragraphStyles.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    // Collect LinkAnnotation positions
    val linkAnnotations = annoText.getLinkAnnotations(0, annoText.length)
    linkAnnotations.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    // Collect placeholder info and positions
    val (placeholders, _) = if (annoText.hasInlineContent()) {
        annoText.resolveInlineContent(inlineContent)
    } else {
        Pair(null, null)
    }

    // Add placeholder positions
    placeholders?.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    val sortedPositions = positions.sorted()

    // Process segments by positions
    for (i in 0 until sortedPositions.size - 1) {
        val start = sortedPositions[i]
        val end = sortedPositions[i + 1]

        // Check if this range is a placeholder
        val isPlaceholder = placeholders?.any {
            it.start == start && it.end == end
        } ?: false

        if (isPlaceholder) {
            // Create PlaceholderSpan
            placeholders.find { it.start == start }?.let { placeholder ->
                spans.add(PlaceholderSpan().apply {
                    placeholderSize(
                        this@applyAnnotatedString.scaleToDensity(density, placeholder.item.width.value),
                        this@applyAnnotatedString.scaleToDensity(density, placeholder.item.height.value),
                    )
                })
            }
        } else if (start < end) {
            // Create TextSpan for normal text
            spans.add(TextSpan().apply {
                this.pagerId = this@applyAnnotatedString.pagerId
                text(annoText.text.substring(start, end))

                // Apply SpanStyle
                annoText.spanStyles
                    .filter { range -> !(end <= range.start || start >= range.end) }
                    .forEach { range -> applySpanStyle(range.item, density) }

                // Apply ParagraphStyle
                annoText.paragraphStyles
                    .filter { range -> !(end <= range.start || start >= range.end) }
                    .forEach { range ->
                        range.item.let { style ->
                            applyTextAlign(style.textAlign)
                            setProp(TextConst.LINE_HEIGHT, style.lineHeight.value)
                            applyTextIndent(style.textIndent)
                        }
                    }

                // Handle LinkAnnotation for current range
                val linkAnnotation = linkAnnotations
                    .firstOrNull { range -> !(end <= range.start || start >= range.end) }

                // Apply LinkAnnotation styles if found
                linkAnnotation?.let { range ->
                    val spanStyle = range.item.styles?.style ?: SpanStyle()
                    applySpanStyle(spanStyle, density)

                    // Add click event handler
                    click { _ ->
                        range.item.linkInteractionListener?.onClick(range.item)
                    }

                    // Call applyLinkStyle for future extensions
                    applyLinkStyle(range.item)
                }
            })
        }
    }

    if (spans.isEmpty()) {
        spans.add(TextSpan().apply {
            pagerId = this@applyAnnotatedString.pagerId
            text(annoText.text)
        })
    }

    this.spans(ArrayList(spans))
}

internal fun TextSpan.applyLinkStyle(link: LinkAnnotation) {
    // TODO: Support pressed state
}

// Helper method to apply SpanStyle
internal fun TextSpan.applySpanStyle(spanStyle: SpanStyle, density: Density) {
    // Apply font styles
    if (spanStyle.fontSize.isSpecified) {
        fontSize(scaleToDensity(density, spanStyle.fontSize.value))
    }
    applyFontWeight(spanStyle.fontWeight)
    applyFontStyle(spanStyle.fontStyle)
    applyShadow(spanStyle.shadow)

    applyStyleColor(spanStyle)
    if (spanStyle.brush is SolidColor) {
        color((spanStyle.brush as SolidColor).value.toKuiklyColor())
    } else if (spanStyle.brush is LinearGradient) {
        val linearGradient = spanStyle.brush as LinearGradient
        backgroundLinearGradient(
            linearGradient.direction,
            *linearGradient.resolveForText().colorStops.toTypedArray()
        )
    } else {
        if (spanStyle.color.isSpecified) {
            color(spanStyle.color.toKuiklyColor())
        }
    }

    // Apply text decoration
    spanStyle.textDecoration?.let { applyTextDecoration(it) }

    // Apply letter spacing
    if (spanStyle.letterSpacing.isSpecified) {
        letterSpacing(spanStyle.letterSpacing.value)
    }
}

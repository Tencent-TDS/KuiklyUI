/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.foundation.text.modifiers.TextStringRichElement
import com.tencent.kuikly.compose.material3.EmptyInlineContent
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.tokens.DefaultTextStyle
import com.tencent.kuikly.compose.resources.toKuiklyFontFamily
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.ColorProducer
import com.tencent.kuikly.compose.ui.graphics.LinearGradient
import com.tencent.kuikly.compose.ui.graphics.Shadow
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.isSpecified
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.layout.Placeable
import com.tencent.kuikly.compose.ui.materialize
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
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
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.core.views.ISpan
import com.tencent.kuikly.core.views.PlaceholderSpan
import com.tencent.kuikly.core.views.RichTextAttr
import com.tencent.kuikly.core.views.RichTextView
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.TextSpan
import kotlin.math.floor

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param color Overrides the text color provided in [style]
 */
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null
) {
    _BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color
    )
}

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's
 * used to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param color Overrides the text color provided in [style]
 */
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = EmptyInlineContent,
    color: ColorProducer? = null
) {
    _BasicText(
        annoText = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        color = color
    )
}

@Composable
private fun _BasicText(
    text: String? = null,
    annoText: AnnotatedString? = null,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = EmptyInlineContent,
    color: ColorProducer? = null
) {
    val inText = annoText ?: AnnotatedString(text ?: "")
    val hasInlineContent = inlineContent.isNotEmpty()

    if (hasInlineContent) {
        LayoutWithLinksAndInlineContent(
            modifier = modifier,
            text = inText,
            onTextLayout = onTextLayout,
            inlineContent = inlineContent,
            style = style,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            color = color
        )
    } else {
        BasicTextWithNoInlinContent(
            annoText,
            text,
            style,
            overflow,
            softWrap,
            maxLines,
            onTextLayout,
            inlineContent,
            modifier,
            color
        )
    }
}

@Composable
private fun LayoutWithLinksAndInlineContent(
    modifier: Modifier,
    text: AnnotatedString,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    inlineContent: Map<String, InlineTextContent>,
    style: TextStyle,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    color: ColorProducer?
) {
    // 记录 placeholder 的位置信息
    val measuredPlaceholderPositions = remember<MutableState<List<Rect?>?>> { 
        mutableStateOf(null) 
    }

    // 获取 placeholder 信息
    val (placeholders, inlineComposables) = text.resolveInlineContent(inlineContent)

    Layout(
        content = {
            // 先渲染基础文本
            BasicTextWithNoInlinContent(
                annoText = text,
                text = null,
                style = style,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                onTextLayout = { result ->
                    // 获取 placeholder 的位置信息
                    measuredPlaceholderPositions.value = result.placeholderRects
                    onTextLayout?.invoke(result)
                },
                inlineContent = inlineContent,
                modifier = Modifier,  // 这里不传入外部 modifier，因为我们要在外层 Layout 中处理
                color = color
            )

            if (measuredPlaceholderPositions.value != null) {
                // 渲染 inline content
                InlineChildren(text = text, inlineContents = inlineComposables)
            }
        },
        modifier = modifier,
        measurePolicy = TextWithInlineContentPolicy(
            placements = { measuredPlaceholderPositions.value }
        )
    )
}

/** Measure policy for inline content and links */
private class TextWithInlineContentPolicy(
    private val placements: () -> List<Rect?>?
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        // 测量文本（第一个 measurable）
        val textPlaceable = measurables.first().measure(constraints)
        
        // 获取 inline content 的测量对象
        val inlineContentMeasurables = measurables.drop(1)
        
        // 根据文本布局结果获取 placeholder 的位置
        val placeholderRects = placements()
        
        // 测量 inline content
        val inlineContentPlaceables = placeholderRects?.mapIndexedNotNull { index, rect ->
            rect?.let { r ->
                val measurable = inlineContentMeasurables.getOrNull(index) ?: return@mapIndexedNotNull null
                Pair(
                    measurable.measure(
                        Constraints(
                            maxWidth = floor(r.width).toInt(),
                            maxHeight = floor(r.height).toInt()
                        )
                    ),
                    IntOffset(r.left.fastRoundToInt(), r.top.fastRoundToInt())
                )
            }
        }

        return layout(textPlaceable.width, textPlaceable.height) {
            // 放置文本
            textPlaceable.place(0, 0)
            
            // 放置 inline content
            inlineContentPlaceables?.forEach { (placeable, position) ->
                placeable.place(position)
            }
        }
    }
}

@Composable
private fun BasicTextWithNoInlinContent(
    annoText: AnnotatedString?,
    text: String?,
    style: TextStyle,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    inlineContent: Map<String, InlineTextContent>,
    modifier: Modifier,
    color: ColorProducer?
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    val materializedModifier = currentComposer.materialize(modifier)

    val measurePolicy = EmptyMeasurePolicy

    val inText = annoText ?: AnnotatedString(text ?: "")

    val textElement = TextStringRichElement(
        text = inText,
        style = style,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        color = null,
        inlineContent = inlineContent
    )

    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val textView = RichTextView()
            KNode(textView) {
                getViewAttr().run {

                }
            }
        },
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
            set(modifier) {
                this.modifier = materializedModifier then textElement
            }

            set(text) {
                if (text == null) return@set
                withTextView {
                    text(text)
                }
                this.modifier = materializedModifier then textElement
            }

            set(annoText) {
                if (annoText == null) return@set
                withTextView {
                    applyAnnotatedString(annoText, inlineContent)
                }
                this.modifier = materializedModifier then textElement
            }

            // 添加对 inlineContent 的监听
            set(inlineContent) {
                if (annoText == null) return@set
                withTextView {
                    applyAnnotatedString(annoText, inlineContent)
                }
                this.modifier = materializedModifier then textElement
            }

            // 样式属性
            set(style) {
                withTextView {
                    applyTextStyle(style)
                }
                this.modifier = materializedModifier then textElement
            }

            set(color) {
                withTextView {
                    val overrideColorVal = color?.invoke()
                    overrideColorVal?.also {
                        applyColor(it)
                    }
                }
                this.modifier = materializedModifier then textElement
            }

            // 布局相关属性
            set(softWrap) {
                withTextView {
                    applySoftWrap(softWrap)
                }
                this.modifier = materializedModifier then textElement
            }

            set(overflow) {
                if (softWrap) {
                    withTextView {
                        applyOverflow(overflow)
                    }
                }
                this.modifier = materializedModifier then textElement
            }

            set(maxLines) {
                if (softWrap) {
                    withTextView {
                        applyMaxLines(maxLines)
                    }
                }
                this.modifier = materializedModifier then textElement
            }
        },
    )
}


// 扩展函数用于处理各种文本属性
private fun TextAttr.applyTextStyle(style: TextStyle) {
    // 字体相关
    if (style.fontSize.isSpecified) {
        fontSize(style.fontSize.value)
    }
    applyFontWeight(style.fontWeight)
    applyFontStyle(style.fontStyle)
    applyFontFamily(style.fontFamily)
    applyShadow(style.shadow)

    // 布局相关
    if (style.letterSpacing.isSpecified) {
        letterSpacing(style.letterSpacing.value)
    }
    if (style.lineHeight.isSpecified) {
        lineHeight(style.lineHeight.value)
    }

    applyTextAlign(style.textAlign)
    applyTextIndent(style.textIndent)

    if (style.brush is SolidColor) {
        color((style.brush as SolidColor).value.toKuiklyColor())
    } else if (style.brush is LinearGradient) {
        val linearGradient = style.brush as LinearGradient
        backgroundLinearGradient(
            linearGradient.direction,
            *linearGradient.colorStops.toTypedArray()
        )
    } else {
        if (style.color.isSpecified) {
            // 装饰相关
            color(style.color.toKuiklyColor())
        }
    }

    if (style.background.isSpecified) {
        backgroundColor(style.background.toKuiklyColor())
    }
    style.textDecoration?.let { applyTextDecoration(it) }
}

private fun TextAttr.applyColor(color: Color) {
    color(color.toKuiklyColor())
}

private fun TextAttr.applyFontStyle(fontStyle: FontStyle?) {
    if (fontStyle == FontStyle.Italic) {
        fontStyleItalic()
    } else {
        fontStyleNormal()
    }
}

private fun TextAttr.applyFontWeight(fontWeight: FontWeight?) {
    when (fontWeight) {
        FontWeight.W100 -> fontWeightLight()
        FontWeight.W200 -> fontWeightExtraLight()
        FontWeight.W300 -> fontWeightLight()
        FontWeight.W400 -> fontWeightNormal()
        FontWeight.W500 -> fontWeightMedium()
        FontWeight.W600 -> fontWeightSemiBold()
        FontWeight.W700 -> fontWeightBold()
        FontWeight.W800 -> fontWeightBold()
        FontWeight.W900 -> fontWeightBold()
        null -> fontWeightNormal()
    }
}

private fun TextAttr.applyFontFamily(fontFamily: FontFamily?) {
    when (fontFamily) {
        is GenericFontFamily -> fontFamily(fontFamily.name)
        is FontListFontFamily -> fontFamily(fontFamily.fonts.toKuiklyFontFamily())
        else -> if (this.getProp(TextConst.FONT_FAMILY) != null) {
            fontFamily("")
        }
    }
}

private fun TextAttr.applyShadow(shadow: Shadow?) {
    if (shadow == null) {
        // 当shadow为空时，重置阴影设置
        textShadow(0f, 0f, 0f, Color.Transparent.toKuiklyColor())
    } else if (shadow.color != Color.Unspecified || shadow.offset != Offset.Zero || shadow.blurRadius > 0) {
        textShadow(
            offsetX = shadow.offset.x,
            offsetY = shadow.offset.y,
            radius = shadow.blurRadius,
            color = shadow.color.toKuiklyColor()
        )
    }
}

private fun TextAttr.applyTextAlign(textAlign: TextAlign?) {
    when (textAlign) {
        TextAlign.Left -> textAlignLeft()
        TextAlign.Center -> textAlignCenter()
        TextAlign.Right -> textAlignRight()
        null -> textAlignLeft()
        else -> textAlignLeft()
    }
}

private fun TextAttr.applyTextIndent(textIndent: TextIndent?) {
    if (textIndent != null && textIndent.firstLine.isSpecified) {
        firstLineHeadIndent(textIndent.firstLine.value)
    } else {
        // 当 textIndent 为 null 或 firstLine 为 Unspecified 时，重置为 0
        firstLineHeadIndent(0f)
    }
}

private fun TextAttr.applyTextDecoration(decoration: TextDecoration?) {
    when (decoration) {
        TextDecoration.Underline -> textDecorationUnderLine()
        TextDecoration.LineThrough -> textDecorationLineThrough()
        null -> {} // 不设置装饰
        else -> {} // 其他装饰暂不支持
    }
}

private fun TextAttr.applySoftWrap(softWrap: Boolean) {
    if (softWrap) {
        textOverFlowWordWrapping()
    } else {
        textOverFlowClip()
        lines(1)
    }
}

private fun TextAttr.applyOverflow(overflow: TextOverflow) {
    when (overflow) {
        TextOverflow.Clip -> textOverFlowClip()
        TextOverflow.Ellipsis -> textOverFlowTail()
        else -> textOverFlowClip()
    }
}

private fun TextAttr.applyMaxLines(maxLines: Int) {
    lines(maxLines)
}

private object EmptyMeasurePolicy : MeasurePolicy {
    private val placementBlock: Placeable.PlacementScope.() -> Unit = {}
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight, placementBlock = placementBlock)
    }
}

/**
 * CompositionLocal containing the preferred [TextStyle] that will be used by [Text] components by
 * default. To set the value for this CompositionLocal, see [ProvideTextStyle] which will merge any
 * missing [TextStyle] properties with the existing [TextStyle] set in this CompositionLocal.
 *
 * @see ProvideTextStyle
 */
val LocalTextStyle = compositionLocalOf(structuralEqualityPolicy()) { DefaultTextStyle }

// TODO(b/156598010): remove this and replace with fold definition on the backing CompositionLocal
/**
 * This function is used to set the current value of [LocalTextStyle], merging the given style
 * with the current style values for any missing attributes. Any [Text] components included in
 * this component's [content] will be styled with this style unless styled explicitly.
 *
 * @see LocalTextStyle
 */
@Composable
fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit) {
    val mergedStyle = LocalTextStyle.current.merge(value)
    CompositionLocalProvider(LocalTextStyle provides mergedStyle, content = content)
}

private inline fun ComposeUiNode.withTextView(action: RichTextAttr.() -> Unit) {
    (this as? KNode<*>)?.run {
        (view as? RichTextView)?.run {
            getViewAttr().run(action)
        }
    }
}

private fun RichTextAttr.applyAnnotatedString(
    annoText: AnnotatedString,
    inlineContent: Map<String, InlineTextContent> = EmptyInlineContent
) {
    val spans = arrayListOf<ISpan>()
    
    // 收集所有的样式变化点
    val positions = mutableSetOf<Int>()
    positions.add(0)
    positions.add(annoText.text.length)
    
    // 收集所有 SpanStyle 的位置
    annoText.spanStyles.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }
    
    // 收集所有 ParagraphStyle 的位置
    annoText.paragraphStyles.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    // 获取所有 LinkAnnotation 并添加位置
    val linkAnnotations = annoText.getLinkAnnotations(0, annoText.length)
    linkAnnotations.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    // 获取 placeholder 信息和位置
    val (placeholders, _) = if (annoText.hasInlineContent()) {
        annoText.resolveInlineContent(inlineContent)
    } else {
        Pair(null, null)
    }

    // 添加 placeholder 的位置
    placeholders?.forEach { range ->
        positions.add(range.start)
        positions.add(range.end)
    }

    val sortedPositions = positions.sorted()

    // 按照位置点分段处理
    for (i in 0 until sortedPositions.size - 1) {
        val start = sortedPositions[i]
        val end = sortedPositions[i + 1]

        // 检查这个范围是否是 placeholder
        val isPlaceholder = placeholders?.any {
            it.start == start && it.end == end
        } ?: false

        if (isPlaceholder) {
            // 是 placeholder，创建 PlaceholderSpan
            placeholders?.find { it.start == start }?.let { placeholder ->
                spans.add(PlaceholderSpan().apply {
                    placeholderSize(placeholder.item.width.value, placeholder.item.height.value)
                })
            }
        } else if (start < end) {
            // 是普通文本，创建 TextSpan
            spans.add(TextSpan().apply {
                this.pagerId = this@applyAnnotatedString.pagerId
                text(annoText.text.substring(start, end))

                // 应用适用的 SpanStyle
                annoText.spanStyles
                    .filter { range -> !(end <= range.start || start >= range.end) }
                    .forEach { range -> applySpanStyle(range.item) }

                // 应用适用的 ParagraphStyle
                annoText.paragraphStyles
                    .filter { range -> !(end <= range.start || start >= range.end) }
                    .forEach { range ->
                        range.item.let { style ->
                            applyTextAlign(style.textAlign)
                            lineHeight(style.lineHeight.value)
                            applyTextIndent(style.textIndent)
                        }
                    }

                // 处理适用于当前范围的 LinkAnnotation
                val linkAnnotation = linkAnnotations
                    .firstOrNull { range -> !(end <= range.start || start >= range.end) }

                // 如果找到适用的 LinkAnnotation，应用其样式
                linkAnnotation?.let { range ->
                    val spanStyle = range.item.styles?.style ?: SpanStyle()
                    applySpanStyle(spanStyle)

                    // 添加点击事件处理
                    click { _ ->
                        range.item.linkInteractionListener?.onClick(range.item)
                    }

                    // 调用 applyLinkStyle 以支持将来的扩展
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

    spans(spans)
}

private fun TextSpan.applyLinkStyle(link: LinkAnnotation) {
    // TODO: 支持按压态等
}

// 添加一个辅助方法来应用 SpanStyle
private fun TextSpan.applySpanStyle(spanStyle: SpanStyle) {
    // 应用字体样式
    if (spanStyle.fontSize.isSpecified) {
        fontSize(spanStyle.fontSize.value)
    }
    applyFontWeight(spanStyle.fontWeight)
    applyFontStyle(spanStyle.fontStyle)
    applyShadow(spanStyle.shadow)


    if (spanStyle.brush is SolidColor) {
        color((spanStyle.brush as SolidColor).value.toKuiklyColor())
    } else if (spanStyle.brush is LinearGradient) {
        val linearGradient = spanStyle.brush as LinearGradient
        backgroundLinearGradient(
            linearGradient.direction,
            *linearGradient.colorStops.toTypedArray()
        )
    } else {
        if (spanStyle.color.isSpecified) {
            // 装饰相关
            color(spanStyle.color.toKuiklyColor())
        }
    }

    // 应用背景色
    if (spanStyle.background.isSpecified) {
        backgroundColor(spanStyle.background.toKuiklyColor())
    }

    // 应用文本装饰
    spanStyle.textDecoration?.let { applyTextDecoration(it) }
    
    // 应用字母间距
    if (spanStyle.letterSpacing.isSpecified) {
        letterSpacing(spanStyle.letterSpacing.value)
    }
}

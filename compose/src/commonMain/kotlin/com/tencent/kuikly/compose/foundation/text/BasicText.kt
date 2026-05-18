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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.foundation.text.modifiers.TextStringRichElement
import com.tencent.kuikly.compose.material3.EmptyInlineContent
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
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
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
import com.tencent.kuikly.compose.ui.text.resolveDefaults
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.style.TextIndent
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.isSpecified
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.compose.extension.scaleToDensity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
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
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val fontSizeScale = configuration.fontSizeScale
    val fontWeightScale = configuration.fontWeightScale

    val measurePolicy = EmptyMeasurePolicy

    val inText = annoText ?: AnnotatedString(text ?: "")
    val finalStyle = resolveDefaults(style, layoutDirection)

    val textElement = TextStringRichElement(
        text = text,
        annotatedText = annoText,
        style = finalStyle,
        density = density,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        inlineContent = inlineContent,
        fontSizeScale = fontSizeScale,
        fontWeightScale = fontWeightScale
    )

    val materializedModifier = currentComposer.materialize(modifier then textElement)

    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val textView = RichTextView()
            KNode(textView) {
                getViewAttr().run {
                    didSetTextGradient = true
                }
            }
        },
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
            set(materializedModifier, ComposeUiNode.SetModifier)
        },
    )
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

@Deprecated(
    "Use com.tencent.kuikly.compose.material3.LocalTextStyle instead",
    ReplaceWith("com.tencent.kuikly.compose.material3.LocalTextStyle")
)
val LocalTextStyle get() = com.tencent.kuikly.compose.material3.LocalTextStyle

@Deprecated(
    "Use com.tencent.kuikly.compose.material3.ProvideTextStyle instead",
    ReplaceWith("com.tencent.kuikly.compose.material3.ProvideTextStyle(value, content)")
)
@Composable
fun ProvideTextStyle(value: TextStyle, content: @Composable () -> Unit) =
    com.tencent.kuikly.compose.material3.ProvideTextStyle(value, content)
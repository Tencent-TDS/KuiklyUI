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
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.ColorProducer
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasurable
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasureScope
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.LayoutModifierNode
import com.tencent.kuikly.compose.ui.node.invalidateMeasurement
import com.tencent.kuikly.compose.ui.node.requireDensity
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.text.MultiParagraph
import com.tencent.kuikly.compose.ui.text.TextLayoutInput
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Constraints.Companion.fitPrioritizingWidth
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.constrain
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.views.PlaceholderSpan
import com.tencent.kuikly.core.views.RichTextView
import kotlin.math.ceil

/**
 * Node that implements Text for [String].
 *
 * It has reduced functionality, and as a result gains in performance.
 *
 * Note that this Node never calculates [TextLayoutResult] unless needed by semantics.
 */
internal class TextStringRichNode(
    private var text: AnnotatedString,
    private var style: TextStyle,
//    private var fontFamilyResolver: FontFamily.Resolver,
    private var onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    private var overflow: TextOverflow = TextOverflow.Clip,
    private var softWrap: Boolean = true,
    private var maxLines: Int = Int.MAX_VALUE,
    private var minLines: Int = DefaultMinLines,
    private var overrideColor: ColorProducer? = null,
    private var inlineContent: Map<String, InlineTextContent> = EmptyInlineContent
) : Modifier.Node(), LayoutModifierNode {

    private var cacheResult: TextLayoutResult? = null

    /**
     * Element has text params to update
     */
    fun updateText(text: AnnotatedString): Boolean {
        if (this.text == text) return false
        this.text = text
        return true
    }

    /**
     * Element has layout related params to update
     */
    fun updateLayoutRelatedArgs(
        style: TextStyle,
        minLines: Int,
        maxLines: Int,
        softWrap: Boolean,
//        fontFamilyResolver: FontFamily.Resolver,
        overflow: TextOverflow,
        inlineContent: Map<String, InlineTextContent>,
    ): Boolean {
        var changed: Boolean

        changed = !this.style.hasSameLayoutAffectingAttributes(style)
        this.style = style

        if (this.minLines != minLines) {
            this.minLines = minLines
            changed = true
        }

        if (this.maxLines != maxLines) {
            this.maxLines = maxLines
            changed = true
        }

        if (this.softWrap != softWrap) {
            this.softWrap = softWrap
            changed = true
        }

        if (this.inlineContent != inlineContent) {
            this.inlineContent = inlineContent
            changed = true
        }

//        if (this.fontFamilyResolver != fontFamilyResolver) {
//            this.fontFamilyResolver = fontFamilyResolver
//            changed = true
//        }

        if (this.overflow != overflow) {
            this.overflow = overflow
            changed = true
        }

        return changed
    }

    /**
     * Element has callback parameters to update
     */
    fun updateCallbacks(
        onTextLayout: ((TextLayoutResult) -> Unit)?,
    ): Boolean {
        var changed = false

        if (this.onTextLayout != onTextLayout) {
            this.onTextLayout = onTextLayout
            changed = true
        }
        return changed
    }

    /**
     * request invalidate based on the results of [updateText] and [updateLayoutRelatedArgs]
     */
    fun doInvalidations(
//        drawChanged: Boolean,
        textChanged: Boolean,
        layoutChanged: Boolean,
        callbacksChanged: Boolean
    ) {
        if (!isAttached) {
            // no-up for !isAttached. The node will invalidate when attaching again.
            return
        }

        if (textChanged || layoutChanged || callbacksChanged) {
            invalidateMeasurement()
        }
    }

    override fun onReset() {
        super.onReset()
        // todo: jonas 清理状态
    }

    fun genTextLayoutResult(size: IntSize): TextLayoutResult {
        // 存储所有占位符的矩形区域
        val placeholderRects = mutableListOf<Rect>()

        val textView = (requireLayoutNode() as? KNode<RichTextView>)?.view
        // 遍历所有文本片段,处理占位符
        textView?.getViewAttr()?.getSpans()?.forEachIndexed { index, span ->
            if (span !is PlaceholderSpan) return@forEachIndexed
            
            // 获取占位符的位置和大小信息
            val rectStr = textView.shadow?.callMethod("spanRect", index.toString())
            if (rectStr.isNullOrEmpty()) return@forEachIndexed
            
            // 解析位置和大小信息
            val rectComponents = rectStr.split(" ")
            if (rectComponents.size < 4) return@forEachIndexed
            
            // 提取坐标和尺寸
            val (x, y, width, height) = rectComponents.take(4).map { 
                it.toFloatOrNull() ?: 0f 
            }
            
            // 更新占位符的frame并添加到矩形列表
            span.spanFrame = Frame(x, y, width, height)
            placeholderRects.add(
                Rect(
                    offset = Offset(x *  requireDensity().density, y *  requireDensity().density),
                    size = Size(width * requireDensity().density, height *  requireDensity().density)
                )
            )
        }

        return TextLayoutResult(
            TextLayoutInput(
                text,
                style,
//                placeholders.orEmpty(),
                maxLines,
                softWrap,
                overflow,
//                density!!,
//                layoutDirection,
//                fontFamilyResolver,
//                finalConstraints
            ),
            MultiParagraph(placeholderRects = placeholderRects),
            size
        )
    }

    private fun measureTextView(maxWidth: Int, maxHeight: Int): IntSize {
        val textView = (requireLayoutNode() as? KNode<RichTextView>)?.view

        textView?.apply {
            val value = buildValuesPropValue()
            shadow?.setValuesProp(value)
        }

        val size = textView?.shadow?.calculateRenderViewSize(
            maxWidth.toFloat() / requireDensity().density,
            maxHeight.toFloat() / requireDensity().density
        )
        textView?.updateShadow()

        var intSize = IntSize(1000, 1000)
        size?.also {
            intSize = IntSize(
                ceil(it.width * requireDensity().density).toInt(),
                ceil(it.height * requireDensity().density).toInt()
            )
        }
        return intSize
    }

    /**
     * Text layout happens here
     */
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {

        val intSize = measureTextView(constraints.maxWidth, constraints.maxHeight)
        val layoutSize = constraints.constrain(
            intSize
        )

        val result = genTextLayoutResult(layoutSize)

        // 布局变化了
        if (cacheResult !== result) {
            onTextLayout?.invoke(result)
            cacheResult = result
        }

        // then allow children to measure _inside_ our final box, with the above placeholders
        val placeable = measurable.measure(
            fitPrioritizingWidth(
                minWidth = layoutSize.width,
                maxWidth = layoutSize.width,
                minHeight = layoutSize.height,
                maxHeight = layoutSize.height
            )
        )

        return layout(
            layoutSize.width,
            layoutSize.height,
//            baselineCache!!
        ) {
            placeable.place(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val intSize = measureTextView(Constraints.Infinity, height)
        return intSize.width
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val intSize = measureTextView(width, Constraints.Infinity)
        return intSize.height
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        val intSize = measureTextView(Constraints.Infinity, height)
        return intSize.width
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        val intSize = measureTextView(width, Constraints.Infinity)
        return intSize.height
    }
}
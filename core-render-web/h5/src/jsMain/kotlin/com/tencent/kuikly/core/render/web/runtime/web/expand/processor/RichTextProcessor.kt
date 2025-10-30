package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.processor.FontSizeToLineHeightMap
import com.tencent.kuikly.core.render.web.processor.IRichTextProcessor
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.ktx.indexOfChild
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toNumberFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.ktx.width
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.get
import kotlin.math.max

/**
 * mini app text process object
 */
object RichTextProcessor : IRichTextProcessor {
    // canvas context instance
    private var canvasContext: CanvasRenderingContext2D? = null

    // Current default font
    private var defaultFontFamily: String = ""

    // Whether support fontBoundingBoxAscent, default believe support
    private var isSupportFontBoundingBox: Boolean = true

    // Rich text placeholder attribute setting
    private const val PLACEHOLDER_WIDTH = "placeholderWidth"
    private const val PLACEHOLDER_HEIGHT = "placeholderHeight"
    private const val COLOR = "color"
    private const val FONT_SIZE = "fontSize"
    private const val TEXT_DECORATION = "textDecoration"
    private const val FONT_WEIGHT = "fontWeight"
    private const val FONT_STYLE = "fontStyle"
    private const val FONT_FAMILY = "fontFamily"
    private const val LETTER_SPACING = "letterSpacing"
    private const val STROKE_WIDTH = "strokeWidth"
    private const val STROKE_COLOR = "strokeColor"
    private const val FONT_VARIANT = "fontVariant"
    private const val HEAD_INDENT = "headIndent"
    private const val LINE_HEIGHT = "lineHeight"

    private val measureElement: HTMLElement by lazy {
        kuiklyDocument.createElement(ElementType.P).unsafeCast<HTMLElement>().apply {
            // 初始化基础样式
            id = "measure_iframe"
            style.display = "inline-block"
            style.asDynamic().webkitLineClamp = ""
            style.asDynamic().webkitBoxOrient = ""
            style.whiteSpace = "pre-wrap"
            style.overflowY = "auto"
            style.top = "-5000px"
            style.position = "absolute"
        }
    }

    /**
     * Set multi-line text style
     */
    private fun setMultiLineStyle(lines: Int, ele: HTMLElement) {
        // If text is multi-line, need to set multi-line properties
        if (lines > 0) {
            ele.style.display = "-webkit-box"
            ele.style.asDynamic().webkitLineClamp = lines.toString()
            ele.style.asDynamic().webkitBoxOrient = "vertical"
            // When participating in calculation, whiteSpace needs to be set to non-wrapping for
            // calculating actual width, after calculation,
            // if exceeds one line, then need to set to multi-line wrapping
            ele.style.whiteSpace = "pre-wrap"
            ele.style.overflowY = "hidden"
        } else {
            // Clear multi-line style
            ele.style.display = "inline-block"
            ele.style.asDynamic().webkitLineClamp = ""
            ele.style.asDynamic().webkitBoxOrient = ""
            ele.style.whiteSpace = "pre-wrap"
            ele.style.overflowY = "auto"
        }
    }

    /**
     * Insert method for text node, because kuikly text node needs to be measured on host side
     */
    private fun insertChild(parent: Element, child: Element, index: Int) {
        if (parent.childElementCount == 0 || index >= parent.childElementCount) {
            // No child node or index out of range, append to the end
            parent.appendChild(child)
        } else {
            // Otherwise find actual index position, first find dom node to be inserted
            val beforeChild = parent.children[index]
            // Then find WebElement node index corresponding to dom node
            val insertIndex = indexOfChild(beforeChild)
            if (insertIndex >= 0) {
                // Found position then insert to specified position
                parent.insertBefore(child, parent.childNodes[insertIndex])
            } else {
                parent.appendChild(child)
            }
        }
    }

    /**
     * Calculate the actual space size occupied by the element using DOM method
     *
     * @param constraintSize Constraint size
     */
    private fun calculateRenderViewSizeByDom(constraintSize: SizeF, view: KRRichTextView, renderText: String): SizeF {
        val ele = view.ele
        val originParent = ele.parentElement
        val index = indexOfChild(ele)
        var newEle = measureElement
        if(view.isRichTextValues()) {
            newEle = ele
        } else {
            // Copy all styles at once using cssText for better performance
            newEle.style.cssText = ele.style.cssText
            // Set content after style copying to avoid potential style interference
            newEle.innerText = renderText.ifEmpty { ele.innerText }
        }

        // Remove width
        newEle.style.width = ""
        // Remove specified height
        newEle.style.height = ""
        if (constraintSize.width > 0) {
            // If constraint size exists, use the constraint size
            newEle.style.maxWidth = constraintSize.width.toPxF()
        }
        // No truncation or ellipsis when calculating actual size
        newEle.style.whiteSpace = "pre-wrap"
        // If lines are set, also need to limit maximum number of lines
        if (view.numberOfLines > 0) {
            setMultiLineStyle(view.numberOfLines, newEle)
            setMultiLineStyle(view.numberOfLines, ele)
        }
        // Insert the node into the page to complete rendering, used to get the actual size of the node
        kuiklyDocument.body?.appendChild(newEle)
        // Element width
        var w = newEle.offsetWidth
        // Element height
        var h = newEle.offsetHeight.toFloat()
        // Special case handling, if line height is set but the actual height is much smaller than
        // the expected value, need to remove multi-line style
        if (view.numberOfLines > 0) {
            // Single line height, if lineHeight is specified, use the specified one
            val singleLineHeight = if (newEle.style.lineHeight != "") {
                newEle.style.lineHeight.pxToFloat()
            } else {
                FontSizeToLineHeightMap.getLineHeight(newEle.style.fontSize.pxToFloat())
            }
            // Expected height
            val expectHeight = singleLineHeight * view.numberOfLines
            // If expected height minus actual height, difference is greater than half of single
            // line height, then consider the line count setting is wrong, and there aren't actually
            // that many lines
            if (expectHeight - h > singleLineHeight / 2) {
                // Need to remove multi-line style
                setMultiLineStyle(0, ele)
                setMultiLineStyle(0, newEle)
                // And get height and width again
                w = newEle.offsetWidth
                h = newEle.offsetHeight.toFloat()
            }
        }

        // After getting the size, remove the node from the page
        kuiklyDocument.body?.removeChild(newEle)
        Log.trace("calculate size by dom, size: ", w, h)
        // Actual width
        val realWidth = if (w < constraintSize.width) w + 0.5f else constraintSize.width
        // Actual height
        val realHeight = h

        if (index != -1 && originParent != null) {
            // After recalculating element size, the old element has been removed, if the node
            // itself was already inserted into the page, need to reinsert it into the original
            // parent node
            insertChild(originParent, newEle, index)
        }
        Log.trace("real size by dom, size:", realWidth, realHeight)
        return SizeF(realWidth, realHeight)
    }

    /**
     * Get canvas context
     */
    private fun getCanvasContext(): CanvasRenderingContext2D? {
        // Initialize canvas context
        if (canvasContext == null) {
            val canvas = kuiklyDocument.createElement(ElementType.CANVAS) as HTMLCanvasElement
            // Canvas context
            canvasContext = canvas.getContext("2d") as CanvasRenderingContext2D
        }

        return canvasContext
    }

    /**
     * Get default font family
     */
    private fun getDefaultFontFamily(): String {
        // Initialize default font
        if (defaultFontFamily == "") {
            // Record default font
            defaultFontFamily =
                kuiklyDocument.documentElement?.let { kuiklyWindow.getComputedStyle(it).fontFamily } ?: ""
        }

        return defaultFontFamily
    }

    private fun isSupportLetterSpacing(view: KRRichTextView): Boolean {
        return view.ele.style.letterSpacing.isNotEmpty() || view.ele.style.letterSpacing == "0px"
    }

    /**
     * Calculate the actual space size occupied by the element using Canvas method
     *
     * @param constraintSize Constraint size
     */
    private fun calculateRenderViewSizeByCanvas(
        constraintSize: SizeF,
        view: KRRichTextView,
        renderText: String
    ): SizeF {
        if (!isSupportFontBoundingBox || isSupportLetterSpacing(view)) {
            // If this property is not supported, use the DOM method
            return calculateRenderViewSizeByDom(constraintSize, view, renderText)
        }

        val ele = view.ele
        val style = view.ele.style

        // No truncation or ellipsis when calculating actual size
        style.whiteSpace = "pre-wrap"
        // canvas context
        val canvasCtx = getCanvasContext()
        // Default font family
        val defaultFont = getDefaultFontFamily()

        // Set font weight, font size, font family
        val fontWeight = ele.style.fontWeight
        val fontSize = ele.style.fontSize
        var fontFamily = ele.style.fontFamily
        val lineHeight = ele.style.lineHeight
        if (fontFamily == "") {
            fontFamily = defaultFont
        }
        // Canvas font style to set
        val font = "$fontWeight $fontSize $fontFamily"
        // Set canvas font style
        canvasCtx?.font = font
        // Get all text line list based on line breaks
        val textArray = renderText.asDynamic().split("\n").unsafeCast<JsArray<String>>()
        // Maximum width of a single line
        var maxWidth = 0f
        // Total number of lines
        var lines = 0f
        // Total height
        var totalHeight = 0f
        // Measure width of each line
        textArray.forEach { line ->
            // Measure the actual content of the text node
            val metrics = canvasCtx?.measureText(line)
            // If browser doesn't support fontBoundingBox property, height calculation is inaccurate.
            // Use DOM calculation instead
            if (metrics == null) {
                isSupportFontBoundingBox = false
                return@forEach
            }
            if (jsTypeOf(metrics.fontBoundingBoxAscent) != "number") {
                // Does not support new font canvas properties, use DOM calculation instead
                isSupportFontBoundingBox = false
                return@forEach
            }
            // Total lines plus 1
            lines += 1
            // Update maximum width
            maxWidth = max(maxWidth, metrics.width.toFloat())
            // Calculate total height
            totalHeight += (metrics.fontBoundingBoxAscent + metrics.fontBoundingBoxDescent).toFloat()
        }

        if (!isSupportFontBoundingBox) {
            // If new properties are not supported, use DOM calculation
            return calculateRenderViewSizeByDom(constraintSize, view, renderText)
        }

        // If new properties are supported, use canvas measurement values for calculation
        // Remove width
        style.width = ""
        // Remove height
        style.height = ""
        if (constraintSize.width > 0) {
            // If constraint size exists, use the constraint size
            style.maxWidth = constraintSize.width.toPxF()
        }

        Log.trace(
            "canvas measure size: ",
            ele.innerText,
            maxWidth,
            totalHeight,
            constraintSize.width
        )
        // Real width
        val realWidth: Float
        // Real height
        val realHeight: Float
        // Determine whether element width exceeds constraint width, if exceeds, use constraint width as actual width
        if (maxWidth < constraintSize.width || constraintSize.width == 0f) {
            // Not exceed constraint width
            realWidth = maxWidth + 0.5f
            realHeight = if (lineHeight != "") {
                // If lineHeight is set, use lineHeight as line height
                lineHeight.pxToFloat() * lines
            } else {
                // If not specified, use canvas calculated height
                totalHeight
            }
            Log.trace("canvas real size: ", realWidth, realHeight)
            // Return size
            return SizeF(realWidth, realHeight)
        } else {
            // Exceed constraint width, because text wrapping may have extra space occupied, unable
            // to accurately calculate, so use DOM calculation
            return calculateRenderViewSizeByDom(constraintSize, view, renderText)
        }
    }

    /**
     * Create internal span
     */
    private fun createSpan(value: JSONObject, view: KRRichTextView): HTMLSpanElement {
        val span = kuiklyDocument.createElement(ElementType.SPAN).unsafeCast<HTMLSpanElement>()
        val text = view.getText(value) ?: return span
        // span content using innerText set, prevent xss
        span.innerText = text
        val style = span.style
        val color = value.optString(COLOR, "")
        if (color.isNotEmpty()) {
            style.color = color.toRgbColor()
        }
        val fontSize = value.optDouble(FONT_SIZE, 0.0)
        if (fontSize != 0.0) {
            style.fontSize = fontSize.toPxF()
        }
        val fontFamily = value.optString(FONT_FAMILY)
        if (fontFamily.isNotEmpty()) {
            style.fontFamily = fontFamily
        }
        val fontWeight = value.optString(FONT_WEIGHT)
        if (fontWeight.isNotEmpty()) {
            style.fontWeight = fontWeight
        }
        val fontStyle = value.optString(FONT_STYLE)
        if (fontStyle.isNotEmpty()) {
            style.fontStyle = fontStyle
        }
        val fontVariant = value.optString(FONT_VARIANT)
        if (fontVariant.isNotEmpty()) {
            style.fontVariant = fontVariant
        }
        val strokeColor = value.optString(STROKE_COLOR).toRgbColor()
        val strokeWidth = value.optDouble(STROKE_WIDTH, 0.0)
        if (strokeWidth != 0.0) {
            val usedStrokeWidth = strokeWidth / 4
            style.asDynamic().webkitTextStroke = "${usedStrokeWidth}px $strokeColor"
        }
        val lineSpacing = value.optDouble(LETTER_SPACING, -1.0)
        if (lineSpacing != -1.0) {
            // Set lineHeight based on line spacing
            style.lineHeight = lineSpacing.toNumberFloat().toString()
        }
        val lineHeight = value.optDouble(LINE_HEIGHT, -1.0)
        if (lineHeight != -1.0) {
            // Set lineHeight based on line height
            style.lineHeight = lineHeight.toNumberFloat().toPxF()
        }

        val textShadow = value.optString(KRCssConst.TEXT_SHADOW)
        if (textShadow.isNotEmpty()) {
            val textShadowSpilt = textShadow.asDynamic().split(" ")
            val offsetX = "${textShadowSpilt[0]}px"
            val offsetY = "${textShadowSpilt[1]}px"
            val radius = "${textShadowSpilt[2]}px"
            val shadowColor = textShadowSpilt[3].unsafeCast<String>().toRgbColor()
            style.textShadow = "$offsetX $offsetY $radius $shadowColor"
        }

        val textDecoration = value.optString(TEXT_DECORATION)
        if (textDecoration.isNotEmpty()) {
            style.textDecoration = textDecoration
        }
        val textIndent = value.optDouble(HEAD_INDENT, 0.0)
        if (textIndent != 0.0) {
            style.textIndent = textIndent.toPxF()
        }
        // Placeholder span width
        val placeHolderWidth = value.optDouble(PLACEHOLDER_WIDTH, 0.0)
        // Placeholder span height
        val placeHolderHeight = value.optDouble(PLACEHOLDER_HEIGHT, 0.0)
        // If placeholder span has width and height, set them
        if (placeHolderWidth != 0.0 && placeHolderHeight != 0.0) {
            style.width = placeHolderWidth.toPxF()
            style.height = placeHolderHeight.toPxF()
            // This type of span is set to inline-block type
            style.display = "inline-block"
            // Vertical alignment center
            style.verticalAlign = "middle"
        }
        return span
    }

    /**
     * measure real text size
     */
    override fun measureTextSize(constraintSize: SizeF, view: KRRichTextView, renderText: String): SizeF {
        // Calculate text width and height by canvas, width can be measured by measureText, remember
        // to get fontFamily, height through fontAscent plus fontDecent, if lineHeight is set, use
        // lineHeight as line height. Remember to handle line breaks, if there are line breaks,
        // need to calculate width in segments, and consider height after line breaks. If there are
        // multiple child nodes, also need to calculate width in segments here, this will be
        // optimized later todo
        return if (view.ele.children.length > 0) {
            // There are child nodes, need to loop calculation, temporarily use Dom method for calculation
            calculateRenderViewSizeByDom(constraintSize, view, renderText)
        } else {
            // No child nodes, single line calculation
            calculateRenderViewSizeByCanvas(constraintSize, view, renderText)
        }
    }

    /**
     * create rich text spans
     */
    override fun setRichTextValues(richTextValues: JSONArray, view: KRRichTextView) {
        for (i in 0 until richTextValues.length()) {
            view.ele.appendChild(createSpan(richTextValues.optJSONObject(i) ?: JSONObject(), view))
        }
    }
}
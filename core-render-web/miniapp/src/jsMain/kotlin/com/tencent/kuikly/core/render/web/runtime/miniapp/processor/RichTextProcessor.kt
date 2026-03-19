package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.expand.components.RichTextSpan
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.const.KRCssConst.TEXT_SHADOW
import com.tencent.kuikly.core.render.web.ktx.height
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toNumberFloat
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.ktx.width
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.processor.IRichTextProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal.isIOS
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniImageElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement
import com.tencent.kuikly.core.render.web.utils.Log
import kotlinx.dom.clear
import org.w3c.dom.HTMLElement
import kotlin.js.json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

/**
 * mini app text process object
 */
object RichTextProcessor : IRichTextProcessor {
    // Fixed android width ratio magic value, temporarily set to 1.05,
    // because Android real machine canvas measurement width result is smaller
    private const val WIDTH_RATIO_MAGIC = 1.05f

    // Default font weight
    private const val FONT_WEIGHT_DEFAULT = 400

    // Prevent additional width added due to special situation during text rendering
    private const val TEXT_WIDTH_DEFAULT = 0.5f
    // mini app measure text canvas context
    private var measureTextCtx: dynamic = null

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

    // Set placeholder image delay time
    private const val IMAGE_SPAN_DELAY = 100

    /**
     * Set webkit multi-line text style
     */
    private fun setWebkitMultiLineStyle(lines: Int = 0, ele: HTMLElement)  {
        ele.style.display = "-webkit-box"
        ele.style.whiteSpace = "pre-wrap"
        ele.style.asDynamic().webkitBoxOrient = "vertical"
        ele.style.asDynamic().webkitLineClamp = lines.toString()
    }

    /**
     * Clear webkit multi-line text style
     */
    private fun clearWebkitMultiLineStyle(ele: HTMLElement) {
        ele.style.display = ""
        ele.style.whiteSpace = "pre-wrap"
        ele.style.asDynamic().webkitLineClamp = ""
        ele.style.asDynamic().webkitBoxOrient = ""
    }

    /**
     * Set multi-line text style
     */
    private fun setMultiLineStyle(lines: Int = 0, view: KRRichTextView) {
        if (view.isRichText && view.richTextSpanList.length > 0) {
            // Rich text style string setting
            if (lines > 1) {
                // If greater than one line, then process, otherwise, empty
                setWebkitMultiLineStyle(lines, view.ele)
            } else {
                // Clear multi-line style
                clearWebkitMultiLineStyle(view.ele)
            }
        } else {
            // Plain text, directly set multi-line style properties
            if (lines > 0) {
                setWebkitMultiLineStyle(lines, view.ele)
            } else {
                // Clear multi-line style
                clearWebkitMultiLineStyle(view.ele)
            }
        }
    }

    /**
     * get global single offscreen canvas context
     */
    private fun getCanvasContext(): dynamic {
        if (measureTextCtx == null) {
            measureTextCtx =
                NativeApi.plat.createOffscreenCanvas(json("type" to "2d")).getContext("2d")
        }
        return measureTextCtx
    }

    /**
     * Get default font family
     */
    private fun getDefaultFontFamily(): String {
        // iOS: 'San Francisco', 'PingFang SC', sans-serif
        // San Francisco for English, PingFang SC for Chinese.
        // Android: Roboto, 'Noto Sans CJK SC', sans-serif
        // Roboto for English, Noto Sans CJK SC for Chinese.
        // sans-serif: Fallback.
        if (MiniGlobal.isDevTools) {
            // Simulator forces the use of "sans-serif"
            return "sans-serif"
        }
        return if (isIOS)
            RenderConst.IOS_TEXT_FONT
        else
            RenderConst.ANDROID_TEXT_FONT
    }

    /**
     * measure given text width by canvas
     */
    private fun measureTextWidth(
        text: String,
        fontSize: dynamic,
        fontWeight: Int,
        fontFamily: String,
        fontStyle: String = ""
    ): dynamic {
        val ctx = getCanvasContext()
        var usedFontFamily = fontFamily
        if (fontFamily.isEmpty()) {
            usedFontFamily = getDefaultFontFamily()
        }
        val fontStr = "$fontStyle $fontWeight ${fontSize}px $usedFontFamily"
        ctx.font = fontStr
        return ctx.measureText(text)
    }

    /**
     * Process the size data of the remaining lines
     *
     * @param constraintSize Parent container constraint size
     * @param linesSizeList Line data list
     */
    private fun processRemainingLine(
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        view: KRRichTextView
    ) {
        if (constraintSize.width > 0f) {
            // Total number of lines
            val totalLines = ceil(view.currentLineWidth / constraintSize.width).toInt()
            // Remaining width that is not full
            val remainWidth = view.currentLineWidth % constraintSize.width
            if (totalLines > 1) {
                // For parts greater than one line, directly insert full line size list
                repeat(totalLines - 1) {
                    linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
                }
            }
            // Last insert width that is not full line
            linesSizeList.add(SizeF(remainWidth, view.currentLineHeight))
        } else {
            // If there is no constraint, just use the remaining width and height directly
            linesSizeList.add(SizeF(view.currentLineWidth, view.currentLineHeight))
        }
    }

    /**
     * Data class to store placeholder position info during calculation
     */
    private data class PlaceholderLineInfo(
        val span: RichTextSpan,
        val lineIndex: Int,
        val offsetLeft: Float
    )
    
    /**
     * Process the size data of placeholder spans
     * First pass: record line index and offsetLeft, linesSizeList will be updated later
     *
     * @param childSpan Child span node
     * @param constraintSize Parent container constraint size
     * @param linesSizeList Line size list
     * @param placeholderInfoList List to store placeholder line info for second pass
     */
    private fun processPlaceHolderSpan(
        view: KRRichTextView,
        childSpan: RichTextSpan,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        placeholderInfoList: MutableList<PlaceholderLineInfo>? = null,
    ) {
        // Use placeholder height as current line height, need to consider
        // multiple placeholder situations, use the highest as height
        if (childSpan.height > view.currentLineHeight) {
            view.currentLineHeight = childSpan.height
        }
        // Current placeholder plus after width
        val sumWidth = view.currentLineWidth + childSpan.width
        if (sumWidth <= constraintSize.width || constraintSize.width == 0f) {
            // If placeholder span width plus does not exceed one line, or no constraint width,
            // then directly add current line Record offsetLeft, because the current text width
            // calculation has been multiplied by the ratio value, so the actual placeholder
            // offset needs to be divided by the offset ratio
            childSpan.offsetLeft = view.currentLineWidth / WIDTH_RATIO_MAGIC
            // Record line index for second pass (current line is linesSizeList.length)
            placeholderInfoList?.add(PlaceholderLineInfo(childSpan, linesSizeList.length, childSpan.offsetLeft))
            // Then just add width to current line
            view.currentLineWidth = sumWidth
        } else {
            // If it exceeds one line, it needs to be changed, because placeholder span cannot be
            // folded, so placeholder span needs to start from new line head Here, it needs to be
            // noted that because placeholder span will not exceed one line, so no need to consider
            // adding width that exceeds two lines after placeholder span
            // First insert need to be changed line size
            linesSizeList.add(SizeF(view.currentLineWidth, view.currentLineHeight))
            // Save new line width
            view.currentLineWidth = childSpan.width
            // Record offsetLeft
            childSpan.offsetLeft = 0f
            // Record line index for second pass (after adding new line, it's at linesSizeList.length)
            placeholderInfoList?.add(PlaceholderLineInfo(childSpan, linesSizeList.length, 0f))
        }
    }
    
    /**
     * Calculate offsetTop for all placeholders after line sizes are finalized
     * Similar to Android: lineMid - phHeight / 2
     */
    private fun calculatePlaceholderOffsetTop(
        linesSizeList: JsArray<SizeF>,
        placeholderInfoList: List<PlaceholderLineInfo>
    ) {
        placeholderInfoList.forEach { info ->
            // Calculate accumulated height of all lines before this line
            var lineTop = 0f
            for (i in 0 until info.lineIndex) {
                if (i < linesSizeList.length) {
                    lineTop += linesSizeList[i].height
                }
            }
            // Get the height of the line containing this placeholder
            val lineHeight = if (info.lineIndex < linesSizeList.length) {
                linesSizeList[info.lineIndex].height
            } else {
                info.span.height // fallback to placeholder height
            }
            // Calculate vertical center offset (like Android: lineMid - phHeight/2)
            val lineMid = lineTop + lineHeight / 2
            info.span.offsetTop = lineMid - info.span.height / 2
        }
    }

    /**
     * Get and calculate the size list of each line of text
     */
    private fun calculateLinesSize(constraintSize: SizeF, view: KRRichTextView): JsArray<SizeF> {
        val linesSizeList: JsArray<SizeF> = JsArray()
        val lineHeight = view.ele.style.lineHeight
        val realLineHeight = if (lineHeight.isNotEmpty()) lineHeight.pxToFloat() else 0f
        
        // List to store placeholder info for second pass calculation
        val placeholderInfoList = mutableListOf<PlaceholderLineInfo>()

        // First pass: Process child text nodes in a loop to calculate actual line size of each line
        view.richTextSpanList.forEach { childSpan ->
            if (childSpan.width != 0f) {
                // Placeholder Span processing - pass placeholderInfoList to record line info
                processPlaceHolderSpan(view, childSpan, constraintSize, linesSizeList, placeholderInfoList)
            } else {
                // Plain span processing
                processTextSpan(view, childSpan, constraintSize, linesSizeList, realLineHeight)
            }
        }
        // Process remaining lines
        processRemainingLine(constraintSize, linesSizeList, view)
        // Reset current line data
        resetCurrentLineSize(view)
        
        // Second pass: Calculate offsetTop for all placeholders now that line sizes are finalized
        if (placeholderInfoList.isNotEmpty()) {
            calculatePlaceholderOffsetTop(linesSizeList, placeholderInfoList)
        }
        
        // Return line size data list
        return linesSizeList
    }

    /**
     * New line size calculation
     */
    private fun processNewLineSize(
        view: KRRichTextView,
        spanSize: SizeF,
        constraintSize: SizeF,
        realLineHeight: Float,
        linesSizeList: JsArray<SizeF>,
        isLastItem: Boolean,
    ) {
        // Calculate by new line, line height is current span height or parent container specified line height
        view.currentLineHeight = if (realLineHeight > 0f) realLineHeight else spanSize.height
        if (isLastItem) {
            // Last element as remain element processing
            view.currentLineWidth = spanSize.width
        } else {
            // For elements that are not the last, each line of the line is independent
            // Remaining lines
            val totalLines = ceil(spanSize.width / constraintSize.width).toInt()
            // Current line remaining width
            view.currentLineWidth = spanSize.width % constraintSize.width
            // If there are multiple lines, add them to full line size list
            if (totalLines > 1) {
                repeat(totalLines - 1) {
                    linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
                }
            }
            // Then add the last line
            linesSizeList.add(SizeF(view.currentLineWidth, view.currentLineHeight))
        }
    }

    /**
     * Process the size data of participating lines for inline calculation
     */
    private fun processInlineLineSize(
        view: KRRichTextView,
        spanSize: SizeF,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        realLineHeight: Float,
    ) {
        val sumWidth = view.currentLineWidth + spanSize.width
        if (sumWidth <= constraintSize.width || constraintSize.width == 0f) {
            // If it does not exceed one line or no constraint width, then directly add width
            view.currentLineWidth += spanSize.width
            // If line height is greater than current line height, it means line height has not been
            // set or has been set, but current line height is larger, need to set
            if (spanSize.height > view.currentLineHeight) {
                view.currentLineHeight = spanSize.height
            }
        } else {
            // Width that exceeds one line
            val subWidth = sumWidth - constraintSize.width
            // First record full line size, because it is not empty line, so it must have been set line height
            linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
            // Remaining non-one line width
            view.currentLineWidth = subWidth % constraintSize.width
            // Remaining total number of lines
            val totalLines = ceil(subWidth / constraintSize.width).toInt()
            // All parts that exceed one line in remaining lines are calculated as full lines
            if (totalLines > 0) {
                // Because it is already new line, actual line height will not be affected by placeholder
                // span, so need to use current span line height or parent container line height
                view.currentLineHeight = if (realLineHeight > 0f) realLineHeight else spanSize.height
                repeat(totalLines - 1) {
                    // Insert full line size, new line height uses actual size
                    linesSizeList.add(SizeF(constraintSize.width, view.currentLineHeight))
                }
            }
        }
    }

    /**
     * Get size list of non-placeholder spans
     */
    private fun getSpanSizeList(
        value: String,
        fontSize: Float,
        fontWeight: Int,
        fontFamily: String,
        fontStyle: String,
    ): JsArray<SizeF> {
        // First split the text into a list based on line breaks
        val textArray = value.asDynamic().split("\n").unsafeCast<JsArray<String>>()
        // Span size list
        val textSizeList: JsArray<SizeF> = JsArray()
        // Process in a loop
        textArray.forEach { it ->
            // Empty lines are not processed
            if (it != "") {
                // Calculate the width of each line
                val textMetrics =
                    measureTextWidth(it, fontSize.toInt(), fontWeight, fontFamily, fontStyle)
                // Text width
                var textWidth = if (jsTypeOf(textMetrics.actualBoundingBoxLeft) == "number") {
                    (textMetrics.actualBoundingBoxLeft + textMetrics.actualBoundingBoxRight)
                        .unsafeCast<Float>()
                } else {
                    0f
                }
                // Text height，use canvas measure result，plus 1px for round missing
                val textHeight: Float = if (textMetrics.fontBoundingBoxAscent != null && textMetrics.fontBoundingBoxDescent != null) {
                    textMetrics.fontBoundingBoxAscent.unsafeCast<Float>() +
                            textMetrics.fontBoundingBoxDescent.unsafeCast<Float>() + 1f
                } else {
                    // wechat 8.0.49 run in ios 18.2 can not get textMetrics.fontBoundingBoxAscent textMetrics.fontBoundingBoxDescent
                    // use fontSize * 1.2
                    (fontSize * 1.2).toFloat()
                }

                // Add the width and height of each line, using the larger of canvas width and actualBoundingBox
                textWidth = max(textWidth, textMetrics.width.unsafeCast<Float>())
                if (MiniGlobal.isAndroid) {
                    // Android canvas measurement is not accurate, so we need to multiply a magic number
                    textWidth *= WIDTH_RATIO_MAGIC
                }
                textSizeList.add(SizeF(textWidth, textHeight))
            }
        }
        // Return overall width and height
        return textSizeList
    }

    /**
     * Process the size data of text spans
     *
     * @param childSpan Child span node
     * @param constraintSize Parent container constraint size
     * @param realLineHeight Line height
     * @param linesSizeList Line size list
     */
    private fun processTextSpan(
        view: KRRichTextView,
        childSpan: RichTextSpan,
        constraintSize: SizeF,
        linesSizeList: JsArray<SizeF>,
        realLineHeight: Float,
    ) {
        // Get text span line size list, split by line break
        val spanSizeList = getSpanSizeList(
            childSpan.value,
            childSpan.fontSize,
            childSpan.fontWeight,
            childSpan.fontFamily,
            childSpan.fontStyle
        )
        spanSizeList.forEach { item, index ->
            if (index == 0 && view.currentLineWidth != 0f) {
                // If it is multi-line text, it means there is line break, then the first line, and current
                // line width is not 0, then should participate in line accumulation calculation, rather than
                // Start a new line
                processInlineLineSize(view, item, constraintSize, linesSizeList, realLineHeight)
            } else {
                // Other cases are calculated by new line
                // Whether it is the last element, the last element as remain element processing,
                // because there may be other span to be appended later
                val isLastItem = index == spanSizeList.length - 1
                // Process new line
                processNewLineSize(view, item, constraintSize, realLineHeight, linesSizeList, isLastItem)
            }
        }
    }

    /**
     * Reset the size data of the current line
     */
    private fun resetCurrentLineSize(view: KRRichTextView) {
        view.currentLineWidth = 0f
        view.currentLineHeight = 0f
    }

    /**
     * Calculate the size data of plain text
     */
    private fun calculateTextSize(constraintSize: SizeF, view: KRRichTextView): SizeF {
        val ele = view.ele
        // Font size string
        val fontSizeStr = ele.style.fontSize.asDynamic().split("px")[0].unsafeCast<String>()
        // Font size
        val fontSize = fontSizeStr.toFloat()
        // Font weight
        val fontWeight = if (ele.style.fontWeight != "") {
            ele.style.fontWeight.toInt()
        } else {
            FONT_WEIGHT_DEFAULT
        }
        // Actual text height
        val realLineHeight: Float
        // Style's lineHeight
        val lineHeight = ele.style.lineHeight
        // If lineHeight is set, use lineHeight as line height, otherwise use measured line height
        realLineHeight = if (lineHeight != "") {
            lineHeight.pxToFloat()
        } else {
            0f
        }
        // Text span size list
        var linesSizeList = JsArray<SizeF>()
        // Process Text span size list
        processTextSpan(
            view,
            RichTextSpan(
                value = view.rawText,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontFamily = ele.style.fontFamily,
                fontStyle = ele.style.fontStyle
            ), constraintSize, linesSizeList, realLineHeight
        )
        // Process remaining lines
        processRemainingLine(constraintSize, linesSizeList, view)

        // If maximum number of lines is set, and actual exceeds maximum number of lines,
        // use maximum number of lines, otherwise use actual number of lines for processing
        if (view.numberOfLines in 1..linesSizeList.length) {
            // Set multi-line style
            setMultiLineStyle(view.numberOfLines, view)
            // Keep specified height
            linesSizeList = linesSizeList.slice(0, view.numberOfLines)
        } else {
            // Clear multi-line style
            setMultiLineStyle(0, view)
        }

        // Reset current line width and height
        resetCurrentLineSize(view)
        // save current line size
        view.ele.asDynamic().linesCount = linesSizeList.length
        // Return width and height occupied by plain Text
        return calculateTotalSize(linesSizeList)
    }

    /**
     * Calculate the size data of the element occupied
     */
    private fun calculateTotalSize(list: JsArray<SizeF>): SizeF {
        // Line width
        var realWidth = 0f
        // Line height
        var realHeight = 0f

        // Finally, calculate the width and height occupied by text based on the comprehensive line data list
        list.forEach { line ->
            if (line.width > realWidth) {
                // Use the widest line as element width
                realWidth = line.width
            }
            // Height is the height of each line added up
            realHeight += line.height
        }

        // Return element size data, + 0.5 to prevent small decimal calculation error
        return SizeF(realWidth + TEXT_WIDTH_DEFAULT, realHeight)
    }

    /**
     * Calculate the size data of rich text element occupied
     */
    private fun calculateRichTextSize(constraintSize: SizeF, view: KRRichTextView): SizeF {
        // Get the size data list of all lines of the element
        var linesSizeList = calculateLinesSize(constraintSize, view)
        // If maximum number of lines is set, and actual exceeds maximum number of lines,
        // use maximum number of lines, otherwise use actual number of lines for processing
        if (view.numberOfLines in 1..linesSizeList.length) {
            // Set multi-line style
            setMultiLineStyle(view.numberOfLines, view)
            // Keep specified height
            linesSizeList = linesSizeList.slice(0, view.numberOfLines)
        } else {
            // Clear multi-line style
            setMultiLineStyle(0, view)
        }
        // Here style changed, need to re-set divHtml, considering update queue problem,
        // whether need to delay setting, only rich text needs setting
        view.ele.setAttribute("nodes", view.divHtml)

        // Get occupied size position information based on the final size list
        return calculateTotalSize(linesSizeList)
    }

    /**
     * Get innerHTML of child spans
     */
    private fun getChildSpanHtml(view: KRRichTextView): String {
        var spanHtml = ""
        view.childSpanList.forEach { child ->
            val span = child.unsafeCast<MiniSpanElement>()
            spanHtml += "<span style=\"${span.style.cssText}\">${span.textContent}</span>"
        }
        return spanHtml
    }

    /**
     * Create span for internal use
     */
    private fun createSpan(value: JSONObject, view: KRRichTextView): MiniSpanElement {
        val span = MiniSpanElement()
        val text = view.getText(value) ?: return span
        span.innerHTML = text
        span.textContent = text
        val style = span.style
        val color = value.optString(COLOR, "")
        if (color.isNotEmpty()) {
            style.color = color.toRgbColor()
        }
        val fontSize = value.optDouble(FONT_SIZE, 0.0)
        if (fontSize != 0.0) {
            style.fontSize = "${round(fontSize)}px"
        }
        if (style.fontSize.isEmpty()) {
            style.fontSize = view.ele.style.fontSize
        }

        val fontFamily = value.optString(FONT_FAMILY)
        if (fontFamily.isNotEmpty()) {
            style.fontFamily = fontFamily
        } else {
            style.fontFamily = getDefaultFontFamily()
        }

        val textShadow = value.optString(TEXT_SHADOW)
        if (textShadow.isNotEmpty()) {
            val textShadowSpilt = textShadow.asDynamic().split(" ")
            val offsetX = "${textShadowSpilt[0]}px"
            val offsetY = "${textShadowSpilt[1]}px"
            val radius = "${textShadowSpilt[2]}px"
            val shadowColor = textShadowSpilt[3].unsafeCast<String>().toRgbColor()
            style.textShadow = "$offsetX $offsetY $radius $shadowColor"
        }

        val fontWeight = value.optString(FONT_WEIGHT)
        if (fontWeight.isNotEmpty()) {
            style.fontWeight = fontWeight
        } else {
            style.fontWeight = "400"
        }

        val fontStyle = value.optString(FONT_STYLE)
        if (fontStyle.isNotEmpty()) {
            style.fontStyle = fontStyle
        }

        val fontVariant = value.optString(FONT_VARIANT)
        if (fontVariant.isNotEmpty()) {
            style.fontVariant = fontVariant
        }
        val lineSpacing = value.optDouble(LETTER_SPACING, -1.0)
        if (lineSpacing != -1.0) {
            // Set lineHeight according to line spacing
            style.lineHeight = lineSpacing.toNumberFloat().toString()
        }

        val strokeColor = value.optString(STROKE_COLOR).toRgbColor()
        if (strokeColor.isNotEmpty()) {
            style.webkitTextStroke = strokeColor
        }

        val strokeWidth = value.optDouble(STROKE_WIDTH, 0.0)
        if (strokeWidth != 0.0) {
            style.webkitTextStroke = "$strokeColor ${strokeWidth / 4}px"
        }

        val lineHeight = value.optDouble(LINE_HEIGHT, -1.0)
        if (lineHeight != -1.0) {
            // Set lineHeight according to line height
            style.lineHeight = "${lineHeight.toNumberFloat()}px"
        }
        val textDecoration = value.optString(TEXT_DECORATION)
        if (textDecoration.isNotEmpty()) {
            style.textDecoration = textDecoration
        }
        val textIndent = value.optDouble(HEAD_INDENT, 0.0)
        if (textIndent != 0.0) {
            style.textIndent = "${textIndent}px"
        }
        // Placeholder span width
        val placeHolderWidth = value.optDouble(PLACEHOLDER_WIDTH, 0.0)
        // Placeholder span height
        val placeHolderHeight = value.optDouble(PLACEHOLDER_HEIGHT, 0.0)
        // If placeholder span has width and height, then set it
        if (placeHolderWidth != 0.0 && placeHolderHeight != 0.0) {
            style.width = "${placeHolderWidth}px"
            span.offsetWidth = placeHolderWidth.toFloat()
            style.height = "${placeHolderHeight}px"
            span.offsetHeight = placeHolderHeight.toFloat()
            // This type of span is set as inline-block type
            style.display = "inline-block"
            // Vertically centered
            style.verticalAlign = "middle"
        }
        return span
    }

    /**
     * Get placeholder image style
     */
    private fun getPlaceHolderImageStyle(view: MiniElement): String {
        // Style list
        val styleList: JsArray<String> = JsArray()
        // First insert fixed style
        styleList.push("width:100%;height:100%;display:block;")
        // Then insert external style
        styleList.push("border-top-left-radius:${view.style.borderTopLeftRadius};")
        styleList.push("border-top-right-radius:${view.style.borderTopLeftRadius};")
        styleList.push("border-bottom-left-radius:${view.style.borderBottomLeftRadius};")
        styleList.push("border-bottom-right-radius:${view.style.borderBottomRightRadius};")

        return styleList.join("")
    }

    /**
     * Throttled execution function (supports parameters)
     */
    private fun throttle(view: KRRichTextView, action: () -> Unit) {
        // Cancel unexecuted task
        if (view.pendingJob != 0) {
            MiniGlobal.clearTimeout(view.pendingJob)
        }
        // Delay execution task
        view.pendingJob = MiniGlobal.setTimeout({
            action()
        }, IMAGE_SPAN_DELAY)
    }

    /**
     * measure real text size
     */
    override fun measureTextSize(constraintSize: SizeF, view: KRRichTextView, renderText: String): SizeF {
        // real eom
        val ele = view.ele
        // Set element maximum width constraint
        if (constraintSize.width > 0f) {
            ele.style.maxWidth = "${constraintSize.width}px"
        }

        return if (view.isRichText && view.richTextSpanList.length > 0) {
            // Rich text needs loop calculation and comprehensive calculation
            calculateRichTextSize(constraintSize, view)
        } else {
            val textSize = calculateTextSize(constraintSize, view)
            val webkitLineClamp = ele.style.asDynamic().webkitLineClamp.unsafeCast<String>()
            // set lineHeight for text to vertical center, only support single line
            if (ele.style.lineHeight.isEmpty() &&
                !view.rawText.contains("\n") &&
                (webkitLineClamp.isEmpty() || webkitLineClamp.toInt() == 1) &&
                textSize.height != 0f &&
                ele.asDynamic().linesCount == 1
            ) {
                ele.style.lineHeight = "${textSize.height}px"
            }

            return textSize
        }
    }

    fun clearRichTextValues(view: KRRichTextView) {
        view.childSpanList.clear()
        view.richTextSpanList.clear()
        view.imageSpanList.clear()
        view.imageSpanCount = 0
    }

    /**
     * Rich text content setting, here we need to calculate the overall width and height of the rich text,
     * the calculation method is quite complex:
     * 1. The width of each span added together. For placeholder spans, additional height needs to be calculated,
     *    but note that if the placeholder spans are within one line, then multiple height additions are not needed.
     * 2. For single spans, if there are line breaks, they also need to be processed.
     * 3. Use constraint width to calculate single line width, if it exceeds constraints then line break is needed.
     * 4. Also need to record the height of each line, because if a specific number of lines is specified,
     *    when some lines need to be omitted, the height of the preserved lines needs to be calculated.
     */
    override fun setRichTextValues(richTextValues: JSONArray, view: KRRichTextView) {
        // Clear all child nodes
        clearRichTextValues(view)
        
        for (i in 0 until richTextValues.length()) {
            val span = createSpan(richTextValues.optJSONObject(i) ?: JSONObject(), view)
            if (span.textContent != null) {
                // Save child span nodes
                view.childSpanList.add(span)
                val fontSize = span.style.fontSize.asDynamic().split("px")[0].unsafeCast<String>()
                if (span.offsetWidth != 0f) {
                    // If there is offsetWidth, it's a placeholder span case, save width and height
                    view.richTextSpanList.add(
                        RichTextSpan(
                            width = span.offsetWidth,
                            height = span.offsetHeight
                        )
                    )
                    // Placeholder image count plus 1
                    view.imageSpanCount += 1
                    // Placeholder span input
                    view.imageSpanList.add(span)
                } else {
                    // For non-placeholder spans, save the text content value
                    view.richTextSpanList.add(
                        RichTextSpan(
                            value = span.textContent!!,
                            fontSize = fontSize.toFloat(),
                            fontWeight = span.style.fontWeight.toInt(),
                            fontFamily = span.style.fontFamily,
                            fontStyle = span.style.fontStyle
                        )
                    )
                }
            }
        }
        // Calculate span innerHTML
        view.spanHtml = getChildSpanHtml(view)
        // Set rich text content
        view.ele.setAttribute("nodes", view.divHtml)
    }

    /**
     * Insert placeholder image for span
     */
    fun insertPlaceHolderImageView(parentView: KRRichTextView, view: MiniElement, insertIndex: Int) {
        if (view.firstElementChild is MiniImageElement) {
            // Placeholder image
            val image = view.firstElementChild.unsafeCast<MiniImageElement>()
            try {
                // Set placeholder image
                val imgSpan = parentView.imageSpanList[insertIndex]
                val childSpan = parentView.childSpanList[parentView.childSpanList.indexOf(imgSpan)]
                    .unsafeCast<MiniSpanElement>()
                childSpan.textContent = "<img style='${
                    getPlaceHolderImageStyle(view)
                }' mode='${
                    (view.firstElementChild as MiniImageElement).mode
                }' src='${image.src}' />"
                // Set image container corner
                childSpan.style.borderTopLeftRadius = view.style.borderTopLeftRadius
                childSpan.style.borderTopRightRadius = view.style.borderTopRightRadius
                childSpan.style.borderBottomLeftRadius = view.style.borderBottomLeftRadius
                childSpan.style.borderBottomRightRadius = view.style.borderBottomRightRadius
                // Update rich text
                parentView.spanHtml = getChildSpanHtml(parentView)
                // First save rich text placeholder html content
                val richTextViewHtml = parentView.divHtml
                // Throttled execution update node html
                throttle(parentView) {
                    parentView.ele.setAttribute("nodes", richTextViewHtml)
                }
            } catch (e: dynamic) {
                // Set placeholder image exception
                Log.error("set placeholder image error: $e")
            }

        }
    }
}

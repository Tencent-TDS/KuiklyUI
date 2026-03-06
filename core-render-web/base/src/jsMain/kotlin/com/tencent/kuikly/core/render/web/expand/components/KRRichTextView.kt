package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.web.adapter.TextPostProcessorInput
import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.ktx.getCSSBackgroundImage
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.toNumberFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.get

class KRTextProps {
    /**
     * Rich text JSON description data
     */
    private var values: JSONArray? = null

    /**
     * Simple text
     */
    private var text = KRCssConst.EMPTY_STRING

    /**
     * Font size
     */
    private var fontSize = DEFAULT_FONT_SIZE

    /**
     * Text custom post-processor identifier
     */
    var textPostProcessor = ""

    /**
     * Text custom post-processor identifier, processing completion flag
     */
    var textPostProcessorDone = false

    /**
     * Set text properties
     */
    fun setProp(propKey: String, propValue: Any) {
        when (propKey) {
            PROP_KEY_VALUES -> values = JSONArray(propValue.unsafeCast<String>())
            PROP_KEY_TEXT -> text = propValue.unsafeCast<String>()
            PROP_KEY_FONT_SIZE -> fontSize = propValue.toNumberFloat()
            PROP_KEY_TEXT_POST_PROCESSOR -> textPostProcessor = propValue.unsafeCast<String>()
        }
    }

    companion object {
        const val PROP_KEY_VALUES = "values"
        const val PROP_KEY_TEXT = "text"
        const val PROP_KEY_FONT_SIZE = "fontSize"
        const val PROP_KEY_TEXT_POST_PROCESSOR = "textPostProcessor"
        const val DEFAULT_FONT_SIZE = 16f
    }
}

/**
 * Rich text child span data class
 *
 * @param value Rich text content (non-placeholder span has)
 * @param width Rich text width (placeholder span has)
 * @param height Rich text height (placeholder span has)
 * @param fontWeight Rich text font weight (non-placeholder span has)
 * @param fontSize Rich text font size (non-placeholder span has)
 * @param fontFamily Rich text font type (non-placeholder span has)
 * @param fontStyle Rich text font style (non-placeholder span has)
 * @param offsetLeft Rich text distance from left (placeholder span has)
 * @param offsetTop Rich text distance from top (placeholder span has)
 *
 */
data class RichTextSpan(
    val value: String = "",
    val width: Float = 0f,
    val height: Float = 0f,
    val fontWeight: Int = DEFAULT_FONT_WEIGHT,
    val fontSize: Float = 0f,
    val fontFamily: String = "",
    var offsetLeft: Float = 0f,
    var offsetTop: Float = 0f,
    val fontStyle: String = ""
)

private const val DEFAULT_FONT_WEIGHT = 400

/**
 * KRRichTextView, corresponding to Kuikly's RichText
 */
class KRRichTextView : IKuiklyRenderViewExport, IKuiklyRenderShadowExport {
    /**
     * Text properties
     */
    private val textProps = KRTextProps()

    /**
     * Number of lines
     */
    var numberOfLines = 0

    /**
     * Whether it is a rich text
     */
    var isRichText = false

    // Temporarily store text set by kuikly core
    var rawText = ""

    // Width of the current line
    var currentLineWidth = 0f

    // Height of the current line
    var currentLineHeight = 0f

    // Rich text child node list, including node size information
    val richTextSpanList: JsArray<RichTextSpan> = JsArray()

    // Current full span list
    val childSpanList: JsArray<Any> = JsArray()

    // Placeholder Span list
    val imageSpanList: JsArray<Any> = JsArray()

    // Number of placeholder images
    var imageSpanCount = 0

    // HTML content of child spans for mini app
    var spanHtml = ""

    // Pending tasks
    var pendingJob = 0

    // Original HTML content of rich text for mini app
    val divHtml: String
        get() = "<div>${spanHtml}</div>"

    // Default properties
    private var lineBreakMode = ""
    private var values: JSONArray? = null
    private var text = ""
    private var lineSpacing = DEFAULT_LINE_SPACING
    private var textDecoration = ""
    private var textAlign = ""
    private var lineBreakMargin = 0f
    private var fontWeight = ""
    private var fontStyle = "normal"
    private var fontFamily = ""
    private var letterSpacing = 0f
    private var color = ""
    private var fontSize = DEFAULT_ELEMENT_FONT_SIZE
    private var renderText = ""
    private var strokeColor = ""

    // Initialize p tag
    private val textEle = kuiklyDocument.createElement(ElementType.P).apply {
        val style = this.unsafeCast<HTMLParagraphElement>().style
        // set default value for text element
        style.margin = "0"
        // Break by word
        style.wordBreak = "break-word"
        // Default size
        style.fontSize = "${fontSize}px"
        // Default display
        style.display = "inline-block"
        // save rich text element reference
        asDynamic().krRichTextView = this@KRRichTextView
    }

    /**
     * Return text instance
     */
    override val ele: HTMLParagraphElement
        get() = this.textEle.unsafeCast<HTMLParagraphElement>()

    /**
     * Set shadow element properties
     */
    override fun setShadowProp(propKey: String, propValue: Any) {
        val style = ele.style
        textProps.setProp(propKey, propValue)
        when (propKey) {
            NUMBER_OF_LINES -> {
                // Save maximum allowed text lines
                numberOfLines = propValue.unsafeCast<Int>()
            }

            LINE_BREAK_MARGIN -> {
                lineBreakMargin = propValue.toNumberFloat()
                // Save margin value after line break
                // If lineBreakMargin is set, ellipsis is also needed
                setLineBreakMode("")
            }

            LINE_BREAK_MODE -> {
                lineBreakMode = propValue as String
                setLineBreakMode(propValue.unsafeCast<String>())
            }

            HEAD_INDENT -> {
                ele.style.textIndent = propValue.toPxF()
            }

            VALUES -> {
                // If values is set, it means we need to set spans within the text component, special handling needed
                val richTextValues = JSONArray(propValue.unsafeCast<String>())
                // Save the rich text values
                values = richTextValues
                // set rich text sign to true
                isRichText = true
                // Set multiple spans
                KuiklyProcessor.richTextProcessor.setRichTextValues(richTextValues, this)
            }
            // Set text through innerText to prevent xss and other special cases
            TEXT -> {
                // Save original text value
                text = propValue.unsafeCast<String>()
                renderText = text

                // emoji textPostProcessor
                if (textProps.textPostProcessor.isNotEmpty()) {
                    KuiklyRenderAdapterManager.krTextPostProcessorAdapter?.onTextPostProcess(
                        TextPostProcessorInput(textProps.textPostProcessor, text, textProps)
                    )?.text?.let { processedText ->
                        renderText = processedText.unsafeCast<String>()
                        textProps.textPostProcessorDone = true
                    } ?: run {
                        renderText = text
                    }
                } else {
                    renderText = text
                }
                // save the raw text
                rawText = renderText
            }

            KRTextProps.PROP_KEY_TEXT_POST_PROCESSOR -> {
                // If text is processed && emoji not done, do emoji textPostProcessor processing here
                if (ele.innerText.isNotEmpty() && !textProps.textPostProcessorDone) {
                    KuiklyRenderAdapterManager.krTextPostProcessorAdapter?.onTextPostProcess(
                        TextPostProcessorInput(textProps.textPostProcessor, text, textProps)
                    )?.text?.let {
                        // Update only if changed
                        if (it != text) {
                            ele.innerText = it.unsafeCast<String>()
                            textProps.textPostProcessorDone = true
                        }
                    }
                }
            }

            COLOR -> style.color = propValue.unsafeCast<String>().toRgbColor()
            LETTER_SPACING -> style.letterSpacing = propValue.toPxF()
            TEXT_DECORATION -> style.textDecoration = propValue.unsafeCast<String>()
            TEXT_ALIGN -> style.textAlign = propValue.unsafeCast<String>()
            LINE_SPACING -> style.lineHeight = propValue.toNumberFloat().toString()
            LINE_HEIGHT -> style.lineHeight = propValue.toNumberFloat().toPxF()
            FONT_WEIGHT -> style.fontWeight = propValue.unsafeCast<String>()
            FONT_STYLE -> style.fontStyle = propValue.unsafeCast<String>()
            FONT_FAMILY -> style.fontFamily = propValue.unsafeCast<String>()
            FONT_SIZE -> style.fontSize = propValue.toNumberFloat().toPxF()
            BACKGROUND_IMAGE -> setBackgroundImage(propValue)
            STROKE_WIDTH -> setStokeWidth(propValue)
            STROKE_COLOR -> strokeColor = propValue.unsafeCast<String>().toRgbColor()
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun setShadow(shadow: IKuiklyRenderShadowExport) {
        super.setShadow(shadow)
        if(!this.isRichTextValues() && ele.innerText != renderText) {
            // for compose , plain text, isRichText is true, but richTextSpanList.length is 0
            // for web, when set innerText, the span will be removed
            ele.innerText = renderText
        }
    }

    fun isRichTextValues(): Boolean {
        return isRichText && (values?.length() ?: 0) > 0
    }

    /**
     * Calculate actual space size occupied by element, reuse text calculation node
     *
     * @param constraintSize Constraint size
     */
    override fun calculateRenderViewSize(constraintSize: SizeF): SizeF =
        KuiklyProcessor.richTextProcessor.measureTextSize(constraintSize, this, this.renderText)

    override fun call(methodName: String, params: String): Any? {
        return when (methodName) {
            METHOD_GET_PLACEHOLDER_SPAN_RECT -> {
                // Return placeholder span position and size data
                getPlaceholderSpanRect(params.toInt())
            }

            else -> super<IKuiklyRenderShadowExport>.call(methodName, params)
        }
    }

    /**
     * Get text content set
     */
    fun getText(jsonObject: JSONObject): String? {
        var text = jsonObject.optString("value", "")
        if (text.isEmpty()) {
            text = jsonObject.optString("text")
        }
        return text.ifEmpty {
            null
        }
    }

    /**
     * Set text wrapping mode
     */
    private fun setLineBreakMode(lineBreakMode: String) {
        if (this.numberOfLines > 0) {
            return
        }
        when (lineBreakMode) {
            LINE_BREAK_MODE_WORD_WRAP -> {
                // Text exceeds, cut off by text
                ele.style.overflowX = "hidden"
                ele.style.overflowY = "hidden"
                ele.style.whiteSpace = "nowrap"
                ele.style.wordBreak = "break-word"
                ele.style.textOverflow = "ellipsis"
            }
            else -> {
                // Other default tail ellipsis, clip and middle web temporarily do not support
                ele.style.overflowX = "hidden"
                ele.style.overflowY = "hidden"
                ele.style.whiteSpace = "nowrap"
                ele.style.wordBreak = "break-all"
                ele.style.textOverflow = "ellipsis"
            }
        }
    }


    /**
     * Get placeholder span size data
     */
    private fun getPlaceholderSpanRect(index: Int?): String {
        var rectInfo = "0.0 0.0 0.0 0.0"

        if (index != null) {
            // Prefer using richTextSpanList calculated position info (miniapp)
            // index is the span index in the full list, use it directly to get from richTextSpanList
            if (index < richTextSpanList.length) {
                val span = richTextSpanList[index]
                // Check if it's a PlaceholderSpan (width and height are not 0)
                if (span.width != 0f && span.height != 0f) {
                    rectInfo = "${span.offsetLeft} ${span.offsetTop} ${span.width} ${span.height}"
                    return rectInfo
                }
            }

            // Fallback to DOM method (H5 and other platforms)
            val placeholderSpan = ele.childNodes[index].unsafeCast<HTMLElement?>()
            if (
                placeholderSpan != null &&
                placeholderSpan.style.width != "" &&
                placeholderSpan.style.height != ""
            ) {
                with(placeholderSpan) {
                    rectInfo = "$offsetLeft $offsetTop $offsetWidth $offsetHeight"
                }
            }
        }

        return rectInfo
    }

    /**
     * Set background image
     */
    private fun setBackgroundImage(value: Any) {
        // If background image is url or base64, set directly
        val backgroundImagePrefix = "background-image: "
        val stringValue = value.unsafeCast<String>()
        if (stringValue.startsWith(backgroundImagePrefix)) {
            ele.style.backgroundImage =
                stringValue.substring(backgroundImagePrefix.length, stringValue.length - 1)
        } else {
            // Otherwise, it's a gradient font, need to set through linear-gradient
            ele.style.backgroundImage = getCSSBackgroundImage(value.unsafeCast<String>())
            // Then clip background to text
            ele.style.backgroundClip = "text"
            // For compatibility, also set webkit prefixed property
            ele.style.asDynamic().webkitBackgroundClip = "text"
            // Then set text to transparent to show background
            ele.style.color = "transparent"
        }
    }

    /**
    * Set stroke width
     */
    private fun setStokeWidth(value: Any) {
        val strokeWidth = value.unsafeCast<String>()
        if (strokeWidth != "0.0") {
            val usedStrokeWidth = strokeWidth.toDouble() / STROKE_WIDTH_DIVISOR
            ele.style.asDynamic().webkitTextStroke = "${usedStrokeWidth}px $strokeColor"
        }
    }

    companion object {
        const val VIEW_NAME = "KRRichTextView"
        const val GRADIENT_RICH_TEXT_VIEW = "KRGradientRichTextView"
        // Rich text placeholder attribute setting
        private const val PLACEHOLDER_WIDTH = "placeholderWidth"
        private const val PLACEHOLDER_HEIGHT = "placeholderHeight"

        // Text props below
        private const val NUMBER_OF_LINES = "numberOfLines"
        private const val LINE_BREAK_MODE = "lineBreakMode"
        private const val LINE_BREAK_MARGIN = "lineBreakMargin"
        private const val VALUES = "values"
        private const val TEXT = "text"
        private const val TEXT_ALIGN = "textAlign"
        private const val LINE_SPACING = "lineSpacing"
        private const val LINE_HEIGHT = "lineHeight"
        private const val LETTER_SPACING = "letterSpacing"
        private const val COLOR = "color"
        private const val FONT_SIZE = "fontSize"
        private const val TEXT_DECORATION = "textDecoration"
        private const val FONT_WEIGHT = "fontWeight"
        private const val FONT_STYLE = "fontStyle"
        private const val FONT_FAMILY = "fontFamily"
        private const val BACKGROUND_IMAGE = "backgroundImage"
        private const val STROKE_WIDTH = "strokeWidth"
        private const val STROKE_COLOR = "strokeColor"
        private const val FONT_VARIANT = "fontVariant"
        // Rich text placeholder setting method
        private const val METHOD_GET_PLACEHOLDER_SPAN_RECT = "spanRect"
        private const val HEAD_INDENT = "headIndent"
        private const val GRAB_TEXT = "grabText"

        private const val LINE_BREAK_MODE_WORD_WRAP = "wordWrapping"
        
        // Default values
        private const val DEFAULT_LINE_SPACING = 1f
        private const val DEFAULT_ELEMENT_FONT_SIZE = 13f
        private const val STROKE_WIDTH_DIVISOR = 4
    }
}

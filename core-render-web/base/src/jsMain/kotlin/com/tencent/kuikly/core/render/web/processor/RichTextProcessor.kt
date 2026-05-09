package com.tencent.kuikly.core.render.web.processor

import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray

/**
 * Approximate corresponding values for font size and line height
 */
object FontSizeToLineHeightMap {
    //  Default value when line height is not matched
    private const val DEFAULT_LINE_HEIGHT = 1.3

    // Actual size mapping table
    private val mapList = FastMutableMap<String, Int>(
        js(
            """
                {
                 8: 11, 
                 9: 13, 
                 10: 14, 
                 11: 16, 
                 12: 17, 
                 13: 18, 
                 14: 20, 
                 15: 21, 
                 16: 22, 
                 17: 24, 
                 18: 25, 
                 19: 26, 
                 20: 28, 
                 21: 29, 
                 22: 30, 
                 23: 32, 
                 24: 33, 
                 25: 36, 
                 26: 37, 
                 27: 38, 
                 28: 40, 
                 29: 41, 
                 30: 42, 
                 31: 44, 
                 32: 45, 
                 33: 46, 
                 34: 48
                 }
            """
        )
    )

    /**
     * Return line height based on input font size
     */
    fun getLineHeight(fontSize: Float): Float
        // If found in the table, use it, otherwise use the estimated ratio
        = mapList[fontSize.toString()]?.toFloat() ?: (fontSize * DEFAULT_LINE_HEIGHT).toFloat()
}

/**
 * rich text process interface, include text measure, rich text process, etc.
 */
interface IRichTextProcessor {
    /**
     * measure text size
     */
    fun measureTextSize(constraintSize: SizeF, view: KRRichTextView, renderText: String): SizeF

    /**
     * set rich text values
     */
    fun setRichTextValues(richTextValues: JSONArray, view: KRRichTextView)
}
package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal.isIOS
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst

/**
 * Mini program text node, which will determine whether to use rich-text or text based on the situation.
 * If no events have been bound, it will use the static version of the corresponding mini program component.
 */
class MiniParagraphElement(
    nodeName: String = TransformConst.TEXT,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    // Internal real pure text node
    private val realText = MiniSpanElement()

    // Temporarily store text set by kuikly core
    private var rawText = ""

    init {
        realText.parentNode = this
        childNodes.add(realText)
        style.whiteSpace = "pre-wrap"
        style.overflowY = "hidden"
        style.fontFamily = getDefaultFontFamily()
    }

    override var innerHTML: String = ""
        set(value) {
            rawText = value
            realText.textContent = value
            field = value
            updateChildNodes()
        }

    /**
     * innerText property, mini program actually uses innerHTML
     */
    override var innerText: String
        get() = innerHTML
        set(value) {
            innerHTML = value
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

    override fun onTransformData(): String {
        val krRichTextView = this.asDynamic().krRichTextView.unsafeCast<KRRichTextView?>()
        if (krRichTextView != null && krRichTextView.isRichText && krRichTextView.richTextSpanList.length > 0) {
            if (!hasAddEventListener) {
                return TransformConst.STATIC_RICH_TEXT
            }
            return TransformConst.RICH_TEXT
        }
        if (!hasAddEventListener) {
            return TransformConst.STATIC_TEXT
        }
        return super.onTransformData()
    }
}

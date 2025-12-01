package com.tencent.kuikly.h5app.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

/**
 * Custom WebView component
 */
class KRWebView : IKuiklyRenderViewExport {
    override val ele: HTMLIFrameElement =
        document.createElement("iframe").unsafeCast<HTMLIFrameElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SRC -> {
                ele.src = propValue.unsafeCast<String>()
                true
            }

            else -> super.setProp(propKey, propValue)
        }
    }

    companion object {
        const val SRC = "src"
        const val VIEW_NAME = "KRWebView"
    }
}

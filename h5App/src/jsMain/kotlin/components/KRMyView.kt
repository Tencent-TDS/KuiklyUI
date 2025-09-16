package components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

/**
 * Custom MyView
 */
class KRMyView : IKuiklyRenderViewExport {
    private val div = document.createElement("div")

    override val ele: HTMLElement
        get() = div.unsafeCast<HTMLElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            MESSAGE -> {
                ele.innerHTML = propValue.unsafeCast<String>()
                return true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    companion object {
        const val MESSAGE = "message"
        const val VIEW_NAME = "KRMyView"
    }
}
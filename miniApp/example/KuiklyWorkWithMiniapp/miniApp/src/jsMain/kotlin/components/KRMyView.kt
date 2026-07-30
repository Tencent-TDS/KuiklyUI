package components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniDivElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement
import org.w3c.dom.Element

class KRMyView : IKuiklyRenderViewExport {
    private val div = MiniDivElement()
    override val ele: Element
        get() = div.unsafeCast<Element>()
    private val innerText = MiniSpanElement()

    init {
        ele.appendChild(innerText.unsafeCast<Element>())
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            MESSAGE -> {
                innerText.textContent = propValue.unsafeCast<String>()
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
package components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import dom.MiniButtonViewElement
import org.w3c.dom.Element

class KRCustomButtonView : IKuiklyRenderViewExport {
    private val customButtonElement = MiniButtonViewElement()
    override val ele: Element
        get() = customButtonElement.unsafeCast<Element>()

    private var getPhoneNumberCallback: KuiklyRenderCallback? = null

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            OPEN_TYPE -> {
                customButtonElement.openType = propValue as String
                true
            }
            GET_PHONE_NUMBER_CALLBACK -> {
                getPhoneNumberCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    init {
        customButtonElement.addEventListener("getphonenumber", {
            getPhoneNumberCallback?.invoke(mapOf("data" to JSON.stringify(it.detail)))
        })
    }
    companion object {
        const val OPEN_TYPE = "openType"
        const val GET_PHONE_NUMBER_CALLBACK = "getPhoneNumberCallback"
        const val VIEW_NAME = "KRCustomButtonView"
    }
}
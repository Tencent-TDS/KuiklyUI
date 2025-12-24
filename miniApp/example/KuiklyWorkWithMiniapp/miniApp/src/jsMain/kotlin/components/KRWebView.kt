package components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.utils.Log
import dom.MiniWebViewElement
import org.w3c.dom.Element
import kotlin.js.json

class KRWebView : IKuiklyRenderViewExport {
    private val webElement = MiniWebViewElement()
    override val ele: Element
        get() = webElement.unsafeCast<Element>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SRC -> {
                webElement.src = propValue.unsafeCast<String>()
                true
            }

            KRCssConst.FRAME -> {
                if (!MiniGlobal.globalThis.hasWebViewShowDialog.unsafeCast<Boolean>()) {
                    MiniGlobal.globalThis.hasWebViewShowDialog = true
                    NativeApi.plat.showToast(
                        json(
                            "title" to "Mini program web-view will automatically " +
                                    "fill the full screen, cannot set width, height and position",
                            "duration" to 3000,
                            "icon" to "none"
                        )
                    )
                }
                Log.warn(
                    "Mini program web-view will automatically fill the full screen, cannot set width, height and position"
                )
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
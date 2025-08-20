import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.runtime.miniapp.expand.KuiklyRenderViewDelegator
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Transform
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import components.KRMyView
import components.KRWebView
import dom.MiniWebViewElement
import module.KRBridgeModule
import module.KRCacheModule


class ViewPropExternalHandler : IKuiklyRenderViewPropExternalHandler {
    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        return when (propKey) {
            "needCustomWrapper" -> {
                renderViewExport.ele.unsafeCast<MiniElement>().needCustomWrapper = propKey.toBoolean()
                true
            }
            else -> false
        }
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        return when (propKey) {
            "needCustomWrapper" -> {
                true
            }
            else -> false
        }
    }

}


/**
 * Implement the delegate interface provided by Web Render
 */
class KuiklyWebRenderViewDelegator : KuiklyRenderViewDelegatorDelegate {

    // mini render delegate
    val delegate = KuiklyRenderViewDelegator(this)

    /**
     * Register custom modules
     */
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        // Register bridge module
        kuiklyRenderExport.moduleExport(KRBridgeModule.MODULE_NAME) {
            KRBridgeModule()
        }
        kuiklyRenderExport.moduleExport(KRCacheModule.MODULE_NAME) {
            KRCacheModule()
        }
        super.registerExternalModule(kuiklyRenderExport)
    }

    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            viewPropExternalHandlerExport(ViewPropExternalHandler())
        }
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)

        // Add template alias for custom views
        Transform.addComponentsAlias(
            MiniWebViewElement.NODE_NAME,
            MiniWebViewElement.componentsAlias
        )

        // Register custom views
        kuiklyRenderExport.renderViewExport(KRWebView.VIEW_NAME, {
            KRWebView()
        })

        // Register custom views
        kuiklyRenderExport.renderViewExport(KRMyView.VIEW_NAME, {
            KRMyView()
        })
    }
}

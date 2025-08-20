import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyRenderViewDelegator
import components.KRMyView
import components.KRWebView
import components.KuiklyPageView
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
                renderViewExport.ele.setAttribute("data-needCustomWrapper", propValue.toString())
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
                renderViewExport.ele.setAttribute("data-needCustomWrapper", js("undefined"))
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
    // web render delegate
    private val delegate = KuiklyRenderViewDelegator(this)

    /**
     * Initialize
     */
    fun init(
        containerId: String,
        pageName: String,
        pageData: Map<String, Any>,
        size: SizeI,
    ) {
        // Initialize and create view
        delegate.onAttach(
            containerId,
            pageName,
            pageData,
            size,
        )
    }

    /**
     * Page becomes visible
     */
    fun resume() {
        delegate.onResume()
    }

    /**
     * Page becomes invisible
     */
    fun pause() {
        delegate.onPause()
    }

    /**
     * Page unload
     */
    fun detach() {
        delegate.onDetach()
    }

    /**
     * Register custom modules
     */
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)

        // Register bridge module
        kuiklyRenderExport.moduleExport(KRBridgeModule.MODULE_NAME) {
            KRBridgeModule()
        }
        // Register cache module
        kuiklyRenderExport.moduleExport(KRCacheModule.MODULE_NAME) {
            KRCacheModule()
        }
    }

    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        with(kuiklyRenderExport) {
            viewPropExternalHandlerExport(ViewPropExternalHandler())
        }
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)

        // Register custom views
        kuiklyRenderExport.renderViewExport(KRMyView.VIEW_NAME, {
            KRMyView()
        })
        kuiklyRenderExport.renderViewExport(KuiklyPageView.VIEW_NAME, {
            KuiklyPageView()
        })
        kuiklyRenderExport.renderViewExport(KRWebView.VIEW_NAME, {
            KRWebView()
        })
    }
}

package components

import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyView
import module.KRBridgeModule
import module.KRCacheModule

/**
 * Kuikly page-level view
 */
class KuiklyRenderView(delegate: KuiklyRenderViewDelegatorDelegate? = null) : KuiklyView(delegate) {
    /**
     * Register custom modules at view granularity
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
}

package com.tencent.kuikly.core.render.web.runtime.web.expand

import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode
import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.performance.KRMonitorType
import com.tencent.kuikly.core.render.web.performance.KRPerformanceData
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData

/**
 * View level integration view
 */
open class KuiklyView(private val delegate: KuiklyRenderViewDelegatorDelegate? = null) :
    IKuiklyView,
    KuiklyRenderViewDelegatorDelegate {
    // Render view delegator
    private val kuiklyRenderViewDelegator = KuiklyRenderViewDelegator(this)

    override fun onAttach(
        container: Any,
        pageName: String,
        pageData: Map<String, Any>,
        size: SizeI
    ) {
        // Attach view to specified element
        kuiklyRenderViewDelegator.onAttach(container, pageName, pageData, size)
    }

    override fun onPause() {
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onResume() {
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onDetach() {
        kuiklyRenderViewDelegator.onDetach()
    }

    override fun sendEvent(event: String, data: Map<String, Any>) {
        kuiklyRenderViewDelegator.sendEvent(event, data)
    }

    override fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        kuiklyRenderViewDelegator.addKuiklyRenderViewLifeCycleCallback(callback)
    }

    override fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        kuiklyRenderViewDelegator.removeKuiklyRenderViewLifeCycleCallback(callback)
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        // Register view for view delegate
        delegate?.registerExternalRenderView(kuiklyRenderExport)
    }

    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        // Register module for view delegate
        delegate?.registerExternalModule(kuiklyRenderExport)
    }

    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        // Register property handler for view delegate
        delegate?.registerViewExternalPropHandler(kuiklyRenderExport)
    }

    override fun onKuiklyRenderViewCreated() {
        super.onKuiklyRenderViewCreated()
        delegate?.onKuiklyRenderViewCreated()
    }

    override fun onKuiklyRenderContentViewCreated() {
        super.onKuiklyRenderContentViewCreated()
        delegate?.onKuiklyRenderContentViewCreated()
    }

    override fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason?,
        executeMode: KuiklyRenderCoreExecuteMode
    ) {
        super.onPageLoadComplete(isSucceed, errorReason, executeMode)
        delegate?.onPageLoadComplete(isSucceed, errorReason, executeMode)
    }

    override fun onGetLaunchData(data: KRLaunchData) {
        super.onGetLaunchData(data)
        delegate?.onGetLaunchData(data)
    }

    override fun onGetPerformanceData(data: KRPerformanceData) {
        super.onGetPerformanceData(data)
        delegate?.onGetPerformanceData(data)
    }

    override fun coreExecuteMode(): KuiklyRenderCoreExecuteMode =
        delegate?.coreExecuteMode() ?: super.coreExecuteMode()

    override fun performanceMonitorTypes(): List<KRMonitorType> =
        delegate?.performanceMonitorTypes() ?: super.performanceMonitorTypes()

    override fun syncRenderingWhenPageAppear(): Boolean =
        delegate?.syncRenderingWhenPageAppear() ?: super.syncRenderingWhenPageAppear()

    override fun onUnhandledException(
        throwable: Throwable,
        errorReason: ErrorReason,
        executeMode: KuiklyRenderCoreExecuteMode
    ) {
        super.onUnhandledException(throwable, errorReason, executeMode)
        delegate?.onUnhandledException(throwable, errorReason, executeMode)
    }
}

package com.tencent.kuikly.core.render.web.expand

import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.KuiklyRenderView
import com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode
import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.performance.KRMonitorType
import com.tencent.kuikly.core.render.web.performance.KRPerformanceData
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData


typealias KuiklyRenderViewPendingTask = (KuiklyRenderView) -> Unit

/**
 * Internal delegate implementation class for kuikly, defines interfaces available for business extension
 */
interface KuiklyRenderViewDelegatorDelegate {
    /**
     * For business to register renderView and shadow
     */
    fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * For business to register Module
     */
    fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * For business to inject custom property handler for View
     */
    fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * KuiklyRenderCore execution mode, default is JS mode
     * @return Execution mode
     */
    fun coreExecuteMode(): KuiklyRenderCoreExecuteMode = KuiklyRenderCoreExecuteMode.JS

    /**
     * Performance monitoring options, only monitoring enabled by default
     */
    fun performanceMonitorTypes(): List<KRMonitorType> = listOf(KRMonitorType.LAUNCH)

    /**
     * KuiklyRenderView creation callback
     */
    fun onKuiklyRenderViewCreated() {}

    /**
     * Callback when KuiklyRenderView's child View is created
     */
    fun onKuiklyRenderContentViewCreated() {}

    /**
     * Whether first screen is synchronous rendering (synchronous by default)
     */
    fun syncRenderingWhenPageAppear(): Boolean = true

    /**
     * Launch data callback
     */
    fun onGetLaunchData(data: KRLaunchData) {}

    /**
     * Performance data callback
     */
    fun onGetPerformanceData(data: KRPerformanceData) {}

    /**
     * Exception callback
     *
     * @param throwable Exception
     * @param errorReason Failure reason
     * @param executeMode Execution mode
     */
    fun onUnhandledException(
        throwable: Throwable,
        errorReason: ErrorReason,
        executeMode: KuiklyRenderCoreExecuteMode
    ) {
    }

    /**
     * Page load callback
     *
     * @param isSucceed Whether successful
     * @param errorReason Failure reason
     * @param executeMode Execution mode
     */
    fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason? = null,
        executeMode: KuiklyRenderCoreExecuteMode
    ) {
    }
}

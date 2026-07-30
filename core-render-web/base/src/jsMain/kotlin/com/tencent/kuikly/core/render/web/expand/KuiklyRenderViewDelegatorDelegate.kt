@file:JsExport

package com.tencent.kuikly.core.render.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
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
@JsExport
@JsName("KuiklyRenderViewDelegatorDelegate")
@OptIn(ExperimentalJsExport::class)
interface KuiklyRenderViewDelegatorDelegate {
    /**
     * For business to register renderView and shadow
     */
    @JsName("registerExternalRenderView")
    fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * For business to register Module
     */
    @JsName("registerExternalModule")
    fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * For business to inject custom property handler for View
     */
    @JsName("registerViewExternalPropHandler")
    fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * KuiklyRenderCore execution mode, default is JS mode
     * @return Execution mode
     */
    @JsName("coreExecuteMode")
    fun coreExecuteMode(): KuiklyRenderCoreExecuteMode = KuiklyRenderCoreExecuteMode.JS

    /**
     * Performance monitoring options, only monitoring enabled by default
     */
    @JsName("performanceMonitorTypes")
    fun performanceMonitorTypes(): List<KRMonitorType> = listOf(KRMonitorType.LAUNCH)

    /**
     * KuiklyRenderView creation callback
     */
    @JsName("onKuiklyRenderViewCreated")
    fun onKuiklyRenderViewCreated() {}

    /**
     * Callback when KuiklyRenderView's child View is created
     */
    @JsName("onKuiklyRenderContentViewCreated")
    fun onKuiklyRenderContentViewCreated() {}

    /**
     * Whether first screen is synchronous rendering (synchronous by default)
     */
    @JsName("syncRenderingWhenPageAppear")
    fun syncRenderingWhenPageAppear(): Boolean = true

    /**
     * Launch data callback
     */
    @JsName("onGetLaunchData")
    fun onGetLaunchData(data: KRLaunchData) {}

    /**
     * Performance data callback
     */
    @JsName("onGetPerformanceData")
    fun onGetPerformanceData(data: KRPerformanceData) {}

    /**
     * Exception callback
     *
     * @param throwable Exception
     * @param errorReason Failure reason
     * @param executeMode Execution mode
     */
    @JsName("onUnhandledException")
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
    @JsName("onPageLoadComplete")
    fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason? = null,
        executeMode: KuiklyRenderCoreExecuteMode
    ) {
    }
}

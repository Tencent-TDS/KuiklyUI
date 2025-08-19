package com.tencent.kuikly.core.render.web.runtime.web.expand

import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.ktx.SizeI

/**
 * Kuikly View granularity level integration class, if it's page-level integration, use [KuiklyRenderViewDelegator]
 */
interface IKuiklyView {
    /**
     * Initialize KuiklyView
     *
     * @param container DOM container instance that hosts the kuikly view
     * @param pageName Page name
     * @param pageData Parameters passed to the kuikly view
     * @param size View size
     */
    fun onAttach(
        container: Any,
        pageName: String,
        pageData: Map<String, Any>,
        size: SizeI,
    )

    /**
     * View not visible, called when Activity onPause is triggered
     */
    fun onPause()

    /**
     * View visible, called when View becomes visible, generally called during Activity onResume
     */
    fun onResume()

    /**
     * Release internal resources of KuiklyView, called when KuiklyView is destroyed, generally called during Activity onResume or when KuiklyView is removed
     */
    fun onDetach()

    /**
     * Send Native events to Kuikly page
     * @param event Event name
     * @param data Event data
     */
    fun sendEvent(event: String, data: Map<String, Any>)


    /**
     * Register [ KuiklyRenderView ] lifecycle callback
     * @param callback Lifecycle callback
     */
    fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback)

    /**
     * Unregister [ KuiklyRenderView ] lifecycle callback
     * @param callback Lifecycle callback
     */
    fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback)
}

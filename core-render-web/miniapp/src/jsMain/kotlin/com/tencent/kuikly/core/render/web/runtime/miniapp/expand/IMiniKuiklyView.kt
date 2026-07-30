package com.tencent.kuikly.core.render.web.runtime.miniapp.expand

import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.ktx.SizeI

/**
 * Kuikly View granular level access class, if it's page level access, use [ KuiklyRenderViewDelegator ]
 */
interface IMiniKuiklyView {
    /**
     * Initialize KuiklyView
     *
     * @param size View size
     * @param renderParams Parameters passed to kuikly view
     */
    fun onAttach(
        size: SizeI?,
        renderParams: FastMutableMap<String, Any>
    )

    /**
     * View becomes invisible, called at Activity onPause timing
     */
    fun onPause()

    /**
     * View becomes visible, called when View is visible, usually at Activity onResume timing
     */
    fun onResume()

    /**
     * Release internal resources of KuiklyView, called when KuiklyView is destroyed, usually at Activity onResume timing or when KuiklyView is removed
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

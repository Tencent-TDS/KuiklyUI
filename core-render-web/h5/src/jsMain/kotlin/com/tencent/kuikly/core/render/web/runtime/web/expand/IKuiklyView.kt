@file:JsExport

package com.tencent.kuikly.core.render.web.runtime.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.ktx.SizeI

/**
 * Kuikly View granularity level integration class, if it's page-level integration, use [KuiklyRenderViewDelegator]
 */
@JsExport
@JsName("IKuiklyView")
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
     * Imperatively update the root view size of the Kuikly page.
     *
     * Use this when the host container has been resized outside of a plain
     * `window.resize` (e.g. sidebar collapsed, split pane dragged, custom
     * responsive breakpoint), so that Kuikly's Pager can relayout children.
     *
     * The WebRender can also do this automatically for desktop; see
     * `KuiklyProcessor.autoUpdateRootViewSizeOnResize`.
     *
     * @param width  New root view width in pixels
     * @param height New root view height in pixels
     */
    fun updateRootViewSize(width: Int, height: Int)


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

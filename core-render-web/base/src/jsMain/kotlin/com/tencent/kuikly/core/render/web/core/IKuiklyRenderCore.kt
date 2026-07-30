package com.tencent.kuikly.core.render.web.core

import com.tencent.kuikly.core.render.web.IKuiklyRenderView
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import org.w3c.dom.Element

/**
 * Kuikly web view core
 */
interface IKuiklyRenderCore {
    /**
     * Initialize RenderCore
     *
     * @param renderView Root view of kuikly page
     * @param url Page url
     * @param params Page initialization parameters
     * @param contextInitCallback Callback for rendering environment initialization process
     */
    fun init(
        renderView: IKuiklyRenderView,
        url: String,
        params: Map<String, Any>,
        contextInitCallback: IKuiklyRenderContextInitCallback
    )

    /**
     * Send events from Native to kuikly page
     */
    fun sendEvent(event: String, data: Map<String, Any>)

    /**
     * Get kuikly module
     *
     * @param name Module name
     */
    fun <T : IKuiklyRenderModuleExport> module(name: String): T?

    /**
     * Destroy kuikly render core
     */
    fun destroy()

    /**
     * Get [Element] by tag
     *
     * @param tag Tag corresponding to [Element]
     */
    fun getView(tag: Int): Element?
}

/**
 * Callback for rendering environment initialization process
 */
interface IKuiklyRenderContextInitCallback {

    /**
     * Called when initialization of rendering environment starts
     */
    fun onStart()

    /**
     * Called when initialization of rendering environment completes
     */
    fun onFinish()

    /**
     * Called when starting to create page instance in kotlin
     */
    fun onCreateInstanceStart()

    /**
     * Called when page instance creation in kotlin completes
     */
    fun onCreateInstanceFinish()
}

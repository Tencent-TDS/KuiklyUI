package com.tencent.kuikly.core.render.web

import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.SizeI
import org.w3c.dom.Element

/**
 * Represents the root View interface for Kuikly pages. Views implementing this interface are considered root Views
 * for Kuikly pages.
 * External interactions and communication with Kuikly pages are handled through Views implementing this interface.
 */
interface IKuiklyRenderView {
    // View container
    val view: Element

    // Kuikly render class exposed to the page side
    val kuiklyRenderExport: IKuiklyRenderExport

    // Rendering view context object
    val kuiklyRenderContext: IKuiklyRenderContext

    /**
     * Initialize kuiklyRenderView
     *
     * @param rootContainer Root container id or Element
     * @param pageName Page name
     * @param params Page parameters
     * @param size View size
     */
    fun init(
        rootContainer: Any,
        pageName: String,
        params: Map<String, Any>,
        size: SizeI,
    )

    /**
     * Called after RenderView is fully created
     */
    fun didCreateRenderView()

    /**
     * Send Native events to kuikly page
     *
     * @param event Event name sent to kuikly side
     * @param data Event data sent to kuikly side
     */
    fun sendEvent(event: String, data: Map<String, Any>)

    /**
     * Get [KuiklyRenderBaseModule]
     * @param T Module type
     * @param name Module name
     * @return Subclass of [KuiklyRenderBaseModule]
     */
    fun <T : KuiklyRenderBaseModule> module(name: String): T?

    /**
     * View becomes visible
     */
    fun resume()

    /**
     * View becomes invisible
     */
    fun pause()

    /**
     * Destroy rendering view
     */
    fun destroy()

    /**
     * Synchronize layout and rendering
     */
    fun syncFlushAllRenderTasks()

    /**
     * Get [Element] by tag
     *
     * @param tag Tag corresponding to [Element]
     */
    fun getView(tag: Int): Element?
}

/**
 * Kuikly Render View object associated context, used to get context or associate data with View object
 */
interface IKuiklyRenderContext {
    // Get IKuiklyRenderView object
    val kuiklyRenderRootView: IKuiklyRenderView?

    /**
     * Get data associated with view by key
     *
     * @param T Associated data type
     * @param ele View object
     * @param key Key corresponding to the data
     */
    fun <T> getViewData(ele: Element, key: String): T?

    /**
     * Associate data with view object
     *
     * @param ele View object
     * @param key Key for data association
     * @param data Data to be associated
     */
    fun putViewData(ele: Element, key: String, data: Any)

    /**
     * Remove data associated with view
     */
    fun <T> removeViewData(ele: Element, key: String): T?

    /**
     * Get kuikly module
     *
     * @param T Module data type
     * @param name Module name
     */
    fun <T : KuiklyRenderBaseModule> module(name: String): T?

    /**
     * Get [Element] by tag
     *
     * @param tag Tag corresponding to [Element]
     */
    fun getView(tag: Int): Element?
}

/**
 * Get exposed classes for Kuikly pages, which can be exposed to Kuikly side are:
 * IKuiklyRenderViewExport, IKuiklyRenderModuleExport and IKuiklyRenderShadowExport
 */
interface IKuiklyRenderExport: IKuiklyRenderViewPropExternalHandler {
    /**
     * Register and expose module to kuikly pages
     *
     * @param name Module name
     * @param creator Module creation closure
     */
    fun moduleExport(name: String, creator: () -> IKuiklyRenderModuleExport)

    /**
     * Register and expose renderView and shadow to kuikly pages
     *
     * @param viewName View name
     * @param renderViewExportCreator Closure to create renderView
     * @param shadowExportCreator Closure to create shadow
     */
    fun renderViewExport(
        viewName: String,
        renderViewExportCreator: () -> IKuiklyRenderViewExport,
        shadowExportCreator: (() -> IKuiklyRenderShadowExport)? = null,
    )

    /**
     * Create kuikly module
     *
     * @param name Name corresponding to the module
     */
    fun createModule(name: String): IKuiklyRenderModuleExport

    /**
     * Create kuikly renderView export
     *
     * @param name Name corresponding to renderView
     */
    fun createRenderView(name: String): IKuiklyRenderViewExport

    /**
     * Create shadowExport corresponding to renderView
     *
     * @param name Shadow name
     */
    fun createRenderShadow(name: String): IKuiklyRenderShadowExport

    /**
     * add View attribute custom handler
     * @param handler  custom handler
     */
    fun viewPropExternalHandlerExport(handler: IKuiklyRenderViewPropExternalHandler)
}

/**
 * RenderView lifecycle callback
 */
interface IKuiklyRenderViewLifecycleCallback {

    /**
     * Start
     */
    fun onInit()

    /**
     * Preloading of class completed in Dex mode
     */
    fun onPreloadDexClassFinish()

    /**
     * renderCore initialization started
     */
    fun onInitCoreStart()

    /**
     * renderCore initialization completed
     */
    fun onInitCoreFinish()

    /**
     * Rendering environment initialization started
     */
    fun onInitContextStart()

    /**
     * Rendering environment initialization completed
     */
    fun onInitContextFinish()

    /**
     * Page creation started
     */
    fun onCreateInstanceStart()

    /**
     * Page creation completed
     */
    fun onCreateInstanceFinish()

    /**
     * First frame rendering completed
     */
    fun onFirstFramePaint()

    /**
     * View becomes visible
     */
    fun onResume()

    /**
     * View becomes invisible
     */
    fun onPause()

    /**
     * Page exit
     */
    fun onDestroy()

    /**
     * Rendering error
     */
    fun onRenderException(throwable: Throwable, errorReason: ErrorReason)
}

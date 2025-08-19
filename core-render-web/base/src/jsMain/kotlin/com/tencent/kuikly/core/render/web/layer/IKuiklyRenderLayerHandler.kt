package com.tencent.kuikly.core.render.web.layer

import com.tencent.kuikly.core.render.web.IKuiklyRenderView
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.SizeF
import org.w3c.dom.Element

/**
 * Rendering layer handler interface, mainly used to handle view, shadow and module operations
 */
interface IKuiklyRenderLayerHandler {
    /**
     * Initialize, cache root View
     */
    fun init(renderView: IKuiklyRenderView)

    /**
     * Create rendering View
     */
    fun createRenderView(tag: Int, viewName: String)

    /**
     * Remove rendering View
     */
    fun removeRenderView(tag: Int)

    /**
     * Insert child rendering View into parent node
     */
    fun insertSubRenderView(parentTag: Int, childTag: Int, index: Int)

    /**
     * Set rendering View properties
     */
    fun setProp(tag: Int, propKey: String, propValue: Any)

    /**
     * Set Shadow View
     */
    fun setShadow(tag: Int, shadow: IKuiklyRenderShadowExport)

    /**
     * Set rendering View position and size data
     */
    fun setRenderViewFrame(tag: Int, frame: Frame)

    /**
     * Calculate rendering View size data
     */
    fun calculateRenderViewSize(tag: Int, constraintSize: SizeF): SizeF

    /**
     * Call rendering View provided methods
     */
    fun callViewMethod(
        tag: Int,
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    )

    /**
     * Call Module provided methods
     */
    fun callModuleMethod(
        moduleName: String,
        method: String,
        params: Any?,
        callback: KuiklyRenderCallback?
    ): Any?

    /**
     * Create Shadow View
     */
    fun createShadow(tag: Int, viewName: String)

    /**
     * Remove Shadow View
     */
    fun removeShadow(tag: Int)

    /**
     * Set Shadow View properties
     */
    fun setShadowProp(tag: Int, propKey: String, propValue: Any)

    /**
     * Get Shadow View handler
     */
    fun shadow(tag: Int): IKuiklyRenderShadowExport?

    /**
     * Call Shadow View provided methods
     */
    fun callShadowMethod(tag: Int, method: String, params: String): Any?

    /**
     * Get module
     */
    fun <T : IKuiklyRenderModuleExport> module(name: String): T?

    /**
     * Destroy execution environment
     */
    fun onDestroy()

    /**
     * Get child View actual instance
     */
    fun getView(tag: Int): Element?
}

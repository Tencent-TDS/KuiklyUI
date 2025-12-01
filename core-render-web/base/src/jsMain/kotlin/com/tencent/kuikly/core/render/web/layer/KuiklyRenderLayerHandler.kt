package com.tencent.kuikly.core.render.web.layer

import com.tencent.kuikly.core.render.web.IKuiklyRenderView
import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.removeLast
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.remove
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.SizeF
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.get

/**
 * View rendering protocol layer implementation class, mainly used to handle view, shadow and module cache maps
 */
class KuiklyRenderLayerHandler : IKuiklyRenderLayerHandler {
    // View instance reference
    private var renderView: IKuiklyRenderView? = null

    // Cache created renderViewHandler
    private val renderViewRegistry = JsMap<Int, RenderViewHandler>()

    // Cache created shadowExport
    private var shadowRegistry: JsMap<Int, IKuiklyRenderShadowExport>? = null

    // Cache created moduleExport
    private val moduleRegistry = JsMap<String, IKuiklyRenderModuleExport>()

    // renderViewHandler renderView reuse queue map
    private val renderViewReuseQueue = JsMap<String, JsArray<RenderViewHandler>>()

    /**
     * Initialize, cache root View
     */
    override fun init(renderView: IKuiklyRenderView) {
        this.renderView = renderView
    }

    /**
     * Create rendering View
     */
    override fun createRenderView(tag: Int, viewName: String) {
        createRenderViewHandler(tag, viewName)
    }

    /**
     * Remove rendering View
     */
    override fun removeRenderView(tag: Int) {
        innerRemoveRenderView(tag)
    }

    /**
     * Insert child rendering View into parent node
     */
    override fun insertSubRenderView(parentTag: Int, childTag: Int, index: Int) {
        // Is root node
        val isRootViewTag = parentTag == ROOT_VIEW_TAG
        // Get parent element node, if failed, return directly without insertion
        val parentEle = if (isRootViewTag) {
            renderView?.view ?: return
        } else {
            getRenderViewHandler(parentTag)?.viewExport?.ele ?: return
        }
        // Get child View to insert, if failed do not execute insertion
        val viewExport = getRenderViewHandler(childTag)?.viewExport ?: return
        // Actual DOM element to insert
        val childView = viewExport.ele
        // Get reference node for insertion, if index provided, get element at index position
        val referenceEle: Node? = if (index > 0) {
            val size = parentEle.childNodes.length
            if (index < size) {
                parentEle.childNodes[index]
            } else {
                null
            }
        } else {
            null
        }
        // Call insertBefore if reference node exists, otherwise call appendChild
        if (referenceEle != null) {
            parentEle.insertBefore(childView, referenceEle)
        } else {
            parentEle.appendChild(childView)
        }
        // Callback after element insertion
        viewExport.onAddToParent(parentEle)
    }

    /**
     * Set rendering View properties
     */
    override fun setProp(tag: Int, propKey: String, propValue: Any) {
        getRenderViewHandler(tag)?.viewExport?.also {
            var process = it.setProp(propKey, propValue)
            if (!process) {
                process = renderView?.kuiklyRenderExport?.setViewExternalProp(it,
                    propKey,
                    propValue) ?: false
            }
            if (it.reusable && process) {
                // If element is reusable and operation successful, record the operation, web currently has no reusable elements
                recordSetPropOperation(it.ele, propKey)
            }
        }
    }

    /**
     * Set Shadow View
     */
    override fun setShadow(tag: Int, shadow: IKuiklyRenderShadowExport) {
        getRenderViewHandler(tag)?.viewExport?.setShadow(shadow)
    }

    /**
     * Set rendering View position and size data
     */
    override fun setRenderViewFrame(tag: Int, frame: Frame) {
        getRenderViewHandler(tag)?.viewExport?.setProp(KRCssConst.FRAME, frame)
    }

    /**
     * Calculate rendering View size data
     */
    override fun calculateRenderViewSize(tag: Int, constraintSize: SizeF): SizeF {
        // Calculate constraint layout size, return original size if calculation fails
        return getShadowHandler(tag)?.calculateRenderViewSize(constraintSize)
            ?: return constraintSize
    }

    /**
     * Call rendering View provided methods
     */
    override fun callViewMethod(
        tag: Int,
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    ) {
        getRenderViewHandler(tag)?.viewExport?.call(method, params, callback)
    }

    /**
     * Call Module provided methods
     */
    override fun callModuleMethod(
        moduleName: String,
        method: String,
        params: Any?,
        callback: KuiklyRenderCallback?
    ): Any? = getModuleHandler(moduleName)?.call(method, params, callback)

    /**
     * Create Shadow View
     */
    override fun createShadow(tag: Int, viewName: String) {
        if (shadowRegistry == null) {
            // Initialize shadow registry
            shadowRegistry = JsMap()
        }
        val kuiklyRenderExport = renderView?.kuiklyRenderExport ?: return
        // Create and cache shadow object corresponding to view name
        // i.e. renderView.shadowExportCreator[name]?.invoke()
        shadowRegistry?.set(tag, kuiklyRenderExport.createRenderShadow(viewName))
    }

    /**
     * Remove Shadow View
     */
    override fun removeShadow(tag: Int) {
        // Remove cached shadow object
        shadowRegistry?.remove(tag)
    }

    /**
     * Set Shadow View properties
     */
    override fun setShadowProp(tag: Int, propKey: String, propValue: Any) {
        getShadowHandler(tag)?.setShadowProp(propKey, propValue)
    }

    /**
     * Get Shadow View handler
     */
    override fun shadow(tag: Int): IKuiklyRenderShadowExport? = getShadowHandler(tag)

    /**
     * Call Shadow View provided methods
     */
    override fun callShadowMethod(tag: Int, method: String, params: String): Any? =
        shadow(tag)?.call(method, params)

    /**
     * Get module
     */
    override fun <T : IKuiklyRenderModuleExport> module(name: String): T? =
        getModuleHandler(name).unsafeCast<T?>()

    /**
     * Destroy execution environment
     */
    override fun onDestroy() {
        moduleRegistry.forEach { value, _ ->
            // Destroy all modules
            value.onDestroy()
        }
        renderViewRegistry.forEach { value, _ ->
            // Destroy all views
            value.viewExport.onDestroy()
        }
    }

    /**
     * Get child View actual instance
     */
    override fun getView(tag: Int): Element? = renderViewRegistry[tag]?.viewExport?.ele

    /**
     * Get cached renderView handler
     */
    private fun getRenderViewHandler(tag: Int): RenderViewHandler? = renderViewRegistry[tag]

    /**
     * Get cached shadow handler
     */
    private fun getShadowHandler(tag: Int): IKuiklyRenderShadowExport? = shadowRegistry?.get(tag)

    private fun isShadowViewHybridComponent(viewName: String): Boolean {
        return when (viewName) {
            "KRRichTextView" -> true  // KRRichTextView is a hybrid component
            "KRGradientRichTextView" -> true  // Also a hybrid component
            else -> false
        }
    }

    /**
     * Get cached module handler
     */
    private fun getModuleHandler(moduleName: String): IKuiklyRenderModuleExport? {
        var moduleHandler = moduleRegistry[moduleName]
        if (moduleHandler == null) {
            // If not found in cache, create module handler
            moduleHandler = renderView?.kuiklyRenderExport?.createModule(moduleName)?.apply {
                // Set module rendering context
                kuiklyRenderContext = renderView?.kuiklyRenderContext
                // Cache module
                moduleRegistry[moduleName] = this
            }
        }
        return moduleHandler
    }

    /**
     * Record property operation history for specified element
     */
    private fun recordSetPropOperation(ele: Element, propKey: String) {
        val kuiklyRenderViewContext = renderView?.kuiklyRenderContext ?: return
        val setPropOperationSet =
            kuiklyRenderViewContext.getViewData<MutableSet<String>>(ele, KR_SET_PROP_OPERATION)
                ?: mutableSetOf<String>().apply {
                    kuiklyRenderViewContext.putViewData(ele, KR_SET_PROP_OPERATION, this)
                }
        setPropOperationSet.add(propKey)
    }

    /**
     * Prepare for reuse
     */
    private fun prepareForReuse(viewExport: IKuiklyRenderViewExport) {
        // Remove associated data
        renderView?.kuiklyRenderContext?.removeViewData<MutableSet<String>>(
            viewExport.ele,
            KR_SET_PROP_OPERATION
        )?.also {
            for (propKey in it) {
                // Reset properties
                viewExport.resetProp(propKey)
            }
        }
        // Reset shadow object
        viewExport.resetShadow()
    }

    /**
     * Insert render view handler into reuse queue, since reset related methods are not yet complete, web currently has no reusable views
     * reuse is all false
     */
    private fun pushRenderViewHandlerToReuseQueue(
        viewName: String,
        renderViewHandler: RenderViewHandler,
    ) {
        if (!renderViewHandler.viewExport.reusable) {
            // Must be reusable type
            return
        }
        var reuseQueue = renderViewReuseQueue[viewName]
        if (reuseQueue == null) {
            // If no reuse queue exists for the specified view name, initialize empty queue
            reuseQueue = JsArray()
            renderViewReuseQueue[viewName] = reuseQueue
        }
        if (reuseQueue.length >= MAX_REUSE_COUNT) {
            // Reuse count exceeds limit, skip
            return
        }
        // Prepare for reuse
        prepareForReuse(renderViewHandler.viewExport)
        // Insert into reuse queue
        reuseQueue.add(renderViewHandler)
    }

    /**
     * Get the latest element from render view reuse queue
     */
    private fun popRenderViewHandlerFromReuseQueue(viewName: String): RenderViewHandler? {
        val queue = renderViewReuseQueue[viewName]

        if (queue == null || queue.length == 0) {
            return null
        }
        // Return and remove the last element
        return queue.removeLast()
    }

    /**
     * Create renderView handler logic
     */
    private fun createRenderViewHandler(tag: Int, viewName: String) {
        // Return directly if rootView not initialized
        val renderView: IKuiklyRenderView = this.renderView ?: return

        var renderViewHandler = renderViewRegistry[tag]
        if (renderViewHandler == null) {
            // If view instance not in render view queue, try to get handler from reuse queue
            renderViewHandler = popRenderViewHandlerFromReuseQueue(viewName)
        }
        if (renderViewHandler == null) {
            // If still not found, create new view handler
            var viewExport: IKuiklyRenderViewExport? = null
            
            // Only check shadow for components that are known to be shadow-view hybrids
            // This avoids unnecessary shadow lookups for most components
            if (isShadowViewHybridComponent(viewName)) {
                val shadowHandler = getShadowHandler(tag)
                if (shadowHandler is IKuiklyRenderViewExport) {
                    // Use shadow as view export for hybrid components like KRRichTextView
                    viewExport = shadowHandler
                }
            }
            
            // Create view handler, return initialized handler like KRView() etc. created IKuiklyRenderViewExport instance
            renderViewHandler = RenderViewHandler(
                viewName,
                // Call registered constructor like KRView to create instance, i.e. rootView's viewExportCreator[name]?.invoke()
                viewExport ?: renderView.kuiklyRenderExport.createRenderView(viewName)
            )
        }
        // Set id for all elements to facilitate problem investigation
        if (renderViewHandler.viewExport.ele.id == "") {
            renderViewHandler.viewExport.ele.id = tag.toString()
        }
        // Save render context
        renderViewHandler.viewExport.kuiklyRenderContext = renderView.kuiklyRenderContext
        // After successful creation, cache created renderView handler internally
        renderViewRegistry[tag] = renderViewHandler
    }

    /**
     * Internal actual removal of renderView
     */
    private fun innerRemoveRenderView(tag: Int) {
        val renderViewHandler = getRenderViewHandler(tag)
        renderViewHandler?.viewExport?.also {
            // Insert view into reuse queue
            // fixme Currently web has no reusable views, enabling reuse queue requires completing reset prop, reset shadow related methods, otherwise reuse will have issues
            pushRenderViewHandlerToReuseQueue(renderViewHandler.viewName, renderViewHandler)
            // Remove child node DOM from parent node's real DOM
            it.removeFromParent()
            // Execute destroy node callback
            it.onDestroy()
        }
        // Remove kotlin cached view
        renderViewRegistry.remove(tag)
    }

    companion object {
        private const val ROOT_VIEW_TAG = -1
        private const val KR_SET_PROP_OPERATION = "kr_set_prop_operation"
        private const val MAX_REUSE_COUNT = 50
    }
}

/**
 * [IKuiklyRenderViewExport] wrapper class, used to associate with corresponding viewName
 */
data class RenderViewHandler(
    val viewName: String,
    val viewExport: IKuiklyRenderViewExport,
)

package com.tencent.kuikly.core.render.web

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.remove
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.remove
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode
import com.tencent.kuikly.core.render.web.core.IKuiklyRenderContextInitCallback
import com.tencent.kuikly.core.render.web.core.IKuiklyRenderCore
import com.tencent.kuikly.core.render.web.core.KuiklyRenderCore
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.performance.KRMonitorType
import com.tencent.kuikly.core.render.web.performance.KRPerformanceManager
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit

/**
 * Kuikly rendering logic entry point, called by actual business APP
 */
class KuiklyRenderView(
    private var executeMode: KuiklyRenderCoreExecuteMode = KuiklyRenderCoreExecuteMode.JS,
    private val delegate: KuiklyRenderViewDelegatorDelegate? = null
) : IKuiklyRenderView {
    // Internal view container
    private lateinit var _container: Element

    // Rendering core instance
    private var renderCore: IKuiklyRenderCore? = null

    // Internal context instance
    private val _kuiklyRenderViewContext = KuiklyRenderViewContext(this)

    // Lifecycle listener
    private var lifecycleCallbacks = JsArray<IKuiklyRenderViewLifecycleCallback>()

    // Performance monitoring
    private var performanceManager: KRPerformanceManager? = null

    // Manager exposed to kuikly pages
    private val renderExport = KuiklyRenderExport()

    /**
     * Get actual container element
     */
    override val view: Element
        get() = _container

    /**
     * Get actual rendering context
     */
    override val kuiklyRenderContext: IKuiklyRenderContext
        get() = _kuiklyRenderViewContext

    /**
     * Get actual exposed rendering object
     */
    override val kuiklyRenderExport: IKuiklyRenderExport
        get() = renderExport

    /**
     * Initialize view layer
     */
    override fun init(
        rootContainer: Any,
        pageName: String,
        params: Map<String, Any>,
        size: SizeI,
    ) {
        // ignore flat view for h5 & miniapp
        kuiklyWindow.asDynamic().com?.tencent?.kuikly?.core?.setIsIgnoreRenderViewForFlatLayer?.call(null, true)
        // renderView initialization
        dispatchLifecycleStateChanged(STATE_INIT)
        // Initialize performance manager
        initPerformanceManager(pageName)
        // JS library loaded
        dispatchLifecycleStateChanged(STATE_PRELOAD_DEX_CLASS_FINISH)
        val container = if (rootContainer is String || rootContainer is Number) {
            // string, create dom as rootContainer
            kuiklyDocument.getElementById(rootContainer.toString())
        } else {
            // not string, used as rootContainer
            rootContainer.unsafeCast<Element?>()
        }
        // Finally create and initialize rendering core, after page DOM Ready
        if (container !== null) {
            // Create container for kuikly page
            _container = kuiklyDocument.createElement(ElementType.DIV).apply {
                this.unsafeCast<HTMLElement>().style.position = "relative"
            }
            // Insert container
            container.appendChild(_container)
            // Record first byte on screen when root node inserts elements
            registerFirstFrameLifeCycle()
            // Initialize rendering core
            initRenderCore(pageName, params, size)
        } else {
            // Failed to get, log error
            Log.error("container id: $rootContainer is not found")
        }
    }

    override fun didCreateRenderView() {
        // Not used in web
    }

    /**
     * Send event notification to kotlin side
     */
    override fun sendEvent(event: String, data: Map<String, Any>) {
        renderCore?.sendEvent(event, data)
    }

    override fun resume() {
        sendEvent(VIEW_DID_APPEAR, mapOf(VIEW_DID_APPEAR to VIEW_DID_APPEAR_VALUE))
        dispatchLifecycleStateChanged(STATE_RESUME)
    }

    override fun pause() {
        sendEvent(VIEW_DID_DISAPPEAR, mapOf(VIEW_DID_DISAPPEAR to VIEW_DID_DISAPPEAR_VALUE))
        dispatchLifecycleStateChanged(STATE_PAUSE)
    }

    /**
     * Destroy view
     */
    override fun destroy() {
        dispatchLifecycleStateChanged(STATE_DESTROY)
        renderCore?.destroy()
    }

    override fun syncFlushAllRenderTasks() {
        // Synchronize flush data, todo
    }

    /**
     * Get specified module instance
     */
    override fun <T : KuiklyRenderBaseModule> module(name: String): T? = renderCore?.module(name)

    /**
     * Get current DOM element
     */
    override fun getView(tag: Int): Element? = renderCore?.getView(tag)

    /**
     * Register lifecycle callback method
     */
    fun registerCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }

    /**
     * Remove lifecycle callback method
     */
    fun unregisterCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }

    /**
     * Record first byte on screen (project first byte, not page first byte)
     */
    private fun registerFirstFrameLifeCycle() {
        if (js("typeof MutationObserver") != "undefined") {
            // Root node has inserted data, indicating that the first byte rendering has started,
            // record the time, only record once. web can use this. mini app used other method
            val observer = MutationObserver { mutationRecords, mutationObserver ->
                if (mutationRecords.isNotEmpty()) {
                    // Changes detected, record first frame time point
                    dispatchLifecycleStateChanged(STATE_FIRST_FRAME_PAINT)
                    // Stop listening
                    mutationObserver.disconnect()
                }
            }
            // Monitor root node attribute changes
            observer.observe(view, MutationObserverInit(childList = true))
        }
    }

    /**
     * Get system version information from user agent
     */
    private fun getOSInfo(userAgent: String): String {
        var osInfo = "Unknown OS"

        when {
            "Win" in userAgent -> {
                osInfo = "Windows"
                osInfo += when {
                    "Windows NT 10.0" in userAgent -> " 10"
                    "Windows NT 6.3" in userAgent -> " 8.1"
                    "Windows NT 6.2" in userAgent -> " 8"
                    "Windows NT 6.1" in userAgent -> " 7"
                    "Windows NT 6.0" in userAgent -> " Vista"
                    "Windows NT 5.1" in userAgent -> " XP"
                    else -> ""
                }
            }

            "Mac" in userAgent -> {
                osInfo = "MacOS"
                osInfo += when {
                    "Mac OS X 10_15" in userAgent -> " Catalina"
                    "Mac OS X 10_14" in userAgent -> " Mojave"
                    "Mac OS X 10_13" in userAgent -> " High Sierra"
                    else -> ""
                }
            }

            "X11" in userAgent -> osInfo = "UNIX"
            "Linux" in userAgent -> osInfo = "Linux"
        }

        return osInfo
    }

    /**
     * Assemble parameters for opening kuikly page
     */
    private fun generateWithParams(params: Map<String, Any>, size: SizeI): Map<String, Any> {
        // System initialization information
        val statusBarHeight = params[STATUS_BAR_HEIGHT] ?: 0
        return mutableMapOf<String, Any>().apply {
            // Pass page size
            put(ROOT_VIEW_WIDTH, size.first.toFloat())
            put(ROOT_VIEW_HEIGHT, size.second.toFloat())
            put(PLATFORM, params[PLATFORM] ?: PLATFORM_VALUE)
            put(DEVICE_WIDTH, params[DEVICE_WIDTH] ?: kuiklyWindow.screen.width)
            put(DEVICE_HEIGHT, params[DEVICE_HEIGHT] ?: kuiklyWindow.screen.height)
            put(STATUS_BAR_HEIGHT, statusBarHeight)
            put(OS_VERSION, getOSInfo(kuiklyWindow.navigator.userAgent))
            put(ACTIVITY_WIDTH, params[ACTIVITY_WIDTH] ?: 0)
            put(ACTIVITY_HEIGHT, params[ACTIVITY_HEIGHT] ?: 0)
            put(SAFE_AREA_INSETS, "$statusBarHeight 0 0 0")
            put(APP_VERSION, kuiklyWindow.navigator.appVersion)

            // Page parameters
            put(PARAMS, params[PARAMS] ?: mutableMapOf<String, Any>())
            put(DENSITY, kuiklyWindow.devicePixelRatio)
        }
    }

    /**
     * Initialize rendering core
     */
    private fun initRenderCore(
        pageName: String,
        params: Map<String, Any>,
        size: SizeI,
    ) {
        dispatchLifecycleStateChanged(STATE_INIT_CORE_START)
        // Create and initialize renderCore
        renderCore = createRenderCore().apply {
            init(
                this@KuiklyRenderView,
                pageName,
                generateWithParams(params, size),
                // context lifecycle callback method
                object : IKuiklyRenderContextInitCallback {
                    override fun onStart() {
                        dispatchLifecycleStateChanged(STATE_INIT_CONTEXT_START)
                    }

                    override fun onFinish() {
                        dispatchLifecycleStateChanged(STATE_INIT_CONTEXT_FINISH)
                    }

                    override fun onCreateInstanceStart() {
                        dispatchLifecycleStateChanged(STATE_CREATE_INSTANCE_START)
                    }

                    override fun onCreateInstanceFinish() {
                        dispatchLifecycleStateChanged(STATE_CREATE_INSTANCE_FINISH)
                    }
                }
            )
        }
        dispatchLifecycleStateChanged(STATE_INIT_CORE_FINISH)
    }

    /**
     * Create renderCore instance
     */
    private fun createRenderCore(): IKuiklyRenderCore = KuiklyRenderCore()

    /**
     * Initialize performance data manager and return corresponding instance
     */
    private fun initPerformanceManager(pageName: String): KRPerformanceManager? {
        // Currently only supports startup data and memory data (memory only supports chrome)
        performanceManager = KRPerformanceManager(
            pageName,
            KuiklyRenderCoreExecuteMode.JS,
            listOf(KRMonitorType.LAUNCH, KRMonitorType.MEMORY)
        )
        return performanceManager
    }

    /**
     * Dispatch renderView lifecycle state change event
     */
    private fun dispatchLifecycleStateChanged(state: Int) {
        when (state) {
            STATE_INIT -> {
                lifecycleCallbacks.forEach { it ->
                    it.onInit()
                }
            }

            STATE_PRELOAD_DEX_CLASS_FINISH -> {
                lifecycleCallbacks.forEach { it ->
                    it.onPreloadDexClassFinish()
                }
            }

            STATE_INIT_CORE_START -> {
                lifecycleCallbacks.forEach { it ->
                    it.onInitCoreStart()
                }
            }

            STATE_INIT_CORE_FINISH -> {
                lifecycleCallbacks.forEach { it ->
                    it.onInitCoreFinish()
                }
            }

            STATE_INIT_CONTEXT_START -> {
                lifecycleCallbacks.forEach { it ->
                    it.onInitContextStart()
                }
            }

            STATE_INIT_CONTEXT_FINISH -> {
                lifecycleCallbacks.forEach { it ->
                    it.onInitContextFinish()
                }
            }

            STATE_CREATE_INSTANCE_START -> {
                lifecycleCallbacks.forEach { it ->
                    it.onCreateInstanceStart()
                }
            }

            STATE_CREATE_INSTANCE_FINISH -> {
                lifecycleCallbacks.forEach { it ->
                    it.onCreateInstanceFinish()
                }
            }

            STATE_FIRST_FRAME_PAINT -> {
                lifecycleCallbacks.forEach { it ->
                    it.onFirstFramePaint()
                }
            }

            STATE_RESUME -> {
                lifecycleCallbacks.forEach { it ->
                    it.onResume()
                }
            }

            STATE_PAUSE -> {
                lifecycleCallbacks.forEach { it ->
                    it.onPause()
                }
            }

            STATE_DESTROY -> {
                lifecycleCallbacks.forEach { it ->
                    it.onDestroy()
                }
            }
        }
    }

    companion object {
        // pageData passed in parameters
        private const val ROOT_VIEW_WIDTH = "rootViewWidth"
        private const val ROOT_VIEW_HEIGHT = "rootViewHeight"
        private const val STATUS_BAR_HEIGHT = "statusBarHeight"
        private const val DEVICE_WIDTH = "deviceWidth"
        private const val DEVICE_HEIGHT = "deviceHeight"
        private const val APP_VERSION = "appVersion"
        private const val PLATFORM = "platform"
        private const val NATIVE_BUILD = "nativeBuild"
        private const val SAFE_AREA_INSETS = "safeAreaInsets"
        private const val ACTIVITY_WIDTH = "activityWidth"
        private const val ACTIVITY_HEIGHT = "activityHeight"
        private const val OS_VERSION = "osVersion"
        private const val ANDROID_BOTTOM_NAV_BAR_HEIGHT = "androidBottomNavBarHeight"
        private const val DENSITY = "density"
        private const val PLATFORM_VALUE = "web"
        private const val PARAMS = "param"

        private const val VIEW_DID_DISAPPEAR = "viewDidDisappear"
        private const val VIEW_DID_DISAPPEAR_VALUE = "1"

        private const val VIEW_DID_APPEAR = "viewDidAppear"
        private const val VIEW_DID_APPEAR_VALUE = "1"

        const val PAGER_EVENT_FIRST_FRAME_PAINT = "pageFirstFramePaint"

        // RenderView lifecycle state
        private const val STATE_INIT = 0
        private const val STATE_PRELOAD_DEX_CLASS_FINISH = STATE_INIT + 1
        private const val STATE_INIT_CORE_START = STATE_PRELOAD_DEX_CLASS_FINISH + 1
        private const val STATE_INIT_CORE_FINISH = STATE_INIT_CORE_START + 1
        private const val STATE_INIT_CONTEXT_START = STATE_INIT_CORE_FINISH + 1
        private const val STATE_INIT_CONTEXT_FINISH = STATE_INIT_CONTEXT_START + 1
        private const val STATE_CREATE_INSTANCE_START = STATE_INIT_CONTEXT_FINISH + 1
        private const val STATE_CREATE_INSTANCE_FINISH = STATE_CREATE_INSTANCE_START + 1
        private const val STATE_FIRST_FRAME_PAINT = STATE_CREATE_INSTANCE_FINISH + 1
        private const val STATE_RESUME = STATE_FIRST_FRAME_PAINT + 1
        private const val STATE_PAUSE = STATE_RESUME + 1
        private const val STATE_DESTROY = STATE_PAUSE + 1

        // kuikly page name prefix
        private const val PAGE_NAME_PREFIX = "kuikly_page_"
    }
}

/**
 * Kuikly Render View's Context
 */
class KuiklyRenderViewContext(private val kuiklyRenderView: IKuiklyRenderView) :
    IKuiklyRenderContext {
    // View associated data
    private val viewData = JsMap<Int, JsMap<String, Any>>()

    override val kuiklyRenderRootView: IKuiklyRenderView
        get() = kuiklyRenderView

    @Suppress("UNCHECKED_CAST")
    override fun <T> getViewData(ele: Element, key: String): T?
    // Current renderView's all sub view data will be saved here, each view's element hashCode is unique
            = viewData[ele.hashCode()]?.get(key) as? T?

    override fun putViewData(ele: Element, key: String, data: Any) {
        val hashCode = ele.hashCode()
        val map = viewData[hashCode] ?: JsMap<String, Any>().apply {
            // If current element doesn't have value, use empty map to initialize
            viewData[hashCode] = this
        }
        // Assign value
        map[key] = data
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> removeViewData(ele: Element, key: String): T? =
        viewData[ele.hashCode()]?.remove(key) as? T?

    override fun <T : KuiklyRenderBaseModule> module(name: String): T? =
        kuiklyRenderView.module(name)

    override fun getView(tag: Int): Element? = kuiklyRenderView.getView(tag)
}

/**
 * Actual register all Render exposed objects, including view, module and shadow
 */
class KuiklyRenderExport : IKuiklyRenderExport {
    // RenderView creator
    private val viewExportCreator = JsMap<String, () -> IKuiklyRenderViewExport>()

    // Module creator
    private val moduleExportCreator = JsMap<String, () -> IKuiklyRenderModuleExport>()

    // Shadow creator
    private val shadowExportCreator = JsMap<String, () -> IKuiklyRenderShadowExport>()

    private val viewPropExternalHandlerList = mutableListOf<IKuiklyRenderViewPropExternalHandler>()

    override fun moduleExport(name: String, creator: () -> IKuiklyRenderModuleExport) {
        // Cache registered module constructor
        moduleExportCreator[name] = creator
    }

    override fun renderViewExport(
        viewName: String,
        renderViewExportCreator: () -> IKuiklyRenderViewExport,
        shadowExportCreator: (() -> IKuiklyRenderShadowExport)?
    ) {
        // Cache registered renderView constructor
        viewExportCreator[viewName] = renderViewExportCreator
        if (shadowExportCreator !== null) {
            // Cache registered shadowView constructor
            this.shadowExportCreator[viewName] = shadowExportCreator
        }
    }

    override fun createModule(name: String): IKuiklyRenderModuleExport {
        // Return new created module instance
        return moduleExportCreator[name]?.invoke()
            ?: throw IllegalStateException("can not find moduleName: $name")
    }

    override fun createRenderView(name: String): IKuiklyRenderViewExport {
        // Return new created View instance
        return viewExportCreator[name]?.invoke()
            ?: throw IllegalStateException("can not find viewName: $name")
    }

    override fun createRenderShadow(name: String): IKuiklyRenderShadowExport {
        // Return new created shadowView instance
        return shadowExportCreator[name]?.invoke()
            ?: throw IllegalStateException("can not find shadowName: $name")
    }

    override fun viewPropExternalHandlerExport(handler: IKuiklyRenderViewPropExternalHandler) {
        viewPropExternalHandlerList.add(handler)
    }

    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        for (handler in viewPropExternalHandlerList) {
            if (handler.setViewExternalProp(renderViewExport, propKey, propValue)) {
                return true
            }
        }
        return false
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        for (handler in viewPropExternalHandlerList) {
            if (handler.resetViewExternalProp(renderViewExport, propKey)) {
                return true
            }
        }
        return false
    }
}

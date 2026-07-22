@file:JsExport

package com.tencent.kuikly.core.render.web.runtime.web.expand

import kotlin.js.JsExport
import kotlin.js.JsName
import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.KuiklyRenderView
import com.tencent.kuikly.core.render.web.context.KuiklyRenderCoreExecuteMode
import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewPendingTask
import com.tencent.kuikly.core.render.web.expand.components.KRActivityIndicatorView
import com.tencent.kuikly.core.render.web.expand.components.KRBlurView
import com.tencent.kuikly.core.render.web.expand.components.KRCanvasView
import com.tencent.kuikly.core.render.web.expand.components.KRHoverView
import com.tencent.kuikly.core.render.web.expand.components.KRImageView
import com.tencent.kuikly.core.render.web.expand.components.KRMaskView
import com.tencent.kuikly.core.render.web.expand.components.KRPagView
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.expand.components.KRScrollContentView
import com.tencent.kuikly.core.render.web.expand.components.KRTextAreaView
import com.tencent.kuikly.core.render.web.expand.components.KRTextFieldView
import com.tencent.kuikly.core.render.web.expand.components.KRVideoView
import com.tencent.kuikly.core.render.web.expand.components.KRView
import com.tencent.kuikly.core.render.web.expand.components.list.KRListView
import com.tencent.kuikly.core.render.web.expand.module.KRCalendarModule
import com.tencent.kuikly.core.render.web.expand.module.KRCodecModule
import com.tencent.kuikly.core.render.web.expand.module.KRLogModule
import com.tencent.kuikly.core.render.web.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.web.expand.module.KRNetworkModule
import com.tencent.kuikly.core.render.web.expand.module.KRNotifyModule
import com.tencent.kuikly.core.render.web.expand.module.KRPerformanceModule
import com.tencent.kuikly.core.render.web.expand.module.KRRouterModule
import com.tencent.kuikly.core.render.web.expand.module.KRSharedPreferencesModule
import com.tencent.kuikly.core.render.web.expand.module.KRSnapshotModule
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.performance.IKRMonitorCallback
import com.tencent.kuikly.core.render.web.performance.KRPerformanceData
import com.tencent.kuikly.core.render.web.performance.KRPerformanceManager
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.module.H5WindowResizeModule
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.AnimationProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.EventProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.ImageProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.ListProcessor
import com.tencent.kuikly.core.render.web.runtime.web.expand.processor.RichTextProcessor
import com.tencent.kuikly.core.render.web.utils.Log
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.Date

/**
 * Host project can simplify KuiklyRenderCore integration through this class, which is integrated at page granularity
 */
@JsExport
@JsName("KuiklyRenderViewDelegator")
class KuiklyRenderViewDelegator(private val delegate: KuiklyRenderViewDelegatorDelegate) {
    // The root renderView of the kuikly page
    private var renderView: KuiklyRenderView? = null

    // Performance monitoring
    private var performanceManager: KRPerformanceManager? = null

    // Page parameters
    private var pageData: Map<String, Any>? = null

    // Page name
    private var pageName: String? = null

    // Page root container
    private var rootContainer: Any = ""

    // Execution mode
    private var executeMode = KuiklyRenderCoreExecuteMode.JS

    // Whether page loading is complete
    private var isLoadFinish = false

    // Pending task list
    private val pendingTaskList by lazy {
        mutableListOf<KuiklyRenderViewPendingTask>()
    }

    // renderView lifecycle callback
    private val renderViewCallback = object : IKuiklyRenderViewLifecycleCallback {

        override fun onInit() {
            performanceManager?.onInit()
        }

        override fun onPreloadDexClassFinish() {
            performanceManager?.onPreloadDexClassFinish()
        }

        override fun onInitCoreStart() {
            performanceManager?.onInitCoreStart()
        }

        override fun onInitCoreFinish() {
            performanceManager?.onInitCoreFinish()
        }

        override fun onInitContextStart() {
            performanceManager?.onInitContextStart()
        }

        override fun onInitContextFinish() {
            performanceManager?.onInitContextFinish()
        }

        override fun onCreateInstanceStart() {
            performanceManager?.onCreateInstanceStart()
        }

        override fun onCreateInstanceFinish() {
            performanceManager?.onCreateInstanceFinish()
        }

        override fun onFirstFramePaint() {
            isLoadFinish = true
            delegate.onKuiklyRenderContentViewCreated()
            performanceManager?.onFirstFramePaint()
            delegate.onPageLoadComplete(true, executeMode = executeMode)
            sendEvent(KuiklyRenderView.PAGER_EVENT_FIRST_FRAME_PAINT, mapOf())
        }

        override fun onResume() {
            performanceManager?.onResume()
        }

        override fun onPause() {
            performanceManager?.onPause()
        }

        override fun onDestroy() {
            performanceManager?.onDestroy()
        }

        override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {
            performanceManager?.onRenderException(throwable, errorReason)
            handleException(throwable, errorReason)
        }

    }

    /**
     * Called when the page starts
     *
     * @param container Kuikly root View container id or dom element
     * @param pageName Page name
     * @param pageData Page data
     * @param size Root View size
     */
    fun onAttach(
        container: Any,
        pageName: String,
        pageData: Map<String, Any>,
        size: SizeI,
    ) {
        // Initialize kuikly global object
        initGlobalObject()
        // Initialize related parameters
        executeMode = delegate.coreExecuteMode()
        performanceManager = initPerformanceManager(pageName)
        this.rootContainer = container
        this.pageName = pageName
        this.pageData = pageData

        // inject host api and object
        injectHostFunc()
        // Initialize KuiklyRenderView
        loadingKuiklyRenderView(size)
        // If enabled, start forwarding container / window resize as
        // `rootViewSizeDidChanged` so pages relayout responsively.
        maybeStartAutoResizeForwarder(size)
    }

    /**
     * Called when page onDestroy
     */
    fun onDetach() {
        // Stop auto resize forwarder before destroying renderView.
        stopAutoResizeForwarder()
        runKuiklyRenderViewTask {
            it.destroy()
        }
    }

    /**
     * Called when page onPause
     */
    fun onPause() {
        runKuiklyRenderViewTask {
            it.pause()
        }
    }

    /**
     * Called when page onResume
     */
    fun onResume() {
        runKuiklyRenderViewTask {
            it.resume()
        }
    }

    /**
     * Called when page onFontLoaded
     */
    fun onFontLoaded() {
        runKuiklyRenderViewTask {
            it.sendEvent(
                KuiklyRenderView.PAGER_EVENT_ON_FONT_LOADED,
                mapOf()
            )
        }
    }

    /**
     * Send events to Kuikly page
     */
    fun sendEvent(event: String, data: Map<String, Any>) {
        runKuiklyRenderViewTask {
            it.sendEvent(event, data)
        }
    }

    /**
     * Imperatively update the root view size. This forwards a
     * `rootViewSizeDidChanged` event to the Kuikly Pager so pages relayout
     * responsively. Safe to call any time after [onAttach]; if the renderView
     * has not been created yet the call is queued as a pending task.
     *
     * See also `KuiklyProcessor.autoUpdateRootViewSizeOnResize` for the
     * built-in automatic forwarder.
     */
    fun updateRootViewSize(width: Int, height: Int) {
        runKuiklyRenderViewTask {
            it.updateRootViewSize(width, height)
        }
        // Keep the last dispatched size in sync so the auto forwarder does
        // not immediately fire again for the same size.
        lastDispatchedWidth = width
        lastDispatchedHeight = height
    }

    /**
     * Register [KuiklyRenderView] lifecycle callback
     * @param callback Lifecycle callback
     */
    fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        runKuiklyRenderViewTask {
            it.registerCallback(callback)
        }
    }

    /**
     * Unregister [KuiklyRenderView] lifecycle callback
     * @param callback Lifecycle callback
     */
    fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        runKuiklyRenderViewTask {
            it.unregisterCallback(callback)
        }
    }

    /**
     * Get KuiklyRenderContext
     */
    fun getKuiklyRenderContext(): IKuiklyRenderContext? = renderView?.kuiklyRenderContext

    private fun runKuiklyRenderViewTask(task: KuiklyRenderViewPendingTask) {
        val rv = renderView
        if (rv != null) {
            task.invoke(rv)
        } else {
            pendingTaskList.add(task)
        }
    }

    private fun tryRunKuiklyRenderViewPendingTask(kuiklyRenderView: KuiklyRenderView?) {
        kuiklyRenderView?.also { hrv ->
            pendingTaskList.forEach { task ->
                task.invoke(hrv)
            }
            pendingTaskList.clear()
        }
    }

    /**
     * Initialize performance monitoring manager
     */
    private fun initPerformanceManager(pageName: String): KRPerformanceManager? {
        val monitorOptions = delegate.performanceMonitorTypes()
        if (monitorOptions.isNotEmpty()) {
            return KRPerformanceManager(pageName, executeMode, monitorOptions).apply {
                setMonitorCallback(object : IKRMonitorCallback {
                    override fun onLaunchResult(data: KRLaunchData) {
                        // Callback launch performance data
                        delegate.onGetLaunchData(data)
                    }

                    override fun onResult(data: KRPerformanceData) {
                        // Callback performance monitoring data
                        delegate.onGetPerformanceData(data)
                    }
                })
            }
        }
        return performanceManager
    }

    /**
     * Load and initialize renderView
     */
    private fun loadingKuiklyRenderView(size: SizeI) {
        initRenderView(size)
    }

    /**
     * Initialize renderView
     */
    private fun initRenderView(size: SizeI) {
        Log.trace(TAG, "initRenderView, pageName: $pageName")
        // Instantiate renderView
        renderView = KuiklyRenderView(executeMode, delegate)
        // Initialize renderView
        renderView?.apply {
            // Register lifecycle callback
            registerCallback(renderViewCallback)
            // Register view and module, etc.
            registerKuiklyRenderExport(this)
            // Initialize and render
            init(rootContainer, pageName ?: "", pageData ?: mapOf(), size)
        }
        // Lifecycle hook callback
        delegate.onKuiklyRenderViewCreated()
        renderView?.didCreateRenderView()
        if (delegate.syncRenderingWhenPageAppear()) {
            // Synchronously complete all rendering tasks
            renderView?.syncFlushAllRenderTasks()
        }
        // Check if there are any unexecuted rendering tasks
        tryRunKuiklyRenderViewPendingTask(renderView)
    }

    /**
     * Exception handling
     */
    private fun handleException(throwable: Throwable, errorReason: ErrorReason) {
        Log.error(
            TAG,
            "handleException, isLoadFinish: $isLoadFinish, errorReason: $errorReason, error: ${
                throwable.stackTraceToString()
            }"
        )
        // If first frame is not complete, notify loading failure
        if (!isLoadFinish) {
            // Block subsequent exceptions
            renderView?.unregisterCallback(renderViewCallback)
            renderView?.destroy()
            delegate.onPageLoadComplete(false, errorReason, executeMode)
        }
        // Notify the exception to the instance
        delegate.onUnhandledException(throwable, errorReason, executeMode)
        // Notify global exception todo
        // KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter?.uncaughtException(throwable)
        // ?: throw throwable
    }

    /**
     * Register modules and render views, etc.
     */
    private fun registerKuiklyRenderExport(kuiklyRenderView: KuiklyRenderView?) {
        kuiklyRenderView?.kuiklyRenderExport?.also {
            registerModule(it) // Register module
            registerRenderView(it) // Register View
            registerViewExternalPropHandler(it) // Register custom property handler
        }
    }

    /**
     * Register built-in modules
     */
    private fun registerModule(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            moduleExport(KRMemoryCacheModule.MODULE_NAME) {
                KRMemoryCacheModule()
            }
            moduleExport(KRSharedPreferencesModule.MODULE_NAME) {
                KRSharedPreferencesModule()
            }
            moduleExport(KRRouterModule.MODULE_NAME) {
                KRRouterModule()
            }
            moduleExport(KRPerformanceModule.MODULE_NAME) {
                KRPerformanceModule(performanceManager)
            }
            moduleExport(KRNotifyModule.MODULE_NAME) {
                KRNotifyModule()
            }
            moduleExport(KRLogModule.MODULE_NAME) {
                KRLogModule()
            }
            moduleExport(KRCodecModule.MODULE_NAME) {
                KRCodecModule()
            }
            moduleExport(KRSnapshotModule.MODULE_NAME) {
                KRSnapshotModule()
            }
            moduleExport(KRCalendarModule.MODULE_NAME) {
                KRCalendarModule()
            }
            moduleExport(KRNetworkModule.MODULE_NAME) {
                KRNetworkModule()
            }
            moduleExport(H5WindowResizeModule.MODULE_NAME) {
                H5WindowResizeModule()
            }
            // Delegate to external, allowing host project to expose its own modules
            delegate.registerExternalModule(this)
        }
    }

    /**
     * Register custom property handler
     */
    private fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        // Register built-in external prop handlers
        kuiklyRenderExport.viewPropExternalHandlerExport(KRCustomPropsHandler())
        // Delegate to external, allowing host project to expose its own custom property handler
        delegate.registerViewExternalPropHandler(kuiklyRenderExport)
    }

    /**
     * Register built-in views
     */
    private fun registerRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            renderViewExport(KRView.VIEW_NAME, {
                KRView()
            })
            renderView?.let {
                renderViewExport(KRImageView.VIEW_NAME, {
                    KRImageView(it.kuiklyRenderContext)
                })
                // In web, apng is supported by Image
                renderViewExport(KRImageView.APNG_VIEW_NAME, {
                    KRImageView(it.kuiklyRenderContext)
                })
                renderViewExport(KRCanvasView.VIEW_NAME, {
                    KRCanvasView(it.kuiklyRenderContext)
                })
            }
            renderViewExport(KRTextFieldView.VIEW_NAME, {
                KRTextFieldView()
            })
            renderViewExport(KRTextAreaView.VIEW_NAME, {
                KRTextAreaView()
            })
            renderViewExport(KRRichTextView.VIEW_NAME, {
                KRRichTextView()
            }, {
                // shadow view needs an additional registration
                KRRichTextView()
            })
            renderViewExport(KRRichTextView.GRADIENT_RICH_TEXT_VIEW, {
                KRRichTextView()
            }, {
                // shadow view needs an additional registration
                KRRichTextView()
            })
            renderViewExport(KRListView.VIEW_NAME, {
                KRListView()
            })
            renderViewExport(KRListView.VIEW_NAME_SCROLL_VIEW, {
                KRListView()
            })
            renderViewExport(KRScrollContentView.VIEW_NAME, {
                KRScrollContentView()
            })
            renderViewExport(KRHoverView.VIEW_NAME, {
                KRHoverView()
            })
            renderViewExport(KRVideoView.VIEW_NAME, {
                KRVideoView()
            })
            renderViewExport(KRBlurView.VIEW_NAME, {
                KRBlurView()
            })
            renderViewExport(KRActivityIndicatorView.VIEW_NAME, {
                KRActivityIndicatorView()
            })
            renderViewExport(KRPagView.VIEW_NAME, {
                KRPagView()
            })
            renderViewExport(KRMaskView.VIEW_NAME, {
                KRMaskView()
            })
            // Delegate to external, allowing host project to expose its own views
            delegate.registerExternalRenderView(this)
        }
    }

    /**
     * init kuikly global object for web
     */
    private fun initGlobalObject() {
        val dynamicWindow = window.asDynamic()
        if (jsTypeOf(dynamicWindow.kuiklyDocument) == "undefined") {
            // init document
            dynamicWindow.kuiklyDocument = document
        }

        if (jsTypeOf(dynamicWindow.kuiklyWindow) == "undefined") {
            // init window
            dynamicWindow.kuiklyWindow = window
        }
    }

    /**
     * inject h5 api and func
     */
    private fun injectHostFunc() {
        // init animation generator
        KuiklyProcessor.animationProcessor = AnimationProcessor
        // init text processor
        KuiklyProcessor.richTextProcessor = RichTextProcessor
        // init event processor
        KuiklyProcessor.eventProcessor = EventProcessor
        // init image processor
        KuiklyProcessor.imageProcessor = ImageProcessor
        // init list processor
        KuiklyProcessor.listProcessor = ListProcessor
        // init dev environment
        KuiklyProcessor.isDev =
            window.location.href.contains(DEBUG_FIELD)
    }

    // ================= Auto root-view-size forwarder =================
    //
    // Watches the root container (via ResizeObserver when available) and the
    // window (as a fallback / on mobile browsers without ResizeObserver) and
    // forwards size changes to the Kuikly Pager as `rootViewSizeDidChanged`,
    // throttled at 100 ms. Enabled based on `KuiklyProcessor.autoUpdateRootViewSizeOnResize`.

    private var autoResizeObserver: dynamic = null
    private var autoResizeWindowListener: ((Event) -> Unit)? = null
    private var autoResizeThrottleTimer: Int? = null
    private var autoResizeLastInvokeTime: Long = 0L
    private var lastDispatchedWidth: Int = -1
    private var lastDispatchedHeight: Int = -1

    private fun maybeStartAutoResizeForwarder(initialSize: SizeI) {
        if (!shouldAutoForwardResize()) return
        // Seed with the init size so we do not immediately re-fire the same size.
        lastDispatchedWidth = initialSize.first
        lastDispatchedHeight = initialSize.second

        // Resolve the observed DOM container (only used to read a more
        // accurate size when Kuikly is embedded in a sub-region). When the
        // host page uses a full-viewport layout (e.g. an empty `<div id="root">`
        // whose size is not stretched by CSS), `container.clientWidth/Height`
        // stay equal to the last size we imperatively wrote back, and will
        // NOT reflect viewport changes. In that case we must fall back to
        // `window.innerWidth / innerHeight`.
        val container: HTMLElement? = when (val rc = rootContainer) {
            is HTMLElement -> rc
            is String -> document.getElementById(rc) as? HTMLElement
            else -> null
        }

        // Unified size reader: prefer container's real client size when it
        // has an intrinsic size, otherwise fall back to viewport size.
        fun readCurrentSize(): Pair<Int, Int> {
            val cw = container?.clientWidth ?: 0
            val ch = container?.clientHeight ?: 0
            val w = if (cw > 0) cw else window.innerWidth
            val h = if (ch > 0) ch else window.innerHeight
            return w to h
        }

        // (1) Always subscribe to window.resize. This is the most reliable
        //     signal for full-viewport apps whose root container size only
        //     ever changes because the viewport changed.
        val listener: (Event) -> Unit = { _ ->
            val (w, h) = readCurrentSize()
            scheduleAutoResizeDispatch(w, h)
        }
        autoResizeWindowListener = listener
        window.addEventListener("resize", listener)

        // (2) When available, additionally observe the container via
        //     ResizeObserver. This captures cases where the embedding host
        //     resizes only the container (not the whole window). We reuse
        //     the same throttled dispatcher.
        //
        //     Note on Kotlin/JS: `entries` from ResizeObserver is a native
        //     JS Array. Reading it from Kotlin side (even via `asDynamic()`)
        //     is fragile because `(dynamic) -> Unit` lambdas get erased to
        //     `(Any?) -> Unit` at IR level, producing runtime calls like
        //     `entries.get(0)` / `entries.asDynamic()` that do not exist on
        //     a JS array. So we do the read entirely in inline JS and pass
        //     two Ints back through a strongly-typed callback.
        val hasResizeObserver = jsTypeOf(window.asDynamic().ResizeObserver) != "undefined"
        if (hasResizeObserver && container != null) {
            val onSize: (Int, Int) -> Unit = { rw, rh ->
                // Prefer viewport size when container has no intrinsic size,
                // to match the same rule as the window.resize path.
                val (w, h) = readCurrentSize()
                // If container is intrinsically sized, `readCurrentSize`
                // already returns it; otherwise `rw/rh` from contentRect
                // typically equals the last written size and is useless.
                // Either way, `readCurrentSize` gives the right answer, so
                // we ignore `rw/rh` here on purpose but still route through
                // the observer so future improvements can pick a source.
                @Suppress("UNUSED_VARIABLE")
                val _unused = rw + rh
                scheduleAutoResizeDispatch(w, h)
            }
            val roCtor = window.asDynamic().ResizeObserver
            val observer = js(
                "new roCtor(function(entries){" +
                    "var e = entries && entries[0];" +
                    "var r = e && e.contentRect;" +
                    "var w = r ? Math.round(r.width) : 0;" +
                    "var h = r ? Math.round(r.height) : 0;" +
                    "onSize(w, h);" +
                "})"
            )
            observer.observe(container)
            autoResizeObserver = observer
        }
    }

    /**
     * Decide whether the built-in auto forwarder should run.
     *
     * Reads [KuiklyProcessor.autoUpdateRootViewSizeOnResize]. Off by default
     * on both PC and mobile because auto relayout on every resize can have a
     * broad impact (mobile soft-keyboard-triggered resize, desktop layouts
     * that do not want to rescale, etc.). Business code enables it explicitly
     * when it wants the WebRender to auto-forward container/window resize.
     */
    private fun shouldAutoForwardResize(): Boolean {
        return KuiklyProcessor.autoUpdateRootViewSizeOnResize
    }

    private fun scheduleAutoResizeDispatch(width: Int, height: Int) {
        // Skip if size unchanged.
        if (width == lastDispatchedWidth && height == lastDispatchedHeight) return
        // Skip if either dimension is 0 (e.g. detached from DOM briefly).
        if (width <= 0 || height <= 0) return

        val now = Date.now().toLong()
        if (now - autoResizeLastInvokeTime >= AUTO_RESIZE_THROTTLE_MS) {
            autoResizeLastInvokeTime = now
            autoResizeThrottleTimer?.let { window.clearTimeout(it) }
            autoResizeThrottleTimer = null
            dispatchAutoResize(width, height)
        } else {
            autoResizeThrottleTimer?.let { window.clearTimeout(it) }
            autoResizeThrottleTimer = window.setTimeout({
                autoResizeLastInvokeTime = Date.now().toLong()
                autoResizeThrottleTimer = null
                dispatchAutoResize(width, height)
            }, AUTO_RESIZE_THROTTLE_MS)
        }
    }

    private fun dispatchAutoResize(width: Int, height: Int) {
        if (width == lastDispatchedWidth && height == lastDispatchedHeight) return
        lastDispatchedWidth = width
        lastDispatchedHeight = height
        runKuiklyRenderViewTask {
            it.updateRootViewSize(width, height)
        }
    }

    private fun stopAutoResizeForwarder() {
        autoResizeThrottleTimer?.let { window.clearTimeout(it) }
        autoResizeThrottleTimer = null
        // Note: autoResizeObserver is `dynamic`, so `?.let { it.xxx() }` cannot
        // resolve `it` on Kotlin/JS. Use an explicit null check instead.
        val observer = autoResizeObserver
        if (observer != null) {
            try { observer.disconnect() } catch (_: Throwable) { /* ignore */ }
        }
        autoResizeObserver = null
        autoResizeWindowListener?.let { window.removeEventListener("resize", it) }
        autoResizeWindowListener = null
    }

    companion object {
        private const val TAG = "KuiklyRenderViewDelegator"
        // Development environment identifier
        private const val DEBUG_FIELD = "is_dev"
        // Throttle window (ms) for auto resize forwarding. Kept identical to
        // H5WindowResizeHandler.LISTEN_WINDOW_SIZE_CHANGE_INTERVAL for parity.
        private const val AUTO_RESIZE_THROTTLE_MS = 100
    }
}

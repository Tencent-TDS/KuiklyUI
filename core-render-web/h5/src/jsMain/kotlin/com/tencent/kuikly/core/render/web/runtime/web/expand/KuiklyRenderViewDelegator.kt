package com.tencent.kuikly.core.render.web.runtime.web.expand

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

/**
 * Host project can simplify KuiklyRenderCore integration through this class, which is integrated at page granularity
 */
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
    }

    /**
     * Called when page onDestroy
     */
    fun onDetach() {
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
     * Send events to Kuikly page
     */
    fun sendEvent(event: String, data: Map<String, Any>) {
        runKuiklyRenderViewTask {
            it.sendEvent(event, data)
        }
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
            renderViewExport(KRCanvasView.VIEW_NAME, {
                KRCanvasView()
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

    companion object {
        private const val TAG = "KuiklyRenderViewDelegator"
        // Development environment identifier
        private const val DEBUG_FIELD = "is_dev"
    }
}

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand

import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.KuiklyRenderView
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
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
import com.tencent.kuikly.core.render.web.expand.module.KRSharedPreferencesModule
import com.tencent.kuikly.core.render.web.runtime.miniapp.module.MiniRouterModule
import com.tencent.kuikly.core.render.web.expand.module.KRSnapshotModule
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.performance.IKRMonitorCallback
import com.tencent.kuikly.core.render.web.performance.KRPerformanceData
import com.tencent.kuikly.core.render.web.performance.KRPerformanceManager
import com.tencent.kuikly.core.render.web.performance.launch.KRLaunchData
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.Headers
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniDocument
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.ktx.globalThis
import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPageManage
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.AnimationProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.EventProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.ImageProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.ListProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.RichTextProcessor
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Mini program host project can simplify KuiklyRenderCore access through this class
 */
class KuiklyRenderViewDelegator(private val delegate: KuiklyRenderViewDelegatorDelegate) {
    // todo Need to change to mini program access unique path - renderView mapping
    // todo isLoadFinish performanceManager renderView pendingTaskList renderViewCallback
    // Root renderView of kuikly page
    var renderView: KuiklyRenderView? = null

    // Performance monitoring
    private var performanceManager: KRPerformanceManager? = null

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
     * Called when page starts
     */
    fun onAttach(
        pageId: Int,
        pageName: String,
        paramsMap: FastMutableMap<String, Any>,
        size: SizeI?,
    ) {
        // !!!!!! inject global object should before view render, pay attention for this
        initGlobalObject()
        // create mini app root container
        MiniDocument.createRootContainer(pageId)
        // inject host api and object
        injectHostFunc()
        // init render view
        initRenderView(pageId, pageName, paramsMap, size)
    }

    /**
     * Called at page onDestroy
     */
    fun onDetach() {
        runKuiklyRenderViewTask {
            it.destroy()
        }
    }

    /**
     * Called at page onPause
     */
    fun onPause() {
        runKuiklyRenderViewTask {
            it.pause()
        }
    }

    /**
     * Called at page onResume
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
     * Initialize renderView
     */
    private fun initRenderView(
        pageId: Int,
        pageName: String,
        paramsMap: FastMutableMap<String, Any>,
        size: SizeI?
    ) {
        Log.log(TAG, "initRenderView")
        // Instantiate renderView
        renderView = KuiklyRenderView(executeMode, delegate)

        MiniPageManage.currentPage?.renderView = renderView

        performanceManager = initPerformanceManager(pageName)
        // Initialize renderView
        renderView?.apply {
            val usedSize = size ?: SizeI(
                MiniGlobal.windowWidth,
                MiniGlobal.windowHeight,
            )
            // Register lifecycle callback
            registerCallback(renderViewCallback)
            // Register view and module, etc
            registerKuiklyRenderExport(this)
            // Initialize and render
            init(pageId, pageName, paramsMap, usedSize)
        }
        // Lifecycle hook callback
        delegate.onKuiklyRenderViewCreated()
        renderView?.didCreateRenderView()
        if (delegate.syncRenderingWhenPageAppear()) {
            // Synchronize to complete all rendering tasks
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
        // If the first frame is not completed, an exception is considered, and the loading failure is notified
        if (!isLoadFinish) {
            // Suppress subsequent exceptions
            renderView?.unregisterCallback(renderViewCallback)
            renderView?.destroy()
            delegate.onPageLoadComplete(false, errorReason, executeMode)
        }
        // Exception notification to instance
        delegate.onUnhandledException(throwable, errorReason, executeMode)
        // Global exception notification todo
    }

    /**
     * Register module and rendering view, etc
     */
    private fun registerKuiklyRenderExport(kuiklyRenderView: KuiklyRenderView?) {
        kuiklyRenderView?.kuiklyRenderExport?.also {
            registerModule(it) // Register module
            registerRenderView(it) // Register View
            registerViewExternalPropHandler(it) // Register custom attribute processor
        }
    }

    /**
     * Register built-in module
     */
    private fun registerModule(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            moduleExport(KRMemoryCacheModule.MODULE_NAME) {
                KRMemoryCacheModule()
            }
            moduleExport(KRSharedPreferencesModule.MODULE_NAME) {
                KRSharedPreferencesModule()
            }
            moduleExport(MiniRouterModule.MODULE_NAME) {
                MiniRouterModule()
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
            // Delegate to the external host project to expose their own modules
            delegate.registerExternalModule(this)
        }
    }

    /**
     * Register custom property handlers
     */
    private fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        // Delegate to the external host project to expose their own custom property handlers
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
                // In web, apng is supported by the Image component
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
                // Shadow view needs an additional registration
                KRRichTextView()
            })
            renderViewExport(KRRichTextView.GRADIENT_RICH_TEXT_VIEW, {
                KRRichTextView()
            }, {
                // Shadow view needs an additional registration
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
            // Delegate to the external host project to expose their own views
            delegate.registerExternalRenderView(this)
        }
    }

    /**
     * Inject kuikly global object to mini app environment
     */
    private fun initGlobalObject() {
        // inject kuikly global document
        if (jsTypeOf(globalThis.kuiklyDocument) == "undefined") {
            globalThis.kuiklyDocument = MiniDocument
        }

        // inject kuikly global window
        if (jsTypeOf(globalThis.kuiklyWindow) == "undefined") {
            globalThis.kuiklyWindow = MiniGlobal
        }

        // inject kuikly register native method
        if (jsTypeOf(globalThis.kuiklyWindow.com) == "undefined") {
            globalThis.kuiklyWindow.com = globalThis.global.com
        }

        // inject kuikly call kotlin method
        if (jsTypeOf(globalThis.kuiklyWindow.callKotlinMethod) == "undefined") {
            globalThis.kuiklyWindow.callKotlinMethod = globalThis.global.callKotlinMethod
        }

        // inject network request Headers class
        if (jsTypeOf(globalThis.Headers) == "undefined") {
            globalThis.Headers = Headers::class.js
        }
    }

    /**
     * inject mini app api and func
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
        KuiklyProcessor.isDev = MiniGlobal.isDev()
    }

    companion object {
        private const val TAG = "KuiklyRenderViewDelegator"
    }
}

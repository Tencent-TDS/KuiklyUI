/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.android.expand

import android.content.Context
import android.content.Intent
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.UiThread
import com.tencent.kuikly.core.render.android.R
import com.tencent.kuikly.core.render.android.KuiklyRenderView
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.css.ktx.stackTraceToString
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.component.*
import com.tencent.kuikly.core.render.android.expand.component.blur.KRBlurView
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerContentView
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
import com.tencent.kuikly.core.render.android.expand.component.pag.KRPAGView
import com.tencent.kuikly.core.render.android.expand.module.KRBackPressModule
import com.tencent.kuikly.core.render.android.expand.module.KRCalendarModule
import com.tencent.kuikly.core.render.android.expand.module.KRCodecModule
import com.tencent.kuikly.core.render.android.expand.module.KRFontModule
import com.tencent.kuikly.core.render.android.expand.module.KRLogModule
import com.tencent.kuikly.core.render.android.expand.module.KRKeyboardModule
import com.tencent.kuikly.core.render.android.expand.module.KRSharedPreferencesModule
import com.tencent.kuikly.core.render.android.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.android.expand.module.KRNetworkModule
import com.tencent.kuikly.core.render.android.expand.module.KRNotifyModule
import com.tencent.kuikly.core.render.android.expand.module.KRPerformanceModule
import com.tencent.kuikly.core.render.android.expand.module.KRRouterModule
import com.tencent.kuikly.core.render.android.expand.module.KRSnapshotModule
import com.tencent.kuikly.core.render.android.expand.module.KRReflectionModule
import com.tencent.kuikly.core.render.android.expand.module.KRVsyncModule
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.performace.IKRMonitorCallback
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import com.tencent.kuikly.core.render.android.performace.KRPerformanceData
import com.tencent.kuikly.core.render.android.performace.KRPerformanceManager
import com.tencent.kuikly.core.render.android.performace.frame.KRFrameMonitor
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 宿主工程可使用此类来简化KuiklyRenderCore的接入, 该类是给页面粒度接入，View粒度使用[KuiklyView]
 */
open class KuiklyRenderViewBaseDelegator(private val delegate: KuiklyRenderViewBaseDelegatorDelegate) {

    companion object {
        private const val TAG = "KuiklyRenderViewBaseDelegator"
    }

    /**
     * [KuiklyRenderView]容器的弱引用
     */
    private var containerViewWeakRef: WeakReference<ViewGroup>? = null

    /**
     * Kuikly页面的根View
     */
    private var renderView: KuiklyRenderView? = null

    /**
     * 性能监控
     */
    private var performanceManager: KRPerformanceManager? = null

    /**
     * 页面参数
     */
    private var pageData: Map<String, Any>? = null

    private var pageName: String? = null
    private var contextCode: String? = null

    /**
     * assets 资源目录
     */
    private var assetsPath: String? = null

    @Volatile
    private var isLoadFinish = false

    /**
     * 执行模式
     */
    private lateinit var executeMode: KuiklyRenderCoreExecuteModeBase

    private val pendingTaskList by lazy {
        mutableListOf<KuiklyRenderViewPendingTask>()
    }

    init {
        KuiklyRenderClassLoad  // 触发Render内部类加载(内部并发)
    }

    private val renderViewCallback = object : IKuiklyRenderViewLifecycleCallback {

        override fun onInit() {
            performanceManager?.onInit()
        }

        override fun onPreloadClassFinish() {
            performanceManager?.onPreloadClassFinish()
        }

        override fun onInitCoreStart() {
            performanceManager?.onInitCoreStart()
        }

        override fun onInitCoreFinish() {
            performanceManager?.onInitCoreFinish()
            performanceManager?.getMonitor<KRFrameMonitor>(KRFrameMonitor.MONITOR_NAME)?.let {
                renderView?.setViewTreeUpdateListener(it.driveFrameDetector)
                renderView?.setKotlinBridgeStatusListener(it.driveFrameDetector)
            }
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
     * 页面onCreate的时候调用
     * @param container Kuikly根View容器
     * @param contextCode 执行上下文code
     * @param pageName 页面名字
     * @param pageData 页面数据
     * @param size 根View大小, 非必传
     * @param assetsPath assets 资源路径
     */
    @UiThread
    fun onAttach(
        container: ViewGroup,
        contextCode: String,
        pageName: String,
        pageData: Map<String, Any>,
        size: Size? = null,
        assetsPath: String? = null
    ) {
        executeMode = delegate.coreExecuteModeX()
        containerViewWeakRef = WeakReference(container)
        performanceManager = initPerformanceManager(pageName)
        this.pageName = pageName
        this.contextCode = contextCode
        this.pageData = pageData
        this.assetsPath = assetsPath
        loadingKuiklyRenderView(size)
    }

    /**
     * 页面onDestroy的时候调用
     */
    @UiThread
    fun onDetach() {
        runKuiklyRenderViewTask {
            it.destroy()
        }
    }

    /**
     * 页面onPause的时候调用
     */
    @UiThread
    fun onPause() {
        runKuiklyRenderViewTask {
            it.pause()
        }
    }

    /**
     * 页面onResume的时候调用
     */
    @UiThread
    fun onResume() {
        runKuiklyRenderViewTask {
            it.resume()
        }
    }

    /**
     * 向Kuikly页面发送事件
     */
    @UiThread
    fun sendEvent(event: String, data: Map<String, Any>) {
        runKuiklyRenderViewTask {
            it.sendEvent(event, data)
        }
    }

    /**
     * 注册[KuiklyRenderView]生命周期回调
     * @param callback 生命周期回调
     */
    fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        runKuiklyRenderViewTask {
            it.registerCallback(callback)
        }
    }

    /**
     * 解注册[KuiklyRenderView]生命周期回调
     * @param callback 生命周期回调
     */
    fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        runKuiklyRenderViewTask {
            it.unregisterCallback(callback)
        }
    }

    /**
     * 分发onActivityResult事件
     * @param requestCode
     * @param resultCode
     * @param data
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        runKuiklyRenderViewTask {
            it.dispatchOnActivityResult(requestCode, resultCode, data)
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

    private fun initPerformanceManager(pageName: String): KRPerformanceManager? {
        val monitorOptions = delegate.performanceMonitorTypes()
        if (monitorOptions.isNotEmpty()) {
            return KRPerformanceManager(pageName, executeMode, monitorOptions).apply {
                setMonitorCallback(object : IKRMonitorCallback {

                    override fun onLaunchResult(data: KRLaunchData) {
                        // 回调启动性能数据
                        delegate.onGetLaunchData(data)
                    }

                    override fun onResult(data: KRPerformanceData) {
                        // 回调性能监控数据
                        delegate.onGetPerformanceData(data)
                    }
                })
            }
        }
        return performanceManager
    }

    private fun loadingKuiklyRenderView(size: Size?) {
        initRenderView(size)
    }

    private fun initRenderView(size: Size?) {
        KuiklyRenderLog.d(TAG, "initRenderView, executeMode: $executeMode, pageName: $pageName")
        val containerView = containerViewWeakRef?.get() ?: return
        renderView = KuiklyRenderView(containerView.context, executeMode, delegate.enablePreloadClass(), delegate).apply {
            registerCallback(renderViewCallback)
            registerKuiklyRenderExport(this)
            init(
                contextCode ?: KRCssConst.EMPTY_STRING,
                pageName ?: KRCssConst.EMPTY_STRING,
                pageData ?: mapOf(),
                size,
                assetsPath
            )
            layoutParams = if (size == null) {
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            } else {
                FrameLayout.LayoutParams(size.width, size.height)
            }
        }
        containerView.addView(renderView)
        delegate.onKuiklyRenderViewCreated()
        if (delegate.syncRenderingWhenPageAppear()) {
            renderView?.syncFlushAllRenderTasks()
        }
        pageName = null
        pageData = null
        contextCode = null
        tryRunKuiklyRenderViewPendingTask(renderView)

    }

    private fun handleException(throwable: Throwable, errorReason: ErrorReason) {
        KuiklyRenderLog.e(TAG, "handleException, isLoadFinish: $isLoadFinish, errorReason: $errorReason, error: ${throwable.stackTraceToString()}")
        // 首帧没完成则异常，通知加载失败
        if (!isLoadFinish) {
            // 屏蔽后续异常
            renderView?.unregisterCallback(renderViewCallback)
            renderView?.destroy()
            delegate.onPageLoadComplete(false, errorReason, executeMode)
        }
        // 异常通知到实例
        delegate.onUnhandledException(throwable, errorReason, executeMode)
        // 通知全局异常
        KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter?.uncaughtException(throwable) ?: throw throwable
    }

    private fun registerKuiklyRenderExport(kuiklyRenderView: KuiklyRenderView?) {
        kuiklyRenderView?.kuiklyRenderExport?.also {
            registerModule(it) // 注册module
            registerRenderView(it) // 注册View
            registerViewExternalPropHandler(it) // 注册自定义属性处理器
        }
    }

    private fun registerRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            renderViewExport(KRView.VIEW_NAME, { context ->
                KRView(context)
            })
            renderViewExport(KRRichTextView.VIEW_NAME, { context ->
                KRRichTextView(context)
            }, {
                KRRichTextShadow()
            })
            renderViewExport(KRRichTextView.GRADIENT_RICH_TEXT_VIEW, { context ->
                KRRichTextView(context)
            }, {
                KRRichTextShadow()
            })
            renderViewExport(KRImageView.VIEW_NAME, { context ->
                KRImageView(context)
            })
            renderViewExport(KRWrapperImageView.VIEW_NAME, { context ->
                KRWrapperImageView(context)
            })
            renderViewExport(KRRecyclerView.VIEW_NAME, { context ->
                createHRRecyclerView(context)
            })
            renderViewExport(KRRecyclerView.VIEW_NAME_SCROLL_VIEW, { context ->
                createHRRecyclerView(context)
            })
            renderViewExport(KRRecyclerContentView.VIEW_NAME, { context ->
                KRRecyclerContentView(context)
            })
            renderViewExport(KRTextFieldView.VIEW_NAME, { context ->
                KRTextFieldView(context, delegate.softInputMode())
            })
            renderViewExport(KRTextAreaView.VIEW_NAME, { context ->
                KRTextAreaView(context, delegate.softInputMode())
            })
            renderViewExport(KRCanvasView.VIEW_NAME, { context ->
                KRCanvasView(context)
            })
            renderViewExport(KRHoverView.VIEW_NAME, { context ->
                KRHoverView(context)
            })
            renderViewExport(KRActivityIndicatorView.VIEW_NAME, { context ->
                KRActivityIndicatorView(context)
            })
            renderViewExport(KRBlurView.VIEW_NAME, { context ->
                KRBlurView(context)
            })
            renderViewExport(KRPAGView.VIEW_NAME, { context ->
                KRPAGView(context)
            })
            renderViewExport(KRMaskView.VIEW_NAME, { context ->
                KRMaskView(context)
            })
            renderViewExport(KRVideoView.VIEW_NAME, { context ->
                KRVideoView(context)
            })
            renderViewExport(KRAPNGView.VIEW_NAME, { context ->
                KRAPNGView(context)
            })
            renderViewExport(KRModalView.VIEW_NAME, { context ->
                KRModalView(context)
            })

            delegate.registerExternalRenderView(this) // 代理给外部，让宿主工程暴露自己的View
        }
    }

    private fun registerModule(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            moduleExport(KRMemoryCacheModule.MODULE_NAME) {
                KRMemoryCacheModule()
            }
            moduleExport(KRSharedPreferencesModule.MODULE_NAME) {
                KRSharedPreferencesModule()
            }
            moduleExport(KRNotifyModule.MODULE_NAME) {
                KRNotifyModule()
            }
            moduleExport(KRSnapshotModule.MODULE_NAME) {
                KRSnapshotModule()
            }
            moduleExport(KRRouterModule.MODULE_NAME) {
                KRRouterModule()
            }
            moduleExport(KRNetworkModule.MODULE_NAME) {
                KRNetworkModule()
            }
            moduleExport(KRCalendarModule.MODULE_NAME) {
                KRCalendarModule()
            }
            moduleExport(KRCodecModule.MODULE_NAME) {
                KRCodecModule()
            }
            moduleExport(KRReflectionModule.MODULE_NAME) {
                KRReflectionModule()
            }
            moduleExport(KRLogModule.MODULE_NAME) {
                KRLogModule()
            }
            moduleExport(KRKeyboardModule.MODULE_NAME) {
                KRKeyboardModule()
            }
            moduleExport(KRPerformanceModule.MODULE_NAME) {
                KRPerformanceModule(performanceManager)
            }
            moduleExport(KRFontModule.MODULE_NAME) {
                KRFontModule()
            }
            moduleExport(KRVsyncModule.MODULE_NAME) {
                KRVsyncModule()
            }
            moduleExport(KRBackPressModule.MODULE_NAME) {
                KRBackPressModule()
            }
            delegate.registerExternalModule(this) // 代理给外部，让宿主工程可以暴露自己的module
            delegate.registerTDFModule(this)
        }
    }

    private fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        delegate.registerViewExternalPropHandler(kuiklyRenderExport) // 代理给外部
    }

    private fun createHRRecyclerView(context: Context): IKuiklyRenderViewExport {
        // recyclerview的滚动条必须使用xml加载才能生效
        // 必须cloneInContext来替换成自己的context，不然始终是activity的context
        return LayoutInflater.from(context).cloneInContext(context)
            .inflate(
                R.layout.kuikly_kr_recycler_view_layout,
                null
            ) as IKuiklyRenderViewExport
    }

    /**
     * 将返回键事件交由框架处理，建议参考以下接入方式：
     *
     *     override fun dispatchKeyEvent(event: KeyEvent): Boolean {
     *         if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
     *             kuiklyRenderViewDelegator.onBackPressed()
     *             return true
     *         }
     *         return super.dispatchKeyEvent(event)
     *     }
     *
     */
    fun onBackPressed(): Boolean {
        val sendTime = System.currentTimeMillis()
        val isBackPressedConsumed = AtomicBoolean(false)

        // 触发 onBackPressed 逻辑
        runKuiklyRenderViewTask {
            it.onBackPressed()
        }

        // 消费直至超时或结果设定
        waitForBackConsumedResult(sendTime, isBackPressedConsumed)

        return isBackPressedConsumed.get()
    }

    /**
     * 等待Kuikly侧通过路由Module返回Back键的消费状态
     */
    private fun waitForBackConsumedResult(sendTime: Long, consumeResult: AtomicBoolean) {
        val looping = AtomicBoolean(true)
        while (looping.get()) {
            Thread.sleep(10)
            runKuiklyRenderViewTask {
                it.module<KRBackPressModule>(KRBackPressModule.MODULE_NAME)?.apply {
                    if (this.backConsumedTime > sendTime) {
                        consumeResult.set(this.isBackConsumed)
                        looping.set(false)
                    }
                }
            }
            // 超时退出
            if ((System.currentTimeMillis() - sendTime) > 200L) {
                looping.set(false)
            }
        }
    }
}

private typealias KuiklyRenderViewPendingTask = (KuiklyRenderView) -> Unit

/**
 * kuikly Core内部的代理实现类
 * 定义了可供业务拓展的一些接口
 */
interface KuiklyRenderViewBaseDelegatorDelegate {

    /**
     * 供业务注册Module
     * @param kuiklyRenderExport
     */
    fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * 供业务注册RenderView和shadow
     * @param kuiklyRenderExport
     */
    fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * 注册 TDF 通用 Module
     */
    fun registerTDFModule(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * 供业务注入View的自定义属性handler
     * @param kuiklyRenderExport
     */
    fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {}

    /**
     * KuiklyRenderCore的执行模式, 默认JVM模式
     * @return 执行模式
     */
    fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase = KuiklyRenderCoreExecuteModeBase.JVM

    /**
     * 性能监控选项，默认只开启动监控
     */
    fun performanceMonitorTypes() : List<KRMonitorType> {
        return listOf(KRMonitorType.LAUNCH)
    }

    /**
     * [KuiklyRenderView]创建回调
     */
    fun onKuiklyRenderViewCreated() {}

    /**
     * [KuiklyRenderView]的子View已经创建回调
     */
    fun onKuiklyRenderContentViewCreated() {}

    /**
     * 首屏是否同步渲染（默认为同步方式）
     * @return  是否同步渲染首屏
     */
    fun syncRenderingWhenPageAppear() : Boolean { return true }

    /**
     * 是否支持并行加载Class
     */
    fun enablePreloadClass(): Boolean = true

    /**
     * 为保证双端一致性，Kuikly 默认会将 Input 组件所在的 Activity#window 设置为 [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING]
     * 如果业务是局部使用 Kuikly 或使用 Dialog、PopupWindow 等场景，不希望影响到 Activity#window 的 softInputMode，可以修改重载该方法进行设置
     *
     * @return 与 Window#setSoftInputMode 参数一致，如果不设置则返回 null
     */
    fun softInputMode(): Int? {
        return WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
    }

    /**
     * 回调启动数据
     */
    fun onGetLaunchData(data: KRLaunchData) {}

    /**
     * 回调性能数据
     */
    fun onGetPerformanceData(data: KRPerformanceData) {}

    /**
     * 异常回调
     *
     * @param throwable 异常
     * @param errorReason 失败原因
     * @param executeMode 执行模式
     */
    fun onUnhandledException(throwable: Throwable,
                             errorReason: ErrorReason,
                             executeMode: KuiklyRenderCoreExecuteModeBase) {}

    /**
     * 页面加载回调
     *
     * @param isSucceed 是否成功
     * @param errorReason 失败原因
     * @param executeMode 执行模式
     */
    fun onPageLoadComplete(isSucceed: Boolean,
                           errorReason: ErrorReason? = null,
                           executeMode: KuiklyRenderCoreExecuteModeBase) {}

    /**
     * 是否同步发送
     * @param event 事件名称
     * @return 是否同步，默认为false
     */
    fun syncSendEvent(event: String): Boolean {
        return false
    }
    
    fun useHostDisplayMetrics(): Boolean {
        return false
    }

    fun enableContextReplace(): Boolean {
        return false
    }

}
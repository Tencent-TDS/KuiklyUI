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

@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.tencent.kuikly.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.lazy.layout.LocalKuiklyPrefetchScheduler
import com.tencent.kuikly.compose.foundation.lazy.layout.PrefetchScheduler
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.container.SlotProvider
import com.tencent.kuikly.compose.coroutines.internal.ComposeDispatcher
import com.tencent.kuikly.compose.foundation.event.OnBackPressedDispatcher
import com.tencent.kuikly.compose.foundation.event.OnBackPressedDispatcherOwner
import com.tencent.kuikly.compose.platform.Configuration
import com.tencent.kuikly.compose.profiler.RecompositionProfiler
import com.tencent.kuikly.compose.profiler.RecompositionTracker
import com.tencent.kuikly.compose.profiler.output.ProfilerOverlaySlot
import com.tencent.kuikly.core.module.FileModule
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.platform.WindowInfoImpl
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.scene.KuiklyComposeScene
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.compose.ui.KuiklyImageCacheManager
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.platform.LocalOnBackPressedDispatcherOwner
import com.tencent.kuikly.core.base.BackPressHandler
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.event.layoutFrameDidChange
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.VsyncModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.lifecycle.Lifecycle
import com.tencent.kuikly.lifecycle.LifecycleOwner
import com.tencent.kuikly.lifecycle.LifecycleRegistry
import com.tencent.kuikly.lifecycle.ViewModelStore
import com.tencent.kuikly.lifecycle.ViewModelStoreOwner
import com.tencent.kuikly.lifecycle.compose.LocalLifecycleOwner
import com.tencent.kuikly.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.coroutines.CoroutineContext

fun ComposeContainer.setContent(content: @Composable () -> Unit) {
    this.content = content
}

open class ComposeContainer :
    Pager(),
    OnBackPressedDispatcherOwner {

    companion object {
        /**
         * 当 Kuikly Compose 和原生 Compose 同时存在时，可以通过设置此配置为 `false` 来禁止Kuikly消费State变更，
         * 从而解决原生 Compose 的重组状态偶现丢失和 ANR 死锁的问题。
         *
         * 建议在ComposeContainer.willInit方法内使用，在setContent之前设置
         */
        var enableConsumeSnapshot: Boolean = true

        /**
         * 鸿蒙 native vsync 帧驱动总开关（debug/A-B 对比用），默认开启。
         * true: 走 native KRVsyncModule(OH_NativeVSync)，帧节拍与屏幕刷新率对齐；
         * false: 回退 12ms Timer 轮询（改造前行为），用于对比测试。
         * 注：宿主 native 库过旧(无 KRVsyncModule)时由 watchdog 自动兜底退回
         * 12ms Timer，行为与改造前一致，故无需按宿主灰度关闭本开关。
         */
        var ohosUseNativeVsync: Boolean = true

        /** watchdog 超时时间：超过该时长未收到首个 vsync tick 则退回 Timer */
        private const val OHOS_VSYNC_WATCHDOG_DELAY_MS = 100
    }

    override var ignoreLayout = true
    override var didCreateBody: Boolean = false

    private val lifecycleOwner: LifecycleOwner = object : LifecycleOwner {
        override val lifecycle = LifecycleRegistry(this)
        override val pagerId get() = this@ComposeContainer.pagerId
    }
    private val viewModelStoreOwner: ViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    var layoutDirection: LayoutDirection = LayoutDirection.Ltr

    private var mediator: ComposeSceneMediator? = null

    // —— 鸿蒙 vsync 驱动状态（回调与 watchdog 均在 context 线程，无需原子操作）——
    /** 是否已收到首个 native vsync tick */
    private var ohosVsyncTickArrived = false

    /** 帧驱动已停止（页面销毁），使在途 watchdog 回调失效 */
    private var ohosVsyncDriverStopped = false

    /** watchdog 兜底退回的 12ms Timer */
    private var ohosVsyncFallbackTimer: Timer? = null

    internal var content: (@Composable () -> Unit)? = null

    private val windowInfo = WindowInfoImpl()

    private val rootKView: DivView by lazy {
        DivView()
    }

    internal val imageCacheManager by lazy(LazyThreadSafetyMode.NONE) {
        KuiklyImageCacheManager(this)
    }

    private var configuration: Configuration? = null

    /**
     * Profiler 生命周期监听器，负责在 Profiler start 时把 FileModule 实例传入。
     * 页面销毁时注销，避免内存泄漏。
     */
    private val fileModuleListener = object : RecompositionProfiler.ProfilerLifecycleListener {
        override fun onProfilerStarted(tracker: RecompositionTracker) {
            getModule<FileModule>(FileModule.MODULE_NAME)?.let {
                RecompositionProfiler.setFileModule(it)
            }
        }
        override fun onProfilerStopped() { /* nothing */ }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        val ctx = this
        addChild(rootKView) {
            attr {
                absolutePositionAllZero()
            }
            event {
                layoutFrameDidChange { }
            }
        }
        // 注册 Profiler 生命周期回调，确保 start() 在页面创建后调用时也能拿到 FileModule
        RecompositionProfiler.addLifecycleListener(fileModuleListener)
    }

    override fun onCreatePager(
        pagerId: String,
        pageData: JSONObject,
    ) {
        super.onCreatePager(pagerId, pageData)

        val frame =
            Frame(
                0f,
                0f,
                getPager().pageData.pageViewWidth,
                getPager().pageData.pageViewHeight,
            )
        setFrameToRenderView(frame)
        rootKView.setFrameToRenderView(frame)
        updateWindowContainer(frame)

        createMediatorIfNeeded()
        setComposeContent {
            this.content?.invoke()
        }
        startFrameDispatcher()

        // 如果 Profiler 已启用（start 先于页面创建），此时补传 FileModule
        if (RecompositionProfiler.isEnabled) {
            getModule<FileModule>(FileModule.MODULE_NAME)?.let {
                RecompositionProfiler.setFileModule(it)
            }
        }
    }

    private fun startFrameDispatcher() {
        mediator?.renderFrame()
        val pageData = getPager().pageData
        when {
            pageData.isMiniApp || pageData.isWeb -> {
                mediator?.startFrameDispatcher()
            }
            pageData.isOhOs -> {
                if (ohosUseNativeVsync) {
                    startOhosNativeVsyncDriver()
                } else {
                    // A/B 开关关闭：回到改造前的 12ms Timer 行为
                    mediator?.startFrameDispatcher()
                }
            }
            else -> {
                getModule<VsyncModule>(VsyncModule.MODULE_NAME)?.registerVsyncWithFrameInterval { frameIntervalNanos ->
                    mediator?.renderFrame(frameIntervalNanos)
                }
            }
        }
    }

    /**
     * 鸿蒙：native KRVsyncModule(OH_NativeVSync) 驱动帧调度，节拍与屏幕刷新率对齐。
     * 旧宿主 native 库未内置该模块时，注册请求被转发到 ArkTS 层且不会有回调，
     * 页面将失去帧驱动(仅首帧可渲染、无法滚动)。由 watchdog 超时退回 12ms Timer 兜底。
     */
    private fun startOhosNativeVsyncDriver() {
        ohosVsyncTickArrived = false
        ohosVsyncDriverStopped = false
        getModule<VsyncModule>(VsyncModule.MODULE_NAME)?.registerVsyncWithFrameInterval { frameIntervalNanos ->
            ohosVsyncTickArrived = true
            mediator?.renderFrame(frameIntervalNanos)
        }
        // watchdog 不保存 setTimeout 返回值、不显式取消：pager 销毁时
        // GlobalFunctions.destroyGlobalFunction(pagerId) 会清理该 pager 全部函数引用，
        // 到期 fire 为 no-op；另有 ohosVsyncDriverStopped 同线程标志兜底，故无需 ref。
        setTimeout(pagerId, OHOS_VSYNC_WATCHDOG_DELAY_MS) {
            if (!ohosVsyncTickArrived && !ohosVsyncDriverStopped) {
                KLog.i(
                    "ComposeContainer",
                    "ohos native vsync no callback, fallback to 12ms timer"
                )
                getModule<VsyncModule>(VsyncModule.MODULE_NAME)?.unRegisterVsync()
                ohosVsyncFallbackTimer = mediator?.startFrameDispatcher()
            }
        }
    }

    private fun stopFrameDispatcher() {
        val pageData = getPager().pageData
        if (pageData.isMiniApp || pageData.isWeb) {
            // miniApp/Web 与历史行为保持一致，不停止 Timer
        } else {
            ohosVsyncDriverStopped = true
            ohosVsyncFallbackTimer?.cancel()
            ohosVsyncFallbackTimer = null
            getModule<VsyncModule>(VsyncModule.MODULE_NAME)?.unRegisterVsync()
        }
    }

    override fun created() {
        super.created()
        updateLifecycleState(Lifecycle.State.CREATED)
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        mediator?.updateAppState(true)
        updateLifecycleState(Lifecycle.State.RESUMED)
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        updateLifecycleState(Lifecycle.State.CREATED)
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        stopFrameDispatcher()
        mediator?.updateAppState(false)
        dispose()
        updateLifecycleState(Lifecycle.State.DESTROYED)
        RecompositionProfiler.removeLifecycleListener(fileModuleListener)
    }

    private fun updateLifecycleState(state: Lifecycle.State) {
        (lifecycleOwner.lifecycle as LifecycleRegistry).currentState = state
    }

    @OptIn(InternalComposeUiApi::class)
    private fun createComposeScene(
        invalidate: () -> Unit,
        coroutineContext: CoroutineContext,
        prefetchScheduler: PrefetchScheduler,
    ): ComposeScene =
        KuiklyComposeScene(
            rootKView,
            Density(pagerDensity()),
            layoutDirection = layoutDirection,
            boundsInWindow = IntRect(0, 0, windowInfo.containerSize.width, windowInfo.containerSize.height),
            invalidate = invalidate,
            coroutineContext = coroutineContext,
            prefetchScheduler = prefetchScheduler,
        )

    private fun createMediatorIfNeeded() {
        if (configuration == null) {
            configuration = Configuration(this)
        }
        if (mediator == null) {
            mediator = createMediator()
        }
    }

    private fun dispose() {
        viewModelStoreOwner.viewModelStore.clear()
        mediator?.dispose()
        mediator = null
    }

    @OptIn(InternalComposeUiApi::class)
    private fun createMediator(): ComposeSceneMediator {
        val mediator =
            ComposeSceneMediator(
                rootKView,
                windowInfo,
                ComposeDispatcher(pagerId),
                pagerDensity(),
                ::createComposeScene,
            )
        return mediator
    }

    override fun onReceivePagerEvent(pagerEvent: String, eventData: JSONObject) {
        super.onReceivePagerEvent(pagerEvent, eventData)
        if (pagerEvent == PAGER_EVENT_ROOT_VIEW_SIZE_CHANGED) {
            val densityInfo = eventData.optString(DENSITY_INFO, "")
            if(densityInfo.isNotEmpty()) {
                val info = JSONObject(densityInfo)
                val newDensity = info.optDouble(DENSITY_INFO_KEY_NEW_DENSITY)
                mediator?.updateDensity(newDensity.toFloat())
            }
            val width = eventData.optDouble(WIDTH)
            val height = eventData.optDouble(HEIGHT)
            val newFrame = Frame(0f, 0f, width.toFloat(), height.toFloat())
            setFrameToRenderView(newFrame)
            rootKView.setFrameToRenderView(newFrame)
            updateWindowContainer(newFrame)
        } else if (pagerEvent == PAGER_EVENT_WINDOW_SIZE_CHANGED) {
            configuration?.onWindowSizeChanged(eventData.optDouble(WIDTH),eventData.optDouble(
                HEIGHT))
        } else if (pagerEvent == PAGER_EVENT_CONFIGURATION_DID_CHANGED) {
            val fontWeightScale = eventData.optDouble("fontWeightScale", 1.0)
            val fontSizeScale = eventData.optDouble("fontSizeScale", 1.0)
            configuration?.onFontConfigChange(fontSizeScale, fontWeightScale)
        }
    }

    private fun updateWindowContainer(frame: Frame) {
        windowInfo.containerSize = IntSize(
            (frame.width * pagerDensity()).fastRoundToInt(),
            (frame.height * pagerDensity()).fastRoundToInt()
        )
        configuration?.onRootViewSizeChanged(frame.width.toDouble(), frame.height.toDouble())
        mediator?.viewWillLayoutSubviews()
    }

    override fun onDestroyPager() {
        super.onDestroyPager()
    }

    @OptIn(InternalComposeApi::class)
    @Composable
    internal fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) {
        val slotProvider = remember { SlotProvider() }
        CompositionLocalProvider(
            LocalLifecycleOwner provides lifecycleOwner,
            LocalViewModelStoreOwner provides viewModelStoreOwner,
            // Kuikly
            LocalActivity provides this,
            LocalOnBackPressedDispatcherOwner provides this,
            LocalSlotProvider provides slotProvider,
            LocalConfiguration provides configuration!!,
            LocalKuiklyPrefetchScheduler provides mediator!!.prefetchScheduler,
        ) {
            content()
            LocalSlotProvider.current.slots.forEach { slotContent ->
                key(slotContent.first) {
                    slotContent.second?.invoke()
                }
            }
            // Profiler Overlay — 始终最后渲染，覆盖所有业务弹层
            val overlayStrategy = RecompositionProfiler.currentOverlayStrategy
            if (overlayStrategy != null) {
                ProfilerOverlaySlot(overlayStrategy)
            }
        }
    }

    private fun setComposeContent(content: @Composable () -> Unit) {
        mediator?.setContent {
            ProvideContainerCompositionLocals(content = content)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
            }
        }
    }

    override fun getBackPressHandler(): BackPressHandler {
        return onBackPressedDispatcher
    }

    override val onBackPressedDispatcher: OnBackPressedDispatcher by lazy { OnBackPressedDispatcher() }

    override fun isAccessibilityRunning(): Boolean {
        return pageData.isAccessibilityRunning
    }

    /**
     * 注册扩展module接口，（注：注册时机为override ComponentActivity.createExternalModules中统一注册）
     */
    override fun createExternalModules(): Map<String, Module>? {
        return null
    }
}

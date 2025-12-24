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

@file:OptIn(ExperimentalComposeUiApi::class)

package com.tencent.kuikly.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.container.SlotProvider
import com.tencent.kuikly.compose.coroutines.internal.ComposeDispatcher
import com.tencent.kuikly.compose.foundation.event.OnBackPressedDispatcher
import com.tencent.kuikly.compose.foundation.event.OnBackPressedDispatcherOwner
import com.tencent.kuikly.compose.platform.Configuration
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.GlobalSnapshotManager
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
import com.tencent.kuikly.core.module.VsyncModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
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

    internal var content: (@Composable () -> Unit)? = null

    private val windowInfo = WindowInfoImpl()

    private val rootKView: DivView by lazy {
        DivView()
    }

    internal val imageCacheManager by lazy(LazyThreadSafetyMode.NONE) {
        KuiklyImageCacheManager(this)
    }

    private var configuration: Configuration? = null

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
    }

    private fun startFrameDispatcher() {
        mediator?.renderFrame()
        if (!getPager().pageData.isAndroid) {
            mediator?.startFrameDispatcher()
        } else {
            getModule<VsyncModule>(VsyncModule.MODULE_NAME)?.registerVsync {
                mediator?.renderFrame()
            }
        }
    }

    private fun stopFrameDispatcher() {
        if (!getPager().pageData.isAndroid) {
        } else {
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
    }

    private fun updateLifecycleState(state: Lifecycle.State) {
        (lifecycleOwner.lifecycle as LifecycleRegistry).currentState = state
    }

    @OptIn(InternalComposeUiApi::class)
    private fun createComposeScene(
        invalidate: () -> Unit,
        coroutineContext: CoroutineContext,
    ): ComposeScene =
        KuiklyComposeScene(
            rootKView,
            Density(pagerDensity()),
            layoutDirection = layoutDirection,
            boundsInWindow = IntRect(0, 0, windowInfo.containerSize.width, windowInfo.containerSize.height),
            invalidate = invalidate,
            coroutineContext = coroutineContext,
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
            val width = eventData.optDouble(WIDTH)
            val height = eventData.optDouble(HEIGHT)
            val newFrame = Frame(0f, 0f, width.toFloat(), height.toFloat())
            setFrameToRenderView(newFrame)
            rootKView.setFrameToRenderView(newFrame)
            updateWindowContainer(newFrame)
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
            LocalConfiguration provides configuration!!
        ) {
            content()
            LocalSlotProvider.current.slots.forEach { slotContent ->
                key(slotContent.first) {
                    slotContent.second?.invoke()
                }
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
}

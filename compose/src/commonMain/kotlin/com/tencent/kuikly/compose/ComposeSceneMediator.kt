/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.tencent.kuikly.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.lazy.layout.FramePrefetchScheduler
import com.tencent.kuikly.compose.foundation.lazy.layout.PrefetchScheduler
import com.tencent.kuikly.compose.foundation.lazy.layout.createDefaultKuiklyPrefetchScheduler
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.platform.WindowInfo
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.container.SuperTouchManager
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.DivView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalComposeUiApi::class)
class ComposeSceneMediator(
    private val container: DivView,
    private val windowInfo: WindowInfo,
    private val coroutineContext: CoroutineContext,
    private val density: Float,
    private val composeSceneFactory: (
        invalidate: () -> Unit,
        coroutineContext: CoroutineContext,
        prefetchScheduler: PrefetchScheduler,
    ) -> ComposeScene
) {

    @OptIn(ExperimentalFoundationApi::class)
    internal val prefetchScheduler: FramePrefetchScheduler =
        createDefaultKuiklyPrefetchScheduler()

    private var hasStartRender = false
    val superTouchManager = SuperTouchManager()

    fun updateAppState(isApplicationActive: Boolean) {
        scene.vsyncTickConditions.isApplicationActive = isApplicationActive
        if (isApplicationActive) {
            // resume后 强制Draw两次 避免动画不刷新
            onComposeSceneInvalidate()
        }
    }

    @OptIn(InternalComposeUiApi::class)
    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            ::onComposeSceneInvalidate,
            coroutineContext,
            prefetchScheduler,
        )
    }

    fun onComposeSceneInvalidate() {
        scene.vsyncTickConditions.needRedraw()
    }

    @OptIn(InternalComposeUiApi::class)
    fun setContent(content: @Composable () -> Unit) {
        if (hasStartRender) {
            return
        }
        scene.setContent(content)
        hasStartRender = true
    }

    fun dispose() {
        prefetchScheduler.cancelAll()
        scene.close()
    }

    fun viewWillLayoutSubviews() {
        val boundsInWindow = IntRect(
            offset = IntOffset.Zero,
            size = IntSize(
                width = windowInfo.containerSize.width,
                height = windowInfo.containerSize.height,
            )
        )
        scene.boundsInWindow = boundsInWindow
        onComposeSceneInvalidate()
    }

    @OptIn(DelicateCoroutinesApi::class, InternalComposeUiApi::class)
    fun startFrameDispatcher(): Timer {
        val timer = Timer()
        timer.schedule(0, 12) {
            renderFrame()
        }
        return timer
    }

    private var lastFrameTimeNanos = 0L

    /**
     * @param frameTimeNanos 本帧时间戳(纳秒)。VsyncModule 驱动时为真实 vsync 时间戳,
     * 以减少动画时间抖动;其他驱动(12ms Timer)沿用 [DateTime.nanoTime]。
     */
    fun renderFrame(frameTimeNanos: Long = DateTime.nanoTime()) {
        // 帧时钟必须单调递增,防御 vsync 时间戳与 nanoTime 时基的微小偏差
        val timestamp = if (frameTimeNanos > lastFrameTimeNanos) frameTimeNanos else lastFrameTimeNanos + 1
        lastFrameTimeNanos = timestamp
        scene.vsyncTickConditions.onDisplayLinkTick {
            scene.render(null, timestamp)
        }
    }

    fun updateDensity(toFloat: Float) {
        scene.density = Density(toFloat)
    }

    init {
        superTouchManager.manage(container, scene)
    }
}

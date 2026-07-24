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
import com.tencent.kuikly.core.module.VsyncFrameInfo
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

    fun renderFrame(frameInfo: VsyncFrameInfo? = null) {
        val timestampNanos = DateTime.nanoTime()
        val frameTimestampMillis =
            frameInfo?.timestampMillis?.takeIf { it > 0.0 }
                ?: timestampNanos.toDouble() / NANOS_PER_MILLISECOND
        val frameIntervalMillis =
            frameInfo?.frameIntervalMillis
                ?.takeIf { it in MIN_FRAME_INTERVAL_MILLIS..MAX_FRAME_INTERVAL_MILLIS }
                ?: DEFAULT_FRAME_INTERVAL_MILLIS
        val frameDeadlineMillis =
            frameInfo?.targetTimestampMillis
                ?.takeIf {
                    it >= frameTimestampMillis &&
                        it <= frameTimestampMillis + MAX_DEADLINE_INTERVALS * frameIntervalMillis
                }
                ?: frameTimestampMillis + frameIntervalMillis
        scene.vsyncTickConditions.updateFrameTiming(
            frameTimestampMillis = frameTimestampMillis,
            frameIntervalMillis = frameIntervalMillis,
            frameDeadlineMillis = frameDeadlineMillis,
        )
        scene.vsyncTickConditions.onDisplayLinkTick {
            scene.render(null, timestampNanos)
        }
    }

    fun updateDensity(toFloat: Float) {
        scene.density = Density(toFloat)
    }

    init {
        superTouchManager.manage(container, scene)
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000.0
        const val DEFAULT_FRAME_INTERVAL_MILLIS = 1_000.0 / 60.0
        const val MIN_FRAME_INTERVAL_MILLIS = 1.0
        const val MAX_FRAME_INTERVAL_MILLIS = 100.0
        const val MAX_DEADLINE_INTERVALS = 2.0
    }
}

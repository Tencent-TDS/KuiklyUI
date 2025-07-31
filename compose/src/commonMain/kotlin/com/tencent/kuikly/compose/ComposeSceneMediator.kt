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

@file:OptIn(InternalComposeUiApi::class)

package com.tencent.kuikly.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.platform.WindowInfo
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.container.SlotProvider
import com.tencent.kuikly.compose.container.SuperTouchManager
import com.tencent.kuikly.compose.platform.Configuration
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
    private val composeSceneFactory: (invalidate: () -> Unit, coroutineContext: CoroutineContext) -> ComposeScene
) {

    private var hasStartRender = false
    val configuration = Configuration()
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
            coroutineContext
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
        scene.setContent {
            ProvideComposeSceneMediatorCompositionLocals {
                content()
                LocalSlotProvider.current.slots.forEach { slotContent ->
                    key(slotContent.first) {
                        slotContent.second?.invoke()
                    }
                }
            }
        }
        hasStartRender = true
    }

    fun dispose() {
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

    fun renderFrame() {
        val timestamp = DateTime.nanoTime()
        scene.vsyncTickConditions.onDisplayLinkTick {
            scene.render(null, timestamp)
        }
    }

    @Composable
    private fun ProvideComposeSceneMediatorCompositionLocals(content: @Composable () -> Unit) {
        val slotProvider = remember { SlotProvider() }
        CompositionLocalProvider(
            LocalSlotProvider provides slotProvider,
            LocalConfiguration provides configuration,
            content = content
        )
    }

    init {
        superTouchManager.manage(container, scene)
    }
}

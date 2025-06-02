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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.platform.InteractionView
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.platform.WindowInfo
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.scene.ComposeScenePointer
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.container.SlotProvider
import com.tencent.kuikly.compose.platform.Configuration
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.timer.Timer
import com.tencent.kuikly.core.views.DivEvent
import com.tencent.kuikly.core.views.DivView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalComposeUiApi::class)
class ComposeSceneMediator(
    private val container: DivView,
    private val windowInfo: WindowInfo,
    private val coroutineContext: CoroutineContext,
    private val density: Float,
    private val composeSceneFactory: (invalidate: () -> Unit, coroutineContext: CoroutineContext) -> ComposeScene,
) {
    private var hasStartRender = false

    val configuration = Configuration()

    /**
     * Touch事件已经被Native侧消费，Compose要做一些取消Touch状态的处理
     * 否则会错误的触发点击
     */
    private var touchConsumeByNative = false

    @OptIn(InternalComposeUiApi::class)
    private val scene: ComposeScene by lazy {
        composeSceneFactory(
            ::onComposeSceneInvalidate,
            coroutineContext,
        )
    }

    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    private val touchesDelegate: InteractionView.Delegate by lazy {
        object : InteractionView.Delegate {
            override fun pointInside(
                x: Float,
                y: Float,
            ): Boolean = true

            override fun onTouchesEvent(
                touches: List<Touch>,
                type: PointerEventType,
                timestamp: Long,
                isConsumeByNative: Boolean,
            ): ProcessResult =
                scene.sendPointerEvent(
                    eventType = type,
                    pointers =
                    touches.map { touch ->
                        val position = Offset(touch.pageX * density, touch.pageY * density)
                        ComposeScenePointer(
                            id = PointerId(touch.pointerId),
                            position = position,
                            pressed = (type != PointerEventType.Release),
                            type = PointerType.Touch,
                        )
                    },
                    timeMillis = timestamp,
                    nativeEvent =
                    if (isConsumeByNative) {
                        "cancel"
                    } else {
                        null
                    },
                )
        }
    }

    init {
        container.getViewAttr().superTouch(true)
        container.getViewEvent().run {
            touchDown(true) {
                touchConsumeByNative = false
                val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Press, it.timestamp)

                if (result.dispatchedToAPointerInputModifier) {
                    getView()?.getViewAttr()?.forceUpdate = true
                    getView()?.getViewAttr()?.consumeTouchDown(true)
                }
            }
            setTouchMove(true)
            touchUp(false) {
                touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp, touchConsumeByNative)
                if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                    container.getViewAttr().preventTouch(false)
                    container.getViewEvent().setTouchMove(true)
                }
            }
            touchCancel(false) {
                touchConsumeByNative = true

                touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp, touchConsumeByNative)
                if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                    container.getViewAttr().preventTouch(false)
                    container.getViewEvent().setTouchMove(true)
                }
            }
        }
    }

    private fun DivEvent.setTouchMove(isSync: Boolean) {
        touchMove(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Move, it.timestamp)
            if (result.anyMovementConsumed) {
                container.getViewAttr().preventTouch(true)
                container.getViewEvent().setTouchMove(false)
            }
        }
    }

    fun updateAppState(isApplicationActive: Boolean) {
        scene.vsyncTickConditions.isApplicationActive = isApplicationActive
        if (isApplicationActive) {
            // resume后 强制Draw两次 避免动画不刷新
            onComposeSceneInvalidate()
        }
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
                val currentConfiguration = LocalConfiguration.current
                key(currentConfiguration.pageViewWidth, currentConfiguration.pageViewHeight) {
                    content()
                }
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
        val boundsInWindow =
            IntRect(
                offset = IntOffset.Zero,
                size =
                    IntSize(
                        width = windowInfo.containerSize.width,
                        height = windowInfo.containerSize.height,
                    ),
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

}

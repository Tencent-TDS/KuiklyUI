/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

@file:OptIn(InternalComposeUiApi::class)

package com.tencent.kuikly.compose.container

import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.platform.InteractionView
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.scene.ComposeScenePointer
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.views.DivEvent
import com.tencent.kuikly.core.views.DivView

class SuperTouchManager {

    private lateinit var container: DivView

    private lateinit var scene: ComposeScene

    private var layoutNode: KNode<*>? = null

    private var _useSyncMove: Boolean? = null
    private val DivEvent.useSyncMove
        get() = _useSyncMove ?: (!getPager().pageData.isOhOs).also { _useSyncMove = it }

    internal fun manage(container: DivView, scene: ComposeScene, layoutNode: KNode<*>? = null) {
        this.layoutNode = layoutNode
        this.container = container
        this.scene = scene
        this.container.getViewAttr().superTouch(true)
        this.container.getViewEvent().run {
            setTouchDown(true)
            setTouchMove(useSyncMove)
            setTouchUp(false)
            setTouchCancel(false)
            setMouseHover()
        }
    }

    fun DivEvent.setTouchDown(isSync: Boolean) {
        touchDown(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Press, it.timestamp)
            com.tencent.kuikly.core.log.KLog.i("KuiklyDiag",
                "touchDown(Press) pos=${it.touches.firstOrNull()?.let { t -> "${t.x},${t.y}" }}, " +
                    "dispatched=${result.dispatchedToAPointerInputModifier}, consumed=${it.consumed}")
            if (result.dispatchedToAPointerInputModifier) {
                getView()?.getViewAttr()?.forceUpdate = true
                getView()?.getViewAttr()?.consumeTouchDown(true)
            }
        }
    }

    internal fun DivEvent.setTouchUp(isSync: Boolean) {
        touchUp(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp, it.consumed)
            com.tencent.kuikly.core.log.KLog.i("KuiklyDiag",
                "touchUp(Release) pos=${it.touches.firstOrNull()?.let { t -> "${t.x},${t.y}" }}, " +
                    "dispatched=${result.dispatchedToAPointerInputModifier}, nativeConsumed=${it.consumed}")
            if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                container.getViewAttr().preventTouch(false)
                if (useSyncMove) {
                    container.getViewEvent().setTouchMove(true)
                }
            }
        }
    }

    internal fun DivEvent.setTouchMove(isSync: Boolean) {
        touchMove(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Move, it.timestamp, it.consumed)
            if (!it.consumed) {
                if (result.anyMovementConsumed) {
                    container.getViewAttr().preventTouch(true)
                    if (useSyncMove) {
                        container.getViewEvent().setTouchMove(false)
                    }
                }
            }
        }
    }

    internal fun DivEvent.setTouchCancel(isSync: Boolean) {
        touchCancel(isSync) {
            com.tencent.kuikly.core.log.KLog.i("KuiklyDiag",
                "touchCancel pos=${it.touches.firstOrNull()?.let { t -> "${t.x},${t.y}" }}, nativeConsumed=${it.consumed}")
            touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp, true)
            if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                container.getViewAttr().preventTouch(false)
                if (useSyncMove) {
                    container.getViewEvent().setTouchMove(true)
                }
            }
        }
    }

    /**
     * 注册 macOS 鼠标悬停事件，将 mouseEnter/mouseExit 转换为
     * PointerType.Mouse 类型的 PointerEvent 注入 Compose pointer 系统，
     * 使 HitPathTracker 自动合成 PointerEventType.Enter/Exit，
     * 从而驱动 HoverInteraction 和 Modifier.hoverable() 标准 API。
     */
    @OptIn(ExperimentalComposeUiApi::class)
    internal fun DivEvent.setMouseHover() {
        mouseEnter {
            sendHoverPointerEvent(PointerEventType.Enter)
        }
        mouseExit {
            sendHoverPointerEvent(PointerEventType.Exit)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun sendHoverPointerEvent(eventType: PointerEventType) {
        val pageDensity = container.getPager().pagerDensity()
        val containerWidth = (container.getViewAttr().getProp("width") as? Number)?.toFloat() ?: 0f
        val containerHeight = (container.getViewAttr().getProp("height") as? Number)?.toFloat() ?: 0f
        // Enter 时坐标在容器内（中心），Exit 时坐标在容器外
        val position = when (eventType) {
            PointerEventType.Enter -> Offset(
                containerWidth * pageDensity * 0.5f,
                containerHeight * pageDensity * 0.5f
            )
            else -> Offset(-1f, -1f)
        }
        // 直接发送 Enter/Exit 类型，PointerType.Mouse 保证 issuesEnterExitEvent = true，
        // HitPathTracker 会直接传递 Enter/Exit 给 PointerInputModifierNode
        scene.sendPointerEvent(
            eventType = eventType,
            pointers = listOf(
                ComposeScenePointer(
                    id = HOVER_POINTER_ID,
                    position = position,
                    pressed = false,
                    type = PointerType.Mouse,
                )
            ),
            timeMillis = com.tencent.kuikly.core.datetime.DateTime.currentTimestamp(),
            rootNode = layoutNode
        )
    }

    companion object {
        /** hover 专用的 pointerId，与 touch 事件的 pointerId 隔离 */
        private val HOVER_POINTER_ID = PointerId(Long.MAX_VALUE - 1)
    }

    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    private val touchesDelegate: InteractionView.Delegate by lazy {
        object : InteractionView.Delegate {
            override fun pointInside(x: Float, y: Float): Boolean = true
            override fun onTouchesEvent(touches: List<Touch>, type: PointerEventType, timestamp: Long,
                                        isConsumeByNative: Boolean): ProcessResult {
                // because density may change by sizeChange Event on ohos
                // here need to fetch the density realtime
                val pageDensity = container.getPager().pagerDensity()
                return scene.sendPointerEvent(
                    eventType = type,
                    pointers = touches.map { touch ->
                        val position = Offset(touch.x * pageDensity, touch.y * pageDensity)
                        ComposeScenePointer(
                            id = PointerId(touch.pointerId),
                            position = position,
                            pressed = (type != PointerEventType.Release),
                            type = PointerType.Touch,
                        )
                    },
                    timeMillis = timestamp,
                    nativeEvent = if (isConsumeByNative) {
                        "cancel"
                    } else {
                        null
                    },
                    rootNode = layoutNode
                )
            }
        }
    }

}
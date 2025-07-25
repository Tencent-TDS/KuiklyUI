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

    private var density = 3f

    internal fun manage(container: DivView, scene: ComposeScene, layoutNode: KNode<*>? = null) {
        this.layoutNode = layoutNode
        this.density = container.getPager().pagerDensity()
        this.container = container
        this.scene = scene
        this.container.getViewAttr().superTouch(true)
        this.container.getViewEvent().run {
            setTouchDown(true)
            setTouchMove(true)
            setTouchUp(false)
            setTouchCancel(false)
        }
    }

    fun DivEvent.setTouchDown(isSync: Boolean) {
        touchDown(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Press, it.timestamp)
            if (result.dispatchedToAPointerInputModifier) {
                getView()?.getViewAttr()?.forceUpdate = true
                getView()?.getViewAttr()?.consumeTouchDown(true)
            }
        }
    }

    internal fun DivEvent.setTouchUp(isSync: Boolean) {
        touchUp(isSync) {
            touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp)
            if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                container.getViewAttr().preventTouch(false)
                container.getViewEvent().setTouchMove(true)
            }
        }
    }

    internal fun DivEvent.setTouchMove(isSync: Boolean) {
        touchMove(isSync) {
            val result = touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Move, it.timestamp, it.consumed)
            if (!it.consumed) {
                if (result.anyMovementConsumed) {
                    container.getViewAttr().preventTouch(true)
                    container.getViewEvent().setTouchMove(false)
                }
            }
        }
    }

    internal fun DivEvent.setTouchCancel(isSync: Boolean) {
        touchCancel(isSync) {
            touchesDelegate.onTouchesEvent(it.touches, PointerEventType.Release, it.timestamp, true)
            if (container.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                container.getViewAttr().preventTouch(false)
                container.getViewEvent().setTouchMove(true)
            }
        }
    }

    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    private val touchesDelegate: InteractionView.Delegate by lazy {
        object : InteractionView.Delegate {
            override fun pointInside(x: Float, y: Float): Boolean = true
            override fun onTouchesEvent(touches: List<Touch>, type: PointerEventType, timestamp: Long,
                                        isConsumeByNative: Boolean): ProcessResult {
                return scene.sendPointerEvent(
                    eventType = type,
                    pointers = touches.map { touch ->
                        val position = Offset(touch.pageX * density, touch.pageY * density)
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
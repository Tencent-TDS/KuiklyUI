/*
 * Copyright 2024 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerId
import com.tencent.kuikly.compose.ui.input.pointer.PointerType
import com.tencent.kuikly.compose.ui.input.pointer.ProcessResult
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasurePolicy
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.materialize
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.node.NodeCoordinator
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.scene.ComposeScenePointer
import com.tencent.kuikly.compose.ui.scene.LocalComposeScene
import com.tencent.kuikly.compose.ui.semantics.dialog
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.compose.ui.util.fastMap
import com.tencent.kuikly.compose.ui.util.fastMaxBy
import com.tencent.kuikly.compose.container.LocalSlotProvider
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.platform.LocalOnBackPressedDispatcherOwner
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.event.Touch
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ModalView
import com.tencent.kuikly.core.views.willDismiss
import kotlin.js.JsName
import kotlin.math.min

@JsName("funDialogProperties")
fun DialogProperties(
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    usePlatformDefaultWidth: Boolean = true,
    scrimColor: Color = DefaultScrimColor
): DialogProperties {
    return KuiklyDialogProperties(
        dismissOnBackPress,
        dismissOnClickOutside,
        usePlatformDefaultWidth,
        scrimColor = scrimColor
    )
}

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
 *  * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 * dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 * the platform default, which is smaller than the screen width.
 * **Might be used only as named argument**.
 */
@Immutable
interface DialogProperties {
    val dismissOnBackPress: Boolean get() = true
    val dismissOnClickOutside: Boolean get() = true
    val usePlatformDefaultWidth: Boolean get() = true
}

/**
 * The default scrim opacity.
 */
private const val DefaultScrimOpacity = 0.6f
internal val DefaultScrimColor = Color.Black.copy(alpha = DefaultScrimOpacity)

/**
 * Properties used to customize the behavior of a [Dialog].
 *
 * @property dismissOnBackPress whether the popup can be dismissed by pressing the back button
 *  * on Android or escape key on desktop.
 * If true, pressing the back button will call onDismissRequest.
 * @property dismissOnClickOutside whether the dialog can be dismissed by clicking outside the
 * dialog's bounds. If true, clicking outside the dialog will call onDismissRequest.
 * @property usePlatformDefaultWidth Whether the width of the dialog's content should be limited to
 * the platform default, which is smaller than the screen width.
 * @property usePlatformInsets Whether the size of the dialog's content should be limited by
 * platform insets.
 * @property useSoftwareKeyboardInset Whether the size of the dialog's content should be limited by
 * software keyboard inset.
 * @property scrimColor Color of background fill.
 */
@Immutable
internal class KuiklyDialogProperties(
    override val dismissOnBackPress: Boolean = true,
    override val dismissOnClickOutside: Boolean = true,
    override val usePlatformDefaultWidth: Boolean = true,
    val usePlatformInsets: Boolean = true,
    val useSoftwareKeyboardInset: Boolean = true,
    val scrimColor: Color = Color.Transparent,
    val contentAlignment: Alignment? = null
) : DialogProperties {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KuiklyDialogProperties) return false

        if (dismissOnBackPress != other.dismissOnBackPress) return false
        if (dismissOnClickOutside != other.dismissOnClickOutside) return false
        if (usePlatformDefaultWidth != other.usePlatformDefaultWidth) return false
        if (usePlatformInsets != other.usePlatformInsets) return false
        if (useSoftwareKeyboardInset != other.useSoftwareKeyboardInset) return false
        if (scrimColor != other.scrimColor) return false
        if (contentAlignment != other.contentAlignment) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dismissOnBackPress.hashCode()
        result = 31 * result + dismissOnClickOutside.hashCode()
        result = 31 * result + usePlatformDefaultWidth.hashCode()
        result = 31 * result + usePlatformInsets.hashCode()
        result = 31 * result + useSoftwareKeyboardInset.hashCode()
        result = 31 * result + scrimColor.hashCode()
        result = 31 * result + (contentAlignment?.hashCode() ?: 0)
        return result
    }
}

private fun DialogProperties.asKuiklyDialogProperties(): KuiklyDialogProperties {
    return this as? KuiklyDialogProperties ?: KuiklyDialogProperties(
        dismissOnBackPress,
        dismissOnClickOutside,
        usePlatformDefaultWidth
    )
}

/**
 * Opens a dialog with the given content.
 *
 * A dialog is a small window that prompts the user to make a decision or enter
 * additional information. A dialog does not fill the screen and is normally used
 * for modal events that require users to take an action before they can proceed.
 *
 * The dialog is visible as long as it is part of the composition hierarchy.
 * In order to let the user dismiss the Dialog, the implementation of [onDismissRequest] should
 * contain a way to remove the dialog from the composition hierarchy.
 *
 * Example usage:
 *
 * @sample androidx.compose.ui.samples.DialogSample
 *
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @param properties [DialogProperties] for further customization of this dialog's behavior.
 * @param content The content to be displayed inside the dialog.
 */
@NonRestartableComposable
@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) = DialogLayout(
    modifier = Modifier.semantics { dialog() },
    onDismissRequest = onDismissRequest,
    properties = properties.asKuiklyDialogProperties(),
    content = content
)

private object DialogMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val node = (this as? NodeCoordinator)?.layoutNode as? KNode<*>
        return if (node != null) {
            val platformConstraints = calculatePlatformConstrains(node)
            val placeables = measurables.fastMap { it.measure(platformConstraints) }
            layout(0, 0) {
                placeables.fastForEach {
                    it.place(0, 0)
                }
            }
        } else {
            layout(0, 0) {}
        }
    }

    private fun MeasureScope.calculatePlatformConstrains(node: KNode<*>): Constraints {
        val (width, height) = node.view.getPager().pageData.let {
            (if (it.activityWidth > 0f) it.activityWidth else it.deviceWidth) to
                    (if (it.activityHeight > 0f) it.activityHeight else it.deviceHeight)
        }
        return Constraints(maxWidth = width.dp.roundToPx(), maxHeight = height.dp.roundToPx())
    }
}

private class DialogContentMeasurePolicy(
    val usePlatformDefaultWidth: Boolean,
    val properties: KuiklyDialogProperties
): MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val dialogConstraints = if (usePlatformDefaultWidth) {
            constraints.copy(
                maxWidth = min(preferredDialogWidth(constraints), constraints.maxWidth)
            )
        } else {
            constraints
        }
        val placeables = measurables.fastMap { it.measure(dialogConstraints) }
        val contentSize = IntSize(
            width = placeables.fastMaxBy { it.width }?.width ?: dialogConstraints.minWidth,
            height = placeables.fastMaxBy { it.height }?.height ?: dialogConstraints.minHeight
        )

        val position = if (properties.contentAlignment == Alignment.BottomCenter) {
            IntOffset(
                x = (constraints.maxWidth - contentSize.width) / 2,
                y = constraints.maxHeight - contentSize.height
            )
        } else {
            IntOffset(
                x = (constraints.maxWidth - contentSize.width) / 2,
                y = (constraints.maxHeight - contentSize.height) / 2
            )
        }

        return layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.fastForEach {
                it.place(position.x, position.y)
            }
        }
    }

    // Ported from Android. See https://cs.android.com/search?q=abc_config_prefDialogWidth
    private fun Density.preferredDialogWidth(constraints: Constraints): Int {
        val smallestWidth = min(constraints.maxWidth, constraints.maxHeight).toDp()
        return when {
            smallestWidth >= 600.dp -> 580.dp
            smallestWidth >= 480.dp -> 440.dp
            else -> 320.dp
        }.roundToPx()
    }
}

@Composable
private fun DialogLayout(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    properties: KuiklyDialogProperties,
    content: @Composable () -> Unit
) {
    val currentContent by rememberUpdatedState(content)
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    val slotProvider = LocalSlotProvider.current

    // 插槽标识符
    var slotId = remember { 0 }
    val backPressedDispatcher= LocalOnBackPressedDispatcherOwner.current


    DisposableEffect(Unit) {
        // 插槽内容
        slotId = slotProvider.addSlot {
            ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
                factory = {
                    KNode(ModalView().also {
                        it.inWindow = true
                    }) {
                        getViewEvent().willDismiss {
                            backPressedDispatcher.onBackPressedDispatcher.dispatchOnBackEvent()
                        }
                    }
                },
                update = {
                    set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
                    @OptIn(ExperimentalComposeUiApi::class)
                    set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
                    set(DialogMeasurePolicy, ComposeUiNode.SetMeasurePolicy)
                    set(modifier, ComposeUiNode.SetModifier)
                },
                content = {
                    DialogContent(
                        properties = properties,
                        onDismissRequest = {
                            onDismissRequest()
                        },
                        content = currentContent
                    )
                }
            )
        }

        onDispose {
            slotProvider.removeSlot(slotId)
        }
    }
}

@OptIn(InternalComposeUiApi::class)
@Composable
private fun DialogContent(
    properties: KuiklyDialogProperties,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap
    val scene = LocalComposeScene.current
    val measurePolicy = remember(properties.usePlatformDefaultWidth, properties) {
        DialogContentMeasurePolicy(properties.usePlatformDefaultWidth, properties)
    }
    val modifier: Modifier
    val wrappedContent: @Composable () -> Unit

    BackHandler {
        onDismissRequest()
    }

    if (properties.dismissOnClickOutside) {
        modifier = currentComposer.materialize(Modifier.clickable { onDismissRequest() })
        wrappedContent = {
            Box(modifier = Modifier.clickable {  }) {
                content()
            }
        }
    } else {
        modifier = Modifier
        wrappedContent = content
    }
    ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
        factory = {
            val view = DialogContentView(scene, properties.scrimColor)
            val layoutNode = KNode(view)
            view.layoutNode = layoutNode
            layoutNode
        },
        update = {
            set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
            @OptIn(ExperimentalComposeUiApi::class)
            set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(modifier, ComposeUiNode.SetModifier)
        },
        content = wrappedContent
    )
}

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
private class DialogContentView(
    private val scene: ComposeScene?,
    private val scrimColor: Color
) : DivView() {
    lateinit var layoutNode: KNode<DialogContentView>

    private var touchConsumeByNative = false
    override fun willInit() {
        super.willInit()
        val ctx = this
        getViewAttr().apply {
            superTouch(true)
            backgroundColor(ctx.scrimColor.toKuiklyColor())
        }
        getViewEvent().apply {
            touchDown(true) {
                ctx.touchConsumeByNative = false
                val result = ctx.onTouchesEvent(
                    it.touches,
                    PointerEventType.Press,
                    it.timestamp
                )
                if (result.dispatchedToAPointerInputModifier) {
                    getView()?.getViewAttr()?.forceUpdate = true
                    getView()?.getViewAttr()?.consumeTouchDown(true)
                }
            }
            ctx.setTouchMove(true)
            touchUp(false) {
                ctx.onTouchesEvent(
                    it.touches,
                    PointerEventType.Release,
                    it.timestamp,
                    ctx.touchConsumeByNative
                )
                if (ctx.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                    ctx.getViewAttr().preventTouch(false)
                    ctx.setTouchMove(true)
                }
            }
            touchCancel(false) {
                ctx.touchConsumeByNative = true

                ctx.onTouchesEvent(
                    it.touches,
                    PointerEventType.Release,
                    it.timestamp,
                    ctx.touchConsumeByNative
                )
                if (ctx.getViewAttr().getProp(StyleConst.PREVENT_TOUCH) == true) {
                    ctx.getViewAttr().preventTouch(false)
                    ctx.setTouchMove(true)
                }
            }
        }
    }

    private fun setTouchMove(isSync: Boolean) {
        val ctx = this
        getViewEvent().touchMove(isSync) {
            val handled = ctx.onTouchesEvent(
                it.touches,
                PointerEventType.Move,
                it.timestamp
            )
            if (handled.anyMovementConsumed) {
                ctx.getViewAttr().preventTouch(true)
                ctx.setTouchMove(false)
            }
        }
    }

    private fun onTouchesEvent(
        touches: List<Touch>,
        type: PointerEventType,
        timestamp: Long,
        isConsumeByNative: Boolean = false
    ): ProcessResult {
        return scene?.sendPointerEvent(
            eventType = type,
            pointers = touches.map { touch ->
                val position = Offset(
                    touch.pageX * getPager().pagerDensity(),
                    touch.pageY * getPager().pagerDensity()
                )
                ComposeScenePointer(
                    id = PointerId(touch.pointerId.toLong()),
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
        ) ?: ProcessResult(false, false)
    }
}

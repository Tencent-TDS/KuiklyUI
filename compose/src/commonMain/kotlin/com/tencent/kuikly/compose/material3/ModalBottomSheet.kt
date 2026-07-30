/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *2
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.gestures.detectVerticalDragGestures
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.animation.core.MutableTransitionState
import com.tencent.kuikly.compose.ui.window.DefaultScrimColor
import com.tencent.kuikly.compose.ui.window.KuiklyDialogProperties
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.tencent.kuikly.compose.animation.core.animate
import kotlin.math.roundToInt

/**
 * A modal bottom sheet that slides up from the bottom of the screen.
 * 
 * This component provides a modal bottom sheet that slides up from the bottom of the screen,
 * with a background scrim and animation effects. It can be dismissed by tapping the scrim
 * or pressing the back button.
 * 
 * Parameters:
 * @param visible Controls the visibility of the bottom sheet
 * @param onDismissRequest Callback to be invoked when the bottom sheet needs to be dismissed
 * @param modifier Modifier to be applied to the bottom sheet
 * @param containerColor Background color of the bottom sheet
 * @param contentColor Color of the content inside the bottom sheet
 * @param tonalElevation Elevation of the bottom sheet
 * @param scrimColor Color of the background scrim
 * @param dismissOnDrag Whether the bottom sheet can be dismissed by dragging down. Defaults to false.
 * @param content Content of the bottom sheet
 * 
 * Example:
 * ```
 * var showBottomSheet by remember { mutableStateOf(false) }
 * 
 * Button(onClick = { showBottomSheet = true }) {
 *     Text("Show Bottom Sheet")
 * }
 * 
 * ModalBottomSheet(
 *     visible = showBottomSheet,
 *     onDismissRequest = { showBottomSheet = false },
 *     dismissOnDrag = true
 * ) {
 *     Column(
 *         modifier = Modifier.padding(16.dp),
 *         horizontalAlignment = Alignment.CenterHorizontally
 *     ) {
 *         Text("Bottom Sheet Content")
 *         Button(onClick = { showBottomSheet = false }) {
 *             Text("Close")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun ModalBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dismissOnDrag: Boolean = false,
    dismissThreshold: Float = 0.25f,
    animationDurationMillis: Int = 250,
    content: @Composable ColumnScope.() -> Unit
) {
    var visibleState = remember { MutableTransitionState(false) }
    val scope = rememberCoroutineScope()

    // 拖拽相关的状态
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    // 优化：让 scrim 的透明度跟随 visibleState.targetState 同步变化
    // 这样 scrim 会在退出动画开始时就开始淡出，而不是等到动画完成
    // 使用 targetState 而不是 currentState，这样当 targetState 变为 false 时，scrim 立即开始淡出
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visibleState.targetState) 1f else 0f,
        animationSpec = tween(durationMillis = animationDurationMillis), // 与 slideOutVertically 的默认动画时长保持一致
        label = "scrimAlpha"
    )

    // 根据动画透明度动态计算 scrim 颜色
    val animatedScrimColor = remember(scrimColor, scrimAlpha) {
        scrimColor.copy(alpha = scrimColor.alpha * scrimAlpha)
    }

    LaunchedEffect(visible) {
        if (visible && !visibleState.currentState) {
            // 启动动画
            visibleState.targetState = true
            dragOffset = 0f
        } else if (!visible && visibleState.currentState) {
            visibleState.targetState = false
            dragOffset = 0f
        }
    }

    LaunchedEffect(visibleState.isIdle, visibleState.currentState, visibleState.targetState) {
        // 动画播完，调用onDismissRequest;
        if (visibleState.isIdle && !visibleState.currentState) {
            onDismissRequest.invoke()
        }
    }

    if (visible || visibleState.currentState) {
        Dialog(
            onDismissRequest = {
                visibleState.targetState = false
            },
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                scrimColor = animatedScrimColor, // 使用动态计算的 scrim 颜色
                contentAlignment = Alignment.BottomCenter
            )
        ) {
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = animationDurationMillis),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = animationDurationMillis),
                    targetOffsetY = { it },
                ),
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            sheetHeight = size.height.toFloat()
                        }
                        .then(
                            if (dismissOnDrag) {
                                Modifier
                                    .offset { IntOffset(0, dragOffset.roundToInt()) }
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                animationJob?.cancel()
                                            },
                                            onDragEnd = {
                                                // 如果拖拽超过阈值，则关闭
                                                // pointerInputScope 扩展了 Density，可以直接使用 toPx()
                                                val threshold = if (sheetHeight > 0f) {
                                                    sheetHeight * dismissThreshold
                                                } else {
                                                    100.dp.toPx()
                                                }
                                                
                                                if (dragOffset > threshold) {
                                                    scope.launch {
                                                        visibleState.targetState = false
                                                    }
                                                } else {
                                                    // 否则回弹
                                                    animationJob = scope.launch {
                                                        animate(
                                                            initialValue = dragOffset,
                                                            targetValue = 0f,
                                                            animationSpec = tween(durationMillis = animationDurationMillis)
                                                        ) { value, _ ->
                                                            dragOffset = value
                                                        }
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                animationJob = scope.launch {
                                                    animate(
                                                        initialValue = dragOffset,
                                                        targetValue = 0f,
                                                        animationSpec = tween(durationMillis = animationDurationMillis)
                                                    ) { value, _ ->
                                                        dragOffset = value
                                                    }
                                                }
                                            },
                                            onVerticalDrag = { _, dragAmount ->
                                                // 更新拖拽偏移量，允许向上回弹但不能小于0
                                                val newOffset = dragOffset + dragAmount
                                                dragOffset = newOffset.coerceAtLeast(0f)
                                            }
                                        )
                                    }
                            } else {
                                Modifier
                            }
                        ),
                    color = containerColor,
                    contentColor = contentColor,
                    tonalElevation = tonalElevation
                ) {
                    Column(content = content)
                }
            }
        }
    }
}

/**
 * Contains useful Defaults for [ModalBottomSheet].
 */
internal object BottomSheetDefaults {
    /**
     * The default container color for [ModalBottomSheet].
     */
    val ContainerColor: Color = Color.White

    /**
     * The default scrim color used by [ModalBottomSheet].
     */
    val ScrimColor: Color = DefaultScrimColor
}

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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.pointerInputFilter
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.itemsIndexed
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.util.fastAll
import com.tencent.kuikly.compose.ui.util.fastAny
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.KuiklyDialogProperties
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 管理 Dialog 的显示/退出动画状态。
 *
 * 外部 [visible] 与内部 [visibleState] 分离时，若返回键/拖拽关闭只把 [visibleState] 置 false，
 * 而 [visible] 仍为 true，则 `visible && !visibleState` 会在下一帧把弹窗重新打开。
 * [isDismissing] 用于标记「正在执行退出动画」，避免该竞态。
 */
@Composable
private fun rememberDialogDismissState(
    visible: Boolean,
    dismissDelayMillis: Int,
    onDismissRequest: () -> Unit,
): DialogDismissState {
    var visibleState by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val dismissDialog: () -> Unit = remember(scope, dismissDelayMillis) {
        {
            if (visibleState && !isDismissing) {
                isDismissing = true
                visibleState = false
                scope.launch {
                    delay(dismissDelayMillis.toLong())
                    currentOnDismissRequest()
                    isDismissing = false
                }
            }
            Unit
        }
    }

    if (visible && !visibleState && !isDismissing) {
        visibleState = true
    } else if (!visible && visibleState) {
        dismissDialog()
    }

    return DialogDismissState(
        visibleState = visibleState,
        showDialog = visible || visibleState,
        dismissDialog = dismissDialog,
    )
}

private data class DialogDismissState(
    val visibleState: Boolean,
    val showDialog: Boolean,
    val dismissDialog: () -> Unit,
)

/**
 * 拖拽关闭的配置参数
 *
 * @param dismissThreshold 触发关闭的拖拽距离占内容高度的比例，默认 0.25
 * @param velocityThreshold 触发关闭的速度阈值（像素/秒）
 * @param touchSlopPx 手势方向判定的滑动阈值（像素）
 */
data class DragToDismissConfig(
    val dismissThreshold: Float = 0.25f,
    val maxDismissThresholdPx: Float = 100f,
    val velocityThreshold: Float = 1000f,
    val touchSlopPx: Float = 30f
)

/**
 * 可拖拽关闭的容器组件。
 *
 * 将内容包裹在此容器中，用户可以通过向下拖拽来关闭弹窗。
 * 拖拽过程中内容会跟手向下偏移，松手后根据拖拽距离和速度决定是否关闭。
 *
 * 手势冲突处理策略（两阶段协作）：
 * - Initial 阶段：如果已处于拖拽模式，消费所有事件，子列表收不到
 * - Final 阶段：如果未进入拖拽模式，检查子列表是否消费了事件，
 *   若子列表不可滚动且向下滑动超过阈值，则启动拖拽模式
 */
@Composable
fun DragToDismissContainer(
    onDismiss: () -> Unit,
    config: DragToDismissConfig = DragToDismissConfig(),
    /**
     * 可选回调：拖拽过程中实时通知当前偏移量（像素）。
     * 可用于在拖拽时同步驱动外部动画（如视频跟手缩放），消除停顿感。
     * 回弹/取消时会以 0f 回调，表示偏移归零。
     */
    onDragOffsetChange: ((Float) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentOnDragOffsetChange by rememberUpdatedState(onDragOffsetChange)
    val dragOffsetY = remember { Animatable(0f) }
    var contentHeight by remember { mutableFloatStateOf(0f) }

    // 手势追踪状态
    var lastY by remember { mutableFloatStateOf(0f) }
    var lastTime by remember { mutableStateOf(0L) }
    var downY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    // 方向判定：null=未判定，true=向下拖拽，false=非拖拽（交给子组件）
    var directionDecided by remember { mutableStateOf<Boolean?>(null) }
    var isPointerDown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .onSizeChanged { size ->
                contentHeight = size.height.toFloat()
            }
            // ═══ Initial 阶段：拖拽模式下拦截事件 + 驱动偏移 ═══
            .pointerInputFilter(
                pass = PointerEventPass.Initial,
                onTouchEvent = { event: PointerEvent ->
                    if (!dragging) return@pointerInputFilter

                    val change = event.changes.firstOrNull() ?: return@pointerInputFilter

                    // 如果已经进入拖拽模式，但发现 change 被消费了，
                    // 说明子组件抢占了事件，终止拖拽并回弹
                    if (event.changes.fastAny { it.isConsumed }) {
                        dragging = false
                        directionDecided = null
                        if (dragOffsetY.value > 0f) {
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                currentOnDragOffsetChange?.invoke(0f)
                            }
                        }
                        return@pointerInputFilter
                    }

                    // 已进入拖拽模式 → 消费所有事件，子列表收不到
                    event.changes.fastForEach { it.consume() }

                    if (change.pressed && isPointerDown) {
                        // 手指移动：驱动偏移量
                        val delta = change.position.y - lastY
                        lastY = change.position.y
                        lastTime = change.uptimeMillis
                        val newOffset = (dragOffsetY.value + delta).coerceAtLeast(0f)
                        scope.launch {
                            dragOffsetY.snapTo(newOffset)
                            currentOnDragOffsetChange?.invoke(newOffset)
                        }
                    } else if (!change.pressed && isPointerDown) {
                        // 手指抬起：判断关闭或回弹
                        isPointerDown = false
                        if (dragOffsetY.value > 0f) {
                            val dt = (change.uptimeMillis - lastTime).coerceAtLeast(1)
                            val velocityY = (change.position.y - lastY) / dt * 1000f
                            val threshold = min(
                                contentHeight * config.dismissThreshold,
                                config.maxDismissThresholdPx
                            )
                            if (dragOffsetY.value > threshold || velocityY > config.velocityThreshold) {
                                currentOnDismiss()
                            } else {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    // 回弹完成后通知偏移归零
                                    currentOnDragOffsetChange?.invoke(0f)
                                }
                            }
                        }
                        dragging = false
                        directionDecided = null
                    }
                },
                onTouchCancel = {
                    if (dragging) {
                        isPointerDown = false
                        dragging = false
                        directionDecided = null
                        if (dragOffsetY.value > 0f) {
                            scope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                // 取消手势后回弹完成，通知偏移归零
                                currentOnDragOffsetChange?.invoke(0f)
                            }
                        }
                    }
                }
            )
            // ═══ Final 阶段：方向判定 + 拖拽启动 ═══
            .pointerInputFilter(
                pass = PointerEventPass.Final,
                shareWithSiblings = false,
                onTouchEvent = { event: PointerEvent ->
                    // 已进入拖拽模式时，所有逻辑都在 Initial 阶段处理，Final 不做任何事
                    if (dragging) return@pointerInputFilter

                    val change = event.changes.firstOrNull() ?: return@pointerInputFilter

                    if (change.pressed && !isPointerDown) {
                        // ── 手指按下 ──
                        isPointerDown = true
                        downY = change.position.y
                        lastY = change.position.y
                        lastTime = change.uptimeMillis
                        // 如果上次拖拽未完全回弹（偏移 > 0），直接进入拖拽模式
                        if (dragOffsetY.value > 0f) {
                            dragging = true
                            directionDecided = true
                        } else {
                            directionDecided = null
                        }
                    } else if (change.pressed && isPointerDown) {
                        // ── 手指移动（方向判定阶段） ──
                        if (directionDecided == null) {
                            val dy = change.position.y - downY
                            if (abs(dy) > config.touchSlopPx) {
                                // 判断子列表是否可以滚动（通过 change.isConsumed 判断）
                                val listCanScroll = change.isConsumed
                                if (dy > 0 && !listCanScroll) {
                                    // 向下滑动 + 子列表不可滚动 → 进入拖拽模式
                                    directionDecided = true
                                    dragging = true
                                    // 注意：dragging = true 后，从下一个事件开始
                                    // Initial 阶段将全量拦截，子列表不再收到事件
                                } else {
                                    // 向上滑动 或 子列表可滚动 → 放弃拖拽
                                    directionDecided = false
                                }
                            }
                        }
                        lastY = change.position.y
                        lastTime = change.uptimeMillis
                    } else if (!change.pressed && isPointerDown) {
                        // ── 手指抬起（未进入拖拽模式的情况） ──
                        isPointerDown = false
                        directionDecided = null
                    }
                },
                onTouchCancel = {
                    if (!dragging) {
                        isPointerDown = false
                        directionDecided = null
                    }
                }
            )
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
    ) {
        content()
    }
}

/**
 * 底部弹窗对话框
 * 从屏幕底部滑入，支持点击背景关闭和拖拽关闭
 *
 * @param visible 控制弹窗显示/隐藏
 * @param onDismissRequest 关闭回调
 * @param enableDragToDismiss 是否支持拖拽关闭
 * @param content 弹窗内容
 */
@Composable
fun BottomSheetDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    enableDragToDismiss: Boolean = true,
    content: @Composable () -> Unit
) {
    val dismissDelayMillis = 250
    val dialogState = rememberDialogDismissState(visible, dismissDelayMillis, onDismissRequest)

    val scrimAlpha by animateFloatAsState(
        targetValue = if (dialogState.visibleState) 1f else 0f,
        animationSpec = tween(durationMillis = dismissDelayMillis)
    )

    if (dialogState.showDialog) {
        Dialog(
            onDismissRequest = dialogState.dismissDialog,
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                scrimColor = Color.Black.copy(alpha = 0.4f * scrimAlpha),
                contentAlignment = Alignment.BottomCenter
            )
        ) {
            if (dialogState.visibleState) {
                BackHandler(onBack = dialogState.dismissDialog)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val enterAnim = slideInVertically(
                    animationSpec = tween(durationMillis = dismissDelayMillis),
                    initialOffsetY = { it }
                ) + fadeIn()
                val exitAnim = slideOutVertically(
                    animationSpec = tween(durationMillis = dismissDelayMillis),
                    targetOffsetY = { it }
                ) + fadeOut()

                AnimatedVisibility(
                    visible = dialogState.visibleState,
                    enter = enterAnim,
                    exit = exitAnim
                ) {
                    if (enableDragToDismiss) {
                        DragToDismissContainer(
                            onDismiss = dialogState.dismissDialog,
                        ) {
                            BottomSheetContent(content)
                        }
                    } else {
                        BottomSheetContent(content)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color.White)
    ) {
        // 拖拽指示条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0))
            )
        }
        content()
    }
}

/**
 * 全屏弹窗对话框
 */
@Composable
fun FullScreenDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissDelayMillis = 200
    val dialogState = rememberDialogDismissState(visible, dismissDelayMillis, onDismissRequest)

    if (dialogState.showDialog) {
        Dialog(
            onDismissRequest = dialogState.dismissDialog,
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                scrimColor = Color.Transparent
            )
        ) {
            if (dialogState.visibleState) {
                BackHandler(onBack = dialogState.dismissDialog)
            }
            val alpha by animateFloatAsState(
                targetValue = if (dialogState.visibleState) 1f else 0f,
                animationSpec = tween(durationMillis = dismissDelayMillis)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f * alpha))
                    .clickable(onClick = dialogState.dismissDialog)
            ) {
                Box(modifier = Modifier.clickable { /* 阻止点击穿透到背景 */ }) {
                    content()
                }
            }
        }
    }
}

/**
 * 居中弹窗对话框
 */
@Composable
fun CenterDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissDelayMillis = 200
    val dialogState = rememberDialogDismissState(visible, dismissDelayMillis, onDismissRequest)

    if (dialogState.showDialog) {
        Dialog(
            onDismissRequest = dialogState.dismissDialog,
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                scrimColor = Color.Black.copy(alpha = 0.4f)
            )
        ) {
            if (dialogState.visibleState) {
                BackHandler(onBack = dialogState.dismissDialog)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val alpha by animateFloatAsState(
                    targetValue = if (dialogState.visibleState) 1f else 0f,
                    animationSpec = tween(durationMillis = dismissDelayMillis)
                )
                Box(
                    modifier = Modifier
                        .alpha(alpha)
                        .clickable { /* 阻止点击穿透到背景 */ }
                ) {
                    content()
                }
            }
        }
    }
}

@Page("BottomSheetDragDemo")
class BottomSheetDragDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("BottomSheet + DragToDismiss Demo") {
                BottomSheetDragDemoContent()
            }
        }
    }
}

@Composable
fun BottomSheetDragDemoContent() {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showBottomSheetNoDrag by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    var showCenter by remember { mutableStateOf(false) }
    var showSimpleDrag by remember { mutableStateOf(false) }

    val longListItems = remember { (1..50).map { "列表项 $it" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { showBottomSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("底部弹窗（带长列表 + 拖拽关闭）")
        }

        Button(
            onClick = { showBottomSheetNoDrag = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("底部弹窗（带长列表，禁止拖拽）")
        }

        Button(
            onClick = { showSimpleDrag = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("DragToDismiss 最简接入（长列表）")
        }

        Button(
            onClick = { showFullScreen = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("全屏弹窗")
        }

        Button(
            onClick = { showCenter = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("居中弹窗")
        }
    }

    // ═══ 底部弹窗（带长列表 + 拖拽关闭）═══
    BottomSheetDialog(
        visible = showBottomSheet,
        onDismissRequest = { showBottomSheet = false },
        enableDragToDismiss = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "长列表演示（可拖拽关闭）",
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().bouncesEnable(false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(longListItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = item,
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ═══ 底部弹窗（禁止拖拽）═══
    BottomSheetDialog(
        visible = showBottomSheetNoDrag,
        onDismissRequest = { showBottomSheetNoDrag = false },
        enableDragToDismiss = false
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "长列表演示（禁止拖拽关闭）",
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(longListItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = item,
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ═══ 全屏弹窗 ═══
    FullScreenDialog(
        visible = showFullScreen,
        onDismissRequest = { showFullScreen = false }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("全屏弹窗", fontSize = 20.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("点击背景可关闭", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showFullScreen = false }) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    // ═══ 居中弹窗 ═══
    CenterDialog(
        visible = showCenter,
        onDismissRequest = { showCenter = false }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("居中弹窗", fontSize = 20.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Text("点击背景或返回键可关闭", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { showCenter = false }) {
                    Text("关闭")
                }
            }
        }
    }

    // ═══ DragToDismissContainer 最简接入示例 ═══
    // 不经过 BottomSheetDialog 封装，直接展示原始写法
    SimpleDragToDismissDialog(
        visible = showSimpleDrag,
        onDismissRequest = { showSimpleDrag = false },
        items = longListItems
    )
}

/**
 * DragToDismissContainer 最简接入示例
 *
 * 直接组合：Dialog → AnimatedVisibility → DragToDismissContainer → LazyColumn
 * 供开发者参考复制，无额外封装。
 */
@Composable
private fun SimpleDragToDismissDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    items: List<String>
) {
    val dismissDelayMillis = 250
    val dialogState = rememberDialogDismissState(visible, dismissDelayMillis, onDismissRequest)

    if (dialogState.showDialog) {
        Dialog(
            onDismissRequest = dialogState.dismissDialog,
            properties = KuiklyDialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
                scrimColor = Color.Black.copy(alpha = 0.4f),
                contentAlignment = Alignment.BottomCenter
            )
        ) {
            if (dialogState.visibleState) {
                BackHandler(onBack = dialogState.dismissDialog)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val enterAnim = slideInVertically(
                    animationSpec = tween(durationMillis = dismissDelayMillis),
                    initialOffsetY = { it }
                ) + fadeIn()
                val exitAnim = slideOutVertically(
                    animationSpec = tween(durationMillis = dismissDelayMillis),
                    targetOffsetY = { it }
                ) + fadeOut()

                AnimatedVisibility(
                    visible = dialogState.visibleState,
                    enter = enterAnim,
                    exit = exitAnim
                ) {
                    DragToDismissContainer(
                        onDismiss = dialogState.dismissDialog,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(Color.White)
                        ) {
                            // 拖拽指示条
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFE0E0E0))
                                )
                            }
                            Text(
                                text = "最简接入：DragToDismiss + LazyColumn",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(420.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(items) { index, item ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            elevation = CardDefaults.cardElevation(2.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                                        ) {
                                            Text(
                                                text = item,
                                                fontSize = 14.sp,
                                                color = Color(0xFF333333),
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.foundation.gestures.awaitEachGesture
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxScope
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.util.fastForEach
import com.tencent.kuikly.core.datetime.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


/**
 * 活跃的边缘方向
 */
enum class ActiveEdge {
    NONE, LEFT, RIGHT
}


/** 长按确认超时时间（毫秒） */
private const val LONG_PRESS_TIMEOUT_MS = 300L

/** 双击判定的最大间隔时间（毫秒），两次点击间隔小于此值视为双击 */
private const val DOUBLE_CLICK_TIMEOUT_MS = 250L

/** 点击判定的最大移动距离（像素） */
private const val CLICK_SLOP_PX = 40f

/** 长按等待期间的滑动判定阈值（像素），超过此距离视为滑动而非长按 */
private const val TOUCH_SLOP_PX = 30f


/**
 * 统一手势覆盖层
 *
 * 负责处理视频播放区域内的所有手势交互：
 * - 点击任意位置：暂停/恢复播放
 * - 长按左右边缘：触发临时 2 倍速预览
 * - 长按边缘后下滑：锁定/取消 2 倍速
 *
 * 边缘长按采用延迟确认机制：手指在边缘区域按下后，需持续按住超过 [LONG_PRESS_TIMEOUT_MS] 毫秒
 * 且移动距离不超过 [TOUCH_SLOP_PX] 像素，才会确认为长按并触发倍速预览。
 * 在等待确认期间不消费事件，确保 VerticalPager 等父级组件的正常滑动翻页不受影响。
 *
 * @param modifier 外部传入的 Modifier
 * @param contentAlignment 内容对齐方式
 * @param edgeTriggerWidthPx 左右边缘命中区宽度（像素）
 * @param speedLockDistancePx 下滑锁定阈值距离（像素）
 * @param onLongTouchEdge 长按边缘确认后的回调
 * @param onLongTouchCancel 长按边缘后手指抬起且未达到锁定阈值时的取消回调
 * @param onLongTouchSwipeThreshold 长按边缘后下滑达到锁定阈值（但未松手）的回调
 * @param onLongTouchSwipeThresholdCancel 下滑达到阈值后又滑回阈值内的回调
 * @param onLongTouchAndSwiped 长按边缘后下滑达到锁定阈值并松手的回调
 * @param onLongTouchMiddle 长按非边缘区域（中间区域）确认后的回调
 * @param onClickAnyWhere 点击任意位置的回调，用于触发暂停或恢复播放
 * @param onDoubleClick 双击回调，不为空时启用双击判定；双击时触发此回调（参数为触点坐标 dp）而非 [onClickAnyWhere]
 * @param content 内容区域
 */
@Composable
fun WSVideoGestureOverlay(
    key: Any,
    modifier: Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    edgeTriggerWidthPx: Float,
    speedLockDistancePx: Float,
    onLongTouchEdge: () -> Unit,
    onLongTouchCancel: () -> Unit,
    onLongTouchSwipeThreshold: () -> Unit,
    onLongTouchSwipeThresholdCancel: () -> Unit,
    onLongTouchAndSwiped: () -> Unit,
    onLongTouchMiddle: () -> Unit,
    onClickAnyWhere: () -> Unit,
    onDoubleClick: ((x: Float, y: Float) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    // 使用 rememberUpdatedState 确保 pointerInput 协程中始终引用最新的回调
    val currentOnLongTouchEdge by rememberUpdatedState(onLongTouchEdge)
    val currentOnLongTouchCancel by rememberUpdatedState(onLongTouchCancel)
    val currentOnLongTouchSwipeThreshold by rememberUpdatedState(onLongTouchSwipeThreshold)
    val currentOnLongTouchSwipeThresholdCancel by rememberUpdatedState(onLongTouchSwipeThresholdCancel)
    val currentOnLongTouchAndSwiped by rememberUpdatedState(onLongTouchAndSwiped)
    val currentOnLongTouchMiddle by rememberUpdatedState(onLongTouchMiddle)
    val currentOnClickAnyWhere by rememberUpdatedState(onClickAnyWhere)
    val currentOnDoubleClick by rememberUpdatedState(onDoubleClick)
    val density = LocalDensity.current
    val clickHandler = rememberClickOrDoubleClickHandler(key)

    Box(
        modifier = modifier
            .pointerInput(key, edgeTriggerWidthPx, speedLockDistancePx) {
                awaitEachGesture {
                    val down = awaitPointerEvent(PointerEventPass.Main)
                    val firstChange = down.changes.firstOrNull() ?: return@awaitEachGesture
                    // 如果事件已被子组件消费（如全屏按钮），则跳过本轮手势
                    if (firstChange.isConsumed) return@awaitEachGesture
                    val downPosition = firstChange.position
                    val componentWidth = size.width.toFloat()

                    // 判断是否在边缘区域
                    val edge = when {
                        downPosition.x <= edgeTriggerWidthPx -> ActiveEdge.LEFT
                        downPosition.x >= componentWidth - edgeTriggerWidthPx -> ActiveEdge.RIGHT
                        else -> ActiveEdge.NONE
                    }

                    if (edge == ActiveEdge.NONE) {
                        // 非边缘区域：长按等待确认阶段
                        val middleLongPressConfirmed = withTimeoutOrNull(LONG_PRESS_TIMEOUT_MS) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull()
                                    ?: return@withTimeoutOrNull false

                                if (!change.pressed) {
                                    // 手指在长按确认前抬起 → 视为点击
                                    val dx = change.position.x - downPosition.x
                                    val dy = change.position.y - downPosition.y
                                    if (dx * dx + dy * dy < CLICK_SLOP_PX * CLICK_SLOP_PX) {
                                        handleClickOrDoubleClick(
                                            clickHandler = clickHandler,
                                            currentOnClickAnyWhere,
                                            currentOnDoubleClick,
                                            downPosition,
                                            density.density,
                                        )
                                    }
                                    return@withTimeoutOrNull false
                                }

                                // 手指移动距离超过滑动阈值 → 视为滑动，放弃长按识别
                                val dx = change.position.x - downPosition.x
                                val dy = change.position.y - downPosition.y
                                if (dx * dx + dy * dy > TOUCH_SLOP_PX * TOUCH_SLOP_PX) {
                                    return@withTimeoutOrNull false
                                }
                            }
                            @Suppress("UNREACHABLE_CODE")
                            false
                        }

                        // withTimeoutOrNull 返回 null 表示超时 → 长按确认成功
                        if (middleLongPressConfirmed != null) return@awaitEachGesture

                        // 长按中间区域已确认
                        currentOnLongTouchMiddle()

                        // 消费后续事件直到手指抬起，阻止父级组件响应
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull() ?: break
                            event.changes.fastForEach { it.consume() }
                            if (!change.pressed) break
                        }
                        return@awaitEachGesture
                    }

                    // ========== 边缘区域：长按等待确认阶段 ==========
                    // 在超时前不消费事件，让 VerticalPager 等父级组件可以正常响应滑动
                    val longPressConfirmed = withTimeoutOrNull(LONG_PRESS_TIMEOUT_MS) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull()
                                ?: return@withTimeoutOrNull false

                            if (!change.pressed) {
                                // 手指在长按确认前抬起 → 视为点击
                                val dx = change.position.x - downPosition.x
                                val dy = change.position.y - downPosition.y
                                if (dx * dx + dy * dy < CLICK_SLOP_PX * CLICK_SLOP_PX) {
                                    handleClickOrDoubleClick(
                                        clickHandler = clickHandler,
                                        currentOnClickAnyWhere,
                                        currentOnDoubleClick,
                                        downPosition,
                                        density.density,
                                    )
                                }
                                return@withTimeoutOrNull false
                            }

                            // 手指移动距离超过滑动阈值 → 视为滑动，放弃长按识别
                            val dx = change.position.x - downPosition.x
                            val dy = change.position.y - downPosition.y
                            if (dx * dx + dy * dy > TOUCH_SLOP_PX * TOUCH_SLOP_PX) {
                                return@withTimeoutOrNull false
                            }
                        }
                        @Suppress("UNREACHABLE_CODE")
                        false
                    }

                    // withTimeoutOrNull 返回 null 表示超时 → 长按确认成功
                    if (longPressConfirmed != null) return@awaitEachGesture

                    // ========== 长按已确认：进入倍速预览 ==========
                    currentOnLongTouchEdge()

                    // 追踪手指移动，从此刻开始消费所有事件
                    // 使用 Initial pass 优先拦截，防止向下滑动时触发父级列表的下拉刷新
                    var thresholdReached = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: break

                        // 消费所有后续事件，阻止父级 VerticalPager 翻页和下拉刷新
                        event.changes.fastForEach { it.consume() }

                        if (!change.pressed) {
                            // 手指抬起
                            if (thresholdReached) {
                                // 达到阈值后松手 → 确认锁定/取消
                                currentOnLongTouchAndSwiped()
                            } else {
                                // 未达到阈值松手 → 取消
                                currentOnLongTouchCancel()
                            }
                            break
                        }

                        // 计算 Y 偏移量（向下为正），检查是否达到锁定阈值
                        val offsetY = change.position.y - downPosition.y
                        if (!thresholdReached && offsetY >= speedLockDistancePx) {
                            thresholdReached = true
                            currentOnLongTouchSwipeThreshold()
                        } else if (thresholdReached && offsetY < speedLockDistancePx) {
                            thresholdReached = false
                            currentOnLongTouchSwipeThresholdCancel()
                        }
                    }
                }
            },
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

/**
 * 上一次点击的时间戳，用于双击判定
 */
private class ClickOrDoubleClickHandler(
    private val scope: CoroutineScope,
) {
    private var lastClickTimeMs: Long = 0L
    private var pendingSingleClickJob: Job? = null

    fun handle(
        onClickAnyWhere: () -> Unit,
        onDoubleClick: ((x: Float, y: Float) -> Unit)?,
        touchPosition: Offset,
        densityValue: Float,
    ) {
        if (onDoubleClick == null) {
            onClickAnyWhere()
            return
        }

        val currentTime = DateTime.currentTimestamp()
        val elapsed = currentTime - lastClickTimeMs
        lastClickTimeMs = currentTime

        if (elapsed < DOUBLE_CLICK_TIMEOUT_MS) {
            pendingSingleClickJob?.cancel()
            pendingSingleClickJob = null
            lastClickTimeMs = 0L
            val xDp = touchPosition.x / densityValue
            val yDp = touchPosition.y / densityValue
            onDoubleClick(xDp, yDp)
        } else {
            pendingSingleClickJob?.cancel()
            pendingSingleClickJob = scope.launch {
                delay(DOUBLE_CLICK_TIMEOUT_MS)
                onClickAnyWhere()
            }
        }
    }
}

@Composable
private fun rememberClickOrDoubleClickHandler(key: Any): ClickOrDoubleClickHandler {
    val scope = rememberCoroutineScope()
    return remember(key, scope) { ClickOrDoubleClickHandler(scope) }
}

/**
 * 处理点击或双击逻辑
 *
 * 当 [onDoubleClick] 为 null 时，直接触发 [onClickAnyWhere]（单击暂停/恢复）。
 * 当 [onDoubleClick] 不为 null 时，启用双击判定：
 * - 两次点击间隔小于 [DOUBLE_CLICK_TIMEOUT_MS] → 判定为双击，触发 [onDoubleClick]，取消待执行的单击
 * - 两次点击间隔大于 [DOUBLE_CLICK_TIMEOUT_MS] → 判定为单击，延迟执行 [onClickAnyWhere]
 */
private fun handleClickOrDoubleClick(
    clickHandler: ClickOrDoubleClickHandler,
    onClickAnyWhere: () -> Unit,
    onDoubleClick: ((x: Float, y: Float) -> Unit)?,
    touchPosition: Offset,
    densityValue: Float,
) {
    clickHandler.handle(onClickAnyWhere, onDoubleClick, touchPosition, densityValue)
}
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

package com.tencent.kuikly.compose.profiler

/**
 * 重组事件基类，所有重组追踪事件的父类。
 *
 * @property timestampMs 事件发生的时间戳（毫秒）
 */
sealed class RecompositionEvent(
    val timestampMs: Long
) {
    /**
     * 事件类型名称，用于日志和 JSON 序列化
     */
    abstract val eventType: String
}

/**
 * 重组帧开始事件。
 * 当 ComposeSceneRecomposer 开始执行一次重组帧时记录。
 *
 * @property frameId 帧序号，递增唯一标识
 */
class RecompositionFrameStartEvent(
    timestampMs: Long,
    val frameId: Long
) : RecompositionEvent(timestampMs) {
    override val eventType: String = "frame_start"
}

/**
 * 重组帧结束事件。
 * 当 ComposeSceneRecomposer 完成一次重组帧时记录。
 *
 * @property frameId 帧序号，与对应的 FrameStart 匹配
 * @property durationMs 帧耗时（毫秒）
 * @property recomposedCount 该帧中重组的 Composable 数量
 */
class RecompositionFrameEndEvent(
    timestampMs: Long,
    val frameId: Long,
    val durationMs: Long,
    val recomposedCount: Int
) : RecompositionEvent(timestampMs) {
    override val eventType: String = "frame_end"
}

/**
 * Recomposition reason classification.
 */
enum class RecompositionReason {
    /** Triggered by direct State invalidation */
    STATE_CHANGE,
    /** Unknown reason — may be caused by parent recomposition passing new params, or forced recomposition */
    UNKNOWN
}

/**
 * 单个 Composable 重组事件。
 * 当一个被追踪的 Composable 函数发生重组时记录。
 *
 * @property composableName Composable 的短函数名（去掉包名前缀）
 * @property sourceLocation 源码位置，如 "File.kt:195"（从 CompositionTracer info 提取）
 * @property durationMs 本次重组耗时（毫秒）
 * @property triggerStates 触发本次重组的 State 标识符列表
 * @property parentName 父级 Composable 的短函数名，null 表示顶层
 * @property reason 重组原因分类
 * @property paramChanges 参数变更摘要（解析自编译器 $dirty bitmask）
 * @property scopeKey RecomposeScope 的 identityHashCode，null 表示首次组合（不计入热点）
 */
class ComposableRecomposedEvent(
    timestampMs: Long,
    val composableName: String,
    val sourceLocation: String? = null,
    val durationMs: Long,
    val triggerStates: List<String>,
    val parentName: String? = null,
    val reason: RecompositionReason = RecompositionReason.UNKNOWN,
    val paramChanges: ParamChangeSummary? = null,
    val scopeKey: Int? = null
) : RecompositionEvent(timestampMs) {
    override val eventType: String = "composable_recomposed"
}

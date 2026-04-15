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

import com.tencent.kuikly.compose.profiler.output.escapeJson

/**
 * 单个 Composable 的重组统计汇总。
 *
 * @property name Composable 名称
 * @property recompositionCount 总重组次数
 * @property totalDurationMs 累计重组耗时（毫秒）
 * @property avgDurationMs 平均重组耗时（毫秒）
 * @property maxDurationMs 最大单次重组耗时（毫秒）
 * @property minDurationMs 最小单次重组耗时（毫秒）
 * @property triggerStates 触发重组的所有 State 标识符集合
 * @property isHotspot 是否被标记为热点组件
 * @property paramChangeFrequency 各参数位置被标记为变更的累计频率 (paramIndex → changeCount)
 */
data class ComposableStats(
    val name: String,
    val recompositionCount: Int,
    val totalDurationMs: Long,
    val avgDurationMs: Double,
    val maxDurationMs: Long,
    val minDurationMs: Long,
    val triggerStates: Set<String>,
    val isHotspot: Boolean,
    val paramChangeFrequency: Map<Int, Int> = emptyMap()
)

/**
 * State 变更记录。
 *
 * @property stateKey State 标识符
 * @property changeCount 变更次数
 * @property firstChangeMs 首次变更时间（相对于会话开始的毫秒偏移）
 * @property lastChangeMs 最近变更时间（相对于会话开始的毫秒偏移）
 */
data class StateChangeRecord(
    val stateKey: String,
    val changeCount: Int,
    val firstChangeMs: Long,
    val lastChangeMs: Long
)

/**
 * 重组分析报告。
 * 汇总了从追踪开始到报告生成时刻的所有重组数据。
 *
 * @property sessionId 分析会话唯一标识
 * @property startTimestampMs 追踪开始的时间戳（毫秒）
 * @property durationMs 追踪持续时间（毫秒）
 * @property totalFrames 总重组帧数
 * @property totalRecompositions 总重组次数（所有 Composable 的重组次数之和）
 * @property composables 按 Composable 聚合的重组统计列表，按重组次数降序排列
 * @property hotspots 热点组件列表（重组频率超过阈值的 Composable）
 * @property stateChanges State 变更记录列表，按变更次数降序排列
 * @property filteredNames 当前生效的业务自定义排除名称列表（调用 [RecompositionProfiler.excludeByName] 添加）
 * @property filteredPrefixes 当前生效的业务自定义排除前缀列表（调用 [RecompositionProfiler.excludeByPrefix] 添加）
 */
data class RecompositionReport(
    val sessionId: String,
    val startTimestampMs: Long,
    val durationMs: Long,
    val totalFrames: Long,
    val totalRecompositions: Int,
    val composables: List<ComposableStats>,
    val hotspots: List<ComposableStats>,
    val stateChanges: List<StateChangeRecord>,
    val filteredNames: List<String> = emptyList(),
    val filteredPrefixes: List<String> = emptyList()
) {
    /**
     * 将报告序列化为 JSON 字符串。
     * 字段名自解释，数值字段包含单位后缀（Ms 表示毫秒），方便 AI Agent 解析。
     */
    fun toJson(): String {
        return buildString {
            append("{")
            append("\"sessionId\":\"$sessionId\",")
            append("\"startTimestampMs\":$startTimestampMs,")
            append("\"durationMs\":$durationMs,")
            append("\"totalFrames\":$totalFrames,")
            append("\"totalRecompositions\":$totalRecompositions,")

            // composables
            append("\"composables\":[")
            composables.forEachIndexed { index, stats ->
                if (index > 0) append(",")
                appendComposableStatsJson(stats)
            }
            append("],")

            // hotspots
            append("\"hotspots\":[")
            hotspots.forEachIndexed { index, stats ->
                if (index > 0) append(",")
                appendComposableStatsJson(stats)
            }
            append("],")

            // stateChanges
            append("\"stateChanges\":[")
            stateChanges.forEachIndexed { index, record ->
                if (index > 0) append(",")
                append("{")
                append("\"stateKey\":\"${escapeJson(record.stateKey)}\",")
                append("\"changeCount\":${record.changeCount},")
                append("\"firstChangeMs\":${record.firstChangeMs},")
                append("\"lastChangeMs\":${record.lastChangeMs}")
                append("}")
            }
            append("],")

            // filteredNames
            append("\"filteredNames\":[")
            filteredNames.forEachIndexed { index, name ->
                if (index > 0) append(",")
                append("\"${escapeJson(name)}\"")
            }
            append("],")

            // filteredPrefixes
            append("\"filteredPrefixes\":[")
            filteredPrefixes.forEachIndexed { index, prefix ->
                if (index > 0) append(",")
                append("\"${escapeJson(prefix)}\"")
            }
            append("]")

            append("}")
        }
    }

    private fun StringBuilder.appendComposableStatsJson(stats: ComposableStats) {
        append("{")
        append("\"name\":\"${escapeJson(stats.name)}\",")
        append("\"recompositionCount\":${stats.recompositionCount},")
        append("\"totalDurationMs\":${stats.totalDurationMs},")
        append("\"avgDurationMs\":${stats.avgDurationMs},")
        append("\"maxDurationMs\":${stats.maxDurationMs},")
        append("\"minDurationMs\":${stats.minDurationMs},")
        append("\"isHotspot\":${stats.isHotspot},")
        append("\"triggerStates\":[")
        stats.triggerStates.forEachIndexed { i, state ->
            if (i > 0) append(",")
            append("\"${escapeJson(state)}\"")
        }
        append("]")
        if (stats.paramChangeFrequency.isNotEmpty()) {
            append(",\"paramChangeFrequency\":{")
            stats.paramChangeFrequency.entries.forEachIndexed { i, (paramIdx, count) ->
                if (i > 0) append(",")
                append("\"#$paramIdx\":$count")
            }
            append("}")
        }
        append("}")
    }

    companion object {
        /**
         * 空报告，用于 Profiler 未启用时返回。
         */
        val EMPTY = RecompositionReport(
            sessionId = "",
            startTimestampMs = 0L,
            durationMs = 0L,
            totalFrames = 0L,
            totalRecompositions = 0,
            composables = emptyList(),
            hotspots = emptyList(),
            stateChanges = emptyList()
        )
    }
}

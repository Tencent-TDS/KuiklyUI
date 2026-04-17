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

package com.tencent.kuikly.compose.profiler.output

import com.tencent.kuikly.compose.profiler.ComposableRecomposedEvent
import com.tencent.kuikly.compose.profiler.RecompositionEvent
import com.tencent.kuikly.compose.profiler.RecompositionFrameEndEvent
import com.tencent.kuikly.compose.profiler.RecompositionFrameStartEvent
import com.tencent.kuikly.compose.profiler.RecompositionOutputStrategy
import com.tencent.kuikly.compose.profiler.RecompositionReport
import com.tencent.kuikly.core.log.KLog

/**
 * 日志输出策略。
 *
 * 将重组追踪数据通过 [KLog] 输出结构化日志。
 * Tag：`RCProfiler`，每行单独输出，可用此 Tag 过滤所有重组日志。
 *
 * @param logFrameEvents 是否在每帧结束时输出帧详情日志。默认 true，实时输出每帧重组事件。
 *   设为 false 则只在 Get Report 时输出汇总报告，适合长时间运行减少日志量。
 *
 * 所有平台输出格式一致。
 */
class LogOutputStrategy(
    private val logFrameEvents: Boolean = true
) : RecompositionOutputStrategy {

    companion object {
        const val TAG = "RCProfiler"
    }

    override fun onFrameComplete(events: List<RecompositionEvent>) {
        if (!logFrameEvents) return
        if (events.isEmpty()) return

        // 只有帧内有实际重组发生时才输出
        val hasRecomposition = events.any { it is ComposableRecomposedEvent }
        if (!hasRecomposition) return

        // 每行单独调 KLog，确保每行都带 Tag 前缀，grep 可过滤全部内容
        var indent = 0
        for (event in events) {
            when (event) {
                is RecompositionFrameStartEvent -> {
                    KLog.d(TAG, "Frame #${event.frameId} START (ts=${event.timestampMs}ms)")
                    indent++
                }
                is RecompositionFrameEndEvent -> {
                    indent = (indent - 1).coerceAtLeast(0)
                    KLog.d(TAG, "Frame #${event.frameId} END (duration=${event.durationMs}ms, recomposed=${event.recomposedCount})")
                }
                is ComposableRecomposedEvent -> {
                    if (event.composableName == "<anonymous>") continue
                    val locationInfo = if (event.sourceLocation != null) " @${event.sourceLocation}" else ""
                    val scopeInfo = if (event.scopeKey != null) " [scope=${event.scopeKey}]" else " [scope=none]"
                    val parentInfo = " [parent=${event.parentName ?: "<unknown>"}]"
                    val paramInfo = buildParamChangeString(event)
                    val statesInfo = if (event.triggerStates.isNotEmpty()) {
                        " triggers=[${event.triggerStates.joinToString(", ")}]"
                    } else ""
                    val indent2 = indentStr(indent)
                    KLog.d(TAG, "${indent2}RECOMPOSED: ${event.composableName}$locationInfo (${event.durationMs}ms)$scopeInfo$parentInfo$paramInfo$statesInfo")
                }
            }
        }
    }

    override fun onReportReady(report: RecompositionReport) {
        KLog.i(TAG, "=== Recomposition Report ===")
        KLog.i(TAG, "Session: ${report.sessionId}")
        KLog.i(TAG, "Duration: ${report.durationMs}ms | Frames: ${report.totalFrames} | Recompositions: ${report.totalRecompositions}")

        if (report.hotspots.isNotEmpty()) {
            KLog.i(TAG, "--- HOTSPOTS ---")
            for (hotspot in report.hotspots) {
                val loc = if (hotspot.sourceLocation != null) " @${hotspot.sourceLocation}" else ""
                KLog.i(TAG, "  ${hotspot.name}$loc: ${hotspot.recompositionCount}x (avg=${formatFloat(hotspot.avgDurationMs)}ms, max=${hotspot.maxDurationMs}ms)")
            }
        }

        if (report.composables.isNotEmpty()) {
            KLog.i(TAG, "--- Composables ---")
            for (stats in report.composables) {
                val marker = if (stats.isHotspot) " [HOTSPOT]" else ""
                val paramInfo = if (stats.paramChangeFrequency.isNotEmpty()) {
                    val freqs = stats.paramChangeFrequency.entries
                        .sortedByDescending { it.value }
                        .joinToString(", ") { "#${it.key}:${it.value}x" }
                    " params changed: [$freqs]"
                } else {
                    " no params change"
                }
                val stateInfo = if (stats.triggerStates.isNotEmpty()) {
                    " state changes: [${stats.triggerStates.joinToString(", ")}]"
                } else {
                    " no state change"
                }
                val loc = if (stats.sourceLocation != null) " @${stats.sourceLocation}" else ""
                KLog.i(TAG, "  ${stats.name}$loc: ${stats.recompositionCount}x (avg=${formatFloat(stats.avgDurationMs)}ms)$marker$paramInfo$stateInfo")
                // Scope 分布行
                if (stats.scopeDistribution.isNotEmpty() || stats.noScopeRecompositions > 0) {
                    val scopeInfo = if (stats.scopeDistribution.isNotEmpty()) {
                        val sorted = stats.scopeDistribution.entries.sortedByDescending { it.value }
                        val displayed = sorted.take(5).joinToString(", ") { "${it.key}: ${it.value}x" }
                        val more = if (sorted.size > 5) ", ...+${sorted.size - 5} more" else ""
                        "{$displayed$more}"
                    } else {
                        "{}"
                    }
                    KLog.i(TAG, "    → scopes: $scopeInfo, no-scope: ${stats.noScopeRecompositions}")
                }
            }
        }
    }

    private fun indentStr(level: Int): String = "  ".repeat(level)

    private fun buildParamChangeString(event: ComposableRecomposedEvent): String {
        val changes = event.paramChanges ?: return " params=[no params change]"
        if (!changes.hasChanges) return " params=[no changes] (0/${changes.totalParams})"
        val positions = changes.changedParams.joinToString(", ") { idx ->
            "#$idx"
        }
        return " params changed: [$positions] (${changes.changedParams.size}/${changes.totalParams})"
    }

    private fun formatFloat(value: Double): String {
        val intPart = value.toLong()
        val fracPart = ((value - intPart) * 10).toLong().let { kotlin.math.abs(it) }
        return "$intPart.$fracPart"
    }
}

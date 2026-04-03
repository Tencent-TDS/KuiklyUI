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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.profiler.ComposableRecomposedEvent
import com.tencent.kuikly.compose.profiler.RecompositionEvent
import com.tencent.kuikly.compose.profiler.RecompositionOutputStrategy
import com.tencent.kuikly.compose.profiler.RecompositionReport
import kotlin.concurrent.Volatile

/**
 * Overlay 热点可视化输出策略。
 *
 * 纯数据容器 + 节流刷新：
 * - onFrameComplete 只写普通 Map（apply callback 安全）
 * - flushIfNeeded 由 BaseComposeScene.render() 在 postponeInvalidation 之后调用，
 *   此时 isInvalidationDisabled = false，写 mutableIntStateOf 能正确触发 recomposition
 * - 60 帧计数器节流，约 1 秒刷新一次 UI
 */
class OverlayOutputStrategy : RecompositionOutputStrategy {

    // key = composableName，按函数名聚合。
    //
    // 设计决策（2026-04-02）：
    // 曾尝试用 composableName + scopeKey 区分实例，但 scopeKey 语义是"最近的失效祖先 scope"，
    // 不同驱动方式（State 失效 vs 参数驱动）下 scopeKey 有值/null 不一致，
    // 导致热点列表里有些组件有 #N 序号、有些没有，行为不统一，用户体验差。
    // Overlay 定位是「快速直觉感知」，函数名粒度已够用；实例级细节通过日志 RECOMPOSED 输出查看。
    private val instanceCounts = mutableMapOf<String, InstanceCount>()

    /** Compose State 版本号，驱动 Overlay Composable 重组 */
    internal var dataVersion by mutableIntStateOf(0)
        private set

    /** 累计总重组次数（普通字段） */
    @Volatile
    var totalCount: Int = 0
        private set

    /** 是否有尚未刷新到 UI 的新数据 */
    private var hasPendingUpdate = false

    /** 最多展示的热点条数 */
    var topCount: Int = 5

    /** 是否已暂停 */
    @Volatile
    var paused: Boolean = false

    override fun onFrameComplete(events: List<RecompositionEvent>) {
        if (paused) return

        for (event in events) {
            if (event is ComposableRecomposedEvent) {
                if (event.composableName == "<anonymous>") continue

                totalCount++
                hasPendingUpdate = true

                // 按函数名聚合，key = composableName（详见 specs/overlay-highlight/spec.md 设计决策）
                val key = event.composableName
                val existing = instanceCounts[key]
                if (existing != null) {
                    instanceCounts[key] = existing.copy(totalCount = existing.totalCount + 1)
                } else {
                    instanceCounts[key] = InstanceCount(
                        compositeKey = key,
                        baseName = event.composableName,
                        sourceLocation = event.sourceLocation,
                        totalCount = 1
                    )
                }
            }
        }
    }

    override fun onReportReady(report: RecompositionReport) {
        // overlay 不响应报告事件
    }

    /** 读取当前热点列表，按重组次数降序排列 */
    fun getHotspots(): List<HotspotItem> {
        return instanceCounts.values
            .sortedByDescending { it.totalCount }
            .take(topCount)
            .map { HotspotItem(it.baseName, it.sourceLocation, it.totalCount) }
    }

    /**
     * 由 BaseComposeScene.render() 在 postponeInvalidation 之后调用。
     * 此时 isInvalidationDisabled = false，写 Compose State 能正确触发 recomposition。
     * 有新数据时立即刷新，无新数据时跳过（一次 bool 读取，零开销）。
     */
    fun flushIfNeeded() {
        if (hasPendingUpdate) {
            dataVersion++
            hasPendingUpdate = false
        }
    }

    /** 重置所有计数（Profiler.reset() 时由 tracker 自动调用） */
    override fun onReset() {
        reset()
    }

    /** 手动重置（Overlay 面板内"重置"按钮直接调用） */
    fun reset() {
        instanceCounts.clear()
        totalCount = 0
        hasPendingUpdate = false
        dataVersion++
    }

    /** 暂停/继续更新 */
    fun togglePause() {
        paused = !paused
    }
}

/** 单实例重组计数 */
internal data class InstanceCount(
    val compositeKey: String,
    val baseName: String,
    val sourceLocation: String?,
    val totalCount: Int = 0
)

/** 热点条目（对外只读） */
data class HotspotItem(
    val name: String,
    val sourceLocation: String?,
    val totalCount: Int
)

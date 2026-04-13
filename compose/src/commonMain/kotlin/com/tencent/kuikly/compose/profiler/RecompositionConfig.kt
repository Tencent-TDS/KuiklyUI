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
 * 重组追踪的配置。
 * 通过 [RecompositionProfiler.configure] 设置。
 *
 * @property sampleRate 采样率，取值 0.0f ~ 1.0f。1.0f 表示全量采集，0.5f 表示约 50% 的帧被记录。默认 1.0f。
 * @property hotspotThreshold 热点组件阈值：每秒重组次数超过此值的 Composable 将被标记为热点。默认 10。
 * @property maxEventBufferSize 事件缓冲区最大容量，取值范围 1～10,000,000。超过此数量时将丢弃最旧的事件。默认 100000。
 * @property enableStateTracking 是否启用 State 变更追踪。关闭可降低追踪开销。默认 true。
 * @property includeFrameworkComposables 是否包含框架内部 Composable（如 Row/Column measure policy、Runtime 内部函数等）。
 *   默认 false，只监控业务代码的 Composable。设为 true 可查看所有 Composable 的重组情况。
 * @property enableOverlay 是否启用悬浮 Overlay 热点可视化面板。默认 false。
 * @property overlayTopCount Overlay 面板显示的热点 Composable 最大条数。默认 5。
 * @property enableLog 是否启用日志输出（LogOutputStrategy）。仅在 start/stop 期间有效。默认 true。
 * @property enableFile 是否启用文件写入（FileOutputStrategy）。
 *   开启后每 2 秒 append 帧数据到 profiler_frames.jsonl，
 *   stop() 和 getReport(saveToFile=true) 时写 profiler_report.json。默认 true。
 */
data class RecompositionConfig(
    val sampleRate: Float = 1.0f,
    val hotspotThreshold: Int = 10,
    val maxEventBufferSize: Int = 100_000,
    val enableStateTracking: Boolean = true,
    val includeFrameworkComposables: Boolean = false,
    val enableOverlay: Boolean = false,
    val overlayTopCount: Int = 10,
    val enableLog: Boolean = true,
    val enableFile: Boolean = true,
) {
    init {
        require(sampleRate in 0.0f..1.0f) {
            "sampleRate must be between 0.0 and 1.0, got $sampleRate"
        }
        require(hotspotThreshold > 0) {
            "hotspotThreshold must be positive, got $hotspotThreshold"
        }
        require(maxEventBufferSize in 1..10_000_000) {
            "maxEventBufferSize must be between 1 and 10_000_000, got $maxEventBufferSize"
        }
        require(overlayTopCount in 1..20) {
            "overlayTopCount must be between 1 and 20, got $overlayTopCount"
        }
    }

    companion object {
        /**
         * 默认配置
         */
        val DEFAULT = RecompositionConfig()
    }
}

/**
 * 配置构建器，用于 DSL 风格的配置。
 *
 * 用法:
 * ```
 * RecompositionProfiler.configure {
 *     sampleRate = 0.5f
 *     hotspotThreshold = 20
 * }
 * ```
 */
class RecompositionConfigBuilder {
    var sampleRate: Float = 1.0f
    var hotspotThreshold: Int = 10
    var maxEventBufferSize: Int = 10000
    var enableStateTracking: Boolean = true
    var includeFrameworkComposables: Boolean = false
    var enableOverlay: Boolean = false
    var overlayTopCount: Int = 10
    var enableLog: Boolean = true
    var enableFile: Boolean = true

    internal fun build(): RecompositionConfig = RecompositionConfig(
        sampleRate = sampleRate,
        hotspotThreshold = hotspotThreshold,
        maxEventBufferSize = maxEventBufferSize,
        enableStateTracking = enableStateTracking,
        includeFrameworkComposables = includeFrameworkComposables,
        enableOverlay = enableOverlay,
        overlayTopCount = overlayTopCount,
        enableLog = enableLog,
        enableFile = enableFile,
    )
}

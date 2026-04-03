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

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.compose.profiler.output.FileOutputStrategy
import com.tencent.kuikly.compose.profiler.output.LogOutputStrategy
import com.tencent.kuikly.compose.profiler.output.OverlayOutputStrategy
import com.tencent.kuikly.compose.ui.SynchronizedObject
import com.tencent.kuikly.compose.ui.synchronized
import com.tencent.kuikly.core.module.FileModule
import kotlin.concurrent.Volatile

/**
 * 重组性能分析工具的主入口。
 *
 * 提供启停控制、配置管理、报告获取等公开 API。
 * 所有公开方法保证线程安全和幂等性。
 *
 * 使用示例：
 * ```
 * // 配置并启动
 * RecompositionProfiler.configure {
 *     sampleRate = 0.5f
 *     hotspotThreshold = 20
 * }
 * RecompositionProfiler.start()
 *
 * // 获取报告
 * val report = RecompositionProfiler.getReport()
 * println(report.toJson())
 *
 * // 停止
 * RecompositionProfiler.stop()
 * ```
 */
object RecompositionProfiler {

    private val lock = SynchronizedObject()

    // ========== Lifecycle Listener 机制 ==========

    /**
     * Profiler 生命周期回调接口。
     * [BaseComposeScene] 通过此接口在 profiler 启停时注册/注销 CompositionObserver。
     */
    internal interface ProfilerLifecycleListener {
        fun onProfilerStarted(tracker: RecompositionTracker)
        fun onProfilerStopped()
    }

    private val lifecycleListeners = mutableSetOf<ProfilerLifecycleListener>()

    /**
     * 注册生命周期监听器。
     * 如果 profiler 已启用，会立即回调 [ProfilerLifecycleListener.onProfilerStarted]。
     */
    internal fun addLifecycleListener(listener: ProfilerLifecycleListener) {
        synchronized(lock) {
            lifecycleListeners.add(listener)
            tracker?.let { listener.onProfilerStarted(it) }
        }
    }

    /**
     * 注销生命周期监听器。
     */
    internal fun removeLifecycleListener(listener: ProfilerLifecycleListener) {
        synchronized(lock) {
            lifecycleListeners.remove(listener)
        }
    }

    /**
     * 由 ComposeContainer 在 onProfilerStarted 时传入 FileModule 实例。
     * 如果 enableFile=true 且尚未创建 FileOutputStrategy，则自动创建并注册。
     */
    internal fun setFileModule(fileModule: FileModule) {
        synchronized(lock) {
            if (config.enableFile && fileStrategy == null) {
                val strategy = FileOutputStrategy(fileModule)
                fileStrategy = strategy
                tracker?.addOutputStrategy(strategy)
                strategy.activate()
            }
        }
    }

    /** FileOutputStrategy 持有，stop 时写报告 */
    private var fileStrategy: FileOutputStrategy? = null

    /**
     * 内部追踪引擎实例，供 [BaseComposeScene] 帧追踪使用。
     * 仅在 [isEnabled] 为 true 时非空。
     * stop() 后置 null，但 stoppedTracker 保留快照供 getReport 使用。
     */
    @Volatile
    internal var tracker: RecompositionTracker? = null
        private set

    /**
     * stop() 后保留的 tracker 快照，供 stop 后调用 getReport() 使用。
     * 下次 start() 时清除。
     */
    @Volatile
    private var stoppedTracker: RecompositionTracker? = null

    /**
     * 当前配置
     */
    @Volatile
    private var config: RecompositionConfig = RecompositionConfig.DEFAULT

    /**
     * Overlay 策略引用（Compose State，驱动 ComposeContainer 重组以显示/隐藏 Overlay）
     */
    internal val overlayStrategyState = mutableStateOf<OverlayOutputStrategy?>(null)
    private var overlayStrategy: OverlayOutputStrategy?
        get() = overlayStrategyState.value
        set(value) { overlayStrategyState.value = value }

    /**
     * 追踪是否已启用（Compose State 版本）。
     */
    internal val enabledState = mutableStateOf(false)

    /**
     * 追踪是否已启用。
     */
    @Volatile
    var isEnabled: Boolean = false
        private set

    /**
     * Overlay 是否已启用（由 [RecompositionConfig.enableOverlay] 控制）。
     * ComposeContainer 读取此值决定是否渲染 ProfilerOverlaySlot。
     */
    val isOverlayEnabled: Boolean
        get() = overlayStrategy != null && isEnabled

    /**
     * 获取当前 Overlay 策略实例，供 ComposeContainer 渲染 UI 使用。
     * 使用 Compose State 版本，确保 start/stop 时 ComposeContainer 能感知变化并重组。
     */
    internal val currentOverlayStrategy: OverlayOutputStrategy?
        get() = overlayStrategyState.value

    /**
     * 配置追踪参数。
     * 如果 Profiler 已启用，新配置将在下一帧生效。
     *
     * @param block 配置 DSL 块
     */
    fun configure(block: RecompositionConfigBuilder.() -> Unit) {
        val builder = RecompositionConfigBuilder().apply {
            // 复制当前配置作为默认值
            sampleRate = config.sampleRate
            hotspotThreshold = config.hotspotThreshold
            maxEventBufferSize = config.maxEventBufferSize
            enableStateTracking = config.enableStateTracking
            includeFrameworkComposables = config.includeFrameworkComposables
            enableLog = config.enableLog
            enableFile = config.enableFile
        }
        builder.block()
        val newConfig = builder.build()
        synchronized(lock) {
            config = newConfig
            tracker?.updateConfig(newConfig)
        }
    }

    /**
     * 停止追踪时使用的 No-op tracer。
     * isTraceInProgress() 返回 false，使编译器注入的 traceEventStart/End 被跳过。
     */
    @OptIn(InternalComposeTracingApi::class)
    private val noOpTracer = object : CompositionTracer {
        override fun isTraceInProgress(): Boolean = false
        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {}
        override fun traceEventEnd() {}
    }

    /**
     * 启动重组追踪。
     * 幂等：重复调用不会有副作用。
     */
    @OptIn(InternalComposeTracingApi::class)
    fun start() {
        synchronized(lock) {
            if (!isEnabled) {
                stoppedTracker = null  // 清除上次 stop 的快照
                fileStrategy = null   // 清除上次的文件策略
                val newTracker = RecompositionTracker()
                newTracker.start(config)
                tracker = newTracker
                isEnabled = true
                enabledState.value = true
                // Register global CompositionTracer to receive compiler-injected callbacks
                Composer.setTracer(newTracker.compositionTracer)
                // 如果配置了 enableLog，自动注册 LogOutputStrategy
                if (config.enableLog) {
                    newTracker.addOutputStrategy(LogOutputStrategy())
                }
                // 如果配置了 enableOverlay，自动创建并注册 OverlayOutputStrategy
                if (config.enableOverlay) {
                    val strategy = OverlayOutputStrategy().also { it.topCount = config.overlayTopCount }
                    overlayStrategy = strategy
                    newTracker.addOutputStrategy(strategy)
                }
                // Notify lifecycle listeners to register CompositionObserver
                // ComposeContainer 会在 onProfilerStarted 里调用 setFileModule
                for (listener in lifecycleListeners) {
                    listener.onProfilerStarted(newTracker)
                }
            }
        }
    }

    /**
     * 停止重组追踪，释放追踪资源。
     * 幂等：重复调用不会有副作用。
     * 停止后仍可通过 [getReport] 获取已采集的数据。
     */
    @OptIn(InternalComposeTracingApi::class)
    fun stop() {
        synchronized(lock) {
            if (isEnabled) {
                isEnabled = false
                enabledState.value = false
                // Unregister tracer (set no-op so isTraceInProgress returns false)
                Composer.setTracer(noOpTracer)
                // Notify lifecycle listeners to unregister CompositionObserver
                for (listener in lifecycleListeners) {
                    listener.onProfilerStopped()
                }
                tracker?.stop()
                // 保留快照供 stop 后调用 getReport()
                stoppedTracker = tracker
                tracker = null
                overlayStrategy = null
                // 写聚合报告文件
                val report = stoppedTracker?.generateReport() ?: RecompositionReport.EMPTY
                fileStrategy?.deactivate(report)
                fileStrategy = null
            }
        }
    }

    /**
     * 获取当前的重组分析报告。
     * Profiler 运行中或 stop 后均可调用（stop 后返回最后一次采集的数据）。
     * 如果从未启动过，返回空报告。
     *
     * @param saveToFile 是否同时将报告写入 profiler_report.json 文件。
     *   需要 enableFile=true 且 Profiler 正在运行（stop 后 fileStrategy 已释放）。默认 true。
     */
    fun getReport(saveToFile: Boolean = true): RecompositionReport {
        val (report, trackerRef) = synchronized(lock) {
            val t = tracker ?: stoppedTracker
            (t?.generateReport() ?: RecompositionReport.EMPTY) to t
        }
        if (saveToFile) {
            fileStrategy?.writeReport(report)
        }
        // 触发所有策略的 onReportReady（日志输出等）
        trackerRef?.notifyReportReady(report)
        return report
    }

    /**
     * 重置已采集的所有数据，从零开始统计。
     * 仅在 Profiler 启用时有效。
     */
    fun reset() {
        synchronized(lock) {
            tracker?.reset()
        }
    }

    /**
     * 添加输出策略。
     *
     * @param strategy 输出策略实例
     */
    fun addOutputStrategy(strategy: RecompositionOutputStrategy) {
        synchronized(lock) {
            tracker?.addOutputStrategy(strategy)
        }
    }

    /**
     * 移除输出策略。
     *
     * @param strategy 要移除的输出策略实例
     */
    fun removeOutputStrategy(strategy: RecompositionOutputStrategy) {
        synchronized(lock) {
            tracker?.removeOutputStrategy(strategy)
        }
    }
}

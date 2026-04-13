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

import androidx.compose.runtime.InternalComposeTracingApi
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.core.datetime.DateTime
import kotlin.random.Random

/**
 * 重组追踪引擎核心。
 *
 * 负责：
 * - 通过 [CompositionTracer] 接收编译器注入的重组回调（自动覆盖所有 Composable）
 * - 维护事件缓冲区
 * - 管理 Snapshot 观察者，追踪 State 变更
 * - 采样率控制
 * - 生成分析报告
 *
 * 本类非线程安全，由 [RecompositionProfiler] 负责同步。
 */
internal class RecompositionTracker {

    companion object {
        /**
         * stateChangeAccumulator 的最大 key 数量上限。
         *
         * 策略：超限后不再新增 key，已有 key 继续更新计数（非 FIFO，不淘汰旧 key）。
         * 原因：每个 key 代表不同的 State 对象，FIFO 淘汰哪个都是信息损失，
         *       而"不新增 key"的影响极小——正常业务页面的 State 对象通常只有几十到一两百个。
         *
         * 内存估算：key 为格式化 String（~100 bytes），value 为 3 字段对象（~50 bytes），
         *           500 条约 75KB，对 profiler 场景完全可接受。
         */
        private const val MAX_STATE_CHANGE_ENTRIES = 500
    }

    private var config: RecompositionConfig = RecompositionConfig.DEFAULT

    /** 当前帧内的重组计数 */
    private var currentFrameRecomposedCount: Int = 0

    /** 事件缓冲区，使用 ArrayDeque 保证 removeFirst() 为 O(1) */
    private val events = ArrayDeque<RecompositionEvent>()

    /** 帧计数器（包含虚拟帧，仅用作 frameId） */
    private var frameCounter: Long = 0L

    /** 实际有重组事件并被 flush 的帧计数（用于 Report.totalFrames） */
    private var flushedFrameCounter: Long = 0L

    /** 当前帧是否被采样 */
    private var currentFrameSampled: Boolean = true

    /** 追踪开始时间 */
    internal var startTimestampMs: Long = 0L
        private set

    /** 会话 ID */
    internal var sessionId: String = ""
        private set

    /** Snapshot 观察者取消句柄 */
    private var snapshotObserverDisposable: (() -> Unit)? = null

    /** 当前帧内记录的 State 变更 key 集合 */
    private val currentFrameStateChanges = mutableSetOf<String>()

    /** 累积的 State 变更记录 */
    private val stateChangeAccumulator = mutableMapOf<String, MutableStateChangeAccumulator>()

    /**
     * 累积的 Composable 重组统计。
     *
     * Key 为 Composable **函数名**（如 "CounterSection"），而非实例。
     * 同一函数无论被调用多少次（如 LazyColumn item），都归入同一条目。
     * 因此条目数 = 页面中不同 Composable 函数的数量（通常几十个），不会无限增长。
     */
    private val composableAccumulator = mutableMapOf<String, MutableComposableAccumulator>()

    /**
     * Apply callback 中 formatState() 结果的缓存。
     * Key 为 identityHashCode，Value 为格式化后的字符串（如 "State(prev=1, now=2), readers: ..."）。
     *
     * 时序：apply callback 在 traceEventEnd 之前执行（apply → 调度重组 → 重组执行）。
     * apply callback 中 formatState() 能正确读到旧 prevValue（updateLastSeenValue 还未覆盖）。
     * traceEventEnd 精确路径查此缓存，避免依赖已被覆盖的 prevValue。
     * 每帧结束时（onFrameEnd）清空。
     */
    private val stateChangeCache = mutableMapOf<Int, String>()

    /**
     * State 身份注册表。
     * 记录每个 State 的 prevValue 及读取者 Composable，
     * 供 [ProfilerCompositionObserver] 和 [registerSnapshotObserver] 使用。
     *
     * Key 为 identityHashCode（Int），条目数 = 页面上 State 对象实例数量，
     * 有自然上限（通常几十到一两百个），不持有 State 对象引用，不阻止 GC。
     */
    internal val stateIdentityRegistry = StateIdentityRegistry()

    /** CompositionTracer 追踪栈，记录嵌套的 Composable 调用 */
    private val traceStack = mutableListOf<TraceEntry>()

    /**
     * CompositionObserver 实例，用于精确的 scope→state 重组原因追踪。
     * 通过 [BaseComposeScene] 注册到 Composition 实例上。
     */
    internal val compositionObserver: ProfilerCompositionObserver by lazy {
        ProfilerCompositionObserver(this)
    }

    /** 当前是否有精确的 scope→state 映射可用 */
    private var hasPreciseScopeMapping: Boolean = false

    /** 输出策略列表 */
    private val outputStrategies = mutableListOf<RecompositionOutputStrategy>()

    // ========== 框架 Composable 过滤 ==========

    /**
     * 框架内部包名前缀列表。
     * 当 [RecompositionConfig.includeFrameworkComposables] 为 false 时，
     * info 以这些前缀开头的 Composable 将被忽略。
     */
    private val frameworkPrefixes = listOf(
        "androidx.compose.runtime.",
        "androidx.compose.ui.",
        "androidx.compose.foundation.",
        "androidx.compose.material.",
        "androidx.compose.material3.",
        "androidx.compose.animation.",
        "com.tencent.kuikly.compose.foundation.",
        "com.tencent.kuikly.compose.material.",
        "com.tencent.kuikly.compose.material3.",
        "com.tencent.kuikly.compose.ui.",
        "com.tencent.kuikly.compose.animation.",
        "com.tencent.kuikly.compose.runtime.",
        // 图片加载 Composable — 框架内部实现细节
        "rememberAsyncImagePainter",
        "rememberAsyncImagePainterInternal",
        "painterResource",
        // ViewModel 相关内部 Composable（viewModel.kt:95、collectAsState 等）
        "collectAsState",
        "viewModel"
    )

    /**
     * Overlay 内部 Composable 名称前缀，始终过滤，不计入热点。
     * Overlay 自身读取 Profiler 数据时会触发重组，不应污染统计数据。
     */
    private val overlayPrefixes = listOf(
        "com.tencent.kuikly.compose.profiler."
    )

    /**
     * 判断给定的 composable info 是否属于框架内部函数或 Overlay 内部函数。
     */
    private fun isFrameworkComposable(info: String): Boolean {
        return frameworkPrefixes.any { info.startsWith(it) }
            || overlayPrefixes.any { info.startsWith(it) }
    }

    // ========== CompositionTracer 实现 ==========

    /**
     * 全局 CompositionTracer 实例，供 [RecompositionProfiler] 注册到 Composer。
     * 编译器在每个 @Composable 函数中注入了 traceEventStart/End 调用，
     * 注册后即可自动接收所有 Composable 的重组通知。
     */
    @OptIn(InternalComposeTracingApi::class)
    internal val compositionTracer: CompositionTracer = object : CompositionTracer {
        override fun isTraceInProgress(): Boolean = true

        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            onComposableTraceStart(key, info, dirty1, dirty2)
        }

        override fun traceEventEnd() {
            onComposableTraceEnd()
        }
    }

    /** trace 栈条目 */
    private class TraceEntry(
        val key: Int,
        val info: String,
        val startTimeMs: Long,
        val dirty1: Int = 0,
        val dirty2: Int = 0
    )

    /**
     * 启动追踪。
     */
    fun start(config: RecompositionConfig) {
        this.config = config
        this.startTimestampMs = DateTime.currentTimestamp()
        this.sessionId = "rcp-${startTimestampMs}-${Random.nextInt(10000)}"
        this.frameCounter = 0L
        this.flushedFrameCounter = 0L
        events.clear()
        currentFrameStateChanges.clear()
        stateChangeAccumulator.clear()
        composableAccumulator.clear()
        stateIdentityRegistry.clear()

        if (config.enableStateTracking) {
            registerSnapshotObserver()
        }
    }

    /**
     * 停止追踪，释放资源。
     */
    fun stop() {
        unregisterSnapshotObserver()
        traceStack.clear()
        hasPreciseScopeMapping = false
    }

    /**
     * 重置所有采集数据。
     */
    fun reset() {
        events.clear()
        frameCounter = 0L
        flushedFrameCounter = 0L
        currentFrameStateChanges.clear()
        stateChangeCache.clear()
        stateChangeAccumulator.clear()
        composableAccumulator.clear()
        stateIdentityRegistry.clear()
        startTimestampMs = DateTime.currentTimestamp()
        sessionId = "rcp-${startTimestampMs}-${Random.nextInt(10000)}"
        // 通知所有输出策略清空自身数据
        for (strategy in outputStrategies) {
            strategy.onReset()
        }
    }

    /**
     * 触发所有输出策略的 onReportReady，输出报告日志等。
     */
    fun notifyReportReady(report: RecompositionReport) {
        for (strategy in outputStrategies) {
            strategy.onReportReady(report)
        }
    }

    /**
     * 更新配置，下一帧生效。
     */
    fun updateConfig(config: RecompositionConfig) {
        val oldStateTracking = this.config.enableStateTracking
        this.config = config
        // 动态开关 State 追踪
        if (config.enableStateTracking && !oldStateTracking) {
            registerSnapshotObserver()
        } else if (!config.enableStateTracking && oldStateTracking) {
            unregisterSnapshotObserver()
        }
    }

    /**
     * 添加输出策略。
     */
    fun addOutputStrategy(strategy: RecompositionOutputStrategy) {
        if (!outputStrategies.contains(strategy)) {
            outputStrategies.add(strategy)
        }
    }

    /**
     * 移除输出策略。
     */
    fun removeOutputStrategy(strategy: RecompositionOutputStrategy) {
        outputStrategies.remove(strategy)
    }

    // ========== 帧追踪 ==========

    /**
     * 重组帧开始时调用。
     * 返回 true 表示当前帧被采样，应记录详细数据。
     */
    fun onFrameStart(): Boolean {
        frameCounter++
        currentFrameSampled = shouldSampleFrame()
        if (!currentFrameSampled) return false

        currentFrameStateChanges.clear()
        currentFrameRecomposedCount = 0
        val event = RecompositionFrameStartEvent(
            timestampMs = DateTime.currentTimestamp(),
            frameId = frameCounter
        )
        addEvent(event)
        return true
    }

    /**
     * 重组帧结束时调用。
     * recomposedCount 参数已弃用，内部自动统计。
     */
    fun onFrameEnd(recomposedCount: Int) {
        if (!currentFrameSampled) return

        val now = DateTime.currentTimestamp()
        val frameStart = events.lastOrNull { it is RecompositionFrameStartEvent } as? RecompositionFrameStartEvent
        val durationMs = if (frameStart != null) now - frameStart.timestampMs else 0L

        val endEvent = RecompositionFrameEndEvent(
            timestampMs = now,
            frameId = frameCounter,
            durationMs = durationMs,
            recomposedCount = currentFrameRecomposedCount
        )
        addEvent(endEvent)

        flushCurrentFrameEvents()
        currentFrameRecomposedCount = 0
        currentFrameSampled = false
        stateChangeCache.clear()
    }

    /**
     * 由 BaseComposeScene.render() 在 postponeInvalidation 之后调用。
     * 此时 isInvalidationDisabled 已恢复 false，写 Compose State 能正确触发 invalidation。
     * 通知 OverlayOutputStrategy 刷新 UI。
     */
    fun notifyOverlayIfNeeded() {
        for (strategy in outputStrategies) {
            if (strategy is com.tencent.kuikly.compose.profiler.output.OverlayOutputStrategy) {
                strategy.flushIfNeeded()
            }
        }
    }

    // ========== CompositionTracer 回调处理 ==========

    /**
     * CompositionTracer.traceEventStart 回调。
     * 编译器在每个 @Composable 函数的非 skip 路径开头调用此方法。
     *
     * @param key 编译器生成的函数唯一标识
     * @param info 函数名 + 源码位置，如 "CounterSection (Demo.kt:195)"
     * @param dirty1 第一个 $dirty bitmask（参数 #0 ~ #13 的变更状态）
     * @param dirty2 第二个 $dirty bitmask（参数 #14+），无额外参数时为 0
     */
    private fun onComposableTraceStart(key: Int, info: String, dirty1: Int, dirty2: Int) {
        if (!currentFrameSampled) {
            return
        }
        traceStack.add(TraceEntry(key, info, DateTime.currentTimestamp(), dirty1, dirty2))
    }

    /**
     * CompositionTracer.traceEventEnd 回调。
     * 编译器在每个 @Composable 函数的非 skip 路径末尾调用此方法。
     */
    private fun onComposableTraceEnd() {
        if (!currentFrameSampled) return
        if (traceStack.isEmpty()) return

        val entry = traceStack.removeAt(traceStack.lastIndex)

        // 根据配置过滤框架内部 Composable
        if (!config.includeFrameworkComposables && isFrameworkComposable(entry.info)) {
            return
        }

        val now = DateTime.currentTimestamp()
        val durationMs = now - entry.startTimeMs
        val parentInfo = if (traceStack.isNotEmpty()) traceStack.last().info else null

        val composableName = extractComposableName(entry.info)
        val sourceLocation = extractSourceLocation(entry.info)
        val parentName = if (parentInfo != null) extractComposableName(parentInfo) else null

        // Prefer precise scope→state mapping from CompositionObserver,
        // fallback to frame-level coarse-grained state changes.
        //
        // 精确路径查 stateChangeCache（apply callback 里预缓存的 formatState 结果），
        // 因为此时 registry 的 prevValue 已被 updateLastSeenValue 覆盖。
        // Cache miss 时降级调 formatState（显示 value= 格式）。
        val triggerStates: List<String>
        if (hasPreciseScopeMapping) {
            val stateObjects = compositionObserver.getCurrentScopeTriggerStateObjects()
            if (stateObjects != null) {
                // Register reader mappings now
                for (state in stateObjects) {
                    stateIdentityRegistry.recordReader(state, composableName)
                }
                // 查 stateChangeCache 获取 apply callback 里已格式化好的 prev→now 字符串
                triggerStates = stateObjects.map { state ->
                    val hash = com.tencent.kuikly.compose.material3.internal.identityHashCode(state)
                    stateChangeCache[hash] ?: stateIdentityRegistry.formatState(state)
                }
            } else {
                // Forced recomposition or initial composition — use sentinel from observer
                triggerStates = compositionObserver.getCurrentScopeTriggerStates() ?: emptyList()
            }
        } else {
            triggerStates = currentFrameStateChanges.toList()
        }

        // === 参数变更检测（解析编译器 $dirty bitmask） ===
        val paramChanges = DirtyFlagsParser.parse(entry.dirty1, entry.dirty2)
        val reason: RecompositionReason = when {
            triggerStates.isNotEmpty() -> RecompositionReason.STATE_CHANGE
            else -> RecompositionReason.UNKNOWN
        }

        // === Scope key：用于 Overlay 按实例区分，null 表示首次组合 ===
        val scopeKey = compositionObserver.getCurrentScopeKey()

        val event = ComposableRecomposedEvent(
            timestampMs = now,
            composableName = composableName,
            sourceLocation = sourceLocation,
            durationMs = durationMs,
            triggerStates = triggerStates,
            parentName = parentName,
            reason = reason,
            paramChanges = paramChanges,
            scopeKey = scopeKey
        )
        addEvent(event)

        // 累积统计
        currentFrameRecomposedCount++
        val acc = composableAccumulator.getOrPut(composableName) { MutableComposableAccumulator(composableName) }
        acc.recordRecomposition(durationMs, triggerStates, reason, paramChanges)
    }

    // ========== 报告生成 ==========

    /**
     * 生成当前的分析报告。
     */
    fun generateReport(): RecompositionReport {
        val now = DateTime.currentTimestamp()
        val durationMs = now - startTimestampMs
        val durationSeconds = (durationMs / 1000.0).coerceAtLeast(0.001)

        val composableStatsList = composableAccumulator.values.map { acc ->
            val recompositionsPerSecond = acc.count / durationSeconds
            ComposableStats(
                name = acc.name,
                recompositionCount = acc.count,
                totalDurationMs = acc.totalDurationMs,
                avgDurationMs = if (acc.count > 0) acc.totalDurationMs.toDouble() / acc.count else 0.0,
                maxDurationMs = acc.maxDurationMs,
                minDurationMs = acc.minDurationMs,
                triggerStates = acc.allTriggerStates.toSet(),
                isHotspot = recompositionsPerSecond > config.hotspotThreshold,
                paramChangeFrequency = acc.paramChangeFrequency.toMap()
            )
        }.sortedByDescending { it.recompositionCount }

        val hotspots = composableStatsList.filter { it.isHotspot }

        val stateChangeRecords = stateChangeAccumulator.values.map { acc ->
            StateChangeRecord(
                stateKey = acc.stateKey,
                changeCount = acc.count,
                firstChangeMs = acc.firstChangeMs - startTimestampMs,
                lastChangeMs = acc.lastChangeMs - startTimestampMs
            )
        }.sortedByDescending { it.changeCount }

        return RecompositionReport(
            sessionId = sessionId,
            startTimestampMs = startTimestampMs,
            durationMs = durationMs,
            totalFrames = flushedFrameCounter,
            totalRecompositions = composableAccumulator.values.sumOf { it.count },
            composables = composableStatsList,
            hotspots = hotspots,
            stateChanges = stateChangeRecords
        )
    }

    // ========== CompositionObserver 回调 ==========

    /**
     * 由 [ProfilerCompositionObserver.onBeginComposition] 调用。
     * 通知 tracker：本次组合的精确 scope→state 映射已就绪。
     */
    internal fun onCompositionObserverBegin() {
        hasPreciseScopeMapping = true
    }

    /**
     * 由 [ProfilerCompositionObserver.onEndComposition] 调用。
     * 清理精确映射标记。
     */
    internal fun onCompositionObserverEnd() {
        hasPreciseScopeMapping = false
    }

    // ========== 内部方法 ==========

    /**
     * Flush current frame events to output strategies, then remove them from the buffer.
     *
     * Removing flushed events prevents double-output when both flush paths fire for the same frame:
     * - onFrameEnd() path (iOS/normal): called by BaseComposeScene.render() after sendFrame
     * - apply callback sub-composition path: fires when LazyColumn or nested scenes recompose
     *
     * On Android (JVM mode), both paths can fire for the same frame. By removing events after
     * the first flush, the second call finds no FrameStartEvent and outputs nothing.
     */
    private fun flushCurrentFrameEvents() {
        val lastStartIndex = events.indexOfLast { it is RecompositionFrameStartEvent }
        if (lastStartIndex < 0) return
        val frameEvents = events.subList(lastStartIndex, events.size).toList()
        if (frameEvents.any { it is ComposableRecomposedEvent }) {
            flushedFrameCounter++
            for (strategy in outputStrategies) {
                strategy.onFrameComplete(frameEvents)
            }
        }
        // Remove flushed events so a second flush call for the same frame outputs nothing
        while (events.size > lastStartIndex) {
            events.removeLast()
        }
    }

    private fun shouldSampleFrame(): Boolean {
        if (config.sampleRate >= 1.0f) return true
        if (config.sampleRate <= 0.0f) return false
        return Random.nextFloat() < config.sampleRate
    }

    private fun addEvent(event: RecompositionEvent) {
        events.addLast(event)
        // 缓冲区溢出时丢弃最旧事件（O(1)）
        while (events.size > config.maxEventBufferSize) {
            events.removeFirst()
        }
    }

    private fun registerSnapshotObserver() {
        unregisterSnapshotObserver()
        val handle = Snapshot.registerApplyObserver { changedObjects, _ ->
            // Only recover sampling when profiler is actively running.
            // When onFrameEnd already ran (currentFrameSampled=false), sub-composition
            // recompositions (e.g. LazyColumn items) would be missed without this.
            if (RecompositionProfiler.isEnabled && !currentFrameSampled && changedObjects.isNotEmpty()) {
                currentFrameSampled = true
                frameCounter++
                addEvent(RecompositionFrameStartEvent(
                    timestampMs = DateTime.currentTimestamp(),
                    frameId = frameCounter
                ))
            }
            for (obj in changedObjects) {
                // formatState 在 updateLastSeenValue 之前调用：
                // prevValue = 上次 apply 存的旧值（正确），nowValue = 本次 apply 后的新值
                val stateKey = stateIdentityRegistry.formatState(obj)
                onStateChanged(stateKey)

                // 缓存格式化结果，供后续 traceEventEnd 精确路径使用
                val hash = com.tencent.kuikly.compose.material3.internal.identityHashCode(obj)
                stateChangeCache[hash] = stateKey
            }
            // Update lastSeen value for each changed state after formatting,
            // so next apply can show the correct prev value.
            for (obj in changedObjects) {
                stateIdentityRegistry.updateLastSeenValue(obj)
            }
            // Flush any recomposition events that were captured during sub-compositions
            // (e.g. LazyColumn items in nested scenes) that don't have their own onFrameEnd.
            // At this point all traceEventStart/End calls for this apply batch are complete.
            if (currentFrameRecomposedCount > 0 && currentFrameSampled) {
                val now = DateTime.currentTimestamp()
                val frameStart = events.lastOrNull { it is RecompositionFrameStartEvent } as? RecompositionFrameStartEvent
                val durationMs = if (frameStart != null) now - frameStart.timestampMs else 0L
                addEvent(RecompositionFrameEndEvent(
                    timestampMs = now,
                    frameId = frameCounter,
                    durationMs = durationMs,
                    recomposedCount = currentFrameRecomposedCount
                ))
                flushCurrentFrameEvents()
                currentFrameRecomposedCount = 0
                currentFrameSampled = false
            }
        }
        snapshotObserverDisposable = { handle.dispose() }
    }

    private fun unregisterSnapshotObserver() {
        snapshotObserverDisposable?.invoke()
        snapshotObserverDisposable = null
    }

    /**
     * 从 CompositionTracer 的 info 字符串中提取简短的 Composable 函数名。
     * 例如 "CounterSection (Demo.kt:195)" → "CounterSection"
     * 例如 "com.tencent.kuikly.demo.pages.compose.CounterSection (Demo.kt:195)" → "CounterSection"
     */
    private fun extractComposableName(info: String): String {
        // 先去掉源码位置信息 " (Demo.kt:195)"
        val parenIndex = info.indexOf(" (")
        val fullName = if (parenIndex > 0) info.substring(0, parenIndex) else info
        // 如果包含包名（有 .），取最后一段
        val dotIndex = fullName.lastIndexOf('.')
        return if (dotIndex >= 0) fullName.substring(dotIndex + 1) else fullName
    }

    /**
     * 从 CompositionTracer 的 info 字符串中提取源码位置。
     * 例如 "CounterSection (Demo.kt:195)" → "Demo.kt:195"
     */
    private fun extractSourceLocation(info: String): String? {
        val start = info.indexOf(" (")
        if (start < 0) return null
        val end = info.lastIndexOf(')')
        if (end <= start + 2) return null
        return info.substring(start + 2, end)
    }

    private fun onStateChanged(stateKey: String) {
        val now = DateTime.currentTimestamp()
        currentFrameStateChanges.add(stateKey)

        // 已有记录的直接更新；新 key 需检查上限（防止无限积累）
        if (stateChangeAccumulator.containsKey(stateKey)) {
            stateChangeAccumulator[stateKey]!!.recordChange(now)
        } else if (stateChangeAccumulator.size < MAX_STATE_CHANGE_ENTRIES) {
            val acc = MutableStateChangeAccumulator(stateKey, now)
            acc.recordChange(now)
            stateChangeAccumulator[stateKey] = acc
        }
    }

    // ========== 内部累积器 ==========

    private class MutableComposableAccumulator(val name: String) {
        var count: Int = 0
        var totalDurationMs: Long = 0L
        var maxDurationMs: Long = 0L
        var minDurationMs: Long = Long.MAX_VALUE
        val allTriggerStates = mutableSetOf<String>()

        /** Per-parameter-position change frequency (paramIndex → changeCount) */
        val paramChangeFrequency = mutableMapOf<Int, Int>()

        fun recordRecomposition(
            durationMs: Long,
            triggerStates: List<String>,
            reason: RecompositionReason = RecompositionReason.UNKNOWN,
            paramChanges: ParamChangeSummary? = null
        ) {
            count++
            totalDurationMs += durationMs
            if (durationMs > maxDurationMs) maxDurationMs = durationMs
            if (durationMs < minDurationMs) minDurationMs = durationMs
            allTriggerStates.addAll(triggerStates)

            if (paramChanges != null) {
                for (paramIdx in paramChanges.changedParams) {
                    paramChangeFrequency[paramIdx] = (paramChangeFrequency[paramIdx] ?: 0) + 1
                }
            }
        }
    }

    private class MutableStateChangeAccumulator(val stateKey: String, val firstChangeMs: Long) {
        var count: Int = 0
        var lastChangeMs: Long = firstChangeMs

        fun recordChange(timestampMs: Long) {
            count++
            lastChangeMs = timestampMs
        }
    }
}

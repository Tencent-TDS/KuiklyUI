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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONException
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 被测的 tokener。各平台指向该平台桥接层实际使用的实现：ohosArm64 走
 * `JSONTokenerOhos`（K/N 专项优化版），其余平台走 commonMain 的 `JSONTokener`。
 */
expect fun parseWithTokener(json: String): Any?

// Platform-specific functions for memory measurement
expect fun collectGarbage()
expect fun getAllocatedBytes(): Long
expect fun getPlatformName(): String
expect fun getTimeNanosPlatform(): Long

/**
 * 已完成的 GC 轮次计数，取不到时返回 -1。
 *
 * 用于替代 [getAllocatedBytes] 判断分配量：后者是「当前已分配 Page 总大小」这一瞬时驻留水位，
 * 由 GC 触发时机决定，与总分配量无单调关系（详见 kn_json_perf_conversation.md §11）。
 * GC 在堆增长越过阈值时触发，故固定 workload 下 epoch 增量 ≈ 总分配量 / 目标堆大小。
 */
expect fun getGcEpoch(): Long

/** 最近一次 GC 结束后各内存池 `totalObjectsSizeBytes` 之和；不可用时返回 -1。 */
expect fun getHeapObjectBytes(): Long

/** 最近一次 GC 各内存池 `sweptCount` 之和；不可用时返回 -1。 */
expect fun getLastSweptCount(): Long

/** 调试用：dump 最近一次 GC 的池名与统计，确认定制收集器是否填充这些字段。 */
expect fun getGcPoolsDebug(): String

/**
 * 暂停 / 恢复 GC。用于把 STW 排除在解析计时之外。
 * 不支持的平台为空操作。计时迭代内暂停，迭代之间恢复，避免长时间挂起导致 OOM。
 */
expect fun setGcSuspended(suspended: Boolean)

data class TestMetrics(
    val name: String,
    val memBefore: Long,
    val memAfter: Long,
    val gcEpochDelta: Long,
    val sweptCount: Long,
    val retainedBytes: Long,
    val avgNanos: Long,
    val medianNanos: Long,
    val p99Nanos: Long,
    val throughput: Double
)

/**
 * JSON parsing performance test page for Kotlin/Native.
 * Measures parse speed (time) and memory allocation.
 */
@Page("JsonPerfTestPage")
internal class JsonPerfTestPage : BasePager() {

    private var isRunning by observable(false)
    private var isRunningAll by observable(false)
    private var statusText by observable("Preparing test JSON...")
    private var resultLines by observableList<String>()
    private var memBeforeText by observable("Mem before: --")
    private var memAfterText by observable("Mem after: --")
    private var speedText by observable("Avg parse: --")
    private var tokenerText by observable("Tokener: --")
    private val metricsList = mutableListOf<TestMetrics>()
    private var dumpedGcPools = false
    // Retained 测量时把解析结果钉在堆对象上，防止 Release 下局部变量被优化掉导致 GC 收走树。
    private var retainedProbe: Any? = null
    private val retainedAnchor = arrayOfNulls<Any>(1)

    // Pre-built fixtures — generated once in created(), reused by every run.
    private lateinit var jsonSmall1KB: String
    private lateinit var jsonMedium10KB: String
    private lateinit var jsonLarge100KB: String
    private lateinit var jsonHuge1MB: String
    private lateinit var jsonDeepNest: String
    private lateinit var jsonArray1000: String
    private lateinit var jsonEscaped: String

    override fun created() {
        super.created()
        jsonSmall1KB = generateJson(1024)
        jsonMedium10KB = generateJson(10240)
        jsonLarge100KB = generateJson(102400)
        jsonHuge1MB = generateJson(1048576)
        jsonDeepNest = generateDeepNestedJson(50)
        jsonArray1000 = generateLargeArray(1000)
        jsonEscaped = generateEscapedStringJson()
        tokenerText = "Tokener: ${currentTokenerName()}"
        statusText = "Ready. Select a test to run."
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                flexDirectionColumn()
            }

            NavBar {
                attr {
                    title = "JSON Perf Test"
                }
            }

            Text {
                attr {
                    text("K/N JSON Perf Test")
                    fontSize(20f)
                    fontWeightBold()
                    color(Color(0xFF333333))
                    marginTop(10f)
                    marginLeft(16f)
                }
            }

            Text {
                attr {
                    text("Platform: ${getPlatformName()}")
                    fontSize(12f)
                    color(Color(0xFF999999))
                    marginLeft(16f)
                    marginTop(4f)
                }
            }

            Text {
                attr {
                    text(ctx.statusText)
                    fontSize(14f)
                    color(if (ctx.isRunning) Color(0xFFFF8800) else Color(0xFF666666))
                    marginLeft(16f)
                    marginTop(12f)
                    marginBottom(8f)
                }
            }

            // Memory summary
            View {
                attr {
                    flexDirectionRow()
                    marginLeft(16f)
                    marginTop(4f)
                }
                Text {
                    attr {
                        text(ctx.memBeforeText)
                        fontSize(13f)
                        color(Color(0xFF999999))
                        marginRight(20f)
                    }
                }
                Text {
                    attr {
                        text(ctx.memAfterText)
                        fontSize(13f)
                        color(Color(0xFF999999))
                    }
                }
            }

            // Speed summary
            View {
                attr {
                    flexDirectionRow()
                    marginLeft(16f)
                    marginTop(2f)
                }
                Text {
                    attr {
                        text(ctx.speedText)
                        fontSize(13f)
                        color(Color(0xFF999999))
                    }
                }
            }

            // Active tokener (parse 实现开关的当前状态)
            View {
                attr {
                    flexDirectionRow()
                    marginLeft(16f)
                    marginTop(2f)
                }
                Text {
                    attr {
                        text(ctx.tokenerText)
                        fontSize(13f)
                        color(Color(0xFF3F51B5))
                    }
                }
            }

            // "Run All" button (prominent, full-width)
            View {
                attr {
                    marginTop(12f)
                    marginLeft(8f)
                    marginRight(8f)
                }
                ActionButton("▶ RUN ALL TESTS", Color(0xFF4CAF50), ctx.isRunning) {
                    ctx.runAllTests()
                }
            }

            // 一键一致性用例集：对本平台所有 tokener 实现跑同一套用例
            View {
                attr {
                    marginTop(8f)
                    marginLeft(8f)
                    marginRight(8f)
                }
                ActionButton("✔ RUN CONFORMANCE SUITE", Color(0xFF7E57C2), ctx.isRunning) {
                    ctx.runConformanceSuite()
                }
            }

            // Individual test buttons
            View {
                attr {
                    flexDirectionRow()
                    flexWrapWrap()
                    marginTop(8f)
                    marginLeft(8f)
                    marginRight(8f)
                }

                ActionButton("1KB x100") { ctx.runTest("Small (1KB)", ctx.jsonSmall1KB, 100) }
                ActionButton("10KB x100") { ctx.runTest("Medium (10KB)", ctx.jsonMedium10KB, 100) }
                ActionButton("100KB x50") { ctx.runTest("Large (100KB)", ctx.jsonLarge100KB, 50) }
                ActionButton("1MB x25") { ctx.runTest("Huge (1MB)", ctx.jsonHuge1MB, 25) }
                ActionButton("DeepNest x200") { ctx.runTest("Deep (50lv)", ctx.jsonDeepNest, 200, "depth: 50") }
                ActionButton("Array 1000x100") { ctx.runTest("Array 1000", ctx.jsonArray1000, 100, "items: 1000") }
                ActionButton("EscapedStr x200") { ctx.runTest("Escaped Str", ctx.jsonEscaped, 200) }
                ActionButton("GC Collect") { ctx.runGC() }
                ActionButton("Switch Tokener", Color(0xFF3F51B5), ctx.isRunning) { ctx.switchTokener() }
            }

            // Results list
            List {
                attr {
                    marginTop(16f)
                    marginLeft(16f)
                    marginRight(16f)
                    flex(1f)
                }
                vforIndex({ ctx.resultLines }) { line, index, _ ->
                    Text {
                        attr {
                            text(line)
                            fontSize(13f)
                            color(if (index < 3) Color(0xFF333333) else Color(0xFF666666))
                            fontFamily("Courier")
                            marginBottom(2f)
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // Test runners
    // ============================================================

    private fun commitResult(lines: List<String>, doneStatus: String) {
        if (!isRunningAll) {
            isRunning = false
            statusText = doneStatus
            resultLines.clear()
            resultLines.addAll(lines)
        } else {
            resultLines.addAll(lines)
        }
    }

    // KLog 内部依赖 BridgeManager.currentPageId，而该值在 setTimeout 异步回调链中为空，
    // 会导致日志静默丢失。这里临时把 currentPageId 设为当前 pagerId，确保 summary 日志能打到 native。
    @Suppress("DEPRECATION")
    private fun logSummary(msg: String) {
        val lastPageId = BridgeManager.currentPageId
        BridgeManager.currentPageId = pagerId
        try {
            KLog.i("JsonPerfTest", msg)
        } finally {
            BridgeManager.currentPageId = lastPageId
        }
    }

    private fun runTest(name: String, json: String, iterations: Int, sizeExtra: String? = null) {
        if (isRunning && !isRunningAll) return
        if (!isRunningAll) isRunning = true
        val lines = mutableListOf<String>()

        try {
            val actualSize = json.length
            val sizeLine = if (sizeExtra != null) {
                "  JSON size: ${formatBytes(actualSize)} ($sizeExtra)"
            } else {
                "  JSON size: ${formatBytes(actualSize)}"
            }
            lines.add("=== Test: $name ===")
            lines.add(sizeLine)
            lines.add("  Iterations: $iterations")
            lines.add("")
            statusText = "Running: $name..."
            runBenchmark(json, iterations, lines, if (isRunningAll) name else null)
        } catch (e: Exception) {
            lines.add("  ERROR: ${e.message}")
        } finally {
            commitResult(lines, "Done: $name")
        }
    }

    private fun runGC() {
        if (isRunning && !isRunningAll) return
        if (!isRunningAll) isRunning = true
        val lines = mutableListOf<String>()
        try {
            lines.add("=== GC Collect ===")
            val memBefore = getAllocatedBytes()
            memBeforeText = "Mem before GC: ${formatBytes(memBefore.toInt())}"
            lines.add("  Before GC: ${formatBytes(memBefore.toInt())}")

            val startNanos = getTimeNanosPlatform()
            collectGarbage()
            val elapsed = getTimeNanosPlatform() - startNanos

            val memAfter = getAllocatedBytes()
            memAfterText = "Mem after GC: ${formatBytes(memAfter.toInt())}"
            lines.add("  After GC:  ${formatBytes(memAfter.toInt())}")
            lines.add("  Freed:     ${formatBytes((memBefore - memAfter).toInt())}")
            lines.add("  GC time:   ${formatNanos(elapsed)}")
        } catch (e: Exception) {
            lines.add("  ERROR: ${e.message}")
        } finally {
            commitResult(lines, "GC done")
        }
    }

    /**
     * 一键跑解析 / 序列化 / CRUD 一致性用例集。ohos 上会对 legacy 与优化版两份 tokener
     * 各跑一遍并做逐行差分，其余平台只有一份实现，只做绝对校验。
     */
    private fun runConformanceSuite() {
        if (isRunning) return
        isRunning = true
        val lines = mutableListOf<String>()
        var status = "Conformance done"
        try {
            val report = JsonConformance.run()
            lines.add("=== JSON Conformance Suite ===")
            lines.add("  Variants: ${report.variantNames.joinToString(" | ")}")
            lines.addAll(report.summary)
            lines.add("")
            if (report.passed) {
                lines.add("  RESULT: PASS — ${report.checks} checks")
                status = "Conformance PASS (${report.checks} checks)"
            } else {
                lines.add("  RESULT: FAIL — ${report.failures.size} failures / ${report.checks} checks")
                status = "Conformance FAIL (${report.failures.size})"
                val shown = if (report.failures.size > MAX_SHOWN_FAILURES) {
                    report.failures.subList(0, MAX_SHOWN_FAILURES)
                } else {
                    report.failures
                }
                for (failure in shown) {
                    lines.add("  x $failure")
                }
                if (report.failures.size > shown.size) {
                    lines.add("  ... ${report.failures.size - shown.size} more (see log)")
                }
            }
            lines.add("")
            lines.add("  -----------------")
            for (line in lines) {
                logSummary(line)
            }
            // 失败详情全量进日志，UI 只展示前 N 条
            if (!report.passed) {
                for (failure in report.failures) {
                    logSummary("FAIL $failure")
                }
            }
        } catch (e: Exception) {
            lines.add("  ERROR: ${e.message}")
            status = "Conformance ERROR"
            logSummary("Conformance ERROR: ${e.message}")
        } finally {
            tokenerText = "Tokener: ${currentTokenerName()}"
            commitResult(lines, status)
        }
    }

    /** 切换 JSONEngine 实际使用的 tokener（仅 ohos 有两份实现，其余平台为空操作）。 */
    private fun switchTokener() {
        if (isRunning) return
        val variants = tokenerVariants()
        if (variants.size < 2) {
            tokenerText = "Tokener: ${currentTokenerName()} (only one impl)"
            return
        }
        val current = currentTokenerName()
        val next = variants[(variants.indexOfFirst { it.name == current } + 1) % variants.size]
        selectTokener(next.name)
        tokenerText = "Tokener: ${currentTokenerName()}"
        statusText = "Switched tokener to ${currentTokenerName()}"
        logSummary("Switched tokener to ${currentTokenerName()}")
    }

    private fun runBenchmark(json: String, iterations: Int, lines: MutableList<String>, metricsName: String? = null) {
        // Warm-up parse
        try { parseWithTokener(json) } catch (_: Exception) {}

        // GC before measurement
        try { collectGarbage() } catch (_: Exception) {}
        if (!dumpedGcPools) {
            dumpedGcPools = true
            val pools = getGcPoolsDebug()
            lines.add("  GC pools: $pools")
            logSummary("GC pools: $pools")
        }
        val memBefore = try { getAllocatedBytes() } catch (_: Exception) { -1L }

        // Time each iteration with GC suspended, then keep only samples the GC did not
        // enter. suspendGCToggle is best-effort: the runtime resumes suspended GC at
        // safepoints, and parsing hits safepoints constantly, so contaminated samples
        // still occur — they are excluded from statistics rather than retried.
        val rawDurations = LongArray(iterations)
        val cleanFlags = BooleanArray(iterations)
        var gcHitsDuringTimed = 0
        for (i in 0 until iterations) {
            val epochBefore = try { getGcEpoch() } catch (_: Exception) { -1L }
            val elapsed: Long
            try {
                setGcSuspended(true)
                val startIter = getTimeNanosPlatform()
                try {
                    val result = parseWithTokener(json)
                    if (result == null) {
                        lines.add("  WARN: parsed null at iteration $i")
                    }
                } catch (e: JSONException) {
                    lines.add("  ERROR at iteration $i: ${e.message}")
                    return
                }
                elapsed = getTimeNanosPlatform() - startIter
            } finally {
                setGcSuspended(false)
            }
            val epochAfter = try { getGcEpoch() } catch (_: Exception) { -1L }
            val clean = epochBefore < 0 || epochAfter <= epochBefore
            if (!clean) gcHitsDuringTimed++
            rawDurations[i] = elapsed
            cleanFlags[i] = clean
        }

        var cleanCount = 0
        for (f in cleanFlags) if (f) cleanCount++
        // Fall back to all samples if GC contaminated everything, so stats stay defined.
        val useClean = cleanCount >= 3
        val sampleCount = if (useClean) cleanCount else iterations
        val durations = LongArray(sampleCount)
        var w = 0
        for (i in 0 until iterations) {
            if (!useClean || cleanFlags[i]) {
                durations[w++] = rawDurations[i]
            }
        }
        var totalNanos = 0L
        for (d in durations) totalNanos += d

        val memAfter = try { getAllocatedBytes() } catch (_: Exception) { -1L }
        val memDelta = if (memBefore >= 0 && memAfter >= 0) memAfter - memBefore else -1
        // Iterations the GC entered; those samples are excluded from the statistics below.
        val gcEpochDelta = gcHitsDuringTimed.toLong()
        if (gcHitsDuringTimed > 0) {
            lines.add("  GC entered $gcHitsDuringTimed/$iterations iters; stats over $sampleCount clean samples")
        }
        if (!useClean) {
            lines.add("  WARN: too few clean samples ($cleanCount); stats include GC-contaminated iters")
            logSummary("WARN: $metricsName too few clean samples ($cleanCount/$iterations)")
        }

        // Untimed pass: accumulate sweptCount as allocation-volume proxy (object granularity).
        // Kept outside the timed loop so polling / final GC do not pollute Median.
        val sweptCount = measureSweptChurn(json, iterations, lines)

        // Retained size of one parsed tree (object bytes, not page watermark).
        val retainedBytes = measureRetainedBytes(json, lines)

        // Statistics
        durations.sort()
        val minNanos = durations.first()
        val maxNanos = durations.last()
        val avgNanos = totalNanos / sampleCount
        val medianNanos = if (sampleCount % 2 == 0) {
            (durations[sampleCount / 2 - 1] + durations[sampleCount / 2]) / 2
        } else {
            durations[sampleCount / 2]
        }
        val p95Idx = ((sampleCount.toDouble() * 0.95).toLong().coerceAtMost((sampleCount - 1).toLong())).toInt()
        val p99Idx = ((sampleCount.toDouble() * 0.99).toLong().coerceAtMost((sampleCount - 1).toLong())).toInt()
        val p95Nanos = durations[p95Idx]
        val p99Nanos = durations[p99Idx]

        val seconds = totalNanos.toDouble() / 1_000_000_000.0
        val throughput = if (seconds > 0) sampleCount.toDouble() / seconds else 0.0

        lines.add("  --- Parse Time (GC-free samples only: $sampleCount/$iterations) ---")
        lines.add("  Total:      ${formatNanos(totalNanos)}")
        lines.add("  Avg:        ${formatNanos(avgNanos)}")
        lines.add("  Min:        ${formatNanos(minNanos)}")
        lines.add("  Max:        ${formatNanos(maxNanos)}")
        lines.add("  Median:     ${formatNanos(medianNanos)}")
        lines.add("  P95:        ${formatNanos(p95Nanos)}")
        lines.add("  P99:        ${formatNanos(p99Nanos)}")
        lines.add("  Throughput: ${formatDouble(throughput, 1)} parses/sec")
        lines.add("  GC hits:    $gcEpochDelta (excluded from stats)")
        speedText = "Avg: ${formatNanos(avgNanos)} | Median: ${formatNanos(medianNanos)} | P99: ${formatNanos(p99Nanos)}"

        if (memDelta >= 0) {
            val perParse = memDelta / iterations
            lines.add("")
            lines.add("  --- Memory (page watermark; NOT allocation volume) ---")
            lines.add("  Before:     ${formatBytes(memBefore.toInt())}")
            lines.add("  After:      ${formatBytes(memAfter.toInt())}")
            lines.add("  Delta:      ${formatBytes(memDelta.toInt())}")
            lines.add("  Per parse:  ${formatBytes(perParse.toInt())}")
            memBeforeText = "Mem before: ${formatBytes(memBefore.toInt())}"
            memAfterText = "Mem after: ${formatBytes(memAfter.toInt())}"
        }

        if (gcEpochDelta >= 0) {
            lines.add("  GC epochs:  $gcEpochDelta")
        }
        if (sweptCount >= 0) {
            lines.add("  Swept objs: $sweptCount  (${sweptCount / iterations}/parse)")
        }
        if (retainedBytes >= 0) {
            lines.add("  Retained:   ${formatBytes(retainedBytes.toInt())}")
        }

        lines.add("")
        lines.add("  -----------------")

        if (metricsName != null && isRunningAll) {
            metricsList.add(
                TestMetrics(
                    metricsName, memBefore, memAfter, gcEpochDelta,
                    sweptCount, retainedBytes,
                    avgNanos, medianNanos, p99Nanos, throughput
                )
            )
        }
    }

    /**
     * Re-run the same iterations without timing, polling [getLastSweptCount] whenever
     * [getGcEpoch] advances. Final forced GC captures garbage left by the last iterations.
     * Returns -1 if the runtime does not expose sweep statistics.
     */
    private fun measureSweptChurn(json: String, iterations: Int, lines: MutableList<String>): Long {
        try { collectGarbage() } catch (_: Exception) {}
        var lastEpoch = try { getGcEpoch() } catch (_: Exception) { -1L }
        if (lastEpoch < 0) return -1L
        var probe = try { getLastSweptCount() } catch (_: Exception) { -1L }
        if (probe < 0) return -1L

        var totalSwept = 0L
        var missedEpochs = 0L
        for (i in 0 until iterations) {
            try {
                parseWithTokener(json)
            } catch (_: Exception) {
                return -1L
            }
            val epoch = try { getGcEpoch() } catch (_: Exception) { -1L }
            if (epoch > lastEpoch) {
                if (epoch > lastEpoch + 1) {
                    missedEpochs += epoch - lastEpoch - 1
                }
                val swept = try { getLastSweptCount() } catch (_: Exception) { -1L }
                if (swept >= 0) totalSwept += swept
                lastEpoch = epoch
            }
        }
        try { collectGarbage() } catch (_: Exception) {}
        val epoch = try { getGcEpoch() } catch (_: Exception) { -1L }
        if (epoch > lastEpoch) {
            if (epoch > lastEpoch + 1) {
                missedEpochs += epoch - lastEpoch - 1
            }
            val swept = try { getLastSweptCount() } catch (_: Exception) { -1L }
            if (swept >= 0) totalSwept += swept
        }
        if (missedEpochs > 0) {
            lines.add("  WARN: missed $missedEpochs GC epoch(s) while accumulating sweptCount")
        }
        return totalSwept
    }

    /**
     * Bracket retained size of one parsed tree using post-GC object bytes.
     * Pins the tree on [retainedProbe]/[retainedAnchor] so Release escape analysis
     * cannot drop the reference across [collectGarbage].
     */
    private fun measureRetainedBytes(json: String, lines: MutableList<String>): Long {
        try { collectGarbage() } catch (_: Exception) {}
        try { collectGarbage() } catch (_: Exception) {}
        val base = try { getHeapObjectBytes() } catch (_: Exception) { -1L }
        if (base < 0) return -1L

        val tree = try {
            parseWithTokener(json)
        } catch (e: Exception) {
            lines.add("  WARN: retained measure parse failed: ${e.message}")
            return -1L
        }
        retainedProbe = tree
        retainedAnchor[0] = tree

        try { collectGarbage() } catch (_: Exception) {}
        val withTree = try { getHeapObjectBytes() } catch (_: Exception) { -1L }
        val pinned = retainedAnchor[0]
        val probe = retainedProbe
        retainedProbe = null
        retainedAnchor[0] = null

        val delta = if (withTree >= 0) withTree - base else -1L
        val dbg = "Retained dbg: base=$base withTree=$withTree delta=$delta pinned=${pinned != null} probe=${probe != null}"
        lines.add("  $dbg")
        logSummary(dbg)
        return delta
    }

    // ============================================================
    // Auto-run all tests sequentially
    // ============================================================

    private fun runAllTests() {
        if (isRunning) return
        isRunning = true
        isRunningAll = true
        val allLines = mutableListOf<String>()
        allLines.add("╔══════════════════════════════════╗")
        allLines.add("║   AUTO-RUN ALL TESTS STARTED    ║")
        allLines.add("╚══════════════════════════════════╝")
        allLines.add("")
        allLines.add("  Total tests: 7")
        allLines.add("  Tokener: ${currentTokenerName()}")
        allLines.add("  Post-test cleanup: 3x GC @ 2s intervals")
        allLines.add("")
        resultLines.clear()
        resultLines.addAll(allLines)
        speedText = "Avg parse: --"
        metricsList.clear()

        val queue = listOf<(String) -> Unit>(
            { label -> runTest("Small (1KB)", jsonSmall1KB, 100); statusText = "[1/7] $label" },
            { label -> runTest("Medium (10KB)", jsonMedium10KB, 100); statusText = "[2/7] $label" },
            { label -> runTest("Large (100KB)", jsonLarge100KB, 50); statusText = "[3/7] $label" },
            // 1MB allocates enough to trigger GC on most iterations; run enough of them
            // that the GC-free subset is still a usable sample.
            { label -> runTest("Huge (1MB)", jsonHuge1MB, 25); statusText = "[4/7] $label" },
            { label -> runTest("Deep (50lv)", jsonDeepNest, 200, "depth: 50"); statusText = "[5/7] $label" },
            { label -> runTest("Array 1000", jsonArray1000, 100, "items: 1000"); statusText = "[6/7] $label" },
            { label -> runTest("Escaped Str", jsonEscaped, 200); statusText = "[7/7] $label" },
        )

        executeAutoQueue(0, queue)
    }

    private fun executeAutoQueue(index: Int, queue: List<(String) -> Unit>) {
        if (index >= queue.size) {
            // All tests done — generate summary table
            val summary = buildSummaryTable()
            logSummary("===== SUMMARY TABLE (lines=${summary.size}) =====")
            for (line in summary) {
                logSummary(line)
            }
            logSummary("===== END SUMMARY TABLE =====")
            resultLines.addAll(summary)
            isRunning = false
            isRunningAll = false
            statusText = "All 7 tests completed!"
            return
        }

        val label = "Running test ${index + 1}/${queue.size}..."
        queue[index](label)

        // After each test, do 3 GC collects with 1-second intervals
        scheduleGCCollects(3, 1000) {
            executeAutoQueue(index + 1, queue)
        }
    }

    private fun scheduleGCCollects(times: Int, delayMs: Int, onComplete: () -> Unit) {
        if (times <= 0) {
            onComplete()
            return
        }
        statusText = "GC cleanup (${times} remaining)..."
        try { collectGarbage() } catch (_: Exception) {}
        // 必须使用 PagerScope.setTimeout（通过 this 显式指定接收者），
        // 否则会匹配到已废弃的顶层 setTimeout(callback, timeout)，它依赖
        // BridgeManager.currentPageId —— 该值仅在 native→Kotlin 同步调用栈中有效，
        // 在异步回调链中可能为空，导致 GlobalFunctions 注册到空 pagerId，
        // 回调永远无法被路由回来 → executeAutoQueue 无法推进到 index >= queue.size，
        // summary table 永远不生成。
        if (times == 1) {
            this.setTimeout(timeout = delayMs) { onComplete() }
        } else {
            this.setTimeout(timeout = delayMs) {
                scheduleGCCollects(times - 1, delayMs, onComplete)
            }
        }
    }

    private fun buildSummaryTable(): List<String> {
        val lines = mutableListOf<String>()
        lines.add("")
        lines.add("==================== SUMMARY TABLE ====================")
        if (metricsList.isEmpty()) {
            lines.add("  (no metrics collected — tests may have failed early)")
            lines.add("=======================================================")
            return lines
        }
        // GcHits = GC epochs that slipped into a timed iteration (0 = clean; suspend worked).
        lines.add(TableUtil.formatRow("TestName", "Swept", "Retained", "GcHits", "Median", "P99"))
        lines.add("--------------------------------------------------")

        for (m in metricsList) {
            val name = m.name
            val swept = m.sweptCount.toString()
            val retained = m.retainedBytes.toString()
            val gcHits = m.gcEpochDelta.toString()
            val med = formatNanosCompact(m.medianNanos)
            val p99 = formatNanosCompact(m.p99Nanos)
            lines.add(TableUtil.formatRow(name, swept, retained, gcHits, med, p99))
        }

        lines.add("==================================================")
        return lines
    }

    private object TableUtil {
        fun formatRow(c1: String, c2: String, c3: String, c4: String, c5: String, c6: String): String {
            val padded = listOf(
                padRight(c1, 17),
                padRight(c2, 10),
                padRight(c3, 10),
                padRight(c4, 6),
                padRight(c5, 9),
                c6
            )
            return padded.joinToString(" | ")
        }

        private fun padRight(s: String, len: Int): String {
            var result = s
            while (result.length < len) result += " "
            return result
        }
    }

    private fun padRight(s: String, len: Int): String {
        var result = s
        while (result.length < len) result += " "
        return result
    }

    // ---- Compact formatters for table ----

    private fun formatNanosCompact(nanos: Long): String = formatNanos(nanos)
    private fun formatBytesCompact(bytes: Int): String = formatBytes(bytes)

    companion object {
        /** 一致性用例集失败详情在 UI 上的展示上限，全量走日志。 */
        private const val MAX_SHOWN_FAILURES = 30

        private fun formatNanos(nanos: Long): String {
            return when {
                nanos < 1_000 -> "${nanos}ns"
                nanos < 1_000_000 -> "${formatDouble(nanos.toDouble() / 1_000.0, 2)}us"
                nanos < 1_000_000_000 -> "${formatDouble(nanos.toDouble() / 1_000_000.0, 2)}ms"
                else -> "${formatDouble(nanos.toDouble() / 1_000_000_000.0, 3)}s"
            }
        }

        private fun formatBytes(bytes: Int): String {
            val b = if (bytes < 0) -(bytes.toLong()) else bytes.toLong()
            val sign = if (bytes < 0) "-" else ""
            return when {
                b < 1024 -> "$sign${b}B"
                b < 1024 * 1024 -> "$sign${formatDouble(b.toDouble() / 1024.0, 1)}KB"
                else -> "$sign${formatDouble(b.toDouble() / (1024.0 * 1024.0), 2)}MB"
            }
        }

        private fun formatDouble(value: Double, decimals: Int): String {
            val factor = pow10(decimals)
            val rounded = kotlin.math.round(value * factor) / factor
            val intPart = rounded.toLong()
            if (decimals == 0) return intPart.toString()
            val fracPart = kotlin.math.abs((rounded - intPart) * factor).toLong()
            var fracStr = fracPart.toString()
            while (fracStr.length < decimals) fracStr = "0$fracStr"
            return "$intPart.$fracStr"
        }

        private fun pow10(n: Int): Double {
            var result = 1.0
            repeat(n) { result *= 10.0 }
            return result
        }

        private fun generateJson(targetSize: Int): String {
            val sb = StringBuilder()
            sb.append("{")
            sb.append("\"metadata\":{")
            sb.append("\"version\":\"1.0.0\",")
            sb.append("\"generator\":\"K/N Perf Test\",")
            sb.append("\"timestamp\":${getTimeNanosPlatform() / 1_000_000},")
            sb.append("\"description\":\"Auto-generated test JSON for perf benchmarking\"")
            sb.append("},")
            sb.append("\"items\":[")

            val itemCount = maxOf(1, targetSize / 400)
            for (i in 0 until itemCount) {
                if (i > 0) sb.append(",")
                sb.append("{")
                sb.append("\"id\":$i,")
                sb.append("\"name\":\"Item number $i\",")
                sb.append("\"active\":${i % 2 == 0},")
                sb.append("\"score\":${i * 1.5},")
                sb.append("\"tags\":[\"tag_${i % 10}\",\"category_${i % 5}\"],")
                sb.append("\"metadata\":{")
                sb.append("\"created\":\"2024-0${(i % 9) + 1}-${(i % 28) + 1}T${(i % 24).toString().padStart(2, '0')}:${(i % 60).toString().padStart(2, '0')}:00Z\",")
                sb.append("\"priority\":${i % 3},")
                sb.append("\"nullable\":${if (i % 7 == 0) "null" else "\"value_$i\""}")
                sb.append("}")
                sb.append("}")
            }

            sb.append("],")
            sb.append("\"summary\":{")
            sb.append("\"totalItems\":$itemCount,")
            sb.append("\"averageScore\":${itemCount * 0.75},")
            sb.append("\"distribution\":{")
            sb.append("\"low\":${itemCount / 3},")
            sb.append("\"medium\":${itemCount / 3},")
            sb.append("\"high\":${itemCount - 2 * (itemCount / 3)}")
            sb.append("}")
            sb.append("},")
            sb.append("\"unicode_test\":\"Hello \\u4e16\\u754c \\u0393\\u03b5\\u03b9\\u03ac \\u0633\\u0644\\u0627\\u0645\"")
            sb.append("}")
            return sb.toString()
        }

        private fun generateDeepNestedJson(depth: Int): String {
            val sb = StringBuilder()
            for (i in 0 until depth) {
                for (j in 0 until i) sb.append("  ")
                sb.append("{\"level\":$i,\"name\":\"node_$i\",\"children\":")
            }
            sb.append("{\"level\":$depth,\"name\":\"leaf\",\"value\":42}")
            for (i in 0 until depth + 1) {
                sb.append("}")
            }
            return sb.toString()
        }

        private fun generateLargeArray(size: Int): String {
            val sb = StringBuilder()
            sb.append("[")
            for (i in 0 until size) {
                if (i > 0) sb.append(",")
                sb.append("{\"index\":$i,\"value\":\"item_$i\",\"flag\":${i % 2 == 0},\"count\":$i}")
            }
            sb.append("]")
            return sb.toString()
        }

        private fun generateEscapedStringJson(): String {
            val sb = StringBuilder()
            sb.append("{\"strings\":[")
            for (i in 0 until 50) {
                if (i > 0) sb.append(",")
                sb.append("\"")
                sb.append("Line $i\\n")
                sb.append("Tab\\there\\t")
                sb.append("Quote: \\\"hello\\\" ")
                sb.append("Backslash: \\\\ ")
                sb.append("Unicode: \\u0041\\u0042\\u0043 ")
                sb.append("Chinese: \\u4e2d\\u6587 ")
                sb.append("Special: !@#\$%^&*()_+-=[]{}|;:',.<>?/ ")
                sb.append("Pad_")
                for (k in 0 until (i % 20 + 5)) sb.append("x")
                sb.append("\"")
            }
            sb.append("]}")
            return sb.toString()
        }
    }
}

// ============================================================
// ActionButton reusable component
// ============================================================

internal class ActionButtonView(
    private val label: String,
    private val bgColor: Color = Color(0xFF2196F3),
    private val disabled: Boolean = false,
    private val onClick: () -> Unit
) : com.tencent.kuikly.core.base.ComposeView<com.tencent.kuikly.core.base.ComposeAttr, com.tencent.kuikly.core.base.ComposeEvent>() {

    override fun createEvent() = com.tencent.kuikly.core.base.ComposeEvent()
    override fun createAttr() = com.tencent.kuikly.core.base.ComposeAttr()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Button {
                attr {
                    height(36f)
                    borderRadius(18f)
                    margin(4f)
                    paddingLeft(14f)
                    paddingRight(14f)
                    backgroundColor(if (ctx.disabled) Color(0xFFBDBDBD) else ctx.bgColor)
                    titleAttr {
                        text(ctx.label)
                        fontSize(13f)
                        color(Color.WHITE)
                    }
                }
                event {
                    click { if (!ctx.disabled) ctx.onClick() }
                }
            }
        }
    }
}

private fun com.tencent.kuikly.core.base.ViewContainer<*, *>.ActionButton(
    label: String,
    bgColor: Color = Color(0xFF2196F3),
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    addChild(ActionButtonView(label, bgColor, disabled, onClick)) {}
}

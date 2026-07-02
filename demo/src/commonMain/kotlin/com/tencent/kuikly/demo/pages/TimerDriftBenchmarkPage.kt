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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * TimerDriftBenchmarkPage
 *
 * 目的：验证 KRThread timer 路径（uv_timer 到点 → 重新入队 → OnAsync 阶段 2 消费）
 * 在真实工作负载下的准时性代价。
 *
 * 设计要点：
 * 1) 定时器使用 [PagerScope.setTimeout] 递归自调用。
 *    这是唯一走 C++ 侧 KuiklyRenderNativeMethodSetTimeout=13 → KRContextScheduler::ScheduleTask
 *    → KRThread::DispatchAsync(task, delayMs) → uv_timer 的路径。
 *    （kuikly Timer.schedule 走的是协程 delay，路径不同，不是本次要测的对象。）
 * 2) 期望时刻在每次 tick 之后基于"当前实际触发时刻 + period"更新：
 *    drift 语义 = "这一次 setTimeout 调度相对期望的额外延迟"，不背历史累计。
 *    这样能干净地测出 uv_timer → 重新入队 → OnAsync 消费的每次调度成本。
 * 3) drift 用 [DateTime.nanoTime]（纳秒单调时钟），毫秒时钟精度不足以体现微秒级差异。
 * 4) 下部 List 走 vforLazy，装 10000 条含 base64 图 + 文字的 item，为 UI/主线程制造压力，
 *    使 KRThread 的执行权互斥（DirectRunOnCurThread borrow vs OnAsync 批处理）真的有争用机会。
 * 5) 停止条件通过 [running] 标志 + Long 级 epoch 双保险：
 *    - running=false 让递归链自然终止；
 *    - epoch 变化让"上一轮回调残留"进不了新一轮统计（防止快速切页导致污染）。
 */
@Page("TimerDriftBenchmarkPage")
internal class TimerDriftBenchmarkPage : BasePager() {

    // ----- 状态展示（observable，UI 才会刷新） -----
    private var statLine3 by observable("3ms  : (waiting)")
    private var statLine10 by observable("10ms : (waiting)")
    private var statLine16 by observable("16ms : (waiting)")
    private var statLine20 by observable("20ms : (waiting)")
    private var elapsedLine by observable("elapsed: 0.0s")

    // ----- 统计器（非 observable，避免每 tick 触发 UI diff） -----
    private val stat3 = DriftStat("3ms", periodMs = 3)
    private val stat10 = DriftStat("10ms", periodMs = 10)
    private val stat16 = DriftStat("16ms", periodMs = 16)
    private val stat20 = DriftStat("20ms", periodMs = 20)

    // ----- 运行控制 -----
    private var running = false
    private var runEpoch: Long = 0
    private var startNs: Long = 0

    // ----- List 数据 -----
    private var listedModels by observableList<TimerBenchItem>()

    override fun pageDidAppear() {
        super.pageDidAppear()
        // 首次进入时才灌入列表数据。放到 pageDidAppear 而非 created 是因为：
        //   * created 阶段还在 pager 初始化链路上（KRRenderCore::DidInit → CreateInstance），
        //     一次塞 10000 条 observableList 会拉长冷启动时间，进而影响首次 benchmark 起点；
        //   * pageDidAppear 之后才启动 timer 采样，此时 UI 已就绪，list 数据也随之可视。
        if (listedModels.isEmpty()) {
            val items = ArrayList<TimerBenchItem>(ITEM_COUNT)
            for (i in 0 until ITEM_COUNT) {
                items.add(
                    TimerBenchItem(
                        index = i,
                        title = "Item #$i - ${randomTitle(i)}",
                        imageBase64 = pickImage(i),
                    ),
                )
            }
            listedModels.addAll(items)
        }
        startBenchmark()
    }

    override fun pageDidDisappear() {
        // 页面切走时立刻停止采样：递归 setTimeout 里靠 running / runEpoch 双保险中止链路。
        // 注意 kotlin 侧没有 pageWillDestroy 生命周期钩子，pageDidDisappear 是最贴合的替代。
        stopBenchmark()
        super.pageDidDisappear()
    }

    private fun startBenchmark() {
        if (running) {
            return
        }
        running = true
        runEpoch += 1
        startNs = DateTime.nanoTime()

        stat3.reset()
        stat10.reset()
        stat16.reset()
        stat20.reset()

        // 每个 timer 独立一条递归链。scheduleNext 内部会自行采样 submitNs 作为期望时刻的锚点，
        // 不需要外部再算 startNs+period 传进去。
        scheduleNext(stat3, runEpoch)
        scheduleNext(stat10, runEpoch)
        scheduleNext(stat16, runEpoch)
        scheduleNext(stat20, runEpoch)

        // UI 刷新单独一路，避免每次 tick 都触发 observable 更新。
        scheduleUiRefresh(runEpoch)
    }

    private fun stopBenchmark() {
        running = false
        // 递增 epoch，让在途回调进不了新一轮采样。
        runEpoch += 1
    }

    /**
     * 递归调度下一次采样。
     *
     * 期望时刻在函数入口处采样：`expectedNs = submitNs + periodNs`。
     * `submitNs` 就是**紧贴 setTimeout 调用前**的 nanoTime，不再从上一层作为参数传下来。
     *
     * 为什么锚点是"调用 setTimeout 时刻"而不是"上一次 tick lambda 里采到的 nowNs"？
     * 因为 setTimeout 的语义就是"从调用起至少 delay ms 后再执行"，要用**最靠近调用
     * setTimeout 的那一刻**去锚。上一次 tick lambda 里的 nowNs 之后还会经过：
     *   * stat.record + KLog.i（诊断分支同步跨端 KLog 会耗几百 µs 到 1ms）
     *   * 递归 scheduleNext 的函数调用开销
     * 如果拿"上次 nowNs + period"当下次 expected，这些耗时就会被无偿送给 timer 的
     * period 预算，让下一次 drift 被系统性拉正，掩盖真实负偏差频率。
     *
     * 首次调用（在 [startBenchmark] 里）也走同一入口取 submitNs，跟递归调用完全一致；
     * 不再有"首次用 startNs、之后用 nowNs"这种两套锚点的语义割裂。
     *
     * 为什么不做 `expectedNs + period` 累加？某次主线程繁忙迟到 15ms 后，后续所有
     * tick 都会背 15ms drift 直到调度追齐，混淆"每次调度成本"和"累计漂移"两个概念。
     * 我们要测的是 kuikly `setTimeout` → uv_timer → 重新入队 → OnAsync 消费这条路径
     * **每一次** 引入的额外延迟，所以不累加。
     */
    private fun scheduleNext(stat: DriftStat, epoch: Long) {
        // 在调 setTimeout **之前**立刻取时间，作为本次 expected 的锚。这样能保证：
        //   * kotlin 侧到 setTimeout 之间零间隙，锚点尽可能靠近 uv_timer_start；
        //   * 上一次 tick lambda 内的所有同步耗时（record / KLog / 递归调用开销）都
        //     只影响那一次的 drift，不会污染本次的 expected 预算。
        val submitNs = DateTime.nanoTime()
        val expectedNs = submitNs + stat.periodNs
        setTimeout(stat.periodMs) {
            // 已停止 / 已换轮：直接退出，避免污染新一轮统计。
            if (!running || epoch != runEpoch) {
                return@setTimeout
            }
            val nowNs = DateTime.nanoTime()
            val driftNs = nowNs - expectedNs
            val prevNowNs = stat.lastNowNs
            val tickIndex = stat.record(driftNs, nowNs)

            // 诊断打印：
            //   1) 首 DIAG_HEAD_TICKS 次 tick 无条件打印，便于看冷启动初期时间线；
            //   2) 之后仅当 drift < 0（这类样本理论上不应出现）打印，用于捕捉异常；
            //   3) 打印内容包括：本档 tick 序号、绝对 nowNs、绝对 expectedNs、
            //      driftNs、以及"距上次 tick 的实际间隔"gapNs。gapNs 才是最能说明
            //      问题的指标——如果 gapNs < period，说明这一次 tick 的确"提前"了。
            //
            //   为什么用绝对 ns 而不转 ms：让每一位数字可核对，避免格式化过程再引入
            //   歧义；后续离线分析可以自己减法算相对偏差。
            if (tickIndex <= DIAG_HEAD_TICKS || driftNs < 0L) {
                val gapNs = if (prevNowNs == 0L) -1L else nowNs - prevNowNs
                KLog.i(
                    LOG_TAG,
                    "raw ${stat.label} #$tickIndex now=$nowNs expected=$expectedNs drift=$driftNs prevNow=$prevNowNs gap=$gapNs period=${stat.periodNs}",
                )
            }

            // 递归入口会自行采样新的 submitNs 作为下次 expected 的锚点，见函数 KDoc 说明。
            scheduleNext(stat, epoch)
        }
    }

    private fun scheduleUiRefresh(epoch: Long) {
        setTimeout(UI_REFRESH_MS) {
            if (!running || epoch != runEpoch) {
                return@setTimeout
            }
            val elapsedNs = DateTime.nanoTime() - startNs
            val elapsedText = "elapsed: ${formatNsAsMs(elapsedNs)}ms (samples: 3=${stat3.count}, 10=${stat10.count}, 16=${stat16.count}, 20=${stat20.count})"
            elapsedLine = elapsedText
            val line3 = stat3.render()
            val line10 = stat10.render()
            val line16 = stat16.render()
            val line20 = stat20.render()
            statLine3 = line3
            statLine10 = line10
            statLine16 = line16
            statLine20 = line20

            // 日志同步输出一份，方便事后把 hilog 贴回来做离线分析。
            //
            // 重要：KLog.i 内部会走 BridgeManager.callModuleMethod 同步跨端，自身有成本，
            // 但它只发生在本路的 UI 刷新 tick（500ms 一次）上，与 stat3/10/16/20 的采样 tick
            // 是 **独立递归链**，因此不会污染被测两项的 drift 分布。如果将来把日志下沉到
            // 每次 tick 内部，需要重新评估这一点。
            //
            // 日志前缀 [TimerDrift] 方便在 hilog 里一行命令过滤：
            //   hdc shell hilog | grep TimerDrift
            KLog.i(LOG_TAG, "---- tick ----")
            KLog.i(LOG_TAG, elapsedText)
            KLog.i(LOG_TAG, line3)
            KLog.i(LOG_TAG, line10)
            KLog.i(LOG_TAG, line16)
            KLog.i(LOG_TAG, line20)

            scheduleUiRefresh(epoch)
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color(0xFFF5F5F5)) }

            // 顶部状态区
            View {
                attr {
                    padding(12f)
                    backgroundColor(Color.WHITE)
                }
                Text {
                    attr {
                        fontSize(16f)
                        fontWeight500()
                        color(Color(0xFF222222))
                        text("Timer Drift Benchmark")
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        fontSize(11f)
                        color(Color(0xFF888888))
                        text("drift = actual_fire_time - (previous_fire_time + period), ms with 3 decimals. Scroll list below to add pressure.")
                    }
                }
                Text {
                    attr {
                        marginTop(6f)
                        fontSize(12f)
                        color(Color(0xFF444444))
                        text(ctx.elapsedLine)
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        fontSize(12f)
                        color(Color(0xFF1E5FA0))
                        text(ctx.statLine3)
                    }
                }
                Text {
                    attr {
                        marginTop(2f)
                        fontSize(12f)
                        color(Color(0xFF1E5FA0))
                        text(ctx.statLine10)
                    }
                }
                Text {
                    attr {
                        marginTop(2f)
                        fontSize(12f)
                        color(Color(0xFF1E5FA0))
                        text(ctx.statLine16)
                    }
                }
                Text {
                    attr {
                        marginTop(2f)
                        fontSize(12f)
                        color(Color(0xFF1E5FA0))
                        text(ctx.statLine20)
                    }
                }
            }

            // 下部大列表
            List {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF5F5F5))
                }
                vforLazy({ ctx.listedModels }) { model, _, _ ->
                    View {
                        attr {
                            height(72f)
                            marginLeft(8f)
                            marginRight(8f)
                            marginTop(6f)
                            padding(8f)
                            borderRadius(6f)
                            backgroundColor(Color.WHITE)
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        Image {
                            attr {
                                width(56f)
                                height(56f)
                                borderRadius(4f)
                                src(model.imageBase64)
                            }
                        }
                        View {
                            attr {
                                marginLeft(10f)
                                flex(1f)
                            }
                            Text {
                                attr {
                                    fontSize(14f)
                                    color(Color(0xFF222222))
                                    text(model.title)
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(12f)
                                    color(Color(0xFF888888))
                                    text("index=${model.index}  ${model.subline()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------- helpers --------

    private fun randomTitle(seed: Int): String {
        // 用 index 派生出稳定但看起来"每条不一样"的字符串，避免 Random 引入的额外分配。
        val words = arrayOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta")
        val a = words[(seed * 7 + 13) % words.size]
        val b = words[(seed * 11 + 5) % words.size]
        return "$a-$b-${seed % 997}"
    }

    private fun pickImage(seed: Int): String = IMAGES[seed % IMAGES.size]

    companion object {
        private const val ITEM_COUNT = 10000
        private const val UI_REFRESH_MS = 500
        private const val LOG_TAG = "TimerDrift"
        // 每档前多少次 tick 强制打 raw 日志（用于观察冷启动阶段的原始时间线）。
        // 20 次足以覆盖启动 + 少量稳态样本，同时不至于把 hilog 灌满。
        private const val DIAG_HEAD_TICKS = 20L

        // 两张不同颜色的极小 base64 PNG（复用自 LazyListExamplePage 里已验证过的样例，
        // 保证 kuikly image decoder 一定能识别；避免用远程 URL 引入网络抖动干扰 timer 测量）。
        private const val IMG_A =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAABQCAMAAAC5zwKfAAABQVBMVEUAAAD////v6u/////t5+3y7PL7+/vx7fLg19/07PTr6+vd1Nzh1uHp5Oj09PTu6e749Pjy8vPv7O/o5Oj4+Pns6ez19fXm3Ob4+Pjp4urp4ujz8PLc2N7Z1dvm5Ofh2uH7/Pzn4efz8/P6+vr09/fb0Nvl2uXr5+ri3+Laztr09fbe1t/o3Ofx8fHdz9729vbk2uHw6fDx8vL9/f3d0N38/Pzb1dvi2ODx8vLq6urv8PDt7e3t7+78/P3s6+zj4uTi4OLRxdHn5uf+//7z9PTZz9je297WztXRx9Dg3uHWydXTytLb19vZ0tjl5OX2+Pfo6OjZ19rOw8329ffu5+7d193d0Nzr5Ovn4ejY1djOzM/d1NzUxtTh0+Hh2+Dc2tzTzNLOxc3ay9rV09bS0NPSx9HW0Nb4+Pnp3+nMyMzl2eVVtzfcAAAAOHRSTlMAA0QHPjAP/uMWDfnYlIZgHOTW0LSgko94d1km9fG6rptsa2BI/O3n5NPKxcKsnFVVI/Ty8OqmhNTrSmMAAAL9SURBVFjD7dXXVuJQFIDh0FE60rGOjr1OTUglGtAEkFCCVAWU4vs/wOxzIjNzw5IVnKvJv7yKrs99SoAwMzMzMzMz+79z2i92A3tpy8doK4ljmqYp6CBq/YDhYl8yDI3FsiiW1pYlraEMjsEzirzAR1eX8T4xgJEkCSKDxBIvCGsrS3iYQyBE0xyMyN/ljIt7DHi42apFGDGX27QYPI8Qo3OzNXM6WCgkjYExTp/vBiIzyNOX/KiuB414QYrJIO4eApHhOHwod48FdegzAib+gDBiBg+IQFUdjjbe7o7DfxbdcS4Ihjk4EwziJf8eUG0+t54j27HE7l4M78KhdaETsVIIzOg7SMKK4VXhMTh8brXEKkTSDA0PhXXHO5t34Y+ESBKDICIOL7gsgpcrNMGbrp+uHTQ4+DUHD+sn8zHYlTBVKj1AFEdngMQah7wyPwOnLTf8qQ/+I03Bs3r7ah63ekY9NodF99bPlE/kaEYPOPD0AfGKp9MNuNubNHmje925u7jNihs/UkH82okUFtEbhzwReXBnmsOnljLKJbaPSdJVe4CRi5pmnwdGWPZ7xL9rTzttAQSiOJ0rlUqiKJYpeHvu2fsq6XLVJq9dRW2O8oNvzrk7GBqz1zj2K/tX1/AUx7JVsGqNyWT/NL7f7vfzeSXbOSfmZktFw67qGCOzMDke61Jj8vra7XbjK7CJl1Jevr3NZo/e+Yi0pa2BpN8XbjRqKEAacOiC0MvV25KUz8uDgabfPPuRnM1mz8FbpFRPEHiefxCgO8AKarMIC1TkTkf2WgicxXFpXfjT0XZYz/V66AewQrMI2tOT8gIjdQYBwkiBertQB0uFkxxhTUE7loUBbYZAy2YR18azAXbb71cqAH62G/1GdkuSVGy3NUlSlDcOPC1JGC2NxG5X0yTwKuBVkBcnjHflhWsiadoAgXg++XOSWKZVn0eSoRfYQcQNvHZiyRxbQCryCyRL7h0bsXzBnS2vx+Nxn8TtFuKjsthsFsLMzMzMzMzsH/cLPKnsav8gklIAAAAASUVORK5CYII="
        private val IMAGES = arrayOf(IMG_A)
    }
}

/**
 * 单条 list item 的模型。
 */
internal data class TimerBenchItem(
    val index: Int,
    val title: String,
    val imageBase64: String,
) {
    fun subline(): String = "row=$index"
}

/**
 * 单个周期档位下的 drift 统计器。
 *
 * 记录 count / min / max / sum(avg)；同时用一个环形缓冲保存最近 [SAMPLE_WINDOW] 条样本，
 * 用于估计 p50 / p95 / p99。窗口大小取 1024，兼顾响应性与内存占用。
 */
internal class DriftStat(val label: String, val periodMs: Int) {
    val periodNs: Long = periodMs.toLong() * 1_000_000L

    var count: Long = 0
        private set
    private var minNs: Long = Long.MAX_VALUE
    private var maxNs: Long = Long.MIN_VALUE
    private var sumNs: Long = 0

    // 上一次 tick 采样到的绝对 nanoTime，用于诊断打印"两次 tick 之间的实际间隔"。
    // 首次 tick 前是 0，代表"没有上一次"。
    var lastNowNs: Long = 0L
        private set

    private val ring = LongArray(SAMPLE_WINDOW)
    private var ringWritten = 0

    fun reset() {
        count = 0
        minNs = Long.MAX_VALUE
        maxNs = Long.MIN_VALUE
        sumNs = 0
        ringWritten = 0
        lastNowNs = 0L
    }

    /**
     * 记录一次 drift 样本。
     *
     * @param driftNs 本次 tick 的漂移
     * @param nowNs   本次 tick 的绝对 nanoTime，用于更新 [lastNowNs]（供诊断使用）
     * @return 累计后的 tick 序号（从 1 开始）
     */
    fun record(driftNs: Long, nowNs: Long): Long {
        count += 1
        if (driftNs < minNs) {
            minNs = driftNs
        }
        if (driftNs > maxNs) {
            maxNs = driftNs
        }
        sumNs += driftNs
        ring[(ringWritten % SAMPLE_WINDOW).toInt()] = driftNs
        ringWritten += 1
        lastNowNs = nowNs
        return count
    }

    fun render(): String {
        if (count == 0L) {
            return "$label : (waiting)"
        }
        // 平均值内部按 ns 除完再格式化：先 ns → 保留最多精度，最后转 ms.mmm。
        val avgNs = sumNs / count
        val (p50Ns, p95Ns, p99Ns) = percentilesNs()
        // 定长文案，方便肉眼比对趋势。全部单位 ms，保留 3 位小数。
        return "$label : n=${count}" +
            "  avg=${formatNsAsMs(avgNs)}ms" +
            "  min=${formatNsAsMs(minNs)}ms" +
            "  max=${formatNsAsMs(maxNs)}ms" +
            "  p50=${formatNsAsMs(p50Ns)}ms" +
            "  p95=${formatNsAsMs(p95Ns)}ms" +
            "  p99=${formatNsAsMs(p99Ns)}ms"
    }

    /**
     * 从 ring buffer 取有效样本，排序后取百分位（单位 ns）。
     * 采样窗口只有 1024，这里做 O(n log n) 排序完全够用。
     */
    private fun percentilesNs(): Triple<Long, Long, Long> {
        val n = if (ringWritten < SAMPLE_WINDOW) ringWritten else SAMPLE_WINDOW
        if (n == 0) {
            return Triple(0L, 0L, 0L)
        }
        val copy = LongArray(n)
        for (i in 0 until n) {
            copy[i] = ring[i]
        }
        copy.sort()
        val p50 = copy[(n * 50 / 100).coerceAtMost(n - 1)]
        val p95 = copy[(n * 95 / 100).coerceAtMost(n - 1)]
        val p99 = copy[(n * 99 / 100).coerceAtMost(n - 1)]
        return Triple(p50, p95, p99)
    }

    companion object {
        private const val SAMPLE_WINDOW = 1024
    }
}

/**
 * 把纳秒时长格式化为 "ms.mmm"（3 位小数），支持负值。
 *
 * 说明：
 * * kuikly commonMain 无 String.format，且 Kotlin/Native 上 String.format 也不通用，
 *   这里手写整数运算避开浮点精度问题（浮点在极小/极大值上都会掉尾数）；
 * * 精度 3 位小数 = 精确到 1 µs，对应本 benchmark 的观测量级已经足够；
 * * 负值时把符号提出来单独处理，避免 `-0.001` 被拆成 `-0` + `.001` 这类奇怪拼接。
 */
internal fun formatNsAsMs(ns: Long): String {
    val negative = ns < 0
    val abs = if (negative) -ns else ns
    val ms = abs / 1_000_000L
    // 先取纳秒余数（最多 6 位），四舍五入到 µs（3 位小数）。
    val subMsNs = abs % 1_000_000L
    val us = (subMsNs + 500L) / 1_000L  // ns → µs, 四舍五入
    // 进位：subMsNs 接近 1_000_000 时，us 可能变成 1000，需要往 ms 进 1。
    val carriedMs: Long
    val fracUs: Long
    if (us >= 1000L) {
        carriedMs = ms + 1
        fracUs = us - 1000L
    } else {
        carriedMs = ms
        fracUs = us
    }
    val fracStr = when {
        fracUs >= 100L -> fracUs.toString()
        fracUs >= 10L -> "0$fracUs"
        else -> "00$fracUs"
    }
    val sign = if (negative && (carriedMs != 0L || fracUs != 0L)) "-" else ""
    return "$sign$carriedMs.$fracStr"
}

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
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.BridgeModule
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 测试 kuikly 线程到主线程大量同步调用的性能。
 *
 * 主要功能：
 * 1. 按低 / 中 / 高 三档不同间隔（2s / 1s / 0.2s）周期性调用 BridgeModule.readAssetFileSync
 * 2. 提供按钮控制是否启用自动发请求；页面打开后默认进入高频自动请求
 * 3. 通过输入框控制端侧反复读取次数 N（模拟慢同步调用），默认 50
 * 4. 每次调用输出耗时与内容摘要（长度、首尾片段）
 */
@Page("ReadAssetFileSyncStressPage")
internal class ReadAssetFileSyncStressPage : BasePager() {

    // 测试用的 asset 文件路径（鸿蒙 ohosApp 已编译产物中存在）
    private val assetPath = "AppTabPage/json/test_0.json"

    // 是否自动发起请求；进入页面默认开启
    private var autoRunning by observable(true)

    // 当前选择的频率档位；进入页面默认高频
    private var levelLabel by observable(LEVEL_HIGH_LABEL)
    private var intervalMs by observable(LEVEL_HIGH_INTERVAL)

    // 端侧反复读取的次数 N，默认 50；同步阻塞 kuikly 线程，N 越大单次耗时越长
    private var repeatCount by observable(DEFAULT_REPEAT_COUNT)
    // 输入框中正在编辑的字符串（应用前的草稿值）
    private var repeatCountInput by observable(DEFAULT_REPEAT_COUNT.toString())

    // 统计数据
    private var totalCount by observable(0)
    private var totalCostMs by observable(0L)
    private var lastCostMs by observable(0L)
    private var minCostMs by observable(Long.MAX_VALUE)
    private var maxCostMs by observable(0L)
    private var lastSummary by observable("(尚未发起请求)")
    private var lastTriggerAt by observable(0L)

    // 用于校验定时回调是否仍属于当前一轮 schedule，避免切换间隔时旧回调继续触发
    private var scheduleToken: Int = 0

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "ReadAssetFileSync 压力测试" } }

            List {
                attr { flex(1f) }

                // 说明区
                View {
                    attr {
                        margin(left = 16f, top = 16f, right = 16f, bottom = 8f)
                    }
                    Text {
                        attr {
                            text(
                                "本页面用于测试 kuikly 线程到主线程的同步调用密集场景。\n" +
                                    "调用 BridgeModule.readAssetFileSync 同步读取 asset 文件，" +
                                    "可选低(2s) / 中(1s) / 高(0.2s) 三档触发频率，" +
                                    "端侧将反复读取 N 次以模拟慢同步调用。\n" +
                                    "asset 路径：${ctx.assetPath}"
                            )
                            fontSize(13f)
                            color(Color(0xFF666666))
                        }
                    }
                }

                // 反复读取次数 N 输入区
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        margin(left = 16f, top = 8f, right = 16f, bottom = 4f)
                    }
                    Text {
                        attr {
                            text("端侧反复读取次数 N：")
                            fontSize(14f)
                            color(Color(0xFF333333))
                        }
                    }
                    Input {
                        attr {
                            width(80f)
                            height(36f)
                            marginRight(8f)
                            fontSize(14f)
                            color(Color.BLACK)
                            backgroundColor(Color(0xFFF2F3F5L))
                            text(ctx.repeatCountInput)
                            placeholder("50")
                            placeholderColor(Color.GRAY)
                            keyboardTypeNumber()
                        }
                        event {
                            textDidChange {
                                ctx.repeatCountInput = it.text
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("应用")
                                color(Color.WHITE)
                                fontSize(13f)
                            }
                            backgroundColor(Color(0xFF007AFF))
                            size(width = 70f, height = 36f)
                            borderRadius(6f)
                            marginRight(8f)
                        }
                        event {
                            click {
                                ctx.applyRepeatCount()
                            }
                        }
                    }
                    Text {
                        attr {
                            text("当前=${ctx.repeatCount}")
                            fontSize(13f)
                            color(Color(0xFF007AFF))
                        }
                    }
                }

                // 频率档位选择
                View {
                    attr {
                        flexDirectionRow()
                        margin(left = 16f, top = 12f, right = 16f, bottom = 4f)
                    }
                    Text {
                        attr {
                            text("触发频率：")
                            fontSize(14f)
                            color(Color(0xFF333333))
                        }
                    }
                    Text {
                        attr {
                            text("当前=${ctx.levelLabel}（${ctx.intervalMs}ms）")
                            fontSize(14f)
                            color(Color(0xFF007AFF))
                        }
                    }
                }

                View {
                    attr {
                        flexDirectionRow()
                        margin(left = 16f, top = 4f, right = 16f, bottom = 8f)
                    }
                    val levels = listOf(
                        Triple(LEVEL_LOW_LABEL, LEVEL_LOW_INTERVAL, "低 (2s)"),
                        Triple(LEVEL_MIDDLE_LABEL, LEVEL_MIDDLE_INTERVAL, "中 (1s)"),
                        Triple(LEVEL_HIGH_LABEL, LEVEL_HIGH_INTERVAL, "高 (0.2s)")
                    )
                    levels.forEach { (label, interval, display) ->
                        Button {
                            attr {
                                titleAttr {
                                    text(display)
                                    color(if (ctx.intervalMs == interval) Color.WHITE else Color(0xFF333333))
                                    fontSize(13f)
                                }
                                backgroundColor(
                                    if (ctx.intervalMs == interval) Color(0xFF007AFF) else Color(0xFFEEEEEE)
                                )
                                size(width = 90f, height = 36f)
                                borderRadius(6f)
                                marginRight(8f)
                            }
                            event {
                                click {
                                    ctx.changeLevel(label, interval)
                                }
                            }
                        }
                    }
                }

                // 自动开关 + 单次手动触发
                View {
                    attr {
                        flexDirectionRow()
                        margin(left = 16f, top = 8f, right = 16f, bottom = 8f)
                    }
                    Button {
                        attr {
                            titleAttr {
                                text(if (ctx.autoRunning) "停止自动请求" else "启动自动请求")
                                color(Color.WHITE)
                                fontSize(15f)
                            }
                            backgroundColor(if (ctx.autoRunning) Color(0xFFE53935) else Color(0xFF43A047))
                            size(width = 160f, height = 40f)
                            borderRadius(8f)
                            marginRight(12f)
                        }
                        event {
                            click {
                                ctx.toggleAutoRunning()
                            }
                        }
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("单次触发")
                                color(Color.WHITE)
                                fontSize(15f)
                            }
                            backgroundColor(Color(0xFF007AFF))
                            size(width = 110f, height = 40f)
                            borderRadius(8f)
                        }
                        event {
                            click {
                                ctx.invokeOnce()
                            }
                        }
                    }
                }

                // 重置按钮
                View {
                    attr {
                        flexDirectionRow()
                        margin(left = 16f, top = 0f, right = 16f, bottom = 8f)
                    }
                    Button {
                        attr {
                            titleAttr {
                                text("重置统计")
                                color(Color(0xFF333333))
                                fontSize(14f)
                            }
                            backgroundColor(Color(0xFFEEEEEE))
                            size(width = 110f, height = 36f)
                            borderRadius(6f)
                        }
                        event {
                            click {
                                ctx.resetStats()
                            }
                        }
                    }
                }

                // 统计信息
                View {
                    attr {
                        margin(left = 16f, top = 12f, right = 16f, bottom = 8f)
                        backgroundColor(Color(0xFFF5F5F5))
                        borderRadius(8f)
                        padding(all = 12f)
                    }
                    Text {
                        attr {
                            text(
                                "状态：" + (if (ctx.autoRunning) "自动运行中" else "已停止") + "\n" +
                                    "总调用次数：${ctx.totalCount}\n" +
                                    "累计耗时：${ctx.totalCostMs} ms\n" +
                                    "平均耗时：${
                                        if (ctx.totalCount == 0) 0.0
                                        else (ctx.totalCostMs.toDouble() / ctx.totalCount)
                                    } ms\n" +
                                    "最近一次耗时：${ctx.lastCostMs} ms\n" +
                                    "最小耗时：${if (ctx.minCostMs == Long.MAX_VALUE) 0 else ctx.minCostMs} ms\n" +
                                    "最大耗时：${ctx.maxCostMs} ms\n" +
                                    "最近触发时间：${ctx.lastTriggerAt}"
                            )
                            fontSize(13f)
                            color(Color(0xFF333333))
                        }
                    }
                }

                // 内容摘要
                View {
                    attr {
                        margin(left = 16f, top = 4f, right = 16f, bottom = 16f)
                        backgroundColor(Color(0xFFFFF8E1))
                        borderRadius(8f)
                        padding(all = 12f)
                    }
                    Text {
                        attr {
                            text("最近一次内容摘要：\n${ctx.lastSummary}")
                            fontSize(13f)
                            color(Color(0xFF333333))
                        }
                    }
                }
            }
        }
    }

    /**
     * 切换频率档位
     */
    private fun toggleAutoRunning() {
        autoRunning = !autoRunning
        if (autoRunning) {
            scheduleNext()
        } else {
            // 通过递增 token 让上一轮残留回调失效
            scheduleToken += 1
        }
    }

    private fun changeLevel(label: String, interval: Int) {
        if (intervalMs == interval) {
            return
        }
        levelLabel = label
        intervalMs = interval
        // 重置调度 token，避免上一轮 setTimeout 用旧间隔继续触发
        scheduleToken += 1
        if (autoRunning) {
            scheduleNext()
        }
    }

    private fun scheduleNext() {
        val tokenSnapshot = scheduleToken
        val delay = intervalMs
        setTimeout(delay) {
            // 校验 token 是否仍有效，且仍处于自动运行状态
            if (!autoRunning || tokenSnapshot != scheduleToken) {
                return@setTimeout
            }
            invokeOnce()
            scheduleNext()
        }
    }

    private fun invokeOnce() {
        val bridge: BridgeModule = getPager().acquireModule(BridgeModule.MODULE_NAME)
        val begin = DateTime.currentTimestamp()
        val content = bridge.readAssetFileSync(assetPath, repeatCount)
        val cost = DateTime.currentTimestamp() - begin

        totalCount += 1
        totalCostMs += cost
        lastCostMs = cost
        if (cost < minCostMs) {
            minCostMs = cost
        }
        if (cost > maxCostMs) {
            maxCostMs = cost
        }
        lastTriggerAt = begin
        lastSummary = buildSummary(content)

        KLog.d(
            TAG,
            "readAssetFileSync #$totalCount cost=${cost}ms length=${content.length} repeatCount=$repeatCount"
        )
    }

    /**
     * 应用输入框中的 N 值，限制在 [1, 10000] 范围内
     */
    private fun applyRepeatCount() {
        val parsed = repeatCountInput.trim().toIntOrNull()
        if (parsed == null) {
            // 解析失败时恢复到当前值
            repeatCountInput = repeatCount.toString()
            return
        }
        val clamped = parsed.coerceIn(MIN_REPEAT_COUNT, MAX_REPEAT_COUNT)
        repeatCount = clamped
        repeatCountInput = clamped.toString()
        KLog.d(TAG, "repeatCount applied = $clamped")
    }

    private fun resetStats() {
        totalCount = 0
        totalCostMs = 0L
        lastCostMs = 0L
        minCostMs = Long.MAX_VALUE
        maxCostMs = 0L
        lastSummary = "(已重置)"
        lastTriggerAt = 0L
    }

    private fun buildSummary(content: String): String {
        if (content.isEmpty()) {
            return "(空内容)"
        }
        val length = content.length
        val head = content.substring(0, minOf(40, length)).replace("\n", " ")
        val tail = if (length > 40) {
            content.substring(maxOf(length - 40, 40), length).replace("\n", " ")
        } else {
            ""
        }
        return "length=$length\nhead=$head\ntail=$tail"
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        // 进入页面后默认按当前选中的高频间隔启动自动请求
        if (autoRunning) {
            scheduleNext()
        }
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        // 离开页面时停止自动调用
        autoRunning = false
        scheduleToken += 1
    }

    companion object {
        private const val TAG = "ReadAssetFileSyncStress"

        private const val LEVEL_LOW_LABEL = "低"
        private const val LEVEL_MIDDLE_LABEL = "中"
        private const val LEVEL_HIGH_LABEL = "高"

        private const val LEVEL_LOW_INTERVAL = 2000
        private const val LEVEL_MIDDLE_INTERVAL = 1000
        private const val LEVEL_HIGH_INTERVAL = 200

        private const val DEFAULT_REPEAT_COUNT = 50
        private const val MIN_REPEAT_COUNT = 1
        private const val MAX_REPEAT_COUNT = 10000
    }
}

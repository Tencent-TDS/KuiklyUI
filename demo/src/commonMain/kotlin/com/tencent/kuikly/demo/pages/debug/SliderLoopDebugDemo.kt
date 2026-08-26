package com.tencent.kuikly.demo.pages.debug

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.SliderPage
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 轮播到最后一帧不循环 —— 复现 Demo
 *
 * 问题描述：
 * 使用 SliderPage 纵向轮播提示文案
 *  （isHorizontal = false、loopPlayIntervalTimeMs = 3000、pageItemHeight = 44rpx），
 *  轮播到最后一帧后不再循环回第一帧，而是停在最后一帧。
 *
 * 复现方式：
 *  1. 本地打开本页面：@Page("SliderLoopDebugDemo", supportInLocal = true)。
 *  2. 观察下面的的纵向轮播：
 *     预期行为：第1帧 -> 第2帧 -> 第3帧 -> 第1帧 -> ... 无限循环；
 *     Bug 表现：第1帧 -> 第2帧 -> 第3帧 -> 停住，索引不再变化。
 *  3. 页面内置卡住检测：索引超过 5 秒未变化即判定“卡在最后一帧”，并以红色展示。
 *
 */
@Page("SliderLoopDebugDemo", supportInLocal = true)
internal class SliderLoopDebugDemo : BasePager() {

    companion object {
        /** 轮播事件 */
        private const val LOOP_INTERVAL_MS = 3000

        /** 卡住检测周期 */
        private const val CHECK_TICK_MS = 1000

        /** 索引连续 5 个 tick（5 秒）未变化，判定为卡在最后一帧 */
        private const val STUCK_TICK_THRESHOLD = 5
    }

    /** 单个轮播实验组的状态 */
    private class LoopCase(
        val tag: String,
        val items: List<String>,
        val isHorizontal: Boolean,
    ) {
        var index by observable(0)
        var noChangeTicks by observable(0)
        var stuck by observable(false)
        var compWidth = 343f
    }

    // 纵向 3 条
    private val verticalCase = LoopCase(
        tag = "vertical-3",
        items = listOf("纵向 · 第一帧", "纵向 · 第二帧", "纵向 · 第三帧"),
        isHorizontal = false,
    )

    // 横向 3 条
    private val horizontalCase = LoopCase(
        tag = "horizontal-3",
        items = listOf("横向 · 第一帧", "横向 · 第二帧", "横向 · 第三帧"),
        isHorizontal = true,
    )

    // 纵向 2 条
    private val vertical2Case = LoopCase(
        tag = "vertical-2",
        items = listOf("纵向 · 第 1/2 帧", "纵向 · 第 2/2 帧"),
        isHorizontal = false,
    )

    // 纵向 5 条
    private val vertical5Case = LoopCase(
        tag = "vertical-5",
        items = listOf(
            "纵向 · 第 1/5 帧",
            "纵向 · 第 2/5 帧",
            "纵向 · 第 3/5 帧",
            "纵向 · 第 4/5 帧",
            "纵向 · 第 5/5 帧",
        ),
        isHorizontal = false,
    )

    // 纵向 5 条（无背景)
    private val vertical5NoBgCase = LoopCase(
        tag = "vertical-5-nobg",
        items = listOf(
            "纵向 · 第 1/5 帧",
            "纵向 · 第 2/5 帧",
            "纵向 · 第 3/5 帧",
            "纵向 · 第 4/5 帧",
            "纵向 · 第 5/5 帧",
        ),
        isHorizontal = false,
    )

    private var checkerStarted = false

    override fun created() {
        super.created()
        startStuckChecker()
    }

    /**
     * 每秒检测一次：若某个轮播的索引超过 5 秒没有变化，说明已经停住（正常轮播每 3 秒必变）。
     * setTimeout 为一次性任务，因此每轮结束后重新调度。
     */
    private fun startStuckChecker() {
        if (checkerStarted) return
        checkerStarted = true
        setTimeout(pagerId, CHECK_TICK_MS) {
            tickCase(verticalCase)
            tickCase(horizontalCase)
            tickCase(vertical2Case)
            tickCase(vertical5Case)
            tickCase(vertical5NoBgCase)
            checkerStarted = false
            startStuckChecker()
        }
    }

    private fun tickCase(loopCase: LoopCase) {
        if (loopCase.items.size <= 1 || loopCase.stuck) return
        loopCase.noChangeTicks += 1
        if (loopCase.noChangeTicks >= STUCK_TICK_THRESHOLD) {
            loopCase.stuck = true

        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF5F6FA))
                }

                View {
                    attr {
                        flexDirectionColumn()
                        padding(16f)
                    }

                    ctx.renderLoopCase(
                        container = this,
                        title = "复现组：纵向 3 条",
                        loopCase = ctx.verticalCase,
                    )

                    ctx.renderLoopCase(
                        container = this,
                        title = "对照组：横向 3 条",
                        loopCase = ctx.horizontalCase,
                    )

                    ctx.renderLoopCase(
                        container = this,
                        title = "对照组：纵向 2 条",
                        loopCase = ctx.vertical2Case,
                    )

                    ctx.renderLoopCase(
                        container = this,
                        title = "对照组：纵向 5 条",
                        loopCase = ctx.vertical5Case,
                    )

                    ctx.renderLoopCase(
                        container = this,
                        title = "对照组：纵向 5 条（无背景）",
                        loopCase = ctx.vertical5NoBgCase,
                        hasBackground = false,
                    )
                }
            }
        }
    }

    /**
     * 渲染一个轮播实验组，结构尽量贴近：
     * 渐变描边外框 + 白色圆角内层 + 左 AI 图标 + SliderPage(flex=1) + 右发送按钮
     */
    private fun renderLoopCase(
        container: ViewContainer<*, *>,
        title: String,
        loopCase: LoopCase,
        hasBackground: Boolean = true,
    ) {
        container.View {
            attr {
                flexDirectionColumn()
                backgroundColor(Color(0xFFFFFFFF))
                borderRadius(12f)
                padding(14f)
                marginBottom(16f)
            }

            Text {
                attr {
                    text(title)
                    fontSize(15f)
                    color(Color(0xFF222222))
                    marginBottom(12f)
                }
            }

            View {
                attr {
                    alignSelfStretch()
                    height(44f)
                    if (hasBackground) {
                        boxShadow(BoxShadow(0f, 4f, 5f, Color(0xFFB2FFFC).opacity(0.4f)))
                        backgroundLinearGradient(
                            Direction.TO_RIGHT,
                            ColorStop(Color(0xFF067FE3), 0f),
                            ColorStop(Color(0xFF7452FE), 0.4f),
                            ColorStop(Color(0xFF3BB5DA), 1f)
                        )
                        padding(0.5f)
                    }
                }

                View {
                    attr {
                        flex(1f)
                        alignSelfStretch()
                        flexDirectionRow()
                        alignItemsCenter()
                        padding(0f, 10f, 0f, 10f)
                        if (hasBackground) {
                            backgroundColor(Color(0xFFFFFFFF).opacity(0.95f))
                        }
                    }

                    View {
                        attr {
                            size(36f, 36f)
                            backgroundColor(Color.BLACK)
                        }
                    }

                    SliderPage {
                        event {
                            pageIndexDidChanged {
                                val index = (it as JSONObject).optInt("index")
                                if (index != loopCase.index) {
                                    loopCase.noChangeTicks = 0
                                    loopCase.index = index
                                }
                            }
                        }
                        attr {
                            flex(1f)
                            alignSelfStretch()
                            isHorizontal = loopCase.isHorizontal
                            width(loopCase.compWidth - 56f - 50f)
                            height(44f)
                            defaultPageIndex = 0
                            pageItemWidth = loopCase.compWidth - 56f - 50f
                            pageItemHeight = 44f
                            scrollEnable = loopCase.items.size > 1
                            loopPlayIntervalTimeMs = if (loopCase.items.size > 1) LOOP_INTERVAL_MS else 0 // 3s轮播一次
                            initSliderItems(loopCase.items) { text ->
                                View {
                                    attr {
                                        flex(1f)
                                        alignSelfStretch()
                                        allCenter()
                                    }
                                    Text {
                                        attr {
                                            textAlignCenter()
                                            text(text)
                                            fontSize(14f)
                                            color(Color(0xFF2E2E2E))
                                            fontWeightNormal()
                                            lines(1)
                                            textOverFlowTail()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    View {
                        attr {
                            size(30f, 30f)
                            backgroundColor(Color.RED)
                        }
                    }
                }
            }

            // 状态行：当前帧 + 是否卡住
            vbind({ loopCase.index to loopCase.stuck }) {
                Text {
                    attr {
                        marginTop(10f)
                        fontSize(12f)
                        text(
                            "当前帧：${loopCase.index + 1}/${loopCase.items.size}    " +
                                    if (loopCase.stuck) "⚠ 卡在最后一帧（Bug 复现）" else "轮播中…"
                        )
                        color(if (loopCase.stuck) Color(0xFFD93025) else Color(0xFF34A853))
                    }
                }
            }
        }
    }
}

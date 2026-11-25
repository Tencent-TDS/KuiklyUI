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
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ScrollerView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 复现：还原自选股排行榜真实结构（对应业务 QSRankListHeader / QSRankListRow / QSSyncScrollState）。
 *
 * 结构：
 * - 每行 = 左侧固定列（排名+股票名）+ 右侧横向 Scroller（最新价/涨跌幅/…）
 * - 表头与所有行的 Scroller 共享同一个同步组，横向联动
 * - 联动写法与业务一致：dragBegin 标记活跃源，scroll 只由活跃源驱动，
 *   follower 通过 setContentOffset 跟随；register 时统一显式声明 contentInset.right
 *
 * 观察方式（手动滑动任意一行/表头的横向区域）：
 * - 第一滚动列为红色锚点。固定列与红色列之间出现空隙即为 inset 残留。
 * - 顶部面板显示活跃行、松手静止 offsetX 与判定结论。
 *
 * 日志 tag: [InsetRepro]
 */
@Page("InsetGapRepro")
internal class InsetGapReproPage : BasePager() {

    private val syncState = SyncState()

    private val stocks = listOf(
        "贵州茅台", "五粮液", "宁德时代", "比亚迪", "隆基绿能", "中国中免",
        "药明康德", "伊利股份", "恒瑞医药", "海天味业", "金龙鱼", "山西汾酒",
        "泸州老窖", "迈瑞医疗", "爱尔眼科", "东方财富", "中信证券", "招商银行",
        "平安银行", "兴业银行"
    )

    private val columns = listOf(
        "最新价", "涨跌幅", "涨跌额", "成交量", "成交额", "换手率",
        "市盈率", "市净率", "总市值", "振幅", "最高", "最低", "今开", "昨收"
    )

    private var statusLine by observable("拖动任意一行横向区域开始")
    private var restLine by observable("松手静止：-")

    private val firstColumnWidth = 100f
    private val columnWidth = 88f
    private val rowHeight = 44f

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF5F6FAL))
                flexDirectionColumn()
            }

            // 顶部状态面板
            View {
                attr {
                    paddingTop(52f)
                    paddingLeft(16f)
                    paddingBottom(10f)
                    alignSelfStretch()
                    backgroundColor(Color(0xFF1F2A3DL))
                }
                Text {
                    attr {
                        color(Color.WHITE)
                        fontSize(13f)
                        text(ctx.statusLine)
                    }
                }
                Text {
                    attr {
                        marginTop(4f)
                        color(Color(0xFFFFD166L))
                        fontSize(13f)
                        text(ctx.restLine)
                    }
                }
            }

            // 表头（固定列 + 横向联动 Scroller，与业务 QSRankListHeader 同构）
            View {
                attr {
                    alignSelfStretch()
                    flexDirectionRow()
                    height(36f)
                    backgroundColor(Color(0xFFEAEDF2L))
                }
                Text {
                    attr {
                        width(ctx.firstColumnWidth)
                        marginLeft(16f)
                        fontSize(13f)
                        color(Color(0xFF666666L))
                        text("名称")
                    }
                }
                Scroller {
                    ref {
                        it.view?.let { sv -> ctx.syncState.register(sv, -1) }
                    }
                    attr {
                        absolutePosition(top = 0f, left = ctx.firstColumnWidth + 16f, bottom = 0f, right = 0f)
                        flexDirectionRow()
                        scrollEnable(true)
                        bouncesEnable(true)
                        showScrollerIndicator(false)
                    }
                    event {
                        dragBegin {
                            ctx.syncState.setActiveSource(-1)
                            ctx.syncState.minOffsetX = 0f
                            ctx.statusLine = "拖拽表头：开始"
                            println("[InsetRepro] dragBegin row=header")
                        }
                        scroll { params ->
                            ctx.syncState.onScroll(-1, params.offsetX)
                        }
                        scrollEnd { params ->
                            ctx.syncState.clearActiveSource(-1)
                            val verdict = if (params.offsetX < -0.5f) "❌ 左侧残留 ${(-params.offsetX).toInt()}pt" else "✅ 已回弹归零"
                            ctx.restLine = "松手静止：表头 offsetX=${params.offsetX.toInt()}  最小=${ctx.syncState.minOffsetX.toInt()}  $verdict"
                            println("[InsetRepro] scrollEnd row=header offsetX=${params.offsetX} min=${ctx.syncState.minOffsetX}")
                        }
                    }
                    for (c in ctx.columns.indices) {
                        Text {
                            attr {
                                width(ctx.columnWidth)
                                fontSize(12f)
                                color(Color(0xFF666666L))
                                text(ctx.columns[c])
                            }
                        }
                    }
                }
            }

            // 行列表（竖向）
            List {
                attr {
                    flex(1f)
                    alignSelfStretch()
                }
                for ((idx, name) in ctx.stocks.withIndex()) {
                    View {
                        attr {
                            alignSelfStretch()
                            flexDirectionRow()
                            height(ctx.rowHeight)
                            backgroundColor(if (idx % 2 == 0) Color.WHITE else Color(0xFFF7F8FAL))
                        }
                        // 固定列：排名 + 股票名
                        View {
                            attr {
                                width(ctx.firstColumnWidth)
                                marginLeft(16f)
                                flexDirectionRow()
                                alignItemsCenter()
                            }
                            Text {
                                attr {
                                    width(22f)
                                    fontSize(11f)
                                    color(if (idx < 3) Color(0xFFE5533DL) else Color(0xFF999999L))
                                    text("${idx + 1}")
                                }
                            }
                            Text {
                                attr {
                                    fontSize(14f)
                                    color(Color(0xFF222222L))
                                    text(name)
                                }
                            }
                        }
                        // 可滚动列（与业务 QSRankListRow 同构）
                        Scroller {
                            ref {
                                it.view?.let { sv -> ctx.syncState.register(sv, idx) }
                            }
                            attr {
                                absolutePosition(top = 0f, left = ctx.firstColumnWidth + 16f, bottom = 0f, right = 0f)
                                flexDirectionRow()
                                scrollEnable(true)
                                bouncesEnable(true)
                                showScrollerIndicator(false)
                            }
                            event {
                                dragBegin {
                                    ctx.syncState.setActiveSource(idx)
                                    ctx.syncState.minOffsetX = 0f
                                    ctx.statusLine = "拖拽行 $idx：开始"
                                    println("[InsetRepro] dragBegin row=$idx")
                                }
                                scroll { params ->
                                    ctx.syncState.onScroll(idx, params.offsetX)
                                }
                                scrollEnd { params ->
                                    ctx.syncState.clearActiveSource(idx)
                                    val verdict = if (params.offsetX < -0.5f) "❌ 左侧残留 ${(-params.offsetX).toInt()}pt" else "✅ 已回弹归零"
                                    ctx.restLine = "松手静止：行 $idx offsetX=${params.offsetX.toInt()}  最小=${ctx.syncState.minOffsetX.toInt()}  $verdict"
                                    println("[InsetRepro] scrollEnd row=$idx offsetX=${params.offsetX} min=${ctx.syncState.minOffsetX}")
                                }
                            }
                            for (c in ctx.columns.indices) {
                                View {
                                    attr {
                                        width(ctx.columnWidth)
                                        alignSelfStretch()
                                        flexDirectionRow()
                                        justifyContentFlexEnd()
                                        alignItemsCenter()
                                        paddingRight(12f)
                                        if (c == 0) {
                                            backgroundColor(Color(0xFFE5533DL))
                                        }
                                    }
                                    Text {
                                        attr {
                                            fontSize(13f)
                                            color(if (c == 1) Color(0xFFE5533DL) else Color(0xFF333333L))
                                            text(ctx.mockValue(c, idx))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mockValue(idx: Int, row: Int): String {
        val seed = (row * 7 + idx * 13) % 100
        val dec = (seed * 37) % 100
        val decStr = if (dec < 10) "0$dec" else "$dec"
        return when (idx) {
            0 -> "${180 + seed}.$decStr"
            1 -> "+$seed.${seed % 10}%"
            2 -> "+$seed.${seed % 10}"
            3 -> "$seed.${seed % 10}万手"
            4 -> "$seed.${seed % 10}亿"
            5 -> "${seed % 10}.${seed % 3}%"
            6 -> "${20 + seed % 40}.${seed % 9}"
            7 -> "${3 + seed % 5}.${seed % 8}"
            8 -> "${2000 + seed}亿"
            9 -> "${seed % 8}.${seed % 5}%"
            10 -> "${190 + seed}.$decStr"
            11 -> "${170 + seed}.$decStr"
            12 -> "${175 + seed}.$decStr"
            else -> "${185 + seed}.$decStr"
        }
    }

    /** 与业务 QSSyncScrollState 对齐的简化实现 */
    private class SyncState {
        var contentOffsetX by observable(0f)
        var minOffsetX = 0f
        private var activeRow = -2 // -1 表示表头，-2 表示无
        private val followers = mutableMapOf<Int, ScrollerView<*, *>>()

        fun register(sv: ScrollerView<*, *>, rowId: Int) {
            followers[rowId] = sv
            // 与业务一致：注册时一次性显式声明 contentInset.right，inset 只在此处声明
            sv.setContentInset(top = 0f, left = 0f, bottom = 0f, right = 16f, animated = false)
            if (contentOffsetX != 0f) {
                sv.setContentOffset(contentOffsetX, 0f, false)
            }
        }

        fun setActiveSource(rowId: Int) {
            activeRow = rowId
        }

        fun clearActiveSource(rowId: Int) {
            if (activeRow == rowId) activeRow = -2
        }

        fun onScroll(rowId: Int, offsetX: Float) {
            if (rowId != activeRow) return
            if (offsetX < minOffsetX) minOffsetX = offsetX
            contentOffsetX = offsetX
            followers.forEach { (idx, sv) ->
                if (idx != rowId) {
                    sv.setContentOffset(offsetX, 0f, false)
                }
            }
        }
    }
}

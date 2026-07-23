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

package com.tencent.kuikly.calendar

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 年视图子组件，显示 12 个月的压缩日历网格。
 *
 * 功能包括：
 * - 年份标题行，支持前后年切换
 * - 2 列 × 6 行共 12 个月的压缩网格布局
 * - 每个月份显示月份标题、简化星期表头和前 3 行（21 天）日期网格
 * - 今日标记（边框圆圈）
 * - 非当月日期灰色显示
 * - 点击月份可触发 [YearViewEvent.onMonthSelected] 回调，用于切换到月视图
 * - 使用 [Scroller] 包裹月份网格，支持纵向滚动
 *
 * 使用方式：
 * ```
 * YearCalendar {
 *     attr {
 *         initialYear = 2025
 *         firstDayOfWeek = 1
 *         locale = CalendarLocale.chinese()
 *     }
 *     event {
 *         onMonthSelected = { year, month -> /* 切换到月视图 */ }
 *     }
 * }
 * ```
 */
class YearView : ComposeView<YearViewAttr, YearViewEvent>() {

    override fun createAttr(): YearViewAttr = YearViewAttr()

    override fun createEvent(): YearViewEvent = YearViewEvent()

    // region 响应式状态

    /** 当前显示年份 */
    var currentYear: Int by observable(0)

    // endregion

    // region 生命周期

    /**
     * 组件创建回调，从 [YearViewAttr] 读取初始年份。
     *
     * 若 [YearViewAttr.initialYear] 为 0，则使用 [CalendarUtils.today] 获取当前年份。
     */
    override fun created() {
        super.created()
        currentYear = if (attr.initialYear > 0) attr.initialYear else CalendarUtils.today().first
    }

    // endregion

    // region body 布局

    /**
     * 构建年视图的视图层级。
     *
     * 使用 [vbind] 监听 [currentYear] 变化，在年份切换时重建月份网格。
     * 使用 [Scroller] 包裹 12 个月网格，支持纵向滚动。
     */
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
            }

            // 年份标题行
            View {
                attr {
                    flexDirectionRow()
                    justifyContentCenter()
                    alignItemsCenter()
                    height(44f)
                }

                // 前一年按钮
                View {
                    attr {
                        size(44f, 44f)
                        allCenter()
                    }
                    event {
                        click { ctx.currentYear-- }
                    }
                    Text {
                        attr {
                            text("‹")
                            fontSize(24f)
                            color(Color(0xFF333333))
                            textAlignCenter()
                        }
                    }
                }

                // 年份文本 - 使用 vbind 绑定年份变化
                vbind({ ctx.currentYear }) {
                    Text {
                        attr {
                            text(ctx.currentYear.toString())
                            fontSize(17f)
                            fontWeightSemiBold()
                            color(Color(0xFF333333))
                            textAlignCenter()
                        }
                    }
                }

                // 后一年按钮
                View {
                    attr {
                        size(44f, 44f)
                        allCenter()
                    }
                    event {
                        click { ctx.currentYear++ }
                    }
                    Text {
                        attr {
                            text("›")
                            fontSize(24f)
                            color(Color(0xFF333333))
                            textAlignCenter()
                        }
                    }
                }
            }

            // 12 个月网格 - 使用 Scroller 包裹实现纵向滚动
            vbind({ ctx.currentYear }) {
                Scroller {
                    attr {
                        flex(1f)
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            flexWrapWrap()
                        }
                        for (month in 1..12) {
                            // 每个月份占容器一半宽度，实现 2 列布局
                            View {
                                attr {
                                    flex(0.5f)
                                    padding(4f)
                                }

                                // 月份标题
                                Text {
                                    attr {
                                        text(ctx.attr.locale.monthNames[month - 1])
                                        fontSize(13f)
                                        fontWeightSemiBold()
                                        color(Color(0xFF333333))
                                        textAlignCenter()
                                    }
                                }

                                // 简化的星期表头
                                View {
                                    attr {
                                        flexDirectionRow()
                                    }
                                    for (label in ctx.attr.locale.orderedWeekDayShortNames()) {
                                        View {
                                            attr {
                                                flex(1f)
                                                allCenter()
                                            }
                                            Text {
                                                attr {
                                                    text(label)
                                                    fontSize(8f)
                                                    color(Color(0xFF999999))
                                                    textAlignCenter()
                                                }
                                            }
                                        }
                                    }
                                }

                                // 简化的日期网格（只显示前 3 行 = 21 天）
                                val today = CalendarUtils.today()
                                val grid = CalendarUtils.generateMonthGrid(
                                    ctx.currentYear,
                                    month,
                                    ctx.attr.firstDayOfWeek,
                                    today.first,
                                    today.second,
                                    today.third,
                                )
                                val displayCells = grid.take(21)

                                View {
                                    attr {
                                        flexDirectionRow()
                                        flexWrapWrap()
                                    }
                                    for (cell in displayCells) {
                                        val miniCellSize = ctx.attr.miniCellSize
                                        View {
                                            attr {
                                                size(miniCellSize, miniCellSize)
                                                allCenter()
                                                if (cell.isToday) {
                                                    borderRadius(miniCellSize / 2f)
                                                    border(
                                                        Border(
                                                            1f,
                                                            BorderStyle.SOLID,
                                                            ctx.attr.todayColor,
                                                        )
                                                    )
                                                }
                                            }
                                            Text {
                                                attr {
                                                    if (cell.day > 0) {
                                                        text(cell.day.toString())
                                                    } else {
                                                        text("")
                                                    }
                                                    fontSize(10f)
                                                    textAlignCenter()
                                                    color(
                                                        when {
                                                            !cell.isCurrentMonth ->
                                                                ctx.attr.otherMonthTextColor
                                                            cell.isToday -> ctx.attr.todayColor
                                                            else -> ctx.attr.currentMonthTextColor
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 点击月份切换到月视图
                                event {
                                    click {
                                        ctx.event.onMonthSelected?.invoke(
                                            ctx.currentYear,
                                            month,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // endregion
}

/**
 * 年视图属性配置类。
 *
 * 通过 DSL 风格设置年视图的各种属性：
 * ```
 * YearCalendar {
 *     attr {
 *         initialYear = 2025
 *         firstDayOfWeek = 1
 *         locale = CalendarLocale.chinese()
 *         todayColor = Color(0xFFFF3B30)
 *     }
 * }
 * ```
 *
 * @property initialYear 初始年份，0 表示使用当前年份
 * @property firstDayOfWeek 周起始日（0=周日, 1=周一），默认周一
 * @property locale 国际化配置，默认中文
 * @property todayColor 今日标记颜色，默认 0xFFFF3B30
 * @property selectedColor 选中月份背景色，默认 0xFF4A90D9
 * @property currentMonthTextColor 当月日期文字颜色，默认 0xFF333333
 * @property otherMonthTextColor 非当月日期文字颜色，默认 0xFFCCCCCC
 * @property miniCellSize 年视图中日期小格子大小（正方形边长），默认 28f
 */
class YearViewAttr : ComposeAttr() {

    // === 日期配置 ===

    /** 初始年份，0 表示使用当前年份 */
    var initialYear: Int = 0

    // === 视图配置 ===

    /** 周起始日：0=周日, 1=周一，默认周一 */
    var firstDayOfWeek: Int = 1

    /** 国际化配置，默认中文 */
    var locale: CalendarLocale = CalendarLocale.chinese()

    // === 样式配置 ===

    /** 今日标记颜色 */
    var todayColor: Color = Color(0xFFFF3B30)

    /** 选中月份背景色 */
    var selectedColor: Color = Color(0xFF4A90D9)

    /** 当月日期文字颜色 */
    var currentMonthTextColor: Color = Color(0xFF333333)

    /** 非当月日期文字颜色 */
    var otherMonthTextColor: Color = Color(0xFFCCCCCC)

    /** 年视图中日期小格子大小（正方形边长），默认 28f */
    var miniCellSize: Float = 28f
}

/**
 * 年视图事件回调类。
 *
 * 通过 DSL 风格注册事件回调：
 * ```
 * YearCalendar {
 *     event {
 *         onMonthSelected = { year, month ->
 *             println("选中月份: $year-$month")
 *         }
 *     }
 * }
 * ```
 *
 * @property onMonthSelected 点击月份时触发，参数为年份和月份
 */
class YearViewEvent : ComposeEvent() {

    /** 点击月份时的回调，参数为年份和月份 (1-12) */
    var onMonthSelected: ((year: Int, month: Int) -> Unit)? = null
}

/**
 * 在 [ViewContainer] 中添加 [YearView] 的扩展函数。
 *
 * @param init 初始化回调，用于设置属性和事件
 */
fun ViewContainer<*, *>.YearCalendar(init: YearView.() -> Unit) {
    addChild(YearView(), init)
}

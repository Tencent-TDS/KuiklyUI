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
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 周视图子组件，显示单周 7 个日期，支持前后周切换。
 *
 * 功能包括：
 * - 星期表头行（7 个 Text，根据 locale 和 firstDayOfWeek 排列）
 * - 单行日期格子（1 行 × 7 列 = 7 个单元格）
 * - 日期选中高亮（圆形背景色 + 白色文字）
 * - 今日标记（边框圆圈）
 * - 事件标记（底部彩色小圆点）
 * - 非当月日期灰色显示
 * - 支持 [previousWeek] / [nextWeek] 前后周切换
 *
 * 使用方式：
 * ```
 * WeekCalendar {
 *     attr {
 *         initialDate(2025, 7, 23)
 *         firstDayOfWeek = 1
 *         locale = CalendarLocale.chinese()
 *     }
 *     event {
 *         onDateClick = { cell -> /* 处理点击 */ }
 *         onWeekChanged = { year, month, day -> /* 周变化回调 */ }
 *     }
 * }
 * ```
 */
class WeekView : ComposeView<WeekViewAttr, WeekViewEvent>() {

    override fun createAttr(): WeekViewAttr = WeekViewAttr()

    override fun createEvent(): WeekViewEvent = WeekViewEvent()

    // region 响应式状态

    /** 当前显示年份 */
    var currentYear: Int by observable(0)

    /** 当前显示月份 (1-12) */
    var currentMonth: Int by observable(0)

    /** 当前显示日期 (1-31)，表示当前周内的某一天 */
    var currentDay: Int by observable(0)

    /** 当前周的 7 个日期单元格数据 */
    var weekCells: List<DayCell> by observable(emptyList())

    // endregion

    // region 生命周期

    /**
     * 组件创建回调，从 [WeekViewAttr] 读取初始日期并构建周网格数据。
     */
    override fun created() {
        super.created()
        initFromDate()
        rebuildWeekData()
    }

    // endregion

    // region 初始化

    /**
     * 从属性配置中读取初始日期，设置响应式状态。
     *
     * 若 [WeekViewAttr.initialYear] / [WeekViewAttr.initialMonth] / [WeekViewAttr.initialDay]
     * 为 0，则使用 [CalendarUtils.today] 获取当前日期。
     */
    private fun initFromDate() {
        val today = CalendarUtils.today()
        currentYear = if (attr.initialYear > 0) attr.initialYear else today.first
        currentMonth = if (attr.initialMonth > 0) attr.initialMonth else today.second
        currentDay = if (attr.initialDay > 0) attr.initialDay else today.third
    }

    /**
     * 根据当前年月日重建周网格数据。
     *
     * 调用 [CalendarUtils.generateWeekGrid] 生成 7 个 [DayCell]，
     * 正确处理跨月边界。
     */
    private fun rebuildWeekData() {
        val today = CalendarUtils.today()
        weekCells = CalendarUtils.generateWeekGrid(
            year = currentYear,
            month = currentMonth,
            day = currentDay,
            firstDayOfWeek = attr.firstDayOfWeek,
            todayYear = today.first,
            todayMonth = today.second,
            todayDay = today.third,
        )
    }

    // endregion

    // region 前后周切换

    /**
     * 切换到上一周。
     *
     * 将当前日期减 7 天，正确处理跨月、跨年边界。
     * 切换后自动重建网格数据并触发 [WeekViewEvent.onWeekChanged] 回调。
     */
    fun previousWeek() {
        if (currentDay - 7 < 1) {
            currentMonth--
            if (currentMonth < 1) {
                currentMonth = 12
                currentYear--
            }
            currentDay = CalendarUtils.daysInMonth(currentYear, currentMonth) - (7 - currentDay)
        } else {
            currentDay -= 7
        }
        rebuildWeekData()
        event.onWeekChanged?.invoke(currentYear, currentMonth, currentDay)
    }

    /**
     * 切换到下一周。
     *
     * 将当前日期加 7 天，正确处理跨月、跨年边界。
     * 切换后自动重建网格数据并触发 [WeekViewEvent.onWeekChanged] 回调。
     */
    fun nextWeek() {
        val daysInCurMonth = CalendarUtils.daysInMonth(currentYear, currentMonth)
        if (currentDay + 7 > daysInCurMonth) {
            currentDay = currentDay + 7 - daysInCurMonth
            currentMonth++
            if (currentMonth > 12) {
                currentMonth = 1
                currentYear++
            }
        } else {
            currentDay += 7
        }
        rebuildWeekData()
        event.onWeekChanged?.invoke(currentYear, currentMonth, currentDay)
    }

    // endregion

    // region 日期选择

    /**
     * 处理日期单元格点击事件。
     *
     * 触发 [WeekViewEvent.onDateClick] 回调，将点击的 [DayCell] 传递给调用方。
     *
     * @param cell 被点击的日期单元格
     */
    fun selectDate(cell: DayCell) {
        event.onDateClick?.invoke(cell)
    }

    // endregion

    // region body 布局

    /**
     * 构建周视图的视图层级。
     *
     * 使用 [vbind] 监听当前日期 key（year*10000+month*100+day），
     * 在周切换时重建日期格子。
     */
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
            }

            // 星期表头行
            View {
                attr {
                    flexDirectionRow()
                    height(ctx.attr.weekHeaderHeight)
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
                                fontSize(12f)
                                color(ctx.attr.weekHeaderColor)
                                textAlignCenter()
                            }
                        }
                    }
                }
            }

            // 分隔线
            View {
                attr {
                    height(0.5f)
                    backgroundColor(Color(0xFFEEEEEE))
                }
            }

            // 单行日期格子 - 使用 vbind 绑定日期 key，周切换时重建
            vbind({ ctx.currentYear * 10000 + ctx.currentMonth * 100 + ctx.currentDay }) {
                View {
                    attr {
                        flexDirectionRow()
                    }
                    for (cell in ctx.weekCells) {
                        View {
                            attr {
                                flex(1f)
                                size(0f, ctx.attr.cellHeight)
                                allCenter()
                            }
                            event {
                                click { ctx.selectDate(cell) }
                            }

                            // 选中态 + 今日态背景圆
                            vbind({ ctx.attr.selectedDates }) {
                                val isSelected = ctx.attr.selectedDates.contains(cell.date)
                                val isToday = cell.isToday
                                val circleSize = ctx.attr.cellHeight * 0.8f

                                View {
                                    attr {
                                        size(circleSize, circleSize)
                                        allCenter()
                                        borderRadius(circleSize / 2f)

                                        if (isSelected) {
                                            backgroundColor(ctx.attr.selectedColor)
                                        }
                                        if (isToday && !isSelected) {
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
                                            fontSize(14f)
                                            textAlignCenter()
                                            color(
                                                when {
                                                    isSelected -> ctx.attr.selectedTextColor
                                                    !cell.isCurrentMonth ->
                                                        ctx.attr.otherMonthTextColor
                                                    else -> ctx.attr.currentMonthTextColor
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 事件标记小圆点
                            vbind({ ctx.attr.eventMarkMap }) {
                                val mark = ctx.attr.eventMarkMap[cell.date]
                                if (mark != null) {
                                    View {
                                        attr {
                                            size(4f, 4f)
                                            borderRadius(2f)
                                            backgroundColor(mark.color)
                                            positionAbsolute()
                                            bottom(2f)
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

    // endregion
}

/**
 * 周视图属性配置类。
 *
 * 通过 DSL 风格设置周视图的各种属性：
 * ```
 * WeekCalendar {
 *     attr {
 *         initialDate(2025, 7, 23)
 *         firstDayOfWeek = 1
 *         locale = CalendarLocale.chinese()
 *         selectedColor = Color(0xFF4A90D9)
 *     }
 * }
 * ```
 *
 * @property initialYear 初始年份，0 表示使用当前年份
 * @property initialMonth 初始月份 (1-12)，0 表示使用当前月份
 * @property initialDay 初始日期 (1-31)，0 表示使用当前日期
 * @property firstDayOfWeek 周起始日（0=周日, 1=周一），默认周一
 * @property locale 国际化配置，默认中文
 * @property selectedColor 选中日期背景色，默认 0xFF4A90D9
 * @property todayColor 今日标记颜色，默认 0xFFFF3B30
 * @property weekHeaderColor 星期表头文字颜色，默认 0xFF999999
 * @property currentMonthTextColor 当月日期文字颜色，默认 0xFF333333
 * @property otherMonthTextColor 非当月日期文字颜色，默认 0xFFCCCCCC
 * @property selectedTextColor 选中日期文字颜色，默认白色
 * @property cellHeight 日期格子高度，默认 44f
 * @property weekHeaderHeight 星期表头高度，默认 32f
 * @property selectedDates 选中日期集合
 * @property eventMarkMap 事件标记映射
 */
class WeekViewAttr : ComposeAttr() {

    // === 日期配置 ===

    /** 初始年份，0 表示使用当前年份 */
    var initialYear: Int = 0

    /** 初始月份 (1-12)，0 表示使用当前月份 */
    var initialMonth: Int = 0

    /** 初始日期 (1-31)，0 表示使用当前日期 */
    var initialDay: Int = 0

    /**
     * 设置初始日期。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     */
    fun initialDate(year: Int, month: Int, day: Int) {
        initialYear = year
        initialMonth = month
        initialDay = day
    }

    // === 视图配置 ===

    /** 周起始日：0=周日, 1=周一，默认周一 */
    var firstDayOfWeek: Int = 1

    /** 国际化配置，默认中文 */
    var locale: CalendarLocale = CalendarLocale.chinese()

    // === 样式配置 ===

    /** 选中日期背景色 */
    var selectedColor: Color = Color(0xFF4A90D9)

    /** 今日标记颜色 */
    var todayColor: Color = Color(0xFFFF3B30)

    /** 星期表头文字颜色 */
    var weekHeaderColor: Color = Color(0xFF999999)

    /** 当月日期文字颜色 */
    var currentMonthTextColor: Color = Color(0xFF333333)

    /** 非当月日期文字颜色 */
    var otherMonthTextColor: Color = Color(0xFFCCCCCC)

    /** 选中日期文字颜色 */
    var selectedTextColor: Color = Color.WHITE

    /** 日期格子高度，默认 44f */
    var cellHeight: Float = 44f

    /** 星期表头高度，默认 32f */
    var weekHeaderHeight: Float = 32f

    // === 选中与事件标记 ===

    /** 选中日期集合 */
    var selectedDates: Set<CalendarDate> by observable(emptySet())

    /** 事件标记映射，key 为日期，value 为事件标记数据 */
    var eventMarkMap: Map<CalendarDate, CalendarEventMark> by observable(emptyMap())
}

/**
 * 周视图事件回调类。
 *
 * 通过 DSL 风格注册事件回调：
 * ```
 * WeekCalendar {
 *     event {
 *         onDateClick = { cell -> println("点击: ${cell.day}") }
 *         onWeekChanged = { year, month, day -> println("切换周: $year-$month-$day") }
 *     }
 * }
 * ```
 *
 * @property onDateClick 点击日期单元格时触发
 * @property onWeekChanged 周切换时触发，参数为当前年月日
 */
class WeekViewEvent : ComposeEvent() {

    /** 点击日期单元格的回调 */
    var onDateClick: ((DayCell) -> Unit)? = null

    /** 周切换时的回调，参数为当前年份、月份、日期 */
    var onWeekChanged: ((year: Int, month: Int, day: Int) -> Unit)? = null
}

/**
 * 在 [ViewContainer] 中添加 [WeekView] 的扩展函数。
 *
 * @param init 初始化回调，用于设置属性和事件
 */
fun ViewContainer<*, *>.WeekCalendar(init: WeekView.() -> Unit) {
    addChild(WeekView(), init)
}

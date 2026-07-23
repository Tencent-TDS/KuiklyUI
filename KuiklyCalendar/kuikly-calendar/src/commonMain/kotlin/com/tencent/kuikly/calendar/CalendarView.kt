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

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable

/**
 * 日历主组件
 *
 * 通过 ComposeView 三件套封装，支持月视图、日期选择、事件标记等功能。
 * 内部组合 [CalendarHeaderView] 和 [CalendarGridView] 子组件，
 * 管理响应式状态（当前年月、选中日期、视图模式），
 * 并提供 SINGLE / MULTI / RANGE 三种日期选择模式。
 *
 * 使用示例：
 * ```
 * Calendar {
 *     attr {
 *         initialDate(2025, 7, 23)
 *         viewMode = CalendarViewMode.MONTH
 *         selectionMode = CalendarSelectionMode.SINGLE
 *         locale = CalendarLocale.chinese()
 *     }
 *     event {
 *         dateSelectedEvent { selection ->
 *             println("选中: ${selection.dates}")
 *         }
 *         monthChangedEvent { year, month ->
 *             println("切换: $year-$month")
 *         }
 *     }
 * }
 * ```
 */
class CalendarView : ComposeView<CalendarAttr, CalendarEvent>() {

    override fun createAttr(): CalendarAttr = CalendarAttr()

    override fun createEvent(): CalendarEvent = CalendarEvent()

    // region 响应式状态

    /** 当前显示年份 */
    var currentYear: Int by observable(0)

    /** 当前显示月份 (1-12) */
    var currentMonth: Int by observable(0)

    /** 选中日期集合 */
    var selectedDates: Set<CalendarDate> by observable(emptySet())

    /** 当前视图模式 */
    var currentViewMode: CalendarViewMode by observable(CalendarViewMode.MONTH)

    // endregion

    // region 非响应式状态（内部缓存）

    /** 月份网格数据缓存 */
    private var monthGridData: List<DayCell> = emptyList()

    /** 事件标记映射缓存 */
    private var eventMarkMapInternal: Map<CalendarDate, CalendarEventMark> = emptyMap()

    // endregion

    // region 生命周期

    /**
     * 组件创建回调，从 [CalendarAttr] 读取初始日期并构建网格数据。
     */
    override fun created() {
        super.created()
        initFromDate()
        rebuildGridData()
    }

    // endregion

    // region 初始化

    /**
     * 从属性配置中读取初始日期，设置响应式状态。
     *
     * 若 [CalendarAttr.initialYear] / [CalendarAttr.initialMonth] 为 0，
     * 则使用 [CalendarUtils.today] 获取当前日期。
     */
    private fun initFromDate() {
        val today = CalendarUtils.today()
        currentYear = if (attr.initialYear > 0) attr.initialYear else today.first
        currentMonth = if (attr.initialMonth > 0) attr.initialMonth else today.second
        currentViewMode = attr.viewMode

        // 初始选中日期
        if (attr.initialDay > 0) {
            val date = CalendarDate(currentYear, currentMonth, attr.initialDay)
            selectedDates = setOf(date)
        }

        // 事件标记
        eventMarkMapInternal = attr.eventMarks.associateBy { it.date }
    }

    /**
     * 根据当前年月重建月份网格数据。
     */
    private fun rebuildGridData() {
        val today = CalendarUtils.today()
        monthGridData = CalendarUtils.generateMonthGrid(
            year = currentYear,
            month = currentMonth,
            firstDayOfWeek = attr.firstDayOfWeek,
            todayYear = today.first,
            todayMonth = today.second,
            todayDay = today.third,
        )
    }

    // endregion

    // region 月份切换

    /**
     * 切换到上一个月。
     *
     * 若当前月为 1 月，则年份减一并切换到 12 月。
     * 切换后自动重建网格数据并触发 [CalendarEvent.onMonthChanged] 回调。
     */
    fun previousMonth() {
        if (currentMonth == 1) {
            currentMonth = 12
            currentYear--
        } else {
            currentMonth--
        }
        rebuildGridData()
        event.onMonthChanged?.invoke(currentYear, currentMonth)
    }

    /**
     * 切换到下一个月。
     *
     * 若当前月为 12 月，则年份加一并切换到 1 月。
     * 切换后自动重建网格数据并触发 [CalendarEvent.onMonthChanged] 回调。
     */
    fun nextMonth() {
        if (currentMonth == 12) {
            currentMonth = 1
            currentYear++
        } else {
            currentMonth++
        }
        rebuildGridData()
        event.onMonthChanged?.invoke(currentYear, currentMonth)
    }

    // endregion

    // region 日期选择

    /**
     * 处理日期单元格点击事件。
     *
     * 根据 [CalendarAttr.selectionMode] 执行不同的选择逻辑：
     * - [CalendarSelectionMode.SINGLE]：替换为单日期选择
     * - [CalendarSelectionMode.MULTI]：切换选中/取消选中
     * - [CalendarSelectionMode.RANGE]：选择起止日期之间的连续范围
     *
     * 若点击的日期不属于非当月占位格，会自动切换到对应月份。
     * 选择完成后触发 [CalendarEvent.onDateSelected] 回调。
     *
     * @param cell 被点击的日期单元格
     */
    fun selectDate(cell: DayCell) {
        if (cell.day == 0) return // 忽略占位格

        // 如果点击的不是当月日期，自动切换到对应月份
        if (!cell.isCurrentMonth) {
            currentYear = cell.date.year
            currentMonth = cell.date.month
            rebuildGridData()
            event.onMonthChanged?.invoke(currentYear, currentMonth)
        }

        val date = cell.date
        when (attr.selectionMode) {
            CalendarSelectionMode.SINGLE -> {
                selectedDates = setOf(date)
            }
            CalendarSelectionMode.MULTI -> {
                val newSet = selectedDates.toMutableSet()
                if (newSet.contains(date)) newSet.remove(date) else newSet.add(date)
                selectedDates = newSet
            }
            CalendarSelectionMode.RANGE -> {
                val list = selectedDates.toList()
                when {
                    list.isEmpty() || list.size >= 2 -> {
                        selectedDates = setOf(date)
                    }
                    else -> {
                        val start = list[0]
                        val end = if (date > start) date else start
                        val rangeStart = if (date > start) start else date
                        val range = generateDateRange(rangeStart, end)
                        selectedDates = range.toSet()
                    }
                }
            }
        }

        // 计算 timeInMillis 并触发回调
        if (selectedDates.isEmpty()) return
        val firstDate = selectedDates.first()
        val timeMillis = CalendarUtils.dateToTimeMillis(firstDate.year, firstDate.month, firstDate.day)
        event.onDateSelected?.invoke(CalendarSelection(selectedDates.toList(), timeMillis))
    }

    /**
     * 生成从 [start] 到 [end] 的连续日期列表（含两端）。
     *
     * 采用逐日递增方式，自动处理跨月、跨年边界。
     *
     * @param start 起始日期
     * @param end 结束日期（必须 >= [start]）
     * @return 连续日期列表
     */
    private fun generateDateRange(start: CalendarDate, end: CalendarDate): List<CalendarDate> {
        val result = mutableListOf<CalendarDate>()
        var y = start.year
        var m = start.month
        var d = start.day
        while (true) {
            result.add(CalendarDate(y, m, d))
            if (y == end.year && m == end.month && d == end.day) break
            d++
            if (d > CalendarUtils.daysInMonth(y, m)) {
                d = 1
                m++
                if (m > 12) {
                    m = 1
                    y++
                }
            }
        }
        return result
    }

    // endregion

    // region body 布局

    /**
     * 构建日历组件的视图层级。
     *
     * 使用 [vbind] 监听 [currentYear] 变化，在月份切换时重建头部标题和网格数据。
     * 内部组合 [CalendarHeader] 和 [CalendarGrid] 子组件。
     */
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 头部区域 - 独立 vbind 监听年月变化，驱动标题重建
            vbind({ ctx.currentYear * 100 + ctx.currentMonth }) {
                CalendarHeader {
                    attr {
                        title = ctx.attr.locale.formatHeader(ctx.currentYear, ctx.currentMonth)
                        headerHeight = ctx.attr.headerHeight
                        headerTextColor = ctx.attr.headerTextColor
                        showArrows = ctx.attr.showNavigationArrows
                    }
                    event {
                        onPreviousMonth = { ctx.previousMonth() }
                        onNextMonth = { ctx.nextMonth() }
                    }
                }
            }

            // 网格区域 - 独立 vbind 监听年月变化，驱动网格重建
            vbind({ ctx.currentYear * 100 + ctx.currentMonth }) {
                CalendarGrid {
                    attr {
                        cells = ctx.monthGridData
                        selectedDates = ctx.selectedDates
                        weekDayLabels = ctx.attr.locale.orderedWeekDayShortNames()
                        cellSize = if (ctx.attr.cellSize > 0f) ctx.attr.cellSize else 44f
                        weekHeaderHeight = ctx.attr.weekHeaderHeight
                        selectedColor = ctx.attr.selectedColor
                        todayColor = ctx.attr.todayColor
                        weekHeaderColor = ctx.attr.weekHeaderColor
                        currentMonthTextColor = ctx.attr.currentMonthTextColor
                        otherMonthTextColor = ctx.attr.otherMonthTextColor
                        selectedTextColor = ctx.attr.selectedTextColor
                        eventMarkMap = ctx.eventMarkMapInternal
                        gridKey = ctx.currentYear * 100 + ctx.currentMonth
                    }
                    event {
                        onDateClick = { cell -> ctx.selectDate(cell) }
                    }
                }
            }
        }
    }

    // endregion
}

// region 扩展函数注册

/**
 * 日历组件 DSL 入口。
 *
 * 在 [ViewContainer] 中添加 [CalendarView]，支持通过 `attr {}` 和 `event {}` DSL 配置。
 *
 * @param init 初始化回调，用于设置属性和事件
 */
fun ViewContainer<*, *>.Calendar(init: CalendarView.() -> Unit) {
    addChild(CalendarView(), init)
}

// endregion

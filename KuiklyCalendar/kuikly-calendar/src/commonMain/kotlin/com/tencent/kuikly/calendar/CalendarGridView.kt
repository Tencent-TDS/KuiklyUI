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
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 日历网格视图，显示星期表头和日期网格。
 *
 * 功能包括：
 * - 星期表头行（7 个 Text，根据 locale 和 firstDayOfWeek 排列）
 * - 日期网格（flexWrapWrap 布局，42 个单元格）
 * - 日期选中高亮（圆形背景色 + 白色文字）
 * - 今日标记（边框圆圈）
 * - 事件标记（底部彩色小圆点）
 * - 非当月日期灰色显示
 * - 使用 vbind 实现月份切换时网格重建
 *
 * 使用方式：
 * ```
 * CalendarGrid {
 *     attr {
 *         cells = CalendarUtils.generateMonthGrid(2025, 7)
 *         gridKey = 2025 * 100 + 7
 *     }
 *     event {
 *         onDateClick = { cell -> /* 处理点击 */ }
 *     }
 * }
 * ```
 */
class CalendarGridView : ComposeView<CalendarGridAttr, CalendarGridEvent>() {

    override fun createAttr(): CalendarGridAttr = CalendarGridAttr()

    override fun createEvent(): CalendarGridEvent = CalendarGridEvent()

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
                for (label in ctx.attr.weekDayLabels) {
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
                    marginLeft(15f)
                    marginRight(15f)
                }
            }

            // 日期网格 - 使用 vbind 绑定 gridKey，月份切换时重建
            vbind({ ctx.attr.gridKey }) {
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    for (cell in ctx.attr.cells) {
                        View {
                            attr {
                                size(ctx.attr.cellSize, ctx.attr.cellSize)
                                allCenter()
                            }
                            event {
                                click { ctx.event.onDateClick?.invoke(cell) }
                            }

                            // 选中态 + 今日态背景圆
                            vbind({ ctx.attr.selectedDates }) {
                                val isSelected = ctx.attr.selectedDates.contains(cell.date)
                                val isToday = cell.isToday
                                val circleSize = ctx.attr.cellSize * 0.8f

                                View {
                                    attr {
                                        size(circleSize, circleSize)
                                        allCenter()
                                        borderRadius(circleSize / 2f)

                                        if (isSelected) {
                                            backgroundColor(ctx.attr.selectedColor)
                                        }
                                        if (isToday && !isSelected) {
                                            border(Border(1f, BorderStyle.SOLID, ctx.attr.todayColor))
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
                                            color(when {
                                                isSelected -> ctx.attr.selectedTextColor
                                                !cell.isCurrentMonth -> ctx.attr.otherMonthTextColor
                                                else -> ctx.attr.currentMonthTextColor
                                            })
                                        }
                                    }
                                }
                            }

                            // 事件标记小圆点
                            vbind({ ctx.attr.eventMarkMap }) {
                                vif({ ctx.attr.eventMarkMap.containsKey(cell.date) }) {
                                    val mark = ctx.attr.eventMarkMap[cell.date]
                                    View {
                                        attr {
                                            size(4f, 4f)
                                            borderRadius(2f)
                                            if (mark != null) {
                                                backgroundColor(mark.color)
                                            }
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
}

/**
 * 日历网格属性配置。
 *
 * @property cells 42 个网格单元格数据
 * @property selectedDates 选中日期集合
 * @property weekDayLabels 星期表头标签列表（7 个）
 * @property cellSize 格子大小（正方形边长），默认 44f
 * @property weekHeaderHeight 星期表头高度，默认 32f
 * @property selectedColor 选中日期背景色
 * @property todayColor 今日标记颜色
 * @property weekHeaderColor 星期表头文字颜色
 * @property currentMonthTextColor 当月日期文字颜色
 * @property otherMonthTextColor 非当月日期文字颜色
 * @property selectedTextColor 选中日期文字颜色
 * @property eventMarkMap 事件标记映射
 * @property gridKey 网格标识（year*100+month），用于 vbind 触发月份切换重建
 */
class CalendarGridAttr : ComposeAttr() {
    /** 42 个网格单元格数据 */
    var cells: List<DayCell> = emptyList()

    /** 选中日期集合 */
    var selectedDates: Set<CalendarDate> by observable(emptySet())

    /** 星期表头标签列表（7 个，按 firstDayOfWeek 排列） */
    var weekDayLabels: List<String> = listOf("日", "一", "二", "三", "四", "五", "六")

    /** 格子大小（正方形边长） */
    var cellSize: Float = 44f

    /** 星期表头高度 */
    var weekHeaderHeight: Float = 32f

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

    /** 事件标记映射，key 为日期，value 为事件标记数据 */
    var eventMarkMap: Map<CalendarDate, CalendarEventMark> by observable(emptyMap())

    /** 网格标识，用于 vbind 的月份切换重建，建议设为 year*100+month */
    var gridKey: Int by observable(0)
}

/**
 * 日历网格事件回调。
 *
 * @property onDateClick 点击日期单元格时触发
 */
class CalendarGridEvent : ComposeEvent() {
    /** 点击日期单元格的回调 */
    var onDateClick: ((DayCell) -> Unit)? = null
}

/**
 * 在 [ViewContainer] 中添加 [CalendarGridView] 的扩展函数。
 *
 * @param init 初始化回调，用于设置属性和事件
 */
internal fun ViewContainer<*, *>.CalendarGrid(init: CalendarGridView.() -> Unit) {
    addChild(CalendarGridView(), init)
}

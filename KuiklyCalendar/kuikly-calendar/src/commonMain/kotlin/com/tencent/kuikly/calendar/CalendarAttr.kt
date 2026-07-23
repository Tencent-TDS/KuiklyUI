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

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr

/**
 * 日历组件属性配置类
 *
 * 通过 DSL 风格设置日历的各种属性：
 * ```
 * Calendar {
 *     attr {
 *         initialDate(2025, 7, 23)
 *         viewMode = CalendarViewMode.MONTH
 *         selectionMode = CalendarSelectionMode.SINGLE
 *         selectedColor = Color(0xFF4A90D9)
 *     }
 * }
 * ```
 */
class CalendarAttr : ComposeAttr() {

    // === 日期配置 ===

    /** 初始显示年份，0 表示使用当前年份 */
    var initialYear: Int = 0

    /** 初始显示月份 (1-12)，0 表示使用当前月份 */
    var initialMonth: Int = 0

    /** 初始选中日期 (1-31)，0 表示不预选 */
    var initialDay: Int = 0

    /**
     * 设置初始日期
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)，默认 1
     */
    fun initialDate(year: Int, month: Int, day: Int = 1) {
        initialYear = year
        initialMonth = month
        initialDay = day
    }

    // === 视图配置 ===

    /** 日历视图模式，默认月视图 */
    var viewMode: CalendarViewMode = CalendarViewMode.MONTH

    /** 日期选择模式，默认单选 */
    var selectionMode: CalendarSelectionMode = CalendarSelectionMode.SINGLE

    /** 周起始日：0=周日, 1=周一，默认周一 */
    var firstDayOfWeek: Int = 1

    /** 是否显示月份切换箭头，默认 true */
    var showNavigationArrows: Boolean = true

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

    /** 月份标题文字颜色 */
    var headerTextColor: Color = Color(0xFF333333)

    /** 日期格子大小（正方形边长），0 表示自动计算 */
    var cellSize: Float = 0f

    /** 头部区域高度 */
    var headerHeight: Float = 44f

    /** 星期表头行高度 */
    var weekHeaderHeight: Float = 32f

    /** 整体左右内边距 */
    var horizontalPadding: Float = 15f

    // === 事件标记 ===

    /** 事件标记列表，用于在日期上显示事件指示点 */
    var eventMarks: List<CalendarEventMark> = emptyList()

    // === 国际化 ===

    /** 国际化配置，默认中文 */
    var locale: CalendarLocale = CalendarLocale.chinese()
}

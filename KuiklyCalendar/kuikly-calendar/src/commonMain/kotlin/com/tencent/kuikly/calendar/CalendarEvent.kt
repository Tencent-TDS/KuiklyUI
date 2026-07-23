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

import com.tencent.kuikly.core.base.ComposeEvent

/**
 * 日历组件事件回调类
 *
 * 通过 DSL 风格注册事件回调：
 * ```
 * Calendar {
 *     event {
 *         dateSelectedEvent { selection ->
 *             println("选中日期: ${selection.dates}")
 *         }
 *         monthChangedEvent { year, month ->
 *             println("切换月份: $year-$month")
 *         }
 *     }
 * }
 * ```
 */
class CalendarEvent : ComposeEvent() {

    /** 日期选中事件回调，当用户在日历中选中日期时触发 */
    var onDateSelected: ((CalendarSelection) -> Unit)? = null

    /** 月份切换事件回调，当用户切换显示月份时触发 */
    var onMonthChanged: ((year: Int, month: Int) -> Unit)? = null

    /** 视图模式切换事件回调，当视图模式发生变化时触发 */
    var onViewModeChanged: ((CalendarViewMode) -> Unit)? = null

    /**
     * 设置日期选中回调（DSL 风格）
     *
     * @param handler 回调函数，参数为 [CalendarSelection] 选中结果
     */
    fun dateSelectedEvent(handler: (CalendarSelection) -> Unit) {
        onDateSelected = handler
    }

    /**
     * 设置月份切换回调（DSL 风格）
     *
     * @param handler 回调函数，参数为年份和月份
     */
    fun monthChangedEvent(handler: (year: Int, month: Int) -> Unit) {
        onMonthChanged = handler
    }

    /**
     * 设置视图模式切换回调（DSL 风格）
     *
     * @param handler 回调函数，参数为新的 [CalendarViewMode]
     */
    fun viewModeChangedEvent(handler: (CalendarViewMode) -> Unit) {
        onViewModeChanged = handler
    }
}

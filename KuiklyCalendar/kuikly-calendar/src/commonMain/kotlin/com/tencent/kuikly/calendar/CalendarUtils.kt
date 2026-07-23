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

import com.tencent.kuikly.core.datetime.DateTime

/**
 * 日历日期计算工具类。
 *
 * 提供纯 Kotlin 的日期计算方法，避免频繁跨桥调用 [CalendarModule]。
 * 所有月份参数均为 1-based（1 表示一月，12 表示十二月）。
 */
object CalendarUtils {

    private const val MILLIS_PER_DAY = 86_400_000L

    // region daysInMonth

    /**
     * 获取指定月份的天数。
     *
     * 算法参考 [DatePickerView.getDaysInMonth]，正确处理闰年二月：
     * 能被 4 整除但不能被 100 整除，或能被 400 整除的年份为闰年。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 该月的天数（28-31）
     */
    fun daysInMonth(year: Int, month: Int): Int {
        val daysInFebruary =
            if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        val daysInMonths =
            intArrayOf(31, daysInFebruary, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return daysInMonths[month - 1]
    }

    // endregion

    // region dayOfWeek

    /**
     * 计算指定日期是星期几（Tomohiko Sakamoto 算法）。
     *
     * 算法说明：
     * ```
     * val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
     * var year = y
     * if (m < 3) year--
     * return (year + year / 4 - year / 100 + year / 400 + t[m - 1] + d) % 7
     * ```
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     * @return 0=周日, 1=周一, 2=周二, 3=周三, 4=周四, 5=周五, 6=周六
     */
    fun dayOfWeek(year: Int, month: Int, day: Int): Int {
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        var y = year
        if (month < 3) y--
        return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
    }

    // endregion

    // region generateMonthGrid

    /**
     * 生成月份网格数据（6 行 × 7 列 = 42 个单元格）。
     *
     * 生成逻辑：
     * 1. 计算本月 1 号是星期几（[dayOfWeek]）
     * 2. 根据 [firstDayOfWeek] 计算前置占位天数
     * 3. 填充上月尾部的占位 [DayCell]（`isCurrentMonth = false`）
     * 4. 填充本月的 [DayCell]
     * 5. 填充下月头部的占位 [DayCell] 直到 42 个
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param firstDayOfWeek 周起始日（0=周日, 1=周一），默认周一
     * @param todayYear 今天的年份，用于标记今日，0 表示不标记
     * @param todayMonth 今天的月份，0 表示不标记
     * @param todayDay 今天的日期，0 表示不标记
     * @return 42 个 [DayCell] 列表
     */
    fun generateMonthGrid(
        year: Int,
        month: Int,
        firstDayOfWeek: Int = 1,
        todayYear: Int = 0,
        todayMonth: Int = 0,
        todayDay: Int = 0,
    ): List<DayCell> {
        val totalCells = 42
        val firstDow = dayOfWeek(year, month, 1)
        val daysBefore = (firstDow - firstDayOfWeek + 7) % 7
        val daysInCurMonth = daysInMonth(year, month)

        val cells = ArrayList<DayCell>(totalCells)

        for (i in 0 until totalCells) {
            val dayOffset = i - daysBefore // 0-based offset from 1st of month

            if (dayOffset < 0) {
                // 上月占位
                val (pYear, pMonth) = previousMonth(year, month)
                val pDay = daysInMonth(pYear, pMonth) + dayOffset + 1
                cells.add(
                    DayCell(
                        day = pDay,
                        date = CalendarDate(pYear, pMonth, pDay),
                        isCurrentMonth = false,
                        isToday = false,
                    ),
                )
            } else if (dayOffset < daysInCurMonth) {
                // 当月日期
                val d = dayOffset + 1
                cells.add(
                    DayCell(
                        day = d,
                        date = CalendarDate(year, month, d),
                        isCurrentMonth = true,
                        isToday = isToday(year, month, d, todayYear, todayMonth, todayDay),
                    ),
                )
            } else {
                // 下月占位
                val (nYear, nMonth) = nextMonth(year, month)
                val nDay = dayOffset - daysInCurMonth + 1
                cells.add(
                    DayCell(
                        day = nDay,
                        date = CalendarDate(nYear, nMonth, nDay),
                        isCurrentMonth = false,
                        isToday = false,
                    ),
                )
            }
        }

        return cells
    }

    // endregion

    // region generateWeekGrid

    /**
     * 生成单周网格数据（1 行 × 7 列 = 7 个单元格）。
     *
     * 根据指定日期计算其所在周的 7 天，正确处理跨月边界：
     * 若周的起始或结束日期超出当前月份，会自动计算上月/下月的日期。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     * @param firstDayOfWeek 周起始日（0=周日, 1=周一），默认周一
     * @param todayYear 今天的年份，用于标记今日，0 表示不标记
     * @param todayMonth 今天的月份，0 表示不标记
     * @param todayDay 今天的日期，0 表示不标记
     * @return 7 个 [DayCell] 列表
     */
    fun generateWeekGrid(
        year: Int,
        month: Int,
        day: Int,
        firstDayOfWeek: Int = 1,
        todayYear: Int = 0,
        todayMonth: Int = 0,
        todayDay: Int = 0,
    ): List<DayCell> {
        val targetDow = dayOfWeek(year, month, day)
        val daysAfterStart = (targetDow - firstDayOfWeek + 7) % 7
        val startOffset = day - daysAfterStart // 1-based day of month for week start

        val cells = ArrayList<DayCell>(7)

        for (i in 0 until 7) {
            val dayOffset = startOffset + i // 1-based offset from month start
            val (cellYear, cellMonth, cellDay) =
                resolveDate(year, month, dayOffset)
            cells.add(
                DayCell(
                    day = cellDay,
                    date = CalendarDate(cellYear, cellMonth, cellDay),
                    isCurrentMonth = (cellYear == year && cellMonth == month),
                    isToday = isToday(
                        cellYear, cellMonth, cellDay,
                        todayYear, todayMonth, todayDay,
                    ),
                ),
            )
        }

        return cells
    }

    // endregion

    // region isToday

    /**
     * 判断指定日期是否是今天。
     *
     * @param year 待判断年份
     * @param month 待判断月份 (1-12)
     * @param day 待判断日期 (1-31)
     * @param todayYear 今天的年份
     * @param todayMonth 今天的月份 (1-12)
     * @param todayDay 今天的日期 (1-31)
     * @return `true` 如果指定日期与今天相同
     */
    fun isToday(
        year: Int,
        month: Int,
        day: Int,
        todayYear: Int,
        todayMonth: Int,
        todayDay: Int,
    ): Boolean {
        return year == todayYear && month == todayMonth && day == todayDay
    }

    // endregion

    // region today / timestamp

    /**
     * 获取当前日期（UTC 时区）。
     *
     * 使用 [DateTime.currentTimestamp] 获取当前 UTC 毫秒时间戳，
     * 通过 [todayFromTimestamp] 转换为年月日。
     *
     * **注意**：返回的是 UTC 日期，如需本地时区日期，请使用 [todayFromTimestamp]
     * 并传入本地时区偏移后的时间戳。
     *
     * @return [Triple]`(year, month, day)`，month 为 1-based (1-12)
     */
    fun today(): Triple<Int, Int, Int> {
        return todayFromTimestamp(DateTime.currentTimestamp())
    }

    /**
     * 将毫秒时间戳转换为年月日（UTC 时区）。
     *
     * 基于 Unix Epoch（1970-01-01 UTC）计算天数，
     * 然后使用 civil 日历算法转换为年月日。
     *
     * @param timestampMillis UTC 毫秒时间戳
     * @return [Triple]`(year, month, day)`，month 为 1-based (1-12)
     */
    fun todayFromTimestamp(timestampMillis: Long): Triple<Int, Int, Int> {
        val daysSinceEpoch = floorDiv(timestampMillis, MILLIS_PER_DAY)
        return daysSinceEpochToDate(daysSinceEpoch)
    }

    /**
     * 将日期转换为 UTC 毫秒时间戳（00:00:00.000 UTC）。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     * @return UTC 毫秒时间戳
     */
    fun dateToTimeMillis(year: Int, month: Int, day: Int): Long {
        val days = dateToDaysSinceEpoch(year, month, day)
        return days * MILLIS_PER_DAY
    }

    // endregion

    // region monthGridRows

    /**
     * 获取指定月份网格所需的行数。
     *
     * 根据当月 1 号的星期位置和 [firstDayOfWeek] 计算前置占位天数，
     * 结合当月总天数，确定需要 5 行还是 6 行来完整显示该月。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param firstDayOfWeek 周起始日（0=周日, 1=周一），默认周一
     * @return 5 或 6
     */
    fun monthGridRows(year: Int, month: Int, firstDayOfWeek: Int = 1): Int {
        val firstDow = dayOfWeek(year, month, 1)
        val daysBefore = (firstDow - firstDayOfWeek + 7) % 7
        val daysInCurMonth = daysInMonth(year, month)
        val totalCells = daysBefore + daysInCurMonth
        return if (totalCells <= 35) 5 else 6
    }

    // endregion

    // region internal helpers

    /**
     * 计算上月年月。
     *
     * @param year 当前年份
     * @param month 当前月份 (1-12)
     * @return [Pair]`(year, month)` 上月的年月
     */
    private fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)
    }

    /**
     * 计算下月年月。
     *
     * @param year 当前年份
     * @param month 当前月份 (1-12)
     * @return [Pair]`(year, month)` 下月的年月
     */
    private fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
    }

    /**
     * 将基于 1 的日期偏移解析为实际年月日。
     *
     * 当 [dayOffset] 超出当前月份范围时，自动进位到上月或下月：
     * - `dayOffset <= 0`：上月第 `(daysInPrevMonth + dayOffset)` 天
     * - `dayOffset > daysInMonth`：下月第 `(dayOffset - daysInMonth)` 天
     *
     * @param year 基准年份
     * @param month 基准月份 (1-12)
     * @param dayOffset 基于 1 的日期偏移（可为负数或超出当月天数）
     * @return [Triple]`(year, month, day)`
     */
    private fun resolveDate(year: Int, month: Int, dayOffset: Int): Triple<Int, Int, Int> {
        if (dayOffset < 1) {
            val (pY, pM) = previousMonth(year, month)
            val pDays = daysInMonth(pY, pM)
            return Triple(pY, pM, pDays + dayOffset)
        }
        val daysInCurMonth = daysInMonth(year, month)
        if (dayOffset > daysInCurMonth) {
            val (nY, nM) = nextMonth(year, month)
            return Triple(nY, nM, dayOffset - daysInCurMonth)
        }
        return Triple(year, month, dayOffset)
    }

    /**
     * 将自 Unix Epoch（1970-01-01）起的天数转换为年月日。
     *
     * 基于 Howard Hinnant 的 civil_from_days 算法：
     * https://howardhinnant.github.io/date_algorithms.html#civil_from_days
     *
     * @param daysSinceEpoch 自 1970-01-01 起的天数（可为负数）
     * @return [Triple]`(year, month, day)`，month 为 1-based (1-12)
     */
    private fun daysSinceEpochToDate(daysSinceEpoch: Long): Triple<Int, Int, Int> {
        val z = daysSinceEpoch + 719468
        val era = floorDiv(z, 146097)
        val doe = z - era * 146097 // day of era [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365 // year of era [0, 399]
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100) // day of year [0, 365]
        val mp = (5 * doy + 2) / 153 // month index [0, 11]
        val d = doy - (153 * mp + 2) / 5 + 1 // day [1, 31]
        val m = if (mp < 10) mp + 3 else mp - 9 // month [1, 12]
        val adjustedY = if (m <= 2) y + 1 else y
        return Triple(adjustedY.toInt(), m.toInt(), d.toInt())
    }

    /**
     * 将年月日转换为自 Unix Epoch（1970-01-01）起的天数。
     *
     * 基于 Howard Hinnant 的 days_from_civil 算法：
     * https://howardhinnant.github.io/date_algorithms.html#days_from_civil
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日期 (1-31)
     * @return 自 1970-01-01 起的天数
     */
    private fun dateToDaysSinceEpoch(year: Int, month: Int, day: Int): Long {
        val y = if (month <= 2) (year - 1).toLong() else year.toLong()
        val m = if (month <= 2) (month + 9).toLong() else (month - 3).toLong()
        val era = floorDiv(y, 400)
        val yoe = y - era * 400
        val doy = (153 * m + 2) / 5 + day.toLong() - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /**
     * 整数地板除法，结果向负无穷方向取整。
     *
     * Kotlin 的 `/` 运算符向零取整，此方法确保行为与数学地板除一致。
     *
     * @param a 被除数
     * @param b 除数（必须为正数）
     * @return `a / b` 的地板除结果
     */
    private fun floorDiv(a: Long, b: Long): Long {
        var q = a / b
        if ((a xor b) < 0 && q * b != a) q--
        return q
    }

    // endregion
}

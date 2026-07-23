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

/**
 * 日历国际化配置类
 *
 * 提供星期名称、月份名称等本地化数据。
 * 内置中文、英文、日文、韩文四套语言包，支持通过 BCP47 标签获取。
 *
 * 使用示例：
 * ```
 * Calendar {
 *     attr {
 *         locale = CalendarLocale.english()
 *     }
 * }
 * ```
 *
 * @param monthNames 12 个月份名称（1月-12月）
 * @param weekDayNames 7 个星期全称（从周日开始：Sunday, Monday, ...）
 * @param weekDayShortNames 7 个星期缩写（从周日开始：Su, Mo, ...）
 * @param firstDayOfWeek 周起始日（0=周日, 1=周一）
 * @param yearFormat 年份格式化模板，如 "{year}年" 或 "{year}"
 * @param monthFormat 月份格式化模板，如 "{month}月" 或 "{monthName}"
 * @param headerFormat 标题格式化模板，如 "{year}年{month}月" 或 "{monthName} {year}"
 */
data class CalendarLocale(
    val monthNames: List<String>,
    val weekDayNames: List<String>,
    val weekDayShortNames: List<String>,
    val firstDayOfWeek: Int,
    val yearFormat: String = "{year}",
    val monthFormat: String = "{monthName}",
    val headerFormat: String = "{monthName} {year}"
) {
    companion object {
        private val cache = mutableMapOf<String, CalendarLocale>()

        /** 中文简体 */
        fun chinese(): CalendarLocale {
            return cache.getOrPut("zh-CN") {
                CalendarLocale(
                    monthNames = listOf(
                        "1月", "2月", "3月", "4月", "5月", "6月",
                        "7月", "8月", "9月", "10月", "11月", "12月"
                    ),
                    weekDayNames = listOf(
                        "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"
                    ),
                    weekDayShortNames = listOf(
                        "日", "一", "二", "三", "四", "五", "六"
                    ),
                    firstDayOfWeek = 1,
                    yearFormat = "{year}年",
                    monthFormat = "{month}月",
                    headerFormat = "{year}年{month}月"
                )
            }
        }

        /** 英文 */
        fun english(): CalendarLocale {
            return cache.getOrPut("en-US") {
                CalendarLocale(
                    monthNames = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    ),
                    weekDayNames = listOf(
                        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
                    ),
                    weekDayShortNames = listOf(
                        "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
                    ),
                    firstDayOfWeek = 0,
                    yearFormat = "{year}",
                    monthFormat = "{monthName}",
                    headerFormat = "{monthName} {year}"
                )
            }
        }

        /** 日文 */
        fun japanese(): CalendarLocale {
            return cache.getOrPut("ja-JP") {
                CalendarLocale(
                    monthNames = listOf(
                        "1月", "2月", "3月", "4月", "5月", "6月",
                        "7月", "8月", "9月", "10月", "11月", "12月"
                    ),
                    weekDayNames = listOf(
                        "日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日"
                    ),
                    weekDayShortNames = listOf(
                        "日", "月", "火", "水", "木", "金", "土"
                    ),
                    firstDayOfWeek = 0,
                    yearFormat = "{year}年",
                    monthFormat = "{month}月",
                    headerFormat = "{year}年{month}月"
                )
            }
        }

        /** 韩文 */
        fun korean(): CalendarLocale {
            return cache.getOrPut("ko-KR") {
                CalendarLocale(
                    monthNames = listOf(
                        "1월", "2월", "3월", "4월", "5월", "6월",
                        "7월", "8월", "9월", "10월", "11월", "12월"
                    ),
                    weekDayNames = listOf(
                        "일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"
                    ),
                    weekDayShortNames = listOf(
                        "일", "월", "화", "수", "목", "금", "토"
                    ),
                    firstDayOfWeek = 0,
                    yearFormat = "{year}년",
                    monthFormat = "{month}월",
                    headerFormat = "{year}년 {month}월"
                )
            }
        }

        /**
         * 按 BCP47 语言标签获取国际化配置
         * 支持的标签：zh-CN, en-US, ja-JP, ko-KR
         * 未知标签默认返回英文
         */
        fun of(tag: String): CalendarLocale {
            return when (tag) {
                "zh-CN" -> chinese()
                "en-US" -> english()
                "ja-JP" -> japanese()
                "ko-KR" -> korean()
                else -> english()
            }
        }
    }

    /**
     * 格式化日历头部标题
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 格式化后的标题字符串
     */
    fun formatHeader(year: Int, month: Int): String {
        return headerFormat
            .replace("{year}", year.toString())
            .replace("{month}", month.toString())
            .replace("{monthName}", monthNames[month - 1])
    }

    /**
     * 获取星期短名称（考虑 firstDayOfWeek 偏移）
     * @return 按 firstDayOfWeek 排列的 7 个星期短名称
     */
    fun orderedWeekDayShortNames(): List<String> {
        val names = weekDayShortNames.toMutableList()
        // 将列表按 firstDayOfWeek 旋转
        // firstDayOfWeek=0 表示从周日开始（索引0）
        // firstDayOfWeek=1 表示从周一开始（索引1）
        for (i in 0 until firstDayOfWeek) {
            val first = names.removeAt(0)
            names.add(first)
        }
        return names
    }
}

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
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button

/**
 * 日历Module的用例
 */
@Page("CalendarModuleExamplePage")
internal class CalendarModuleExamplePage : Pager() {
    private var case1TimeMillis : Long by observable(0)
    private var case1Date : CustomDate by observable(CustomDate())

    private var case2TimeMillis : Long by observable(0)
    private var cas2Date : CustomDate by observable(CustomDate())

    private var case3TimeMillisOrign: Long = 0
    private var case3DateOrigin: CustomDate = CustomDate()
    private var case3TimeMillisAfter: Long by observable(0)
    private var case3DateAfter: CustomDate by observable(CustomDate())
    private var case3MonthPlus: Int = 0
    private var case3MinusPlus: Int = 0
    private var case3MilliSecondPlus: Int = 0

    private var case4TimeMillis: Long = 0
    private var case4FormatStr: String = ""
    private var case4FormatedTime: String by observable("")

    private var case5Format: String = ""
    private var case5FormatedTime: String = ""
    private var case5TimeMillis: Long by observable(0)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
                allCenter()
            }
            //case1 设置epochTime，获取对应的日历所有字段
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        text("时间戳：${ctx.case1TimeMillis}" + "\n" + "日历：" + "${ctx.case1Date}")
                        fontSize(12f)
                        marginRight(5f)
                    }
                }
                Button {
                    attr {
                        size(100f, 80f)
                        backgroundColor(Color.BLUE)
                        borderRadius(10f)
                        titleAttr {
                            text("时间戳转日历")
                            fontSize(12f)
                            fontWeightBold()
                            color(Color.WHITE)

                        }
                    }
                    event {
                        click {
                            val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                            var calendar = calendarModule.newCalendarInstance(ctx.case1TimeMillis)
                            ctx.case1Date = CustomDate.getDateFromCalendar(calendar)
                        }
                    }
                }
            }

            //case2 设置日历字段，获取对应时间戳
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        fontSize(12f)
                        marginRight(5f)
                        text("日历：${ctx.cas2Date}" + "\n" + "时间戳：" + "${ctx.case2TimeMillis}")
                    }
                }
                Button {
                    attr {
                        size(100f, 80f)
                        backgroundColor(Color.BLUE)
                        borderRadius(10f)
                        titleAttr {
                            text("日历转时间戳")
                            fontSize(12f)
                            fontWeightBold()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                            val calendar = calendarModule.newCalendarInstance(0)
                            calendar.set(ICalendar.Field.YEAR, ctx.cas2Date.year)
                            calendar.set(ICalendar.Field.MONTH, ctx.cas2Date.month)
                            calendar.set(ICalendar.Field.DAY_OF_MONTH, ctx.cas2Date.dayOfMonth)
                            calendar.set(ICalendar.Field.HOUR_OF_DAY, ctx.cas2Date.hour)
                            calendar.set(ICalendar.Field.MINUS, ctx.cas2Date.min)
                            calendar.set(ICalendar.Field.SECOND, ctx.cas2Date.second)
                            calendar.set(ICalendar.Field.MILLISECOND, ctx.cas2Date.millisecond)
                            ctx.case2TimeMillis = calendar.timeInMillis()
                        }
                    }
                }

            }
            //case3 设置时间戳，修改(add/set)日历字段，获取操作后的日历、时间戳
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        fontSize(12f)
                        marginRight(5f)
                        text("原时间戳：${ctx.case3TimeMillisOrign}\n" + "原日历：${ctx.case3DateOrigin}\n" +
                                "操作：月份增加：${ctx.case3MonthPlus}，分钟增加：${ctx.case3MinusPlus},毫秒增加：${ctx.case3MilliSecondPlus}\n" +
                        "操作后时间戳：${ctx.case3TimeMillisAfter}\n" + "操作后日历：${ctx.case3DateAfter}")
                    }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        justifyContentSpaceBetween()
                        alignItemsCenter()
                    }
                    Button {
                        attr {
                            size(100f, 80f)
                            backgroundColor(Color.BLUE)
                            borderRadius(10f)
                            titleAttr {
                                text("日历add操作")
                                fontSize(12f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                                val calendar = calendarModule.newCalendarInstance(ctx.case3TimeMillisOrign)
                                calendar.add(ICalendar.Field.MONTH, ctx.case3MonthPlus)
                                calendar.add(ICalendar.Field.MINUS, ctx.case3MinusPlus)
                                calendar.add(ICalendar.Field.MILLISECOND, ctx.case3MilliSecondPlus)
                                ctx.case3TimeMillisAfter = calendar.timeInMillis()
                                ctx.case3DateAfter = CustomDate.getDateFromCalendar(calendar)
                            }
                        }
                    }
                    Button {
                        attr {
                            size(100f, 80f)
                            backgroundColor(Color.BLUE)
                            borderRadius(10f)
                            titleAttr {
                                text("set dayOfYear:15")//鸿蒙上设置这个字段无效。原因是：将忽略传递到 mktime() 的 tm_wday 和 tm_yday 的值，并在返回时为其分配正确的值。
                                fontSize(12f)
                                fontWeightBold()
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                                val calendar = calendarModule.newCalendarInstance(ctx.case3TimeMillisAfter)
                                calendar.set(ICalendar.Field.DAY_OF_YEAR, 15)
                                calendar.set(ICalendar.Field.DAY_OF_YEAR, 15)
                                calendar.set(ICalendar.Field.DAY_OF_YEAR, 15)
                                ctx.case3TimeMillisAfter = calendar.timeInMillis()
                                ctx.case3DateAfter = CustomDate.getDateFromCalendar(calendar)
                            }
                        }
                    }
                }
            }

            //case4 给定时间戳，按给定格式显示对应格式化日历
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        fontSize(12f)
                        marginRight(5f)
                        text("待格式化时间戳：${ctx.case4TimeMillis}\nformat：${ctx.case4FormatStr}\n" + "格式化日历：${ctx.case4FormatedTime}")
                    }
                }
                Button {
                    attr {
                        size(100f, 80f)
                        backgroundColor(Color.BLUE)
                        borderRadius(10f)
                        titleAttr {
                            text("格式化时间戳")
                            fontSize(12f)
                            fontWeightBold()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                            val formatedTime = calendarModule.formatTime(ctx.case4TimeMillis, ctx.case4FormatStr)
                            ctx.case4FormatedTime = formatedTime
                        }

                    }
                }
            }
            //case5 输入日历格式和此格式的日历，parse解析对应时间戳
            View {
                attr {
                    flexDirectionRow()
                    justifyContentSpaceBetween()
                    alignItemsCenter()
                }
                Text {
                    attr {
                        fontSize(12f)
                        marginRight(5f)
                        text("格式化时间：${ctx.case5FormatedTime}\n" + "格式：${ctx.case5Format}\n" +
                        "解析的时间戳：${ctx.case5TimeMillis}")
                    }
                }
                Button {
                    attr {
                        size(100f, 80f)
                        backgroundColor(Color.BLUE)
                        borderRadius(10f)
                        titleAttr {
                            text("解析时间戳")
                            fontSize(12f)
                            fontWeightBold()
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            val calendarModule = ctx.acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                            val timeMillis = calendarModule.parseFormattedTime(ctx.case5FormatedTime, ctx.case5Format)
                            ctx.case5TimeMillis = timeMillis
                        }

                    }
                }
            }

        }
    }

    override fun created() {
        super.created()
        case1TimeMillis = 1727742600100 //本地时区对应日期：2024-10-01 08:30:00:100 星期二

        cas2Date.year = 2024 //用例时间戳：1727742600100
        cas2Date.month = 9
        cas2Date.dayOfMonth = 1
        cas2Date.hour = 8
        cas2Date.min = 30
        cas2Date.second = 0
        cas2Date.millisecond = 100

        case3TimeMillisOrign = 1727742600100 //本地时区对应日期：2024-10-01 08:30:00:100 星期二
        val calendarModule = acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
        var calendar = calendarModule.newCalendarInstance(case3TimeMillisOrign)
        case3DateOrigin = CustomDate.getDateFromCalendar(calendar)
        case3MonthPlus = 3
        case3MinusPlus = 35
        case3MilliSecondPlus = 1000
        //预期日期：2025-01-01 09:05:01:100，时间戳预期：1735693501100

        case4TimeMillis = 1735693501100
//        case4FormatStr = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        case4FormatStr = "yyyy-MM-dd'T-Date日期分割''单引号123'''HH'minute':mm[{秒}?]:ss.SSS's'"

//        case5Format = "yyyy-MM-dd HH:mm:ss.SSS"
//        case5FormatedTime = "2025-01-01 09:05:01.100"
        case5Format = "yyyy-MM-dd'T-Date日期分割''单引号123'''HH'minute':mm[{秒}?]:ss.SSS's'"
        case5FormatedTime = "2025-01-01T-Date日期分割'单引号123'09minute:05[{秒}?]:01.100s"
    }

}

internal class CustomDate {
    var year: Int = 0
    var month: Int = 0
    var dayOfMonth: Int = 0
    var dayOfYear: Int = 0
    var dayOfWeek: Int = 0
    var hour: Int = 0
    var min: Int = 0
    var second: Int = 0
    var millisecond: Int = 0

    companion object {
        fun getDateFromCalendar(calendar: ICalendar): CustomDate {
            var newDate = CustomDate()
            newDate.let {
                it.year = calendar.get(ICalendar.Field.YEAR)
                it.month = calendar.get(ICalendar.Field.MONTH)
                it.dayOfMonth = calendar.get(ICalendar.Field.DAY_OF_MONTH)
                it.dayOfYear = calendar.get(ICalendar.Field.DAY_OF_YEAR)
                it.dayOfWeek = calendar.get(ICalendar.Field.DAY_OF_WEEK)
                it.hour = calendar.get(ICalendar.Field.HOUR_OF_DAY)
                it.min = calendar.get(ICalendar.Field.MINUS)
                it.second = calendar.get(ICalendar.Field.SECOND)
                it.millisecond = calendar.get(ICalendar.Field.MILLISECOND)
            }
            return newDate
        }
    }

    override fun toString(): String {
        return "${year}-${month + 1}-${dayOfMonth}/dy${dayOfYear}/dw${dayOfWeek} ${hour}:${min}:${second}:${millisecond}"
    }
}

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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar
import com.tencent.kuikly.core.reactive.handler.observable

class Date(
    var year: Int = 0,
    var month: Int = 0,
    var day: Int = 0
) {
    override fun toString(): String {
        return "${year}-${month}-${day}"
    }
}

internal typealias DatePickerChooseEvent = (pickerDate: DatePickerDate) -> Unit
class DatePickerDate(
    val timeInMillis : Long = 0L,
    val date: Date? = null
)

/*
 * 日期选择器组件
 */
class DatePickerView: ComposeView<DatePickerAttr, DatePickerEvent>() {
    override fun createAttr() = DatePickerAttr()

    override fun createEvent() = DatePickerEvent()

    companion object {
        const val ITEM_HEIGHT = 45f
    }

    var date: Date by observable(Date(2023, 1, 1))
    var chooseDate: Date by observable(Date(2023, 1, 1))

    private fun updateDateRow(date: Date) {
        val ctx = this@DatePickerView
        ctx.date = date
        ctx.chooseDate = ctx.date
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val daysInFebruary = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        val daysInMonth = intArrayOf(31, daysInFebruary, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return daysInMonth[month - 1]
    }

    private fun handleChooseEvent() {
        val ctx = this@DatePickerView
        val calendar = PagerManager.getCurrentPager().acquireModule<CalendarModule>(CalendarModule.MODULE_NAME).newCalendarInstance()
        calendar.set(ICalendar.Field.YEAR, ctx.chooseDate.year)
        calendar.set(ICalendar.Field.MONTH, ctx.chooseDate.month - 1)
        calendar.set(ICalendar.Field.DAY_OF_MONTH, ctx.chooseDate.day)
        val timeInMillis = calendar.timeInMillis()
        ctx.event.chooseEvent?.let {
            it(DatePickerDate(timeInMillis = timeInMillis, date = ctx.chooseDate))
        }
    }

    private fun gradientMaskView(reverse: Boolean, topMargin: Float): ViewBuilder {
        return {
            View {
                attr {
                    marginTop(topMargin)
                    if (reverse) {
                        transform(rotate = Rotate(180f))
                    }
                }
                View {
                    attr {
                        height(2 * ITEM_HEIGHT)
                        backgroundLinearGradient(
                            Direction.TO_BOTTOM,
                            ColorStop(Color.WHITE, 0f),
                            ColorStop(Color(0x00FFFFFF), 1f)
                        )
                    }
                }
                View {
                    attr {
                        height(0.5f)
                        marginLeft(16f)
                        marginRight(16f)
                        backgroundColor(Color(0xFFE6E6E6))
                    }
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        val ctx  = this@DatePickerView
        val calendar = PagerManager.getCurrentPager().acquireModule<CalendarModule>(CalendarModule.MODULE_NAME).newCalendarInstance()
        val nowYear = calendar.get(ICalendar.Field.YEAR)
        if (nowYear < 1970) {
            return {}
        }
        return {
            View {
                attr {
                    size(ctx.attr.width(), ctx.attr.height())
                    flexDirectionRow()
                    justifyContentSpaceAround()
                    alignItemsCenter()
                }
                val dataList = arrayListOf<String>()
                for (i in nowYear downTo 1970) {
                    dataList.add(i.toString())
                }
                val yearListForShow = arrayListOf<String>()
                yearListForShow.addAll(dataList.map { "${it}年" })
                ScrollPicker(
                    yearListForShow.toTypedArray()
                ){
                    attr {
                        itemWidth = ctx.attr.width() / 3f
                        itemHeight = ITEM_HEIGHT
                        countPerScreen = 5
                    }
                    event {
                        dragEndEvent { centerValue, centerItemIndex ->
                            ctx.updateDateRow(Date(dataList[centerItemIndex].toInt(), ctx.date.month, ctx.date.day))
                            ctx.handleChooseEvent()
                        }
                    }
                }
                val monthList = arrayListOf<Int>(1,2,3,4,5,6,7,8,9,10,11,12)
                val monthListForShow = arrayListOf<String>()
                monthListForShow.addAll(monthList.map { "${it}月" })
                ScrollPicker(
                    monthListForShow.toTypedArray()
                ){
                    attr {
                        itemWidth = ctx.attr.width() / 3f
                        itemHeight = ITEM_HEIGHT
                        countPerScreen = 5
                    }
                    event {
                        dragEndEvent { centerValue, centerItemIndex ->
                            ctx.updateDateRow(Date(ctx.date.year, monthList[centerItemIndex], ctx.date.day))
                            ctx.handleChooseEvent()
                        }
                    }
                }
                vbind({ctx.date}) {
                    val daysList = ArrayList<String>()
                    for (i in 1..ctx.getDaysInMonth(ctx.date.year, ctx.date.month)) {
                        daysList.add(i.toString())
                    }
                    val dayListForShow = arrayListOf<String>()
                    dayListForShow.addAll(daysList.map { "${it}日" })
                    ScrollPicker(
                        dayListForShow.toTypedArray()
                    ){
                        attr {
                            itemWidth = ctx.attr.width() / 3f
                            itemHeight = ITEM_HEIGHT
                            countPerScreen = 5
                        }
                        event {
                            dragEndEvent { centerValue, centerItemIndex ->
                                ctx.chooseDate = Date(ctx.date.year, ctx.date.month, daysList[centerItemIndex].toInt())
                                ctx.handleChooseEvent()
                            }
                        }
                    }
                }
                View {
                    attr {
                        width(ctx.attr.width())
                        touchEnable(false)
                        absolutePositionAllZero()
                    }
                    apply(ctx.gradientMaskView(false, 0f))
                    apply(ctx.gradientMaskView(true, ITEM_HEIGHT))
                }
            }
        }
    }

}

class DatePickerAttr: ComposeAttr() {
    fun width(): Float = flexNode?.styleWidth ?: 0.0f
    fun height(): Float = flexNode?.styleHeight ?: 0.0f
}

class DatePickerEvent : ComposeEvent() {
    var chooseEvent: DatePickerChooseEvent? = null

    /**
     * 设置日期选择器的选择事件。
     * @param event 一个 DatePickerChooseEvent，当用户选择日期时触发。
     */
    fun chooseEvent(event: DatePickerChooseEvent) {
        chooseEvent = event
    }
}
/*
 * 日期选择器组件
 */
fun ViewContainer<*, *>.DatePicker(init: DatePickerView.() -> Unit) {
    addChild(DatePickerView(), init)
}
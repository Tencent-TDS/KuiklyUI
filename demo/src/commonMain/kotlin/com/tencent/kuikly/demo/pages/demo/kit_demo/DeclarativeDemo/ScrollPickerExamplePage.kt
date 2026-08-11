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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ScrollPickerExamplePage")
internal class ScrollPickerExamplePage: BasePager() {

    private var chooseIdx: Int by observable(0)
    private var chooseValue: String by observable("")

    private var hourStr: String by  observable("")
    private var minuteStr: String by  observable("")

    private var provinceIndex: Int by  observable(0)
    private var provinceName: String by  observable("")
    private var cityStr: String by  observable("")

    private var date: Date by observable(Date(0, 0, 0))
    private var dateTimestamp: Long by observable(0L)
    private var initialDate: Date by observable(Date(2025, 1, 22))

    override fun body(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        return {
            attr {
                flexDirectionColumn()
                justifyContentFlexStart()
                alignItemsCenter()
                backgroundColor(Color(0xFFB0C4DE))
            }
            NavBar {
                attr {
                    width(pagerData.pageViewWidth)
                    title = "ScrollPickerExamplePage"
                }
            }
            Scroller {
                attr {
                    flex(1f)
                    width(pagerData.pageViewWidth)
                }
                View {
                    attr {
                        flexDirectionColumn()
                        alignItemsCenter()
                        width(pagerData.pageViewWidth)
                    }
                    View {
                        attr {
                            flexDirectionRow()
                        }
                        apply(ctx.singlePickerDemo())
                        apply(ctx.multiPickerDemo())
                    }
                    apply(ctx.cascadePickerDemo())
                    apply(ctx.datePickerDemo())
                }
            }
        }
    }
    private fun singlePickerDemo(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        return {
            View {
                attr {
                    marginTop(16f)
                    flexDirectionColumn()
                    allCenter()
                }
                View {
                    attr {
                        flexDirectionRow()
                        allCenter()
                    }
                    Text {
                        attr {
                            text("🤣👉 ")
                        }
                    }
                    ScrollPicker(arrayOf("A","B","C","D","E","F")) {
                        attr {
                            borderRadius(8f)
                            itemWidth = 100f
                            itemHeight = 30f
                            countPerScreen = 3
                            itemBackGroundColor = Color.WHITE
                            itemTextColor = Color.BLACK
                            backgroundColor(Color.WHITE)
                        }
                        event {
                            scrollEndEvent { centerValue, centerItemIndex ->
                                ctx.chooseIdx = centerItemIndex
                                ctx.chooseValue = centerValue
                            }
                        }
                    }
                    Text {
                        attr {
                            text(" 👈🤣")
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        text("当前选中index:${ctx.chooseIdx}, value:${ctx.chooseValue}")
                    }
                }
            }
        }
    }

    private fun multiPickerDemo(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        return {
            View {
                attr {
                    marginTop(16f)
                    flexDirectionColumn()
                    allCenter()
                }
                View {
                    attr {
                        flexDirectionRow()
                        allCenter()
                        borderRadius(8f)
                    }
                    val hours = arrayOf(1,2,3,4,5,6,7,8,9,10,11,12)
                    ScrollPicker(hours.map { "${it}时" }.toTypedArray()) {
                        attr {
                            itemWidth = 100f
                            itemHeight = 45f
                            countPerScreen = 5
                            itemBackGroundColor = Color.WHITE
                            itemTextColor = Color.BLACK
                            backgroundColor(Color.WHITE)
                        }
                        event {
                            scrollEndEvent { centerValue, centerItemIndex ->
                                ctx.hourStr = centerValue
                            }
                        }
                    }
                    val minutes = arrayOf(5,10,15,20,25)
                    ScrollPicker(minutes.map { "${it}分" }.toTypedArray()) {
                        attr {
                            itemWidth = 100f
                            itemHeight = 45f
                            countPerScreen = 5
                            itemBackGroundColor = Color.WHITE
                            itemTextColor = Color.GRAY
                            backgroundColor(Color.WHITE)
                        }
                        event {
                            scrollEndEvent { centerValue, centerItemIndex ->
                                ctx.minuteStr = centerValue
                            }
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(8f)
                        text("现在是：${ctx.hourStr}${ctx.minuteStr}")
                    }
                }
            }
        }
    }

    private fun cascadePickerDemo(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        val citys = arrayOf(
            arrayOf("长沙市","衡阳市","张家界市","常德市","益阳市","岳阳市","株洲市","湘潭市","郴州市","永州市","邵阳市","怀化市","娄底市"),
            arrayOf("广州市","深圳市","清远市","韶关市","河源市","梅州市","潮州市","汕头市","揭阳市","汕尾市","惠州市","东莞市","珠海市","中山市","江门市","佛山市","肇庆市","云浮市","阳江市","茂名市","湛江市"),
            arrayOf("延安市","铜川市","渭南市","咸阳市","宝鸡市","汉中市","榆林市","商洛市","安康市"),
            arrayOf("昆明市","曲靖市","玉溪市","丽江市","昭通市","普洱市","临沧市","保山市"),
            arrayOf("海口市","三亚市","三沙市","儋州市"),
            arrayOf("郑州市","开封市","洛阳市","平顶山市","安阳市","鹤壁市","新乡市","焦作市","濮阳市","许昌市","漯河市","三门峡市","南阳市","商丘市","周口市","驻马店市","信阳市"),
        )
        return {
            View {
                attr {
                    flexDirectionColumn()
                    allCenter()
                    marginTop(16f)
                }
                Text {
                    attr {
                        text("👇当选项间存在联动关系时👇")
                    }
                }
                View {
                    attr {
                        flexDirectionRow()
                        allCenter()
                        borderRadius(8f)
                    }
                    val provinces = arrayOf("湖南省","广东省","陕西省","云南省","海南省","河南省")
                    ScrollPicker(provinces) {
                        attr {
                            itemWidth = 100f
                            itemHeight = 45f
                            countPerScreen = 5
                            itemBackGroundColor = Color.WHITE
                            itemTextColor = Color.BLACK
                            backgroundColor(Color.WHITE)
                        }
                        event {
                            scrollEndEvent { centerValue, centerItemIndex ->
                                ctx.provinceIndex = centerItemIndex
                                ctx.provinceName = centerValue
                            }
                        }
                    }
                    vbind({ctx.provinceIndex}) {
                        ScrollPicker(citys[ctx.provinceIndex]) {
                            attr {
                                itemWidth = 100f
                                itemHeight = 45f
                                countPerScreen = 5
                                itemBackGroundColor = Color.WHITE
                                itemTextColor = Color.GRAY
                                backgroundColor(Color.WHITE)
                            }
                            event {
                                scrollEndEvent { centerValue, centerItemIndex ->
                                    ctx.cityStr = centerValue
                                }
                            }
                        }
                    }
                }
                Text {
                    attr {
                        marginTop(3f)
                        text("我在：${ctx.provinceName}${ctx.cityStr}")
                    }
                }
            }
        }
    }

    private fun datePickerDemo(): ViewBuilder {
        val ctx = this@ScrollPickerExamplePage
        return {
            View {
                attr {
                    flexDirectionColumn()
                    justifyContentFlexStart()
                    alignItemsCenter()
                    width(pagerData.pageViewWidth)
                    marginTop(3f)
                    marginBottom(24f)
                }
                Text {
                    attr {
                        text("选中日期: ${ctx.date}, 时间戳: ${ctx.dateTimestamp}")
                    }
                }
                apply(ctx.dateQuickButtons { ctx.initialDate = it })
                Text {
                    attr {
                        text("初始日期: ${ctx.initialDate}")
                        fontSize(12f)
                        color(Color.GRAY)
                        marginBottom(4f)
                    }
                }
                vbind({ ctx.initialDate }) {
                    DatePicker {
                        attr {
                            width(300f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                            initialDate(ctx.initialDate)
                            initialScrollAnimated = true
                        }
                        event {
                            chooseEvent {
                                it.date?.let { date ->
                                    ctx.date = date
                                }
                                ctx.dateTimestamp = it.timeInMillis
                            }
                        }
                    }
                }
            }
        }
    }

    private fun dateQuickButtons(onSelect: (Date) -> Unit): ViewBuilder {
        return {
            View {
                attr {
                    flexDirectionRow()
                    justifyContentCenter()
                    marginTop(8f)
                    marginBottom(8f)
                }
                Text {
                    attr {
                        text("今天")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2025, 1, 22)) }
                    }
                }
                Text {
                    attr {
                        text("近一月")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2024, 12, 22)) }
                    }
                }
                Text {
                    attr {
                        text("近三月")
                        fontSize(14f)
                        color(Color.WHITE)
                        backgroundColor(Color(0xFF4A90E2))
                        borderRadius(4f)
                        margin(8f, 12f)
                    }
                    event {
                        click { onSelect(Date(2024, 10, 22)) }
                    }
                }
            }
        }
    }

}
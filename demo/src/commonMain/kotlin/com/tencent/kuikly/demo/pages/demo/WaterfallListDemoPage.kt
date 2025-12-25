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
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

internal class WaterFallItem(scope: PagerScope) : BaseObject() {
    var title: String by scope.observable("")
    var bgColor: Color by scope.observable(Color.WHITE)
    var height: Float by scope.observable(0f)
}

@Page("WaterfallListDemoPage")
internal class WaterfallListDemoPage : BasePager() {
    var dataList: ObservableList<WaterFallItem> by observableList<WaterFallItem>()
    var columnCount by observable(2)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "瀑布流组件Demo"
                }
            }

            WaterfallList {
                attr {
                    flex(1f)
                    // columnCount((pagerData.pageViewWidth / 180f).toInt())
                    columnCount(ctx.columnCount)
                    listWidth(pagerData.pageViewWidth)
                    lineSpacing(10f)
                    itemSpacing(10f)
                }

                Refresh {
                    attr {
                        height(50f)
                        width(pagerData.pageViewWidth)
                        backgroundColor(Color.RED)
                    }
                }

                Hover {
                    attr {
                        absolutePosition(top = 0f, left =0f, right =0f)
                        height(40f)
//                        width(pagerData.pageViewWidth)
                        backgroundLinearGradient(
                            Direction.TO_LEFT,
                            ColorStop(Color.RED, 0f),
                            ColorStop(Color.YELLOW, 1f)
                        )
                    }
                    Text {
                        attr {
                            alignSelfCenter()
                            fontSize(30f)
                            text("Hover View")
                        }
                    }
                }

                vforIndex({ ctx.dataList }) { item, index, _ ->
                    View {
                        attr {
                            allCenter()
                            height(if (index == 0) item.height / 3 else item.height)
                            backgroundColor(item.bgColor)
                            borderRadius(8f)
                            if (index == 0) {
                                width(pagerData.pageViewWidth)
                            }
                        }

                        Text {
                            attr {
                                text(item.title)
                                color(Color.WHITE)
                            }
                        }

                        if (index == 0) {
                            Text {
                                attr {
                                    absolutePosition(top = 5f, right = 5f)
                                    backgroundColor(Color.WHITE)
                                    borderRadius(5f)
                                    text("加一列")
                                }
                                event {
                                    click {
                                        ctx.columnCount++
                                    }
                                }
                            }
                            Text {
                                attr {
                                    absolutePosition(top = 30f, right = 5f)
                                    backgroundColor(Color.WHITE)
                                    borderRadius(5f)
                                    text("减一列")
                                }
                                event {
                                    click {
                                        ctx.columnCount--
                                    }
                                }
                            }
                        }

                        event {
                            click {
                                item.height = (150..300).random().toFloat()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        for (index in 0..1000) {
            dataList.add(WaterFallItem(this).apply {
                title = "我是第${this@WaterfallListDemoPage.dataList.size + 1}个卡片"
                height = (200..500).random().toFloat()
                bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
            })
        }

        setTimeout(30) {
            for (index in 0..1000) {
                dataList.add(WaterFallItem(this).apply {
                    title = "我是第${this@WaterfallListDemoPage.dataList.size + 1}个卡片"
                    height = (200..500).random().toFloat()
                    bgColor = Color((0..255).random(), (0..255).random(), (0..255).random(), 1.0f)
                })
            }
        }
    }
}
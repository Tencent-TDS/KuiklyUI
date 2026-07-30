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

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Hover
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ScrollerView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ScrollViewPage")
internal class ScrollViewPage : BasePager() {

    companion object {
        private const val TAG = "ScrollViewPage"
    }

    private val itemList by observableList<ExampleListItemData>()

    private var listViewRef: ViewRef<ListView<*, *>>? = null
    private var scrollViewRef: ViewRef<ScrollerView<*, *>>? = null

    // Y轴偏移
    private var listViewOffsetY by observable(0f)
    private var scrollViewOffsetY by observable(0f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "list Example"
                }
            }

            // List测试
            Text {
                attr {
                    text("List offsetY=${ctx.listViewOffsetY}")
                }
            }
            List {
                ref {
                    ctx.listViewRef = it
                }
                attr {
                    size(pagerData.pageViewWidth, 300f)
                }
                vforIndex({ ctx.itemList }) { item, index, _ ->
                    if (index == 5) {
                        Hover {
                            attr {
                                height(40f)
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
                    } else {
                        ctx.listDemoItem(item, index).invoke(this)
                    }
                }
                event {
                    scroll { param ->
                        ctx.listViewOffsetY = param.offsetY
                        KLog.i(TAG, "List scroll:${param.offsetY}")
                    }
                }
            }


            // Scroller测试
            Text {
                attr {
                    marginTop(30f)
                    text("Scroller offsetY=${ctx.scrollViewOffsetY}")
                }
            }
            Scroller {
                ref {
                    ctx.scrollViewRef = it
                }
                attr {
                    size(pagerData.pageViewWidth, 300f)
                }
                vforIndex({ ctx.itemList }) { item, index, _ ->
                    if (index == 5) {
                        Hover {
                            attr {
                                height(40f)
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
                    } else {
                        ctx.listDemoItem(item, index).invoke(this)
                    }
                }
                event {
                    scroll { param ->
                        ctx.scrollViewOffsetY = param.offsetY
                        KLog.i(TAG, "Scroller scroll:${param.offsetY}")
                    }
                }
            }
        }
    }

    private fun listDemoItem(item: ExampleListItemData, index: Int): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    width(pagerData.pageViewWidth)
                    height(50f)
                    backgroundColor(item.bgColor)
                }
                Text {
                    attr {
                        alignSelfCenter()
                        fontSize(30f)
                        text(index.toString())
                    }
                }
                event {
                    longPress {
                        ctx.itemList.removeLast()
                        ctx.listViewOffsetY = ctx.listViewRef?.view?.curOffsetY ?: 0f
                        ctx.scrollViewOffsetY = ctx.scrollViewRef?.view?.curOffsetY ?: 0f
                        KLog.i(
                            TAG,
                            "移除最后一项 list偏移:${ctx.listViewOffsetY} scroller偏移:${ctx.scrollViewOffsetY}"
                        )
                    }
                }
            }
        }
    }

    override fun created() {
        repeat(5) {
            itemList.add(ExampleListItemData().apply {
                bgColor = Color.BLUE
            })

            itemList.add(ExampleListItemData().apply {
                bgColor = Color.RED
            })

            itemList.add(ExampleListItemData().apply {
                bgColor = Color.GREEN
            })
            itemList.add(ExampleListItemData().apply {
                bgColor = Color.YELLOW
            })
        }
    }
}

internal class ExampleListItemData {
    var bgColor = Color.WHITE
}
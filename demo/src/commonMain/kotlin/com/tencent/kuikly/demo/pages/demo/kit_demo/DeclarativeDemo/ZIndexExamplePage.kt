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
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max
import kotlin.random.Random

internal class ZIndexViewData {
    var zIndex by observable(0)
    var color by observable(Color(0xFF000000))
    var boxShadow by observable(BoxShadow(offsetX = 0f, offsetY = 0f, shadowColor = Color(0x00000000), shadowRadius = 0f))
    var position by observable(Pair<Float, Float>(0f, 0f))
}

internal class ZIndexViewAttr: ComposeAttr() {
    lateinit var zIndexData: ZIndexViewData
}

internal class ZIndexView: ComposeView<ZIndexViewAttr, ComposeEvent>() {
    override fun createAttr(): ZIndexViewAttr {
        return ZIndexViewAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                absolutePosition(
                    left = ctx.attr.zIndexData.position.first,
                    top = ctx.attr.zIndexData.position.second
                )
                size(width = 120f, height = 120f)
                backgroundColor(ctx.attr.zIndexData.color)
                boxShadow(ctx.attr.zIndexData.boxShadow)
                zIndex(ctx.attr.zIndexData.zIndex, false)
            }
            Text { attr { text("zIndex=${ctx.attr.zIndexData.zIndex}") } }
        }
    }
}

internal fun ViewContainer<*, *>.ZIndexView(init: ZIndexView.() -> Unit) {
    addChild(ZIndexView(), init)
}

@Page("ZIndexExamplePage")
internal class ZIndexExamplePage: BasePager() {
    private var randomHelper = Random.Default
    private var dataArray: ObservableList<ZIndexViewData> by observableList<ZIndexViewData>()
    private var shadowList: Array<BoxShadow> = arrayOf(
        BoxShadow(offsetX = 0f, offsetY = 0f, shadowColor = Color(0x00000000), shadowRadius = 0f),
        BoxShadow(offsetX = 5f, offsetY = 5f, shadowColor = Color(0xFF000000), shadowRadius = 5f),
        BoxShadow(offsetX = 10f, offsetY = 10f, shadowColor = Color(0xCC000000), shadowRadius = 10f),
        BoxShadow(offsetX = 15f, offsetY = 15f, shadowColor = Color(0xAA000000), shadowRadius = 15f),
        BoxShadow(offsetX = 20f, offsetY = 20f, shadowColor = Color(0x7F000000), shadowRadius = 20f),
    )
    private var shadowIndex = 0
    private lateinit var zIndexContainer: DivView

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar { attr { title = "View ZIndex & Flex Example" } }
            View {
                attr { flex(1f) }
                View {
                    attr {
                        flex(1f)
                        margin(all = 20f)
                        border(Border(lineWidth = 0.5f, lineStyle = BorderStyle.SOLID, color = Color.BLACK))
                    }
                    vfor({ctx.dataArray}) { itemData ->
                        ZIndexView {
                            attr {
                                zIndexData = itemData
                            }
                        }
                    }
                    ctx.zIndexContainer = this
                }
                View {
                    attr {
                        flexDirectionRow()
                        margin(top = 80f, bottom = 80f)
                        justifyContentSpaceAround()
                        alignItemsCenter()
                    }
                    Button {
                        attr {
                            size(width = 80f, height = 40f)
                            borderRadius(20f)
                            backgroundColor(0xFF99AAFF)
                            titleAttr { text("addView") }
                        }
                        event {
                            click {
                                ctx.addRandomZIndexViewData()
                            }
                        }
                    }
                    Button {
                        attr {
                            size(width = 80f, height = 40f)
                            borderRadius(20f)
                            backgroundColor(0xFF99AAFF)
                            titleAttr { text("boxShadow") }
                        }
                        event {
                            click {
                                ctx.shadowIndex = (ctx.shadowIndex + 1) % ctx.shadowList.size
                                for (data in ctx.dataArray) {
                                    data.boxShadow = ctx.shadowList[ctx.shadowIndex]
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        addRandomZIndexViewData()
    }

    fun addRandomZIndexViewData() {
        var data = ZIndexViewData()
        data.boxShadow = shadowList[shadowIndex]
        data.color = randomColor()
        data.position = randomPosition()
        data.zIndex = dataArray.size + 1
        dataArray.add(0, data)
    }

    private fun randomColor(): Color {
        var rgbValue : Long = 0xE9000000L
        var totalRGBValue = randomHelper.nextInt() % 64 + 192
        var r = randomHelper.nextInt(499999, 999999)
        var g = randomHelper.nextInt(499999, 999999)
        var b = randomHelper.nextInt(499999, 999999)
        var totalRGB = max(max(r, g), b)
        r = r * totalRGBValue / totalRGB
        g = g * totalRGBValue / totalRGB
        b = b * totalRGBValue / totalRGB
        rgbValue += (r * 0x00010000L)
        rgbValue += (g * 0x00000100L)
        rgbValue += (b * 0x00000001L)
        return Color(rgbValue)
    }

    private fun randomPosition(): Pair<Float, Float> {
        var x = randomHelper.nextInt(0, 999999) % (zIndexContainer.flexNode.layoutWidth - 120f).toInt()
        var y = randomHelper.nextInt(0, 999999) % (zIndexContainer.flexNode.layoutHeight - 120f).toInt()
        return Pair(x.toFloat(), y.toFloat())
    }
}

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

package com.tencent.kuikly.demo.pages.demo.catalog

import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import kotlin.math.min
import kotlin.random.Random

private var randomHelper = Random.Default

internal class ExampleItemView : ComposeView<ExampleItemAttr, ComposeEvent>() {
    private val mainColor = randomColor()
    private val avatarLightColor = lightColorFrom(mainColor)
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionRow()
            }
            // avatar area
            View {
                attr {
                    size(width = 52f, height = 52f)
                    margin(all = 16f)
                    backgroundColor(ctx.avatarLightColor)
                    border(
                        Border(
                            lineWidth = 2f,
                            lineStyle = BorderStyle.SOLID,
                            color = ctx.mainColor
                        )
                    )
                    borderRadius(26f)
                    allCenter()
                }
                Text {
                    attr {
                        fontSize(22f)
                        fontWeight700()
                        color(ctx.mainColor)
                        text(ctx.attr.itemData.avatarText)
                    }
                }
            }

            //title / subtitle area
            View {
                attr {
                    flex(1f)
                    margin(top = 18f, bottom = 18f)
                    height(52f)
                    flexDirectionColumn()
                }
                Text {
                    attr {
                        text(ctx.attr.itemData.titleText)
                        fontSize(14f)
                        color(0xFF0000000)
                    }
                }
                Text {
                    attr {
                        marginTop(2f)
                        text(ctx.attr.itemData.subtitleText)
                        fontSize(12f)
                        color(0xFF9F9F9F)
                        lines(2)
                    }
                }

            }

            // jump area
            View {
                attr {
                    justifyContentCenter()
                    alignItemsCenter()
                    alignSelfCenter()
                    marginRight(10f)
                    marginLeft(10f)
                }
                vif({ctx.attr.itemData.declarativeExampleUrl.isNotEmpty()}) {
                    Button {
                        attr {
                            padding(left = 6f, top = 6f, right = 6f, bottom = 6f)
                            borderRadius(allBorderRadius = 4f)
                            backgroundColor(ctx.mainColor)
                            titleAttr {
                                text("跳转Demo")
                                fontSize(15f)
                                color(ctx.avatarLightColor)
                            }
                        }
                        event {
                            click {
                                getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(ctx.attr.itemData.declarativeExampleUrl)
                            }
                        }
                    }
                }
            }

        }
    }
    override fun createAttr() = ExampleItemAttr()
    override fun createEvent() = ComposeEvent()

    private fun randomColor(): Color {
        var rgbValue: Long = 0xFF000000
        val totalRGBValue = randomHelper.nextInt() % 128 + 384
        var r = randomHelper.nextInt(0, 999999)
        var g = randomHelper.nextInt(0, 999999)
        var b = randomHelper.nextInt(0, 999999)
        if (r <= g && r <= b) {
            r = r * 2 / 3
        } else if (g <= r && g <= b) {
            g = g * 2 / 3
        } else if (b <= r && b <= g) {
            b = b * 2 / 3
        }
        val totalRandomValue = r + g + b
        r = min(r * totalRGBValue / totalRandomValue, 0xFF)
        g = min(g * totalRGBValue / totalRandomValue, 0xFF)
        b = min(b * totalRGBValue / totalRandomValue, 0xFF)
        rgbValue += (r * 0x00010000L)
        rgbValue += (g * 0x00000100L)
        rgbValue += (b * 0x00000001L)
        return Color(rgbValue)
    }

    private fun lightColorFrom(color: Color): Color {
        val colorValue: Long = color.toString().toLong()
        var lightColorValue: Long = 0xFF000000
        lightColorValue += (((colorValue.inv() and 0x00FF0000) shr 19) shl 16).inv() and 0x00FF0000
        lightColorValue += (((colorValue.inv() and 0x0000FF00) shr 11) shl 8).inv() and 0x0000FF00
        lightColorValue += (((colorValue.inv() and 0x000000FF) shr 3) shl 0).inv() and 0x000000FF
        return Color(lightColorValue)
    }

}

internal fun ViewContainer<*, *>.ExampleItem(init: ExampleItemView.() -> Unit) {
    addChild(ExampleItemView(), init)
}

internal class ExampleItemAttr : ComposeAttr() {
    lateinit var itemData: ExampleItemData
}
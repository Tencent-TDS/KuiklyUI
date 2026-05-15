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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ColorMatrix
import com.tencent.kuikly.core.base.attr.ColorMatrixConfig
import com.tencent.kuikly.core.base.attr.toColorMatrix
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("ColorMatrixDemo")
internal class ColorMatrixDemoPage : BasePager() {

    private var currentMatrix by observable<ColorMatrix?>(null)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }

            Scroller {
                attr {
                    flex(1f)
                    flexDirectionColumn()
                }

                Text {
                    attr {
                        marginTop(50f)
                        marginLeft(16f)
                        fontSize(18f)
                        color(Color.BLACK)
                        text("ColorMatrix 滤镜示例")
                    }
                }

                Text {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        fontSize(12f)
                        color(Color(0xFF999999))
                        text("平台支持：Android、HarmonyOS（iOS 暂不支持）")
                    }
                }

                // 原图
                Text {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        fontSize(14f)
                        color(Color(0xFF666666))
                        text("当前效果：")
                    }
                }

                Image {
                    attr {
                        marginTop(10f)
                        marginLeft(16f)
                        size(300f, 200f)
                        resizeCover()
                        src("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
                        colorFilter(ctx.currentMatrix)
                    }
                }

                // 按钮区域
                View {
                    attr {
                        marginTop(20f)
                        marginLeft(16f)
                        flexDirectionRow()
                        flexWrapWrap()
                    }

                    // 原图按钮
                    filterButton("原图") { ctx.currentMatrix = null }
                    // 灰度
                    filterButton("灰度") { ctx.currentMatrix = ColorMatrix.rec709Gray }
                    // 高饱和
                    filterButton("高饱和") {
                        ctx.currentMatrix = ColorMatrixConfig(saturation = 2f).toColorMatrix()
                    }
                    // 低对比度
                    filterButton("低对比") {
                        ctx.currentMatrix = ColorMatrixConfig(contrast = 0.5f).toColorMatrix()
                    }
                    // 高亮度
                    filterButton("高亮") {
                        ctx.currentMatrix = ColorMatrixConfig(brightness = 50f).toColorMatrix()
                    }
                    // 半透明
                    filterButton("半透明") {
                        ctx.currentMatrix = ColorMatrixConfig(alpha = 0.5f).toColorMatrix()
                    }
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.filterButton(label: String, onClick: () -> Unit) {
    View {
        attr {
            marginRight(10f)
            marginBottom(10f)
            paddingLeft(12f)
            paddingRight(12f)
            paddingTop(8f)
            paddingBottom(8f)
            backgroundColor(Color(0xFF1890FF))
            borderRadius(4f)
        }
        event {
            click { onClick() }
        }
        Text {
            attr {
                fontSize(14f)
                color(Color.WHITE)
                text(label)
            }
        }
    }
}
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
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.PathApi
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * ClipPath 测试页面
 *
 * 测试目的：
 * 全面测试 clipPath、border、borderRadius 以及其他使用 Layer 的属性之间的兼容性和正确性。
 *
 * 测试用例：
 * 1. 单独使用 clipPath（星形、心形、多边形）
 * 2. 单独使用 borderRadius
 * 3. 单独使用 border
 * 4. clipPath + border（边框应沿着 clipPath 绘制）
 * 5. borderRadius + border（边框应沿着圆角绘制）
 * 6. clipPath + borderRadius（clipPath 优先）
 * 7. clipPath + borderRadius + border（clipPath 优先，边框沿 clipPath）
 * 8. clipPath + boxShadow（不冲突）
 * 9. clipPath + backgroundImage（不冲突）
 * 10. 综合测试
 */
@Page("ClipPathTestPage")
internal class ClipPathTestPage : BasePager() {

    companion object {
        /**
         * 绘制星形路径
         */
        private fun drawStar(path: PathApi, width: Float, height: Float, points: Int = 5) {
            val size = min(width, height)
            val centerX = width / 2
            val centerY = height / 2
            val outerRadius = size / 2
            val innerRadius = outerRadius * 0.4f
            val anglePerPoint = (2 * PI / points).toFloat()

            path.beginPath()
            path.moveTo(
                centerX + outerRadius * cos(-PI.toFloat() / 2),
                centerY + outerRadius * sin(-PI.toFloat() / 2)
            )
            for (i in 1 until points * 2) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = anglePerPoint * i / 2 - PI.toFloat() / 2
                path.lineTo(
                    centerX + radius * cos(angle),
                    centerY + radius * sin(angle)
                )
            }
            path.closePath()
        }

        /**
         * 绘制心形路径（使用贝塞尔曲线）
         */
        private fun drawHeart(path: PathApi, width: Float, height: Float) {
            val scale = min(width, height) / 100f
            val offsetX = (width - 100f * scale) / 2
            val offsetY = (height - 100f * scale) / 2 + 5f * scale

            path.beginPath()
            path.moveTo(offsetX + 50f * scale, offsetY + 90f * scale)
            // 左半边
            path.quadraticCurveTo(
                offsetX + 0f * scale, offsetY + 60f * scale,
                offsetX + 10f * scale, offsetY + 30f * scale
            )
            path.quadraticCurveTo(
                offsetX + 20f * scale, offsetY + 0f * scale,
                offsetX + 50f * scale, offsetY + 20f * scale
            )
            // 右半边
            path.quadraticCurveTo(
                offsetX + 80f * scale, offsetY + 0f * scale,
                offsetX + 90f * scale, offsetY + 30f * scale
            )
            path.quadraticCurveTo(
                offsetX + 100f * scale, offsetY + 60f * scale,
                offsetX + 50f * scale, offsetY + 90f * scale
            )
            path.closePath()
        }

        /**
         * 绘制正多边形路径
         */
        private fun drawPolygon(path: PathApi, width: Float, height: Float, sides: Int) {
            val size = min(width, height)
            val centerX = width / 2
            val centerY = height / 2
            val radius = size / 2

            path.beginPath()
            for (i in 0 until sides) {
                val angle = (2 * PI * i / sides - PI / 2).toFloat()
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.closePath()
        }

        /**
         * 绘制圆形路径（使用 arc）
         */
        private fun drawCircle(path: PathApi, width: Float, height: Float) {
            val size = min(width, height)
            val centerX = width / 2
            val centerY = height / 2
            val radius = size / 2

            path.beginPath()
            path.arc(centerX, centerY, radius, 0f, (2 * PI).toFloat(), false)
            path.closePath()
        }
    }

    override fun body(): ViewBuilder {
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            // 导航栏
            NavBar {
                attr {
                    title = "ClipPath 兼容性测试"
                }
            }

            // 使用 Scroller 作为容器
            Scroller {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        padding(16f)
                    }

                    // ========== 测试组1：单独使用 clipPath ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("1. 单独使用 clipPath")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 1.1 星形裁剪
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.RED)
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形")
                                }
                            }
                        }

                        // 1.2 心形裁剪
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFF69B4))
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形")
                                }
                            }
                        }

                        // 1.3 六边形裁剪
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.BLUE)
                                    clipPath { w, h -> drawPolygon(this, w, h, 6) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六边形")
                                }
                            }
                        }

                        // 1.4 圆形裁剪
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.GREEN)
                                    clipPath { w, h -> drawCircle(this, w, h) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆形(clipPath)")
                                }
                            }
                        }
                    }

                    // ========== 测试组2：单独使用 borderRadius ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("2. 单独使用 borderRadius")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 2.1 统一圆角
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.YELLOW)
                                    borderRadius(16f)
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("统一圆角")
                                }
                            }
                        }

                        // 2.2 不同圆角
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.GREEN)
                                    borderRadius(8f, 16f, 24f, 32f)
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("不同圆角")
                                }
                            }
                        }

                        // 2.3 圆形
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFA500))
                                    borderRadius(40f)
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆形(radius)")
                                }
                            }
                        }
                    }

                    // ========== 测试组3：单独使用 border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("3. 单独使用 border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 3.1 实线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFEEEEEE))
                                    border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("实线边框")
                                }
                            }
                        }

                        // 3.2 虚线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFEEEEEE))
                                    border(Border(2f, BorderStyle.DASHED, Color.BLUE))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("虚线边框")
                                }
                            }
                        }

                        // 3.3 点线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFEEEEEE))
                                    border(Border(2f, BorderStyle.DOTTED, Color.RED))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("点线边框")
                                }
                            }
                        }
                    }

                    // ========== 测试组4：clipPath + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("4. clipPath + border (边框沿clipPath)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 4.1 星形 + 实线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFE4E1))
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    border(Border(2f, BorderStyle.SOLID, Color.RED))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形+实线")
                                }
                            }
                        }

                        // 4.2 心形 + 虚线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFE4E1))
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFFFF69B4)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+虚线")
                                }
                            }
                        }

                        // 4.3 六边形 + 点线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFE0E0FF))
                                    clipPath { w, h -> drawPolygon(this, w, h, 6) }
                                    border(Border(2f, BorderStyle.DOTTED, Color.BLUE))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六边形+点线")
                                }
                            }
                        }
                    }

                    // ========== 测试组5：borderRadius + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("5. borderRadius + border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 5.1 圆角 + 实线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFE0FFE0))
                                    borderRadius(16f)
                                    border(Border(2f, BorderStyle.SOLID, Color.GREEN))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆角+实线")
                                }
                            }
                        }

                        // 5.2 不同圆角 + 虚线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFFFE0))
                                    borderRadius(8f, 16f, 24f, 32f)
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFFFFA500)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("不同圆角+虚线")
                                }
                            }
                        }

                        // 5.3 圆形 + 点线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFE0E0E0))
                                    borderRadius(40f)
                                    border(Border(2f, BorderStyle.DOTTED, Color.GRAY))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆形+点线")
                                }
                            }
                        }
                    }

                    // ========== 测试组6：clipPath + borderRadius ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("6. clipPath + borderRadius (clipPath优先)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 6.1 星形clipPath + borderRadius
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.YELLOW)
                                    borderRadius(40f)  // 应被忽略
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形优先")
                                }
                            }
                        }

                        // 6.2 六边形clipPath + 不同圆角
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.YELLOW)
                                    borderRadius(8f, 16f, 24f, 32f)  // 应被忽略
                                    clipPath { w, h -> drawPolygon(this, w, h, 6) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六边形优先")
                                }
                            }
                        }
                    }

                    // ========== 测试组7：clipPath + borderRadius + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("7. clipPath + borderRadius + border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 7.1 三者同时存在
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFE0E0))
                                    borderRadius(40f)  // 应被 clipPath 覆盖
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    border(Border(2f, BorderStyle.SOLID, Color.RED))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("三者同时")
                                }
                            }
                        }

                        // 7.2 心形 + 圆角 + 虚线边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFE4E1))
                                    borderRadius(16f)  // 应被 clipPath 覆盖
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFFFF69B4)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+虚线")
                                }
                            }
                        }
                    }

                    // ========== 测试组8：clipPath + boxShadow ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("8. clipPath + boxShadow (不冲突)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 8.1 星形 + 阴影
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.WHITE)
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    boxShadow(BoxShadow(2f, 2f, 8f, Color(0x80000000)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形+阴影")
                                }
                            }
                        }

                        // 8.2 心形 + 阴影
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color.WHITE)
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    boxShadow(BoxShadow(2f, 2f, 8f, Color(0x80FF69B4)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+阴影")
                                }
                            }
                        }
                    }

                    // ========== 测试组9：clipPath + backgroundImage ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("9. clipPath + backgroundImage (不冲突)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 9.1 星形 + 渐变背景
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.RED, 0f),
                                        ColorStop(Color.YELLOW, 1f)
                                    )
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形+渐变")
                                }
                            }
                        }

                        // 9.2 心形 + 渐变背景
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundLinearGradient(
                                        Direction.TO_RIGHT,
                                        ColorStop(Color(0xFFFF69B4), 0f),
                                        ColorStop(Color(0xFFFF1493), 1f)
                                    )
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+渐变")
                                }
                            }
                        }

                        // 9.3 六边形 + 渐变 + 边框
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM_RIGHT,
                                        ColorStop(Color.BLUE, 0f),
                                        ColorStop(Color.YELLOW, 1f)
                                    )
                                    clipPath { w, h -> drawPolygon(this, w, h, 6) }
                                    border(Border(2f, BorderStyle.SOLID, Color.WHITE))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六边形+渐变+边框")
                                }
                            }
                        }
                    }

                    // ========== 测试组10：clipPath + boxShadow + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("10. clipPath + boxShadow + border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 10.1 星形 + 阴影 + 边框
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFFD700))
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    boxShadow(BoxShadow(3f, 3f, 10f, Color(0x80000000)))
                                    border(Border(2f, BorderStyle.SOLID, Color(0xFFB8860B)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("星形+阴影+边框")
                                }
                            }
                        }

                        // 10.2 心形 + 阴影 + 虚线边框
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFF69B4))
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    boxShadow(BoxShadow(3f, 3f, 10f, Color(0x80FF1493)))
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFFFF1493)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+阴影+虚线")
                                }
                            }
                        }
                    }

                    // ========== 测试组11：borderRadius + boxShadow + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("11. borderRadius + boxShadow + border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 11.1 圆角 + 阴影 + 实线边框
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFF87CEEB))
                                    borderRadius(16f)
                                    boxShadow(BoxShadow(3f, 3f, 10f, Color(0x80000000)))
                                    border(Border(2f, BorderStyle.SOLID, Color(0xFF4169E1)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆角+阴影+边框")
                                }
                            }
                        }

                        // 11.2 圆形 + 阴影 + 虚线边框
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFF98FB98))
                                    borderRadius(40f)
                                    boxShadow(BoxShadow(3f, 3f, 10f, Color(0x80228B22)))
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFF228B22)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆形+阴影+虚线")
                                }
                            }
                        }
                    }

                    // ========== 测试组12：clipPath + borderRadius + boxShadow + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("12. 四属性组合 (clipPath优先)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 12.1 全属性组合
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(90f)
                                    height(90f)
                                    backgroundColor(Color(0xFFFFE4B5))
                                    borderRadius(45f)  // 被 clipPath 覆盖
                                    clipPath { w, h -> drawStar(this, w, h, 6) }
                                    boxShadow(BoxShadow(4f, 4f, 12f, Color(0x80000000)))
                                    border(Border(2f, BorderStyle.SOLID, Color(0xFFFF8C00)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六角星全属性")
                                }
                            }
                        }

                        // 12.2 五边形全属性
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(90f)
                                    height(90f)
                                    backgroundColor(Color(0xFFE6E6FA))
                                    borderRadius(20f)  // 被 clipPath 覆盖
                                    clipPath { w, h -> drawPolygon(this, w, h, 5) }
                                    boxShadow(BoxShadow(4f, 4f, 12f, Color(0x809370DB)))
                                    border(Border(2f, BorderStyle.DASHED, Color(0xFF9370DB)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("五边形全属性")
                                }
                            }
                        }
                    }

                    // ========== 测试组13：Image 组件的属性组合 ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("13. Image + 多属性组合")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 13.1 Image + clipPath (星形)
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("图片+星形clip")
                                }
                            }
                        }

                        // 13.2 Image + clipPath + border
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    border(Border(2f, BorderStyle.SOLID, Color(0xFFFF69B4)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("图片+心形+边框")
                                }
                            }
                        }

                        // 13.3 Image + borderRadius + border
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    borderRadius(50f)
                                    border(Border(3f, BorderStyle.SOLID, Color.WHITE))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("图片+圆形+边框")
                                }
                            }
                        }
                    }

                    // ========== 测试组14：渐变背景 + 多属性组合 ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("14. 渐变背景 + 多属性组合")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 14.1 渐变 + clipPath + border + shadow
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(100f)
                                    height(100f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color(0xFFFFD700), 0f),
                                        ColorStop(Color(0xFFFFA500), 1f)
                                    )
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    border(Border(3f, BorderStyle.SOLID, Color(0xFFB8860B)))
                                    boxShadow(BoxShadow(4f, 4f, 12f, Color(0x80000000)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("渐变+星形+边框+阴影")
                                }
                            }
                        }

                        // 14.2 渐变 + borderRadius + border + shadow
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(100f)
                                    height(100f)
                                    backgroundLinearGradient(
                                        Direction.TO_RIGHT,
                                        ColorStop(Color(0xFF667eea), 0f),
                                        ColorStop(Color(0xFF764ba2), 1f)
                                    )
                                    borderRadius(20f)
                                    border(Border(3f, BorderStyle.SOLID, Color.WHITE))
                                    boxShadow(BoxShadow(4f, 4f, 12f, Color(0x80000000)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("渐变+圆角+边框+阴影")
                                }
                            }
                        }

                        // 14.3 渐变 + clipPath (心形) + borderRadius(被忽略)
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(100f)
                                    height(100f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color(0xFFFF6B6B), 0f),
                                        ColorStop(Color(0xFFFF69B4), 1f)
                                    )
                                    borderRadius(50f)  // 被 clipPath 覆盖
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    border(Border(2f, BorderStyle.SOLID, Color.WHITE))
                                    boxShadow(BoxShadow(4f, 4f, 12f, Color(0x80FF1493)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("渐变+心形+全属性")
                                }
                            }
                        }
                    }

                    // ========== 测试组15：不同边框样式与 clipPath ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("15. 不同边框样式 + clipPath")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 15.1 clipPath + 实线
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFE8F5E9))
                                    clipPath { w, h -> drawPolygon(this, w, h, 8) }
                                    border(Border(3f, BorderStyle.SOLID, Color(0xFF4CAF50)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("八边形+实线")
                                }
                            }
                        }

                        // 15.2 clipPath + 虚线
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFE3F2FD))
                                    clipPath { w, h -> drawPolygon(this, w, h, 8) }
                                    border(Border(3f, BorderStyle.DASHED, Color(0xFF2196F3)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("八边形+虚线")
                                }
                            }
                        }

                        // 15.3 clipPath + 点线
                        View {
                            attr {
                                marginRight(16f)
                                marginBottom(16f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(80f)
                                    height(80f)
                                    backgroundColor(Color(0xFFFCE4EC))
                                    clipPath { w, h -> drawPolygon(this, w, h, 8) }
                                    border(Border(3f, BorderStyle.DOTTED, Color(0xFFE91E63)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("八边形+点线")
                                }
                            }
                        }
                    }

                    // ========== 测试组16：综合大型测试 ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("16. 综合大型测试")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 16.1 复杂金色星形徽章
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(120f)
                                    height(120f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color(0xFFFFD700), 0f),
                                        ColorStop(Color(0xFFB8860B), 0.5f),
                                        ColorStop(Color(0xFFFFD700), 1f)
                                    )
                                    clipPath { w, h -> drawStar(this, w, h, 8) }
                                    border(Border(3f, BorderStyle.SOLID, Color(0xFF8B4513)))
                                    boxShadow(BoxShadow(6f, 6f, 16f, Color(0x80000000)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("金色八角徽章")
                                }
                            }
                        }

                        // 16.2 爱心按钮效果
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            View {
                                attr {
                                    width(120f)
                                    height(120f)
                                    backgroundLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color(0xFFFF6B6B), 0f),
                                        ColorStop(Color(0xFFEE5A24), 1f)
                                    )
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    border(Border(3f, BorderStyle.SOLID, Color.WHITE))
                                    boxShadow(BoxShadow(6f, 6f, 16f, Color(0x80FF0000)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("立体爱心")
                                }
                            }
                        }
                    }

                    // ========== 测试组17：maskLinearGradient + clipPath (⚠️潜在冲突) ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("17. maskLinearGradient + clipPath (Image)")
                        }
                    }

                    Text {
                        attr {
                            marginBottom(8f)
                            fontSize(12f)
                            color(Color.RED)
                            text("⚠️ 注意：maskLinearGradient 和 clipPath 都使用 layer.mask，可能存在冲突")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 17.1 仅 maskLinearGradient（对照组）
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    maskLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color.WHITE, 0.5f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("仅maskGradient")
                                }
                            }
                        }

                        // 17.2 仅 clipPath（对照组）
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("仅clipPath")
                                }
                            }
                        }

                        // 17.3 maskLinearGradient + clipPath（冲突测试）
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                    maskLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color.WHITE, 0.5f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("clip+mask(顺序1)")
                                }
                            }
                        }

                        // 17.4 clipPath + maskLinearGradient（不同顺序）
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    maskLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color.WHITE, 0.5f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                    clipPath { w, h -> drawStar(this, w, h, 5) }
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("mask+clip(顺序2)")
                                }
                            }
                        }
                    }

                    // ========== 测试组18：maskLinearGradient + borderRadius ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("18. maskLinearGradient + borderRadius (Image)")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 18.1 maskLinearGradient + borderRadius
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    borderRadius(20f)
                                    maskLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color.WHITE, 0.6f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆角+渐变mask")
                                }
                            }
                        }

                        // 18.2 maskLinearGradient + 圆形
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    borderRadius(50f)
                                    maskLinearGradient(
                                        Direction.TO_RIGHT,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("圆形+横向渐变")
                                }
                            }
                        }
                    }

                    // ========== 测试组19：maskLinearGradient + clipPath + border ==========
                    Text {
                        attr {
                            marginTop(16f)
                            marginBottom(8f)
                            fontSize(16f)
                            fontWeightBold()
                            color(Color.BLACK)
                            text("19. maskLinearGradient + clipPath + border")
                        }
                    }

                    View {
                        attr {
                            flexDirectionRow()
                            flexWrap(FlexWrap.WRAP)
                            marginBottom(16f)
                        }

                        // 19.1 三者组合
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawHeart(this, w, h) }
                                    maskLinearGradient(
                                        Direction.TO_BOTTOM,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color.WHITE, 0.5f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                    border(Border(2f, BorderStyle.SOLID, Color(0xFFFF69B4)))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("心形+渐变+边框")
                                }
                            }
                        }

                        // 19.2 六边形 + 渐变mask + 虚线边框
                        View {
                            attr {
                                marginRight(20f)
                                marginBottom(20f)
                                alignItems(FlexAlign.CENTER)
                            }
                            Image {
                                attr {
                                    width(100f)
                                    height(100f)
                                    src("https://img1.baidu.com/it/u=1966616150,2146512490&fm=253&app=138&size=w931&n=0&f=JPEG&fmt=auto?sec=1680886800&t=a25bf0f4ceaaa8bbbbfc12da3d2d3c67")
                                    clipPath { w, h -> drawPolygon(this, w, h, 6) }
                                    maskLinearGradient(
                                        Direction.TO_RIGHT,
                                        ColorStop(Color.WHITE, 0f),
                                        ColorStop(Color(0x00FFFFFF), 1f)
                                    )
                                    border(Border(2f, BorderStyle.DASHED, Color.BLUE))
                                }
                            }
                            Text {
                                attr {
                                    marginTop(4f)
                                    fontSize(10f)
                                    color(Color.GRAY)
                                    text("六边形+渐变+边框")
                                }
                            }
                        }
                    }

                    // 底部间距
                    View {
                        attr {
                            height(80f)
                        }
                    }
                }
            }
        }
    }
}

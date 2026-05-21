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
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.ImageRef
import com.tencent.kuikly.core.module.MemoryCacheModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.ContextApi
import com.tencent.kuikly.core.views.FontStyle
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI

/**
 * Demo page that puts multiple Canvas views inside a vertical Scroller,
 * so we can verify Canvas rendering & layout inside a scroll container
 * across H5 / mini-program / native targets.
 */
@Page("CanvasInScroller")
internal class CanvasInScrollerPage : BasePager() {

    private var imageCacheKey: String = ""

    companion object {
        // Same base64 penguin image as CanvasTestPage so drawImage works on every platform.
        private const val BASE64_IMAGE =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAABQCAMAAAC5zwKfAAABQVBMVEUAAAD////v6u/////t5+3y7PL7+/vx7fLg19/07PTr6+vd1Nzh1uHp5Oj09PTu6e749Pjy8vPv7O/o5Oj4+Pns6ez19fXm3Ob4+Pjp4urp4ujz8PLc2N7Z1dvm5Ofh2uH7/Pzn4efz8/P6+vr09/fb0Nvl2uXr5+ri3+Laztr09fbe1t/o3Ofx8fHdz9729vbk2uHw6fDx8vL9/f3d0N38/Pzb1dvi2ODx8vLq6urv8PDt7e3t7+78/P3s6+zj4uTi4OLRxdHn5uf+//7z9PTZz9je297WztXRx9Dg3uHWydXTytLb19vZ0tjl5OX2+Pfo6OjZ19rOw8329ffu5+7d193d0Nzr5Ovn4ejY1djOzM/d1NzUxtTh0+Hh2+Dc2tzTzNLOxc3ay9rV09bS0NPSx9HW0Nb4+Pnp3+nMyMzl2eVVtzfcAAAAOHRSTlMAA0QHPjAP/uMWDfnYlIZgHOTW0LSgko94d1km9fG6rptsa2BI/O3n5NPKxcKsnFVVI/Ty8OqmhNTrSmMAAAL9SURBVFjD7dXXVuJQFIDh0FE60rGOjr1OTUglGtAEkFCCVAWU4vs/wOxzIjNzw5IVnKvJv7yKrs99SoAwMzMzMzMz+79z2i92A3tpy8doK4ljmqYp6CBq/YDhYl8yDI3FsiiW1pYlraEMjsEzirzAR1eX8T4xgJEkCSKDxBIvCGsrS3iYQyBE0xyMyN/ljIt7DHi42apFGDGX27QYPI8Qo3OzNXM6WCgkjYExTp/vBiIzyNOX/KiuB414QYrJIO4eApHhOHwod48FdegzAib+gDBiBg+IQFUdjjbe7o7DfxbdcS4Ihjk4EwziJf8eUG0+t54j27HE7l4M78KhdaETsVIIzOg7SMKK4VXhMTh8brXEKkTSDA0PhXXHO5t34Y+ESBKDICIOL7gsgpcrNMGbrp+uHTQ4+DUHD+sn8zHYlTBVKj1AFEdngMQah7wyPwOnLTf8qQ/+I03Bs3r7ah63ekY9NodF99bPlE/kaEYPOPD0AfGKp9MNuNubNHmje925u7jNihs/UkH82okUFtEbhzwReXBnmsOnljLKJbaPSdJVe4CRi5pmnwdGWPZ7xL9rTzttAQSiOJ0rlUqiKJYpeHvu2fsq6XLVJq9dRW2O8oNvzrk7GBqz1zj2K/tX1/AUx7JVsGqNyWT/NL7f7vfzeSXbOSfmZktFw67qGCOzMDke61Jj8vra7XbjK7CJl1Jevr3NZo/e+Yi0pa2BpN8XbjRqKEAacOiC0MvV25KUz8uDgabfPPuRnM1mz8FbpFRPEHiefxCgO8AKarMIC1TkTkf2WgicxXFpXfjT0XZYz/V66AewQrMI2tOT8gIjdQYBwkiBertQB0uFkxxhTUE7loUBbYZAy2YR18azAXbb71cqAH62G/1GdkuSVGy3NUlSlDcOPC1JGC2NxG5X0yTwKuBVkBcnjHflhWsiadoAgXg++XOSWKZVn0eSoRfYQcQNvHZiyRxbQCryCyRL7h0bsXzBnS2vx+Nxn8TtFuKjsthsFsLMzMzMzMzsH/cLPKnsav8gklIAAAAASUVORK5CYII="

        private const val TAG = "CanvasInScroller"

        // Helper that draws a filled rect using path + fill (since ContextApi has no fillRect).
        private fun ContextApi.pathRect(x: Float, y: Float, w: Float, h: Float) {
            beginPath()
            moveTo(x, y)
            lineTo(x + w, y)
            lineTo(x + w, y + h)
            lineTo(x, y + h)
            closePath()
        }
    }

    override fun created() {
        super.created()
        // Pre-cache base64 image so drawImage(ImageRef(cacheKey)) works on all platforms.
        val imageParams = JSONObject().apply { put("key", "value") }
        val status = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME)
            .cacheImage(BASE64_IMAGE, imageParams, true) {
                KLog.i(TAG, "image cached, code=" + it.errorCode + ", key=" + it.cacheKey)
                imageCacheKey = it.cacheKey
            }
        if (status.cacheKey.isNotEmpty()) {
            imageCacheKey = status.cacheKey
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "Canvas in Scroller"
                }
            }

            // ---------- Real-device tip ----------
            View {
                attr {
                    flexDirectionRow()
                    alignItemsCenter()
                    padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                    backgroundColor(Color(0xFFFFF3E0L))
                }
                Text {
                    attr {
                        text("提示：小程序请使用真机测试，开发者工具上 Canvas 可能与真机存在差异。")
                        fontSize(12f)
                        color(Color(0xFFD32F2FL))
                    }
                }
            }

            // ---------- Diagnostic: Canvas OUTSIDE Scroller ----------
            // If this shows a red rect but the canvases below are blank,
            // the bug is specific to Canvas-inside-Scroller layout/render timing.
            View {
                attr {
                    height(48f)
                    flexDirectionRow()
                    alignItemsCenter()
                    padding(left = 12f, right = 12f)
                }
                Text {
                    attr {
                        text("对照(Scroller外):")
                        fontSize(13f)
                        color(Color(0xFF666666L))
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(left = 12f)
                            width(120f)
                            height(40f)
                            backgroundColor(Color.WHITE)
                        }
                    }
                ) { context, width, height ->
                    KLog.i(TAG, "outside-canvas drawCallback w=$width h=$height")
                    context.pathRect(4f, 4f, width - 8f, height - 8f)
                    context.fillStyle(Color.RED)
                    context.fill()
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF5F5F5L))
                }

                // ---------- Section 1: basic shapes ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("1. 基础图形 矩形 / 圆 / 直线")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(12f)
                            height(160f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                        }
                    }
                ) { context, width, height ->
                    KLog.i(TAG, "section1-canvas drawCallback w=$width h=$height")
                    // diagonal line
                    context.beginPath()
                    context.moveTo(0f, 0f)
                    context.lineTo(width, height)
                    context.strokeStyle(Color.GRAY)
                    context.lineWidth(2f)
                    context.stroke()

                    // filled rect
                    context.pathRect(20f, 20f, 80f, 60f)
                    context.fillStyle(Color(0xFF4CAF50L))
                    context.fill()

                    // stroked rect
                    context.pathRect(120f, 20f, 80f, 60f)
                    context.strokeStyle(Color(0xFFE91E63L))
                    context.lineWidth(3f)
                    context.stroke()

                    // circle
                    context.beginPath()
                    context.arc(width - 60f, 70f, 40f, 0f, (PI * 2).toFloat(), false)
                    context.fillStyle(Color(0xFF2196F3L))
                    context.fill()
                }

                // ---------- Section 2: Path & gradient ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("2. Path 折线 + 线性渐变描边")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(12f)
                            height(180f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                        }
                    }
                ) { context, width, height ->
                    context.beginPath()
                    context.moveTo(20f, height - 30f)
                    context.lineTo(width / 4f, 40f)
                    context.lineTo(width / 2f, height - 30f)
                    context.lineTo(width * 3f / 4f, 60f)
                    context.lineTo(width - 20f, height - 30f)
                    context.lineWidth(4f)
                    val gradient = context.createLinearGradient(0f, 0f, width, 0f)
                    gradient.addColorStop(0f, Color(0xFFE63029L))
                    gradient.addColorStop(0.5f, Color(0xFFFFC107L))
                    gradient.addColorStop(1f, Color(0xFF2196F3L))
                    context.strokeStyle(gradient)
                    context.stroke()
                }

                // ---------- Section 3: arc as progress ring ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("3. 圆弧 arc - 进度环")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(12f)
                            height(200f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                        }
                    }
                ) { context, width, _ ->
                    val cx = width / 2f
                    val cy = 100f
                    val radius = 70f
                    // background ring
                    context.beginPath()
                    context.arc(cx, cy, radius, 0f, (PI * 2).toFloat(), false)
                    context.lineWidth(14f)
                    context.strokeStyle(Color(0xFFE0E0E0L))
                    context.stroke()
                    // progress arc 70%
                    context.beginPath()
                    val start = (-PI / 2).toFloat()
                    val end = (start + PI * 2 * 0.7).toFloat()
                    context.arc(cx, cy, radius, start, end, false)
                    context.lineCapRound()
                    context.strokeStyle(Color(0xFF673AB7L))
                    context.stroke()
                }

                // ---------- Section 4: text ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("4. 文本 fillText / strokeText / measureText")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(12f)
                            height(160f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                        }
                    }
                ) { context, width, _ ->
                    context.font(FontStyle.NORMAL, FontWeight.BOLD, 22f)
                    context.textAlign(TextAlign.CENTER)
                    context.fillStyle(Color(0xFF333333L))
                    context.fillText("Hello Canvas in Scroller", width / 2f, 50f)

                    context.font(FontStyle.ITALIC, FontWeight.NORMAL, 16f)
                    context.lineWidth(1f)
                    context.strokeStyle(Color(0xFFE91E63L))
                    context.strokeText("strokeText 描边文字", width / 2f, 90f)

                    val metrics = context.measureText("measureText sample")
                    context.font(FontStyle.NORMAL, FontWeight.NORMAL, 14f)
                    context.fillStyle(Color(0xFF666666L))
                    context.fillText(
                        "measureText.width = ${metrics.width.toInt()}",
                        width / 2f,
                        130f
                    )
                }

                // ---------- Section 5: drawImage ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("5. drawImage（base64 image）")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                Canvas(
                    {
                        attr {
                            margin(12f)
                            height(160f)
                            backgroundColor(Color.WHITE)
                            borderRadius(8f)
                        }
                    }
                ) { context, _, _ ->
                    val key = ctx.imageCacheKey
                    if (key.isNotEmpty()) {
                        // 1) original
                        context.drawImage(ImageRef(key), 20f, 20f)
                        // 2) target size
                        context.drawImage(ImageRef(key), 130f, 20f, 100f, 100f)
                        // 3) src crop + dst scale
                        context.drawImage(
                            ImageRef(key),
                            10f, 10f, 60f, 60f,
                            250f, 20f, 100f, 100f
                        )
                    } else {
                        context.fillStyle(Color.GRAY)
                        context.font(FontStyle.NORMAL, FontWeight.NORMAL, 14f)
                        context.textAlign(TextAlign.LEFT)
                        context.fillText("image loading...", 20f, 80f)
                    }
                }

                // ---------- Section 6: many small canvases (force scrolling) ----------
                View {
                    attr { padding(left = 12f, right = 12f, top = 16f, bottom = 6f) }
                    Text {
                        attr {
                            text("6. 多个小 Canvas，验证滚动")
                            fontSize(15f)
                            color(Color(0xFF222222L))
                        }
                    }
                }
                val palette = longArrayOf(
                    0xFFEF5350L, 0xFFAB47BCL, 0xFF5C6BC0L, 0xFF42A5F5L,
                    0xFF26A69AL, 0xFF66BB6AL, 0xFFFFCA28L, 0xFFFF7043L
                )
                for (i in palette.indices) {
                    val ratio = (i + 1) / palette.size.toFloat()
                    val color = Color(palette[i])
                    val label = "Canvas #${i + 1}"
                    Canvas(
                        {
                            attr {
                                margin(left = 12f, right = 12f, top = 6f, bottom = 6f)
                                height(80f)
                                backgroundColor(Color.WHITE)
                                borderRadius(6f)
                            }
                        }
                    ) { context, width, height ->
                        context.pathRect(0f, 0f, width * ratio, height)
                        context.fillStyle(color)
                        context.fill()

                        context.fillStyle(Color.WHITE)
                        context.font(FontStyle.NORMAL, FontWeight.BOLD, 14f)
                        context.textAlign(TextAlign.LEFT)
                        context.fillText(label, 12f, height / 2f + 5f)
                    }
                }

                // tail spacer
                View {
                    attr {
                        height(20f)
                    }
                }
            }
        }
    }
}

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
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.ImageCacheStatus
import com.tencent.kuikly.core.module.ImageRef
import com.tencent.kuikly.core.module.MemoryCacheModule
import com.tencent.kuikly.core.views.Canvas
import com.tencent.kuikly.core.views.FontStyle
import com.tencent.kuikly.core.views.FontWeight
import com.tencent.kuikly.core.views.TextAlign
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.PI

val Float.rem: Float
    get() {
        return this * PagerManager.getCurrentPager().pageData.pageViewWidth / 750
    }
@Page("CanvasTestPage")
internal class CanvasTestPage : BasePager() {
    var imageCacheKey: String = ""
    companion object{
        const val BASE64_IMAGE = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAABQCAMAAAC5zwKfAAABQVBMVEUAAAD////v6u/////t5+3y7PL7+/vx7fLg19/07PTr6+vd1Nzh1uHp5Oj09PTu6e749Pjy8vPv7O/o5Oj4+Pns6ez19fXm3Ob4+Pjp4urp4ujz8PLc2N7Z1dvm5Ofh2uH7/Pzn4efz8/P6+vr09/fb0Nvl2uXr5+ri3+Laztr09fbe1t/o3Ofx8fHdz9729vbk2uHw6fDx8vL9/f3d0N38/Pzb1dvi2ODx8vLq6urv8PDt7e3t7+78/P3s6+zj4uTi4OLRxdHn5uf+//7z9PTZz9je297WztXRx9Dg3uHWydXTytLb19vZ0tjl5OX2+Pfo6OjZ19rOw8329ffu5+7d193d0Nzr5Ovn4ejY1djOzM/d1NzUxtTh0+Hh2+Dc2tzTzNLOxc3ay9rV09bS0NPSx9HW0Nb4+Pnp3+nMyMzl2eVVtzfcAAAAOHRSTlMAA0QHPjAP/uMWDfnYlIZgHOTW0LSgko94d1km9fG6rptsa2BI/O3n5NPKxcKsnFVVI/Ty8OqmhNTrSmMAAAL9SURBVFjD7dXXVuJQFIDh0FE60rGOjr1OTUglGtAEkFCCVAWU4vs/wOxzIjNzw5IVnKvJv7yKrs99SoAwMzMzMzMz+79z2i92A3tpy8doK4ljmqYp6CBq/YDhYl8yDI3FsiiW1pYlraEMjsEzirzAR1eX8T4xgJEkCSKDxBIvCGsrS3iYQyBE0xyMyN/ljIt7DHi42apFGDGX27QYPI8Qo3OzNXM6WCgkjYExTp/vBiIzyNOX/KiuB414QYrJIO4eApHhOHwod48FdegzAib+gDBiBg+IQFUdjjbe7o7DfxbdcS4Ihjk4EwziJf8eUG0+t54j27HE7l4M78KhdaETsVIIzOg7SMKK4VXhMTh8brXEKkTSDA0PhXXHO5t34Y+ESBKDICIOL7gsgpcrNMGbrp+uHTQ4+DUHD+sn8zHYlTBVKj1AFEdngMQah7wyPwOnLTf8qQ/+I03Bs3r7ah63ekY9NodF99bPlE/kaEYPOPD0AfGKp9MNuNubNHmje925u7jNihs/UkH82okUFtEbhzwReXBnmsOnljLKJbaPSdJVe4CRi5pmnwdGWPZ7xL9rTzttAQSiOJ0rlUqiKJYpeHvu2fsq6XLVJq9dRW2O8oNvzrk7GBqz1zj2K/tX1/AUx7JVsGqNyWT/NL7f7vfzeSXbOSfmZktFw67qGCOzMDke61Jj8vra7XbjK7CJl1Jevr3NZo/e+Yi0pa2BpN8XbjRqKEAacOiC0MvV25KUz8uDgabfPPuRnM1mz8FbpFRPEHiefxCgO8AKarMIC1TkTkf2WgicxXFpXfjT0XZYz/V66AewQrMI2tOT8gIjdQYBwkiBertQB0uFkxxhTUE7loUBbYZAy2YR18azAXbb71cqAH62G/1GdkuSVGy3NUlSlDcOPC1JGC2NxG5X0yTwKuBVkBcnjHflhWsiadoAgXg++XOSWKZVn0eSoRfYQcQNvHZiyRxbQCryCyRL7h0bsXzBnS2vx+Nxn8TtFuKjsthsFsLMzMzMzMzsH/cLPKnsav8gklIAAAAASUVORK5CYII="
    }
    override fun created() {
        super.created()
        val status0 = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME).cacheImage(ImageUri.commonAssets("penguin2.png").toUrl("CanvasTestPage"), true){
            KLog.i("CanvasTestPage", "0 Cache Code:" + it.errorCode + ", CacheKey:" + it.cacheKey)
        }

        val status = acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME).cacheImage(BASE64_IMAGE, true){
            KLog.i("CanvasTestPage", "Cache Code:" + it.errorCode + ", CacheKey:" + it.cacheKey)
        }
        if(status.state == ImageCacheStatus.Complete){
            KLog.i("CanvasTestPage", "Cache Code:" + status.errorCode + ", CacheKey:" + status.cacheKey)
            imageCacheKey = status.cacheKey
        }
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "CanvasTestPage"
                 //   backgroundLinearGradient()
                }
            }

            Canvas(
                {
                    attr {
                        overflow( false)
                        absolutePositionAllZero()
                        backgroundColor(Color.GRAY)
                        absolutePosition(top = 220f.rem, left = 320f.rem, 0f, 0f)
                    }
                }
            ) { context, width, height ->
                val centerX = 100f.rem + 12.5f.rem
                val centerY = 100f.rem+ 12.5f.rem
                val radius = 100f.rem
                val startAngle = (PI * 2.25f).toFloat()
                val endAngle = (PI * 0.75f).toFloat()
                val progress = 0.75 // 进度值，范围为 0 到 1

                // 绘制底部圆环
                context.beginPath()
                context.arc(centerX, centerY, radius, startAngle, endAngle, true)
                context.lineWidth(25f.rem)
                context.strokeStyle(Color(0xFFF6C8C2))
                context.lineCapRound()
                context.stroke()

//                val gradient = context.createLinearGradient(0f, 0f, width, height)
//                gradient.addColorStop(0f, Color.YELLOW)
//                gradient.addColorStop(1f, Color.BLUE)

//                context.fillStyle(gradient)

                // 绘制进度圆环
                context.beginPath()
                context.arc(centerX, centerY, radius, startAngle, (startAngle + (endAngle - startAngle) * progress).toFloat(), true)
                context.lineWidth(25f.rem)

                val gradient = context.createLinearGradient(0f, 0f,centerX + radius , centerY + radius)
                gradient.addColorStop(0f, Color.YELLOW)
                gradient.addColorStop(1f, Color.RED)

                context.strokeStyle(gradient)
//                context.createLinearGradient(
//                    0f, 0f, 1f, 0f, ColorStop(Color(0xFFE63029), 0f),
//                    ColorStop(Color(0xEEFF4E92), 1f)
//                )

                context.lineCapRound()
                context.stroke()

                // 绘制底部圆环
                context.beginPath()
                context.arc(centerX, centerY, radius / 3 * 2, startAngle, endAngle, true)
                context.lineWidth(25f.rem)
                context.strokeStyle(Color(0xFFFCEFCC))
                context.lineCapRound()
                context.stroke()

                // 绘制进度圆环
                context.beginPath()
                context.arc(centerX, centerY, radius / 3 * 2, startAngle, (startAngle + (endAngle - startAngle) * progress).toFloat(), true)
                context.lineWidth(25f.rem)
//                context.createLinearGradient(
//                    0f, 0f, 1f, 0f, ColorStop(Color(0xFFB9FE10), 0f),
//                    ColorStop(Color(0xEE95F305), 1f)
//                )

//                val gradient = context.createLinearGradient(0f, 0f,centerX + radius , centerY + radius)
//                gradient.addColorStop(0f, Color.YELLOW)
//                gradient.addColorStop(1f, Color.RED)
                val gradient2 = context.createLinearGradient(0f, 0f,centerX + radius , centerY + radius)
                gradient2.addColorStop(0f, Color.BLACK)
                gradient2.addColorStop(1f, Color.GREEN)
                context.strokeStyle(gradient2)
                context.lineCapRound()
                context.stroke()

                // 绘制底部圆环
                context.beginPath()
                context.arc(centerX, centerY, radius / 3, startAngle, endAngle, true)
                context.lineWidth(25f.rem)
                context.strokeStyle(Color(0xFFC7D8FC))
                context.lineCapRound()
                context.stroke()
//

                // 绘制进度圆环
                context.beginPath()
                context.arc(centerX, centerY, radius / 3, startAngle, (startAngle + (endAngle - startAngle) * progress).toFloat(), true)
                context.lineWidth(25f.rem)
//                context.createLinearGradient(
//                    0f, 0f, 1f, 0f, ColorStop(Color(0xFF53B8DD), 0f),
//                    ColorStop(Color(0xEE72F6D2), 1f)
//                )
                context.lineCapRound()
                context.closePath()

                context.fillStyle(gradient)
                context.fill()

                context.beginPath()
                context.moveTo(50f, 150f)
                context.arc(50f, 150f, 50f, 0f, (PI * 2.5).toFloat(), true)
                context.lineTo(50f, 150f)
                context.strokeStyle(Color(0x00ff00, .5f))
                context.fillStyle(Color(0x00ff00, .25f))
                context.lineWidth(10f)
                context.stroke()
                context.fill()
                context.beginPath()
                context.moveTo(150f, 150f)
                context.arc(150f, 150f, 50f, 0f, (PI * 2.5).toFloat(), false)
                context.lineTo(150f, 150f)
                context.strokeStyle(Color(0xff0000, .5f))
                context.fillStyle(Color(0xff0000, .25f))
                context.lineWidth(5f)
                context.stroke()
                context.fill()
                context.beginPath()
                context.moveTo(50f, 250f)
                context.arc(50f, 250f, 50f, 0f, (PI * -2.5).toFloat(), true)
                context.lineTo(50f, 250f)
                context.strokeStyle(Color(0xffff00, .5f))
                context.fillStyle(Color(0xffff00, .25f))
                context.lineWidth(10f)
                context.stroke()
                context.fill()
                context.beginPath()
                context.moveTo(150f, 250f)
                context.arc(150f, 250f, 50f, 0f, (PI * -2.5).toFloat(), false)
                context.lineTo(150f, 250f)
                context.strokeStyle(Color(0x0000ff, .5f))
                context.fillStyle(Color(0x0000ff, .25f))
                context.lineWidth(5f)
                context.stroke()
                context.fill()

                val x = 50f
                val y = 300f
                context.fillStyle(Color.RED)
                context.font(FontStyle.ITALIC, FontWeight.BOLD, 15f)
                context.textAlign(TextAlign.CENTER)
                val metrics = context.measureText("Hello world")
                context.fillText("Hello world", x, y)
                context.beginPath()
                context.moveTo(x - metrics.actualBoundingBoxLeft, y - metrics.actualBoundingBoxAscent)
                context.lineTo(x + metrics.actualBoundingBoxRight, y - metrics.actualBoundingBoxAscent)
                context.lineTo(x + metrics.actualBoundingBoxRight, y + metrics.actualBoundingBoxDescent)
                context.lineTo(x - metrics.actualBoundingBoxLeft, y + metrics.actualBoundingBoxDescent)
                context.closePath()
                context.lineWidth(1f)
                context.stroke()
                context.lineWidth(2f)
                context.strokeText("Hello world", x, y + 50f)
                context.drawImage(ImageRef(ctx.imageCacheKey), x, y + 100f)
            }
        }

    }

}
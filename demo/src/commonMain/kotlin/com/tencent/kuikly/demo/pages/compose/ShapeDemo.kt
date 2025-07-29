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

package com.tencent.kuikly.demo.pages.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.wrapContentHeight
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Outline
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.RectangleShape
import com.tencent.kuikly.compose.ui.graphics.Shape
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Page("shapedemo")
internal class ShapeDemoPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                Text("clip")
                Row {
                    Text(
                        "the quick brown fox jumps over the lazy dog. THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG.",
                        modifier = Modifier.size(80.dp).clip(shape = CircleShape),
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                    Text(
                        "the quick brown fox jumps over the lazy dog. THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG.",
                        modifier = Modifier.size(80.dp).clip(shape = RectangleShape),
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                    Text(
                        "the quick brown fox jumps over the lazy dog. THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG.",
                        modifier = Modifier.size(80.dp).clip(shape = StarShape),
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }
                Row(Modifier.wrapContentHeight()) {
                    Image(
                        modifier = Modifier.size(80.dp).clip(shape = CircleShape),
                        painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                        contentDescription = null
                    )
                    Image(
                        modifier = Modifier.size(80.dp).clip(shape = RectangleShape),
                        painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                        contentDescription = null
                    )
                    Image(
                        modifier = Modifier.size(80.dp).clip(shape = StarShape),
                        painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                        contentDescription = null
                    )
                }
                Text("background")
                Row(Modifier.wrapContentHeight()) {
                    Box(Modifier.size(80.dp).background(Color.Red, shape = CircleShape))
                    Box(Modifier.size(80.dp).background(Color.Red, shape = RoundedCornerShape(25.dp)))
                    Box(Modifier.size(80.dp).background(Color.Red, shape = RectangleShape))
                }
                Text("border")
                Row(Modifier.wrapContentHeight()) {
                    Box(Modifier.size(80.dp).border(2.dp, Color.Red, CircleShape))
                    Box(Modifier.size(80.dp).border(2.dp, Color.Red, RectangleShape))
                    Image(
                        modifier = Modifier.size(80.dp).border(2.dp, Color.Red, CircleShape),
                        painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                        contentDescription = null
                    )
                    Image(
                        modifier = Modifier.size(80.dp).border(2.dp, Color.Red, RectangleShape),
                        painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private val StarShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val outerRadius = minOf(size.width, size.height) / 2
            val innerRadius = outerRadius * 0.4f // Adjust this to change star pointiness

            val points = 5 // Number of star points
            val anglePerPoint = (2 * PI / points).toFloat()

            moveTo(
                centerX + outerRadius * cos(0f),
                centerY + outerRadius * sin(0f)
            )

            for (i in 1 until points * 2) {
                val radius = if (i % 2 == 0) outerRadius else innerRadius
                val angle = anglePerPoint * i / 2

                lineTo(
                    centerX + radius * cos(angle),
                    centerY + radius * sin(angle)
                )
            }

            close()
        }

        return Outline.Generic(path)
    }
}

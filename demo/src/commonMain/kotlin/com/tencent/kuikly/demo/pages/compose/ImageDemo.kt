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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.ColorFilter
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.painter.BrushPainter
import com.tencent.kuikly.compose.ui.graphics.painter.ColorPainter
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri

@Page("ComposeImageDemo")
internal class ImageDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        // 这里可以修改一些基本配置
        layoutDirection = LayoutDirection.Ltr

        setContent {
            DemoScaffold("Image Demo", true) {
                Text("painter")
                PainterSamples()
                Text("placeholder")
                PlaceholderSamples()
                Text("error")
                ErrorSamples()
                Text("fallback")
                FallbackSamples()
                Text("Alignment and ContentScale")
                AlignmentAndContentScaleSamples()
            }
        }
    }

}

@Composable
private fun SecondaryText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.secondary,
        fontSize = 12.sp
    )
}

@OptIn(InternalResourceApi::class)
private val penguin by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("penguin2.png").toUrl(""))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PainterSamples() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            SecondaryText("rememberAsyncImagePainter")
            Image(
                painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                contentDescription = null,
                Modifier.size(200.dp, 100.dp),
            )
        }
        Column {
            SecondaryText("painterResource")
            Image(
                painter = painterResource(penguin),
                contentDescription = null,
                Modifier.size(200.dp, 100.dp),
            )
        }
        Column {
            SecondaryText("ColorPainter")
            Image(
                painter = ColorPainter(Color.Magenta),
                contentDescription = null,
                Modifier.size(200.dp, 100.dp),
            )
        }
        Column {
            SecondaryText("BrushPainter (LinearGradient)")
            Image(
                painter = BrushPainter(Brush.linearGradient(listOf(Color.Black, Color.White))),
                contentDescription = null,
                Modifier.size(200.dp, 100.dp),
            )
        }
        Column {
            SecondaryText("BrushPainter (SolidColor)")
            Image(
                painter = BrushPainter(SolidColor(Color.Cyan)),
                contentDescription = null,
                Modifier.size(200.dp, 100.dp),
            )
        }
    }
}

@Composable
private fun PlaceholderSamples() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var type by remember { mutableStateOf(0) }
        var count by remember { mutableStateOf(0L) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Raw Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png?t=$count",
                        placeholder = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Wrapped Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png?t=$count",
                        placeholder = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button({ type = 0; ++count }) {
                Text("with color")
            }
            Button({ type = 1; ++count }) {
                Text("brush gradient", fontSize = 12.sp)
            }
            Button({ type = 2; ++count }) {
                Text("brush solid")
            }
            Button({ type = 3; ++count }) {
                Text("with image")
            }
        }
    }
}

@Composable
private fun ErrorSamples() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var type by remember { mutableStateOf(0) }
        var count by remember { mutableStateOf(0L) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Raw Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        "https://httpbin.org/status/404",
                        error = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Wrapped Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        "https://httpbin.org/status/404",
                        error = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button({ type = 0; ++count }) {
                Text("with color")
            }
            Button({ type = 1; ++count }) {
                Text("brush gradient", fontSize = 12.sp)
            }
            Button({ type = 2; ++count }) {
                Text("brush solid")
            }
            Button({ type = 3; ++count }) {
                Text("with image")
            }
        }
    }
}

@Composable
private fun FallbackSamples() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var type by remember { mutableStateOf(0) }
        var count by remember { mutableStateOf(0L) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Raw Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        null,
                        fallback = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            SecondaryText("Wrapped Image")
            key(count) {
                Image(
                    painter = rememberAsyncImagePainter(
                        null,
                        fallback = when (type) {
                            0 -> ColorPainter(Color.Gray)
                            1 -> BrushPainter(Brush.verticalGradient(listOf(Color.Magenta, Color.Cyan)))
                            2 -> BrushPainter(SolidColor(Color.Magenta))
                            3 -> painterResource(penguin)
                            else -> null
                        },
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Button({ type = 0; ++count }) {
                Text("with color")
            }
            Button({ type = 1; ++count }) {
                Text("brush gradient", fontSize = 12.sp)
            }
            Button({ type = 2; ++count }) {
                Text("brush solid")
            }
            Button({ type = 3; ++count }) {
                Text("with image")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlignmentAndContentScaleSamples() {
    var modifierAlpha by remember { mutableStateOf(false) }
    var imageAlpha by remember { mutableStateOf(false) }
    var rotate by remember { mutableStateOf(0) }
    var border by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf(true) }
    var tint by remember { mutableStateOf<ColorFilter?>(null) }
    FlowRow {
        Button({ modifierAlpha = !modifierAlpha }) {
            Text("Modifier.alpha")
        }
        Button({ imageAlpha = !imageAlpha }) {
            Text("Image.alpha")
        }
        Button({ rotate = (rotate + 45) % 360 }) {
            Text("Rotate ${rotate}°")
        }
        Button({ border = !border }) {
            Text("Border")
        }
        Button({ background = !background }) {
            Text("Background")
        }
        Button({ tint = if (tint != null) null else ColorFilter.tint(Color.Yellow) }) {
            Text("tintColor")
        }
    }

    Row {
        val imageModifier = Modifier.padding(bottom = 10.dp).size(50.dp)
            .then(if (modifierAlpha) Modifier.alpha(0.5f) else Modifier)
            .then(if (rotate != 0) Modifier.rotate(rotate.toFloat()) else Modifier)
            .border(if (border) 1.dp else 0.dp, Color.Red)
            .background(if (background) Color.LightGray else Color.Transparent)

        val largePainter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png")
        val smallPainter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png")

        Column {
            Spacer(Modifier.height(20.dp))
            Text("Crop", Modifier.height(60.dp))
            Text("Fit", Modifier.height(60.dp))
            Text("FillHeight", Modifier.height(60.dp))
            Text("FillWidth", Modifier.height(60.dp))
            Text("Inside", Modifier.height(60.dp))
            Text("None", Modifier.height(60.dp))
            Text("FillBounds", Modifier.height(60.dp))
        }

        Column(
            modifier = Modifier.width(90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Center", Modifier.height(20.dp))
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillHeight,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillWidth,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Inside,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.None,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillBounds,
                colorFilter = tint
            )
        }
        Column(
            modifier = Modifier.width(90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TopStart", Modifier.height(20.dp))
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Crop,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Fit,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillHeight,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillWidth,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Inside,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = largePainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillBounds,
                colorFilter = tint
            )
        }
        Column(
            modifier = Modifier.width(90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Center", Modifier.height(20.dp))
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillHeight,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillWidth,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.Inside,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.None,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillBounds,
                colorFilter = tint
            )
        }
        Column(
            modifier = Modifier.width(90.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TopStart", Modifier.height(20.dp))
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Crop,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Fit,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillHeight,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillWidth,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.Inside,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None,
                colorFilter = tint
            )
            Image(
                modifier = imageModifier,
                painter = smallPainter,
                contentDescription = null,
                alpha = if (imageAlpha) 0.5f else 1f,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.FillBounds,
                colorFilter = tint
            )
        }
    }
}

@Composable
private fun ImageLayout() {
    // 分割线与引导语保持一样的宽度
    var leadWidth by remember { mutableStateOf(0F) }

    Column(
        modifier = Modifier.offset(y = 50.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var t by remember { mutableStateOf(0) }
        Row(
            modifier = Modifier.background(Color.Gray)
                .onSizeChanged { leadWidth = it.width.toFloat() }) {
            var imageWidth by remember { mutableStateOf(0f) }
            val painter = rememberAsyncImagePainter(
                "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png?t=$t",
                onState = { state ->
                    imageWidth = if (state is AsyncImagePainter.State.Success) {
                        state.painter.intrinsicSize.let { it.width / it.height * 100 }
                    } else {
                        0f
                    }
                }
            )
            Image(
                painter,
                contentDescription = null,
                modifier = Modifier.height(100.dp).width(imageWidth.dp).clickable {
                    ++t
                },
                contentScale = ContentScale.FillHeight
            )
        }
        Box(
            modifier = Modifier.size(100.dp).background(Color.Red).clickable {
                ++t
            }
        )
    }
}
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTransformGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.wrapContentSize
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.InlineTextContent
import com.tencent.kuikly.compose.foundation.text.appendInlineContent
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Checkbox
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.draw.scale
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.Placeholder
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("DensityScaleDemo")
class DensityScaleDemoPager : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            DensityScaleDemo()
        }
    }
}

@Composable
fun DensityScaleDemo() {
    Column(modifier = Modifier.padding(top = 50.dp, start = 30.dp)) {
        val localDensity = LocalDensity.current.density

        Text("固定Density: ${localDensity}")
        Column(
            modifier = Modifier.size(320.dp).background(Color.Yellow),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(2f, fontScale = 1f)
            ) {
                testContent()
            }
        }
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            var density by remember { mutableStateOf(1f) }
            Text("当前 Density: ${density}")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Button(
                    onClick = { if (density > 0.5f) density -= 0.1f },
                    enabled = density > 0.5f
                ) {
                    Text("-")
                }
                Text(
                    "${density}",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Button(
                    onClick = { if (density < 8f) density += 0.1f },
                    enabled = density < 8f
                ) {
                    Text("+")
                }
                Button(
                    onClick = { density = 2f },
                ) {
                    Text("=2")
                }
                Button(
                    onClick = { density = 1f },
                ) {
                    Text("=1")
                }
            }
            Box(modifier = Modifier.size(320.dp)) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density, fontScale = 1f)
                ) {
                    testContent()
                }
            }
        }
    }
}

@Composable
private fun testContent() {

    val density = LocalDensity.current

    LaunchedEffect(density) {
        println("testContent density change " + density.density)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red)
    ) {
        MultipleImagesTest()
        TextFieldBasicTest()
        Row {
            Column(modifier = Modifier.width(150.dp)) {
                Row {
                    Text("Column", style = TextStyle(
                        letterSpacing = 10.sp,
                    ))
                    Checkbox(checked = true, onCheckedChange = {})
                }
                Text("Column2", style = TextStyle(
                    lineHeight = 50.sp,
                ), modifier = Modifier.background(Color.Yellow))
                Text("Column3")
            }


            // 3. 缩放和旋转示例
            var scale by remember { mutableStateOf(1f) }
            var rotation by remember { mutableStateOf(0f) }

            Column {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.Green)
                        .scale(scale)
                        .rotate(rotation)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rot ->
                                scale *= zoom
                                rotation += rot

                                println("detectTransformGestures - zoom: $scale, rot: $rotation")
                            }
                        }
                ) {
                    Text("缩放和旋转", color = Color.White)
                }
            }
        }

        CustomSnackbar()
        Row {
            LazyColumn {
                items(20) {
                    Row {
                        Text("LazyColumn${it}")
                        Switch(false, onCheckedChange = {

                        })
                    }
                }
            }
            LazyColumn {
                item {
                    Box(modifier = Modifier.size(500.dp)) {
                        ComposeVideoDemoImpl()
                    }
                }
            }
        }
    }
}

@Composable
fun MultipleImagesTest() {
    val inlineContent = mapOf(
        "avatar" to InlineTextContent(
            Placeholder(30.sp, 30.sp)
        ) {
            Image(
                rememberAsyncImagePainter("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg"),
                contentDescription = null,
                modifier = Modifier.size(30.dp, 30.dp)
            )
        },
        "emoji" to InlineTextContent(
            Placeholder(20.sp, 20.sp)
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(Color.Yellow)
            )
        }
    )

    var tailText by remember { mutableStateOf("") }

    Text(
        buildAnnotatedString {
            append("用户")
            appendInlineContent("avatar")
            append("发送")
            append(tailText)
            appendInlineContent("emoji")
            append("表情")
        },
        inlineContent = inlineContent,
        modifier = Modifier.background(Color.LightGray).clickable {
            tailText += "12"
        },
        onTextLayout = {
            println("ScaleOnTextLayout " + it.size)
            it.placeholderRects.forEach {
                println("ScaleOnTextLayout placeHolderRect $it")
            }
        }
    )
}

@Composable
fun TextFieldBasicTest() {
    Text(text = "0 BasicTextField Box居中 红色游标 最长5个中文字符")
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .border(1.dp, color = Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.border(1.dp, color = Color.Red)) {
            var text2 by remember { mutableStateOf("") }
            BasicTextField(
                cursorBrush = SolidColor(Color.Red),
                value = text2,
                onValueChange = {
                    text2 = it
                },
                textStyle = TextStyle(
                    lineHeight = 20.sp,
                ),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
            Box(modifier = Modifier.size(50.dp).clickable {
                text2 = ""
            })
        }
    }
}
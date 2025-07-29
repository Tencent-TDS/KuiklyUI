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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.text.InlineTextContent
import com.tencent.kuikly.compose.foundation.text.appendInlineContent
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Shadow
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.LinkAnnotation
import com.tencent.kuikly.compose.ui.text.LinkInteractionListener
import com.tencent.kuikly.compose.ui.text.Placeholder
import com.tencent.kuikly.compose.ui.text.SpanStyle
import com.tencent.kuikly.compose.ui.text.TextLinkStyles
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.buildAnnotatedString
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextDecoration
import com.tencent.kuikly.compose.ui.text.style.TextIndent
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.text.withLink
import com.tencent.kuikly.compose.ui.text.withStyle
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TextDemo")
class TextDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                ) {
                    item {
                        composeTextDemo()
                    }
                    item {
                        composeAnnotatedTextDemo()
                    }
                    item {
                        composeOnLayoutChangeDemo()
                    }
                    item {
                        composeTextShadowDemo()
                    }
                    item {
                        LinkTextDemo()
                    }
                    item {
                        InlineContentTests()
                    }
                    item {
                        BrushTextDemo()
                    }
                    item {
                        TextIndentDemo()
                    }
                }
            }
        }
    }

    @Composable
    fun LinkTextDemo() {
        // 定义链接文本的样式
        val linkStyle =
            TextLinkStyles(
                style =
                    SpanStyle(
                        color = Color.Blue,
                        fontWeight = FontWeight.Medium,
                    ),
            )

        // 创建带注解的文本
        val annotatedString =
            buildAnnotatedString {
                append("我已阅读并同意")

                // 用户协议链接
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "agreement",
                        styles = linkStyle,
                        linkInteractionListener =
                            LinkInteractionListener {
                                println("click 用户协议")
                            },
                    ),
                ) {
                    append("《用户协议》")
                }

                append("和")

                // 隐私政策链接
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "privacy",
                        styles = linkStyle,
                        linkInteractionListener =
                            LinkInteractionListener {
                                println("click 隐私政策")
                            },
                    ),
                ) {
                    append("《隐私政策》")
                }
            }

        // 显示文本
        Text(
            text = annotatedString,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .alpha(0.65f),
        )
    }

    @Composable
    fun composeOnLayoutChangeDemo() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. 基本布局信息测试
            var layoutInfo by remember { mutableStateOf("等待布局...") }
            Text(
                "这是一段测试文本，用于获取基本布局信息。",
                modifier = Modifier.background(Color.LightGray),
                onTextLayout = { result ->
                    layoutInfo = "文本大小: ${result.size.width}x${result.size.height}"
                },
            )
            Text("布局信息: $layoutInfo")

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 多行文本布局测试
            var multilineInfo by remember { mutableStateOf("等待布局...") }
            Text(
                "这是一段很长的测试文本，我们将它限制在两行内显示，并获取其布局信息。这段文本肯定会超过两行，所以我们可以测试截断的情况。",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().background(Color.Yellow),
                onTextLayout = { result ->
                    multilineInfo =
                        buildString {
                            append("大小: ${result.size.width}x${result.size.height}, ")
                            // append("第一行基线: ${result.firstBaseline}, ")
                            // append("最后行基线: ${result.lastBaseline}, ")
                            // append("是否溢出: ${result.hasVisualOverflow}")
                        }
                },
            )
            Text("多行布局信息: $multilineInfo")

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 动态布局测试
            var width by remember { mutableStateOf(200.dp) }
            var fontSize by remember { mutableStateOf(14.sp) }
            var maxLines by remember { mutableStateOf(2) }
            var widthLayoutInfo by remember { mutableStateOf("等待布局...") }

            Column(
                modifier = Modifier.fillMaxWidth().background(Color.LightGray).padding(8.dp),
            ) {
                Text(
                    """这是一段用于测试动态布局变化的文本。
                    |我们可以通过下面的按钮来调整文本的各种属性，
                    |比如宽度、字体大小、行数等，观察布局的变化情况。
                    |这段文本足够长，可以测试多行效果。
                    """.trimMargin(),
                    modifier =
                        Modifier
                            .width(width)
                            .background(Color.Green)
                            .padding(4.dp),
                    fontSize = fontSize,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        println(" onTextLayout")
                        widthLayoutInfo =
                            buildString {
                                append("宽度: ${width.value}dp\n")
                                append("大小: ${result.size.width}x${result.size.height}\n")
                                append("字号: ${fontSize.value}sp\n")
                                append("最大行数: $maxLines")
                            }
                    },
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 显示布局信息
                Text(
                    widthLayoutInfo,
                    fontSize = 12.sp,
                    modifier = Modifier.background(Color.Yellow).padding(4.dp),
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 控制按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 宽度调整按钮
                    Text(
                        "宽度: ${width.value}dp",
                        modifier =
                            Modifier
                                .background(Color.Blue)
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        width = (width + dragAmount.dp).coerceIn(100.dp, 400.dp)
                                    }
                                },
                    )

                    // 字号调整按钮
                    Text(
                        "字号: ${fontSize.value}sp",
                        modifier =
                            Modifier
                                .background(Color.Red)
                                .padding(8.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            fontSize = (fontSize.value + 1).sp
                                        },
                                        onDoubleTap = {
                                            fontSize = (fontSize.value - 1).sp
                                        },
                                    )
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        // 将拖动距离转换为字体大小的变化（每1像素改变0.1sp）
                                        val newSize = (fontSize.value + dragAmount * 0.1f).coerceIn(10f, 30f)
                                        fontSize = newSize.sp
                                    }
                                },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 行数控制按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(1, 2, 3, 4).forEach { lines ->
                        Text(
                            "$lines 行",
                            modifier =
                                Modifier
                                    .background(if (maxLines == lines) Color.Green else Color.Gray)
                                    .padding(8.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            maxLines = lines
                                        }
                                    },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 快速预设组合
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    data class Preset(
                        val name: String,
                        val width: Dp,
                        val fontSize: TextUnit,
                        val lines: Int,
                    )

                    val presets =
                        listOf(
                            Preset("小屏", 150.dp, 12.sp, 2),
                            Preset("中屏", 250.dp, 16.sp, 3),
                            Preset("大屏", 350.dp, 20.sp, 4),
                        )

                    presets.forEach { preset ->
                        Text(
                            preset.name,
                            modifier =
                                Modifier
                                    .background(Color.Magenta)
                                    .padding(8.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            width = preset.width
                                            fontSize = preset.fontSize
                                            maxLines = preset.lines
                                        }
                                    },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun composeTextDemo() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. 默认情况：softWrap = true, overflow = Clip
            Text(
                "默认情况（softWrap = true, overflow = Clip）：这是一段很长的文本，用来测试文本的自动换行和截断功能。这是一段很长的文本，用来测试文本的自动换行和截断功能。",
                modifier = Modifier.background(Color.Yellow).width(200.dp),
            )

            // 2. softWrap = false：文本强制单行
            Text(
                "单行文本（softWrap = false）：这是一段很长的文本，用来测试文本的自动换行和截断功能。",
                softWrap = false,
                modifier = Modifier.background(Color.LightGray).width(200.dp),
            )

            // 3. softWrap = true, overflow = Ellipsis, maxLines = 2
            Text(
                "两行省略（softWrap = true, overflow = Ellipsis, maxLines = 2）：这是一段很长的文本，用来测试文本的自动换行和截断功能。这是一段很长的文本，用来测试文本的自动换行和截断功能。",
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
                modifier = Modifier.background(Color.Green).width(200.dp),
            )

            // 4. softWrap = true, overflow = Clip, maxLines = 3
            Text(
                "三行截断（softWrap = true, overflow = Clip, maxLines = 3）：这是一段很长的文本，用来测试文本的自动换行和截断功能。这是一段很长的文本，用来测试文本的自动换行和截断功能。",
                overflow = TextOverflow.Clip,
                maxLines = 3,
                modifier = Modifier.background(Color.Blue).width(200.dp),
            )

            // 5. 短文本测试
            Text(
                "短文本测试",
                softWrap = false,
                modifier = Modifier.background(Color.Yellow).width(200.dp),
            )
        }
    }

    @Composable
    fun composeAnnotatedTextDemo() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. 基本的颜色和字体大小测试
            Text(
                buildAnnotatedString {
                    append("默认文本 ")
                    withStyle(
                        SpanStyle(
                            color = Color.Red,
                            fontSize = 20.sp,
                        ),
                    ) {
                        append("红色大号文本")
                    }
                    append(" 默认文本")
                },
                modifier = Modifier.background(Color.Yellow),
            )

            // 2. 字体粗细和样式测试
            Text(
                buildAnnotatedString {
                    append("普通文本 ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        ),
                    ) {
                        append("粗体文本 ")
                    }
                    withStyle(
                        SpanStyle(
                            fontStyle = FontStyle.Italic,
                            fontSize = 18.sp,
                        ),
                    ) {
                        append("斜体文本")
                    }
                },
            )

            // 3. 背景色和下划线测试
            Text(
                buildAnnotatedString {
                    append("开始文本 ")
                    withStyle(
                        SpanStyle(
                            background = Color.LightGray,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append("带背景和下划线")
                    }
                    append(" 结束文本")
                },
            )

            // 4. 多重样式嵌套测试
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 16.sp)) {
                        append("大号文本 ")
                        withStyle(
                            SpanStyle(
                                color = Color.Blue,
                                background = Color.Yellow,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append("蓝色粗体")
                        }
                        append(" 继续大号 ")
                        withStyle(
                            SpanStyle(
                                color = Color.Red,
                                textDecoration = TextDecoration.LineThrough,
                            ),
                        ) {
                            append("红色删除线")
                        }
                    }
                },
            )

            // 5. 字母间距测试
            Text(
                buildAnnotatedString {
                    append("正常间距 ")
                    withStyle(
                        SpanStyle(
                            letterSpacing = 8.sp,
                            fontSize = 16.sp,
                        ),
                    ) {
                        append("加宽间距")
                    }
                    append(" 正常间距")
                },
            )
        }
    }

    @Composable
    fun composeTextShadowDemo() {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "文本阴影效果演示",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            // 1. 基本阴影效果
            Text(
                "基本阴影效果",
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
                style =
                    TextStyle(
                        shadow =
                            Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 3f,
                            ),
                    ),
            )

            // 2. 大偏移阴影
            Text(
                "大偏移阴影",
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
                style =
                    TextStyle(
                        shadow =
                            Shadow(
                                color = Color.Red,
                                offset = Offset(5f, 5f),
                                blurRadius = 0f,
                            ),
                    ),
            )

            // 3. 模糊阴影
            Text(
                "模糊阴影",
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
                style =
                    TextStyle(
                        shadow =
                            Shadow(
                                color = Color.Blue,
                                offset = Offset(0f, 0f),
                                blurRadius = 8f,
                            ),
                    ),
            )

            // 4. 彩色阴影
            Text(
                "彩色阴影",
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
                style =
                    TextStyle(
                        shadow =
                            Shadow(
                                color = Color(0xFF00FF00),
                                offset = Offset(3f, 3f),
                                blurRadius = 4f,
                            ),
                    ),
            )

            // 5. 多阴影叠加效果（通过AnnotatedString实现）
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            shadow =
                                Shadow(
                                    color = Color.Red,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 2f,
                                ),
                        ),
                    ) {
                        append("红色")
                    }
                    append(" ")
                    withStyle(
                        SpanStyle(
                            shadow =
                                Shadow(
                                    color = Color.Green,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 2f,
                                ),
                        ),
                    ) {
                        append("绿色")
                    }
                    append(" ")
                    withStyle(
                        SpanStyle(
                            shadow =
                                Shadow(
                                    color = Color.Blue,
                                    offset = Offset(2f, 2f),
                                    blurRadius = 2f,
                                ),
                        ),
                    ) {
                        append("蓝色")
                    }
                    append(" 阴影效果")
                },
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
            )

            // 6. 无阴影（重置阴影）
            Text(
                "无阴影文本",
                fontSize = 20.sp,
                modifier = Modifier.background(Color.White).padding(8.dp),
                style =
                    TextStyle(
                        shadow = Shadow.None,
                    ),
            )
        }
    }

    @Composable
    fun InlineContentTests() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. 基本用例：placeholder 在文本中间
            BasicInlineContentTest()

            // 2. 边界情况：placeholder 在文本开头
            PlaceholderAtStartTest()

            // 3. 边界情况：placeholder 和 span 起始位置相同
            PlaceholderWithSpanTest()

            // 4. 多个 placeholder 测试
            MultiplePlaceholdersTest()

            // 5. placeholder 和样式混合测试
            PlaceholderWithStyleTest()

            // 6. 不同尺寸的 placeholder 测试
            DifferentSizePlaceholderTest()

            // 8. 图片 inline content 测试
            ImageInlineContentTest()

            // 9. 多图片混排测试
            MultipleImagesTest()
        }
    }

    @Composable
    private fun BasicInlineContentTest() {
        val inlineContent =
            mapOf(
                "icon" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Red))
                    },
            )

        Text(
            buildAnnotatedString {
                append("这是一段带有")
                appendInlineContent("icon")
                append("图标的文本")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun PlaceholderAtStartTest() {
        val inlineContent =
            mapOf(
                "icon" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Blue))
                    },
            )

        Text(
            buildAnnotatedString {
                appendInlineContent("icon")
                append("图标在开头的文本")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun PlaceholderWithSpanTest() {
        val inlineContent =
            mapOf(
                "icon" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Green))
                    },
            )

        Text(
            buildAnnotatedString {
                append("前缀")
                pushStyle(SpanStyle(color = Color.Red, fontSize = 20.sp))
                appendInlineContent("icon") // 这里 placeholder 和 span 的起始位置相同
                append("这段文字是红色的")
                pop()
                append("后缀")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun MultiplePlaceholdersTest() {
        val inlineContent =
            mapOf(
                "red" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Red))
                    },
                "blue" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Blue))
                    },
            )

        Text(
            buildAnnotatedString {
                appendInlineContent("red")
                append("中间")
                appendInlineContent("blue")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun PlaceholderWithStyleTest() {
        val inlineContent =
            mapOf(
                "icon" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Red))
                    },
            )

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 12.sp)) {
                    append("小字体")
                    appendInlineContent("icon")
                }
                withStyle(SpanStyle(fontSize = 20.sp)) {
                    append("大字体")
                    appendInlineContent("icon")
                }
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun DifferentSizePlaceholderTest() {
        val inlineContent =
            mapOf(
                "small" to
                    InlineTextContent(
                        Placeholder(12.sp, 12.sp),
                    ) {
                        Box(Modifier.size(12.dp).background(Color.Red))
                    },
                "medium" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(Modifier.size(20.dp).background(Color.Green))
                    },
                "large" to
                    InlineTextContent(
                        Placeholder(30.sp, 30.sp),
                    ) {
                        Box(Modifier.size(30.dp).background(Color.Blue))
                    },
            )

        Text(
            buildAnnotatedString {
                append("小图标")
                appendInlineContent("small")
                append("中图标")
                appendInlineContent("medium")
                append("大图标")
                appendInlineContent("large")
                append("文本结束")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun ImageInlineContentTest() {
        val inlineContent =
            mapOf(
                "image" to
                    InlineTextContent(
                        Placeholder(40.sp, 40.sp),
                    ) {
                        Image(
                            rememberAsyncImagePainter("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif"),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp, 40.dp).border(2.dp, Color.Red),
                            contentScale = ContentScale.Crop,
                        )
                    },
            )

        Text(
            buildAnnotatedString {
                append("这是一段带有")
                appendInlineContent("image")
                append("图片的文本。")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    private fun MultipleImagesTest() {
        val inlineContent =
            mapOf(
                "avatar" to
                    InlineTextContent(
                        Placeholder(30.sp, 30.sp),
                    ) {
                        Image(
                            rememberAsyncImagePainter("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif"),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp, 30.dp),
                        )
                    },
                "emoji" to
                    InlineTextContent(
                        Placeholder(20.sp, 20.sp),
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .background(Color.Yellow),
                        )
                    },
            )

        Text(
            buildAnnotatedString {
                append("用户")
                appendInlineContent("avatar")
                append("发送了一条消息")
                appendInlineContent("emoji")
                append("表情")
            },
            inlineContent = inlineContent,
            modifier = Modifier.background(Color.LightGray),
        )
    }

    @Composable
    fun BrushTextDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 1. 水平渐变文本
            Text(
                text = "水平渐变文本效果",
                style =
                    TextStyle(
                        brush =
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Blue,
                                        Color.Magenta,
                                    ),
                            ),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            // 2. 垂直渐变文本
            Text(
                text = "垂直渐变文本效果",
                style =
                    TextStyle(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Blue,
                                        Color.Magenta,
                                    ),
                            ),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            // 3. 使用 SpanStyle 的渐变色文本
            Text(
                buildAnnotatedString {
                    // 第一段：水平渐变
                    withStyle(
                        SpanStyle(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Blue,
                                            Color.Magenta,
                                        ),
                                ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append("渐变色")
                    }

                    append(" ")

                    // 第二段：垂直渐变
                    withStyle(
                        SpanStyle(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Blue,
                                            Color.Magenta,
                                        ),
                                ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append("文本")
                    }

                    append(" ")

                    // 第三段：自定义角度渐变
                    withStyle(
                        SpanStyle(
                            brush = SolidColor(Color(0xFF673AB7)),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append("效果")
                    }
                },
            )

            // 4. 纯色文本
            Text(
                text = "纯色文本效果",
                style =
                    TextStyle(
                        color = Color(0xFF673AB7), // Material Deep Purple
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        shadow =
                            Shadow(
                                color = Color(0x33000000),
                                offset = Offset(2f, 2f),
                                blurRadius = 3f,
                            ),
                    ),
            )
        }
    }

    @Composable
    fun TextIndentDemo() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "文本首行缩进效果演示",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 1. 基本首行缩进效果
            Text(
                "【基本缩进】这是一段测试首行缩进效果的文本。首行应该有 16dp 的缩进距离，而后续行则保持正常的左对齐。这样可以看到明显的首行缩进效果。",
                style = TextStyle(
                    textIndent = TextIndent(firstLine = 16.sp),
                    fontSize = 16.sp
                ),
                modifier = Modifier.background(Color.LightGray).padding(8.dp).width(300.dp)
            )

            // 2. 不同缩进距离对比
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8, 16, 24, 32).forEach { indent ->
                    Text(
                        "【${indent}sp缩进】这是一段测试不同缩进距离的文本。当前首行缩进为 ${indent}sp，可以观察不同缩进距离的视觉效果差异。",
                        style = TextStyle(
                            textIndent = TextIndent(firstLine = indent.sp),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.background(Color(0xFFE3F2FD)).padding(8.dp).width(280.dp)
                    )
                }
            }

            // 3. 与其他样式组合使用
            Text(
                "【组合样式】这是一段同时使用首行缩进、字体加粗、颜色和阴影效果的文本。可以看到各种样式效果能够很好地组合在一起使用。",
                style = TextStyle(
                    textIndent = TextIndent(firstLine = 20.sp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    shadow = Shadow(
                        color = Color(0x33000000),
                        offset = Offset(1f, 1f),
                        blurRadius = 2f
                    )
                ),
                modifier = Modifier.background(Color.White).padding(8.dp).width(320.dp)
            )

            // 4. AnnotatedString 中的段落缩进
            Text(
                buildAnnotatedString {
                    append("【富文本缩进】这是第一段文本，展示了 AnnotatedString 中的段落缩进效果。这段文本应该有首行缩进。\n\n")

                    withStyle(SpanStyle(color = Color.Red, fontSize = 18.sp)) {
                        append("这是第二段红色文本，")
                    }
                    append("同样具有首行缩进效果。可以看到不同样式的文本都能正确应用段落缩进。")
                },
                style = TextStyle(
                    textIndent = TextIndent(firstLine = 24.sp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.background(Color(0xFFF3E5F5)).padding(8.dp).width(300.dp)
            )

            // 5. 动态切换缩进效果
            var currentIndent by remember { mutableStateOf(0.sp) }
            var showIndent by remember { mutableStateOf(true) }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "【动态缩进测试】这是一段用来测试动态切换缩进效果的文本。通过点击下面的按钮可以动态调整首行缩进的大小，或者完全取消缩进效果。",
                    style = TextStyle(
                        textIndent = if (showIndent) TextIndent(firstLine = currentIndent) else null,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.background(Color(0xFFE8F5E8)).padding(8.dp).width(300.dp)
                )

                // 控制按钮行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0, 8, 16, 24).forEach { indent ->
                        Text(
                            "${indent}sp",
                            modifier = Modifier
                                .background(
                                    if (currentIndent.value == indent.toFloat() && showIndent)
                                        Color.Green
                                    else
                                        Color.Gray
                                )
                                .padding(8.dp)
                                .clickable {
                                    currentIndent = indent.sp
                                    showIndent = true
                                },
                            color = Color.White
                        )
                    }

                    Text(
                        "无缩进",
                        modifier = Modifier
                            .background(if (!showIndent) Color.Red else Color.Gray)
                            .padding(8.dp)
                            .clickable {
                                showIndent = false
                            },
                        color = Color.White
                    )
                }

                Text(
                    "当前状态: ${if (showIndent) "缩进 ${currentIndent.value}sp" else "无缩进"}",
                    fontSize = 12.sp,
                    color = Color.Blue
                )
            }

            // 6. 长文本缩进效果
            Text(
                "【长文本缩进】这是一段比较长的文本，用来展示在多行文本中首行缩进的效果。" +
                        "只有第一行会有缩进，后续的所有行都会保持正常的左对齐。" +
                        "这样的效果在传统的印刷排版中非常常见，特别是在书籍和报纸的段落排版中。" +
                        "通过合理的首行缩进，可以让段落的开始更加明显，提升文本的可读性和美观性。",
                style = TextStyle(
                    textIndent = TextIndent(firstLine = 32.sp),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.background(Color(0xFFFFF3E0)).padding(12.dp).width(350.dp)
            )
        }
    }
}

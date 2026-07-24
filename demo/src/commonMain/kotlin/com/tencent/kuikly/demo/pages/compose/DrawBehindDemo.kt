@file:OptIn(InternalResourceApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
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
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.material3.Checkbox
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.drawBehind
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.PathEffect
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri
import kotlin.LazyThreadSafetyMode

@Page("DrawBehindDemo")
class DrawBehindDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            DrawBehindScreen()
        }
    }
}

private val penguin by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("penguin2.png").toUrl(""))
}

@Composable
private fun DrawBehindScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = 80.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = "drawBehind + pathEffect 虚线能力验证",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "本页面验证：本次补齐的 drawBehind 与 pathEffect 虚线能力，附着于不同类型组件时是否可稳定生效。",
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )
            Spacer(Modifier.height(24.dp))
        }

        item {
            SectionTitle("一、展示组件")
            Spacer(Modifier.height(8.dp))
        }
        item {
            ScenarioTitle("1. Text：底部虚线")
            ScenarioSummary(
                verify = "Text 直接挂 drawBehind 后，底部虚线是否正常显示。",
                result = "可以；说明 drawBehind 与 pathEffect 虚线能力在普通文本场景已稳定生效。"
            )
            TextDashUnderlineSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("2. Box：虚线边框")
            ScenarioSummary(
                verify = "Box 挂 drawBehind 后，四边虚线边框是否能稳定显示。",
                result = "可以；说明虚线能力不局限于文字，也能挂到容器组件。"
            )
            BoxFrameSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("3. Image：虚线底线")
            ScenarioSummary(
                verify = "Image 挂 drawBehind 后，底部虚线是否能稳定显示。",
                result = "可以；说明虚线能力也能挂到图片组件。"
            )
            ImageAccentSample()
            Spacer(Modifier.height(32.dp))
        }

        item {
            SectionTitle("二、布局容器")
            Spacer(Modifier.height(8.dp))
        }
        item {
            ScenarioTitle("4. Row：整行虚线分隔")
            ScenarioSummary(
                verify = "Row 挂 drawBehind 后，底部虚线是否按容器宽度完整绘制。",
                result = "可以；说明虚线能力可以直接挂在横向布局容器上。"
            )
            RowDividerSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("5. Column：右侧虚线边线")
            ScenarioSummary(
                verify = "Column 挂 drawBehind 后，右侧虚线边线是否能随容器高度完整显示。",
                result = "可以；说明虚线能力对纵向容器同样生效。"
            )
            ColumnBorderSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("6. 列表项容器：底部虚线分隔")
            ScenarioSummary(
                verify = "列表项外层容器挂 drawBehind 后，是否能稳定绘制底部虚线分隔线。",
                result = "可以；说明虚线能力能直接用于列表项装饰。"
            )
            ListItemDividerSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("7. LazyColumn：宿主虚线边框")
            ScenarioSummary(
                verify = "drawBehind 直接挂到 LazyColumn 本体后，列表宿主外框虚线是否稳定显示。",
                result = "可以；说明这次对齐的不只是列表项容器，LazyColumn 宿主本身也能直接派发虚线绘制。"
            )
            LazyColumnDrawBehindSample()
            Spacer(Modifier.height(32.dp))
        }

        item {
            SectionTitle("三、输入组件")
            Spacer(Modifier.height(8.dp))
        }
        item {
            ScenarioTitle("8. BasicTextField：底部虚线")
            ScenarioSummary(
                verify = "BasicTextField 直接挂 drawBehind 后，底部虚线是否直接可见。",
                result = "可以；说明 drawBehind + pathEffect 能挂到最基础的输入组件。"
            )
            BasicTextFieldDrawBehindSample()
            Spacer(Modifier.height(24.dp))
        }
        item {
            ScenarioTitle("9. TextField：底部虚线")
            ScenarioSummary(
                verify = "TextField 挂 drawBehind 后，隐藏默认装饰时底部虚线是否可见。",
                result = "可以；说明 drawBehind + pathEffect 也能用于带 Material 外观的输入组件。"
            )
            TextFieldDrawBehindSample()
            Spacer(Modifier.height(32.dp))
        }

        item {
            SectionTitle("四、复杂控件")
            Spacer(Modifier.height(8.dp))
        }
        item {
            ScenarioTitle("10. Switch / Slider / Checkbox：底部虚线")
            ScenarioSummary(
                verify = "这三个控件直接挂 drawBehind 后，底部虚线是否都能正常显示。",
                result = "Switch 和 Slider 可以；Checkbox 不行，原因是它内部走 Canvas + requiredSize(20dp) 的特殊宿主路径，底部虚线会被裁到贴边。"
            )
            ControlsDrawBehindSample()
            Spacer(Modifier.height(32.dp))
        }

        item {
            SectionTitle("五、空内容节点")
            Spacer(Modifier.height(8.dp))
        }
        item {
            ScenarioTitle("11. Spacer：分隔线")
            ScenarioSummary(
                verify = "没有内容的 Spacer 挂 drawBehind 后，是否也能独立派发绘制。",
                result = "可以；说明 drawBehind 不要求宿主本身必须有文字或图片。"
            )
            SpacerDrawBehindSample()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun ScenarioTitle(text: String) {
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF666666))
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ScenarioSummary(verify: String, result: String) {
    Text(text = "验证：$verify", fontSize = 12.sp, color = Color(0xFF666666))
    Text(text = "结果：$result", fontSize = 12.sp, color = Color(0xFF666666))
    Spacer(Modifier.height(8.dp))
}

private fun Modifier.magentaDashUnderline(): Modifier = drawBehind {
    val strokeWidth = 2.dp.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = Color(0xFFE91E63),
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = strokeWidth,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
    )
}

@Composable
private fun TextDashUnderlineSample() {
    Text(
        text = "这段文字下方有一条洋红色虚线",
        modifier = Modifier.magentaDashUnderline(),
    )
}

@Composable
private fun BoxFrameSample() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                drawRect(color = Color(0x1A2196F3))
                drawLine(Color(0xFF2196F3), Offset(0f, inset), Offset(size.width, inset), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF2196F3), Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF2196F3), Offset(inset, 0f), Offset(inset, size.height), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF2196F3), Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), strokeWidth, pathEffect = dash)
            }
    ) {
        Text(text = "Box", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ImageAccentSample() {
    Image(
        painter = painterResource(penguin),
        contentDescription = null,
        modifier = Modifier
            .size(96.dp)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawRect(color = Color(0x1AFF9800))
                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            }
    )
}

@Composable
private fun RowDividerSample() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .magentaDashUnderline()
    ) {
        Text(text = "Row item 1")
        Spacer(Modifier.width(16.dp))
        Text(text = "Row item 2")
    }
}

@Composable
private fun ColumnBorderSample() {
    Column(
        modifier = Modifier.drawBehind {
            val strokeWidth = 2.dp.toPx()
            val x = size.width - strokeWidth / 2
            drawLine(
                color = Color(0xFF4CAF50),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
            )
        }
    ) {
        Text(text = "Column item 1")
        Text(text = "Column item 2")
    }
}

@Composable
private fun ListItemDividerSample() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Color(0xFF9C27B0),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            }
    ) {
        Text(
            text = "模拟列表项（底部虚线分隔）",
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun LazyColumnDrawBehindSample() {
    val listItems = listOf("Lazy item 1", "Lazy item 2", "Lazy item 3")
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2
                val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                drawLine(Color(0xFF9C27B0), Offset(0f, inset), Offset(size.width, inset), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF9C27B0), Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF9C27B0), Offset(inset, 0f), Offset(inset, size.height), strokeWidth, pathEffect = dash)
                drawLine(Color(0xFF9C27B0), Offset(size.width - inset, 0f), Offset(size.width - inset, size.height), strokeWidth, pathEffect = dash)
            }
    ) {
        items(listItems) { label ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = label)
            }
        }
    }
}

@Composable
private fun BasicTextFieldDrawBehindSample() {
    var text by remember { mutableStateOf("") }
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .size(280.dp, 56.dp)
            .drawBehind {
                drawRect(color = Color(0xFFF5F5F5))
                val strokeWidth = 2.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            }
    )
}

@Composable
private fun TextFieldDrawBehindSample() {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier
            .size(280.dp, 56.dp)
            .drawBehind {
                drawRect(color = Color(0xFFF5F5F5))
                val strokeWidth = 2.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            },
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        )
    )
}

@Composable
private fun ControlsDrawBehindSample() {
    var switchChecked by remember { mutableStateOf(true) }
    var checkboxChecked by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableStateOf(0.4f) }

    Text(text = "Switch", fontSize = 12.sp, color = Color.Gray)
    Switch(
        checked = switchChecked,
        onCheckedChange = { switchChecked = it },
        modifier = Modifier.magentaDashUnderline(),
    )
    Spacer(Modifier.height(8.dp))

    Text(text = "Slider", fontSize = 12.sp, color = Color.Gray)
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        modifier = Modifier
            .fillMaxWidth()
            .magentaDashUnderline(),
    )
    Spacer(Modifier.height(8.dp))

    Text(text = "Checkbox", fontSize = 12.sp, color = Color.Gray)
    Checkbox(
        checked = checkboxChecked,
        onCheckedChange = { checkboxChecked = it },
        modifier = Modifier.magentaDashUnderline(),
    )
}

@Composable
private fun SpacerDrawBehindSample() {
    Text(text = "上方内容", fontSize = 12.sp, color = Color.Gray)
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .drawBehind {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            }
    )
    Text(text = "下方内容", fontSize = 12.sp, color = Color.Gray)
}

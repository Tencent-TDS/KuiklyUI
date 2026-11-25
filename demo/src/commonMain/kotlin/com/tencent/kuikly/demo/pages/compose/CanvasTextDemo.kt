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
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.ExperimentalTextApi
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.drawText
import com.tencent.kuikly.compose.ui.text.drawTextAtBaseline
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.rememberTextMeasurer
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("CanvasTextDemo")
class CanvasTextDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            Column(
                Modifier.fillMaxSize().background(Color.White)
                    .padding(top = LocalActivity.current.pageData.statusBarHeight.dp)
                    .padding(20.dp)
            ) {
                Text("Canvas text", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Left: topLeft     Right: baseline", fontSize = 16.sp)
                Text("Blue: line box     Red: baseline", fontSize = 14.sp)
                BaselineSample("Agjp", TextStyle(fontSize = 16.sp))
                BaselineSample("Agjp", TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold))
                BaselineSample("中文 Ag", TextStyle(fontSize = 22.sp))
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun BaselineSample(label: String, style: TextStyle) {
    val measurer = rememberTextMeasurer()
    val layout = remember(measurer, label, style) { measurer.measure(label, style) }
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
        val top = 16.dp.toPx()
        val baseline = top + layout.firstBaseline
        val right = size.width / 2f
        val box = Size(layout.size.width.toFloat(), layout.size.height.toFloat())
        val border = Color(0xFF327BA8)
        drawRect(border, Offset(0f, top), box, style = Stroke(1.dp.toPx()))
        drawRect(border, Offset(right, top), box, style = Stroke(1.dp.toPx()))
        drawLine(Color(0xFFCF4B43), Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())
        drawText(layout, topLeft = Offset(0f, top))
        drawTextAtBaseline(label, origin = Offset(right, baseline), style = style)
    }
}

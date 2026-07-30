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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyColumnSizeDemo")
class LazyColumnSizeDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumnSizeDemoContent()
            }
        }
    }
}

@Composable
fun LazyColumnSizeDemoContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题
        Text(
            text = "LazyColumn 尺寸变化演示",
            fontSize = 24.sp,
            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 状态显示
        var lazyColumnWidth by remember { mutableStateOf(300.dp) }
        var lazyColumnHeight by remember { mutableStateOf(200.dp) }
        var isHeightZero by remember { mutableStateOf(false) }

        // 状态信息显示
        Text(
            text = "当前尺寸: 宽度=${lazyColumnWidth.value}dp, 高度=${if (isHeightZero) "0" else lazyColumnHeight.value}dp",
            fontSize = 14.sp,
        )

        // 高度变0和还原按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "高度变0",
                modifier = Modifier
                    .background(if (isHeightZero) Color.Green else Color.Gray)
                    .clickable {
                        isHeightZero = true
                    },
                color = Color.White
            )

            Text(
                text = "还原高度",
                modifier = Modifier
                    .background(if (!isHeightZero) Color.Green else Color.Gray)
                    .clickable {
                        isHeightZero = false
                    },
                color = Color.White
            )
        }

        // 单独更改宽度按钮
        Text(
            text = "单独更改宽度",
            fontSize = 16.sp,
            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(150, 200, 250, 300, 350, 400).forEach { width ->
                Text(
                    text = "${width}dp",
                    modifier = Modifier
                        .background(
                            if (lazyColumnWidth.value == width.toFloat()) Color.Blue else Color.LightGray
                        )
                        .clickable {
                            lazyColumnWidth = width.dp
                        },
                    color = if (lazyColumnWidth.value == width.toFloat()) Color.White else Color.Black
                )
            }
        }

        // 单独更改高度按钮
        Text(
            text = "单独更改高度",
            fontSize = 16.sp,
            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(100, 150, 200, 250, 300, 350).forEach { height ->
                Text(
                    text = "${height}dp",
                    modifier = Modifier
                        .background(
                            if (lazyColumnHeight.value == height.toFloat()) Color.Red else Color.LightGray
                        )

                        .clickable {
                            lazyColumnHeight = height.dp
                        },
                    color = if (lazyColumnHeight.value == height.toFloat()) Color.White else Color.Black
                )
            }
        }

        // 宽高一起改的预设按钮
        Text(
            text = "宽高一起改",
            fontSize = 16.sp,
            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data class SizePreset(
                val name: String,
                val width: Dp,
                val height: Dp
            )

            val presets = listOf(
                SizePreset("小", 200.dp, 150.dp),
                SizePreset("中", 300.dp, 200.dp),
                SizePreset("大", 400.dp, 300.dp),
                SizePreset("宽", 500.dp, 150.dp),
                SizePreset("高", 250.dp, 400.dp),
                SizePreset("方形", 300.dp, 300.dp)
            )

            presets.forEach { preset ->
                val isSelected =
                    lazyColumnWidth == preset.width && lazyColumnHeight == preset.height
                Text(
                    text = preset.name,
                    modifier = Modifier
                        .background(if (isSelected) Color.Magenta else Color.LightGray)

                        .clickable {
                            lazyColumnWidth = preset.width
                            lazyColumnHeight = preset.height
                            isHeightZero = false
                        },
                    color = if (isSelected) Color.White else Color.Black
                )
            }
        }

        // 重置按钮
        Text(
            text = "重置",
            modifier = Modifier
                .background(Color.Red)

                .clickable {
                    lazyColumnWidth = 300.dp
                    lazyColumnHeight = 200.dp
                    isHeightZero = false
                },
            color = Color.White
        )

        // 演示用的 LazyColumn
        Box(
            modifier = Modifier
                .background(Color(0xFFF5F5F5))

        ) {
            LazyColumn(
                modifier = Modifier
                    .width(lazyColumnWidth)
                    .height(if (isHeightZero) 0.dp else lazyColumnHeight)
                    .background(Color(0xFFE8F5E8)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(20) { index ->
                    Text(
                        text = "LazyColumn 项目 ${index + 1} LazyColumn 项目 ${index + 1} LazyColumn 项目 ${index + 1} LazyColumn 项目 ${index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when (index % 4) {
                                    0 -> Color(0xFFFFE0B2)
                                    1 -> Color(0xFFC8E6C9)
                                    2 -> Color(0xFFBBDEFB)
                                    else -> Color(0xFFF8BBD9)
                                }
                            ),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 说明文字
        Text(
            text = "说明：\n" +
                    "• 点击'高度变0'按钮可以将 LazyColumn 高度设为 0\n" +
                    "• 点击'还原高度'按钮恢复之前的高度\n" +
                    "• 单独更改宽度：只改变宽度，高度保持不变\n" +
                    "• 单独更改高度：只改变高度，宽度保持不变\n" +
                    "• 宽高一起改：同时改变宽度和高度\n" +
                    "• 重置：恢复到初始状态",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.background(Color(0xFFF9F9F9))
        )
    }
}

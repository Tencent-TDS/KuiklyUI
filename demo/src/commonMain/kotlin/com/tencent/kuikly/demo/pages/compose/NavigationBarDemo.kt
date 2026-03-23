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
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.NavigationBar
import com.tencent.kuikly.compose.material3.NavigationBarItem
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("NavigationBarDemo")
internal class NavigationBarDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("NavigationBar Demo") {
                NavigationBarDemoContent()
            }
        }
    }
}

@Composable
private fun NavigationBarDemoContent() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Search", "Profile")

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            // Simple icon using Canvas since we don't have material-icons in commonMain
                            SimpleIcon(index)
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Page content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = items[selectedItem],
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = "Selected tab: ${selectedItem + 1}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

/**
 * Simple icon composable using Canvas to draw basic shapes.
 * This avoids dependency on material-icons which may not be available in commonMain.
 */
@Composable
private fun SimpleIcon(index: Int) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokeWidth = 2.dp.toPx()
        val color = Color(0xFF333333)
        when (index) {
            0 -> {
                // Home icon - simple house shape
                val path = Path().apply {
                    moveTo(size.width / 2, strokeWidth)
                    lineTo(size.width - strokeWidth, size.height * 0.45f)
                    lineTo(size.width - strokeWidth, size.height - strokeWidth)
                    lineTo(strokeWidth, size.height - strokeWidth)
                    lineTo(strokeWidth, size.height * 0.45f)
                    close()
                }
                drawPath(path, color, style = Stroke(width = strokeWidth))
            }
            1 -> {
                // Search icon - circle + line
                val radius = size.width * 0.3f
                val center = Offset(size.width * 0.4f, size.height * 0.4f)
                drawCircle(color, radius, center, style = Stroke(width = strokeWidth))
                drawLine(
                    color,
                    start = Offset(center.x + radius * 0.7f, center.y + radius * 0.7f),
                    end = Offset(size.width - strokeWidth, size.height - strokeWidth),
                    strokeWidth = strokeWidth
                )
            }
            2 -> {
                // Profile icon - circle (head) + arc (body)
                val headRadius = size.width * 0.2f
                val headCenter = Offset(size.width / 2, size.height * 0.3f)
                drawCircle(color, headRadius, headCenter, style = Stroke(width = strokeWidth))
                val bodyPath = Path().apply {
                    moveTo(strokeWidth, size.height - strokeWidth)
                    quadraticBezierTo(
                        size.width / 2, size.height * 0.45f,
                        size.width - strokeWidth, size.height - strokeWidth
                    )
                }
                drawPath(bodyPath, color, style = Stroke(width = strokeWidth))
            }
        }
    }
}

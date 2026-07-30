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

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("BoxWithConstraintsDemo")
class BoxWithConstraintsDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            BoxWithConstraintsTest()
        }
    }

    @Composable
    fun BoxWithConstraintsTest() {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(Modifier.height(40.dp).fillMaxWidth())

                // 1. 基本使用 - 显示约束信息
                Text("1. 基本使用 - 显示约束信息:")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color.LightGray)
                        .padding(8.dp)
                ) {
                    // 在 BoxWithConstraintsScope 中先保存值到局部变量
                    val currentMaxWidth = maxWidth
                    val currentMaxHeight = maxHeight
                    val currentMinWidth = minWidth
                    val currentMinHeight = minHeight
                    val currentConstraints = constraints
                    
                    Column {
                        Text("maxWidth: $currentMaxWidth", color = Color.Black)
                        Text("maxHeight: $currentMaxHeight", color = Color.Black)
                        Text("minWidth: $currentMinWidth", color = Color.Black)
                        Text("minHeight: $currentMinHeight", color = Color.Black)
                        Text("constraints: ${currentConstraints.maxWidth}x${currentConstraints.maxHeight}", color = Color.Black)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            item {
                // 2. 响应式布局 - 根据宽度显示不同布局
                Text("2. 响应式布局 - 根据宽度显示不同布局:")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Yellow)
                        .padding(8.dp)
                ) {
                    // 保存约束值到局部变量
                    val currentMaxWidth = maxWidth
                    
                    if (currentMaxWidth < 300.dp) {
                        // 小屏幕：垂直布局
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(Color.Blue)
                            ) {
                                Text("图片区域", modifier = Modifier.align(Alignment.Center), color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("标题", color = Color.Black)
                            Text("描述内容 (垂直布局)", color = Color.Gray)
                        }
                    } else {
                        // 大屏幕：水平布局
                        Row {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.Blue)
                            ) {
                                Text("图片", modifier = Modifier.align(Alignment.Center), color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("标题", color = Color.Black)
                                Text("描述内容 (水平布局)", color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            item {
                // 3. 条件性显示组件
                Text("3. 条件性显示组件:")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.Green)
                        .padding(8.dp)
                ) {
                    // 保存约束值到局部变量
                    val currentMaxWidth = maxWidth
                    val currentMaxHeight = maxHeight
                    
                    Column {
                        Text("主要内容", color = Color.White)
                        
                        // 只有当高度足够时才显示额外内容
                        if (currentMaxHeight > 120.dp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("额外信息 (高度足够时显示)", color = Color.Yellow)
                        }
                        
                        // 只有当宽度足够时才显示侧边信息
                        if (currentMaxWidth > 300.dp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("宽度足够时的额外信息", color = Color.LightGray)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            item {
                // 4. 与普通 Box 的对比
                Text("4. 与普通 Box 的对比:")
                
                Text("普通 Box (无约束信息):", color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.LightGray)
                        .padding(8.dp)
                ) {
                    Text("普通 Box 无法获取约束信息", color = Color.Black)
                }
                
                Spacer(Modifier.height(8.dp))
                
                Text("BoxWithConstraints (有约束信息):", color = Color.Gray)
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Cyan)
                        .padding(8.dp)
                ) {
                    // 保存约束值到局部变量
                    val currentMaxWidth = maxWidth
                    val currentMaxHeight = maxHeight
                    
                    Text(
                        "BoxWithConstraints 可以获取约束: ${currentMaxWidth}x${currentMaxHeight}",
                        color = Color.Black
                    )
                }

                Spacer(Modifier.height(40.dp))
            }

            // ========== 交互式用例 - 可以通过点击按钮看到布局变化 ==========
            
            item {
                Text("========== 交互式用例 - 可以通过点击按钮看到布局变化 ==========", color = Color.Blue)
                Spacer(Modifier.height(20.dp))
            }

            // 5. 交互式宽度切换 - 响应式导航演示
            item {
                var containerWidth by remember { mutableStateOf(300.dp) }
                
                Column {
                    Text("5. 交互式宽度切换 - 响应式导航演示:")
                    Text("当前宽度: $containerWidth", color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { containerWidth = 250.dp }) {
                            Text("小屏(250dp)")
                        }
                        Button(onClick = { containerWidth = 400.dp }) {
                            Text("中屏(400dp)")
                        }
                        Button(onClick = { containerWidth = 600.dp }) {
                            Text("大屏(600dp)")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .width(containerWidth)
                            .height(120.dp)
                            .background(Color.Black)
                    ) {
                        val currentMaxWidth = maxWidth
                        
                        if (currentMaxWidth < 300.dp) {
                            // 小屏：底部导航
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("主内容", color = Color.White)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.Blue),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    repeat(3) { index ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            Text("Tab$index", color = Color.White)
                                        }
                                    }
                                }
                            }
                        } else {
                            // 大屏：侧边导航
                            Row {
                                Column(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .fillMaxHeight()
                                        .background(Color.Blue)
                                ) {
                                    repeat(3) { index ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Nav$index", color = Color.White)
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("主内容区域", color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 6. 交互式高度切换 - 表单布局演示
            item {
                var containerHeight by remember { mutableStateOf(150.dp) }
                
                Column {
                    Text("6. 交互式高度切换 - 表单布局演示:")
                    Text("当前高度: $containerHeight", color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { containerHeight = 100.dp }) {
                            Text("矮(100dp)")
                        }
                        Button(onClick = { containerHeight = 150.dp }) {
                            Text("中(150dp)")
                        }
                        Button(onClick = { containerHeight = 200.dp }) {
                            Text("高(200dp)")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(containerHeight)
                            .background(Color.LightGray)
                            .padding(8.dp)
                    ) {
                        val currentMaxHeight = maxHeight
                        
                        if (currentMaxHeight < 130.dp) {
                            // 低矮：只显示核心内容
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .background(Color.White)
                                        .padding(4.dp)
                                ) {
                                    Text("输入框", color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(30.dp)
                                        .background(Color.Blue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("提交", color = Color.White)
                                }
                            }
                        } else if (currentMaxHeight < 180.dp) {
                            // 中等：垂直表单
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("表单标题", color = Color.Black)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.White)
                                        .padding(4.dp)
                                ) {
                                    Text("输入框", color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.Blue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("提交", color = Color.White)
                                }
                            }
                        } else {
                            // 高度足够：完整表单
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("完整表单", color = Color.Black)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.White)
                                        .padding(4.dp)
                                ) {
                                    Text("姓名输入框", color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.White)
                                        .padding(4.dp)
                                ) {
                                    Text("邮箱输入框", color = Color.Gray)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color.Blue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("提交", color = Color.White)
                                }
                                Text("额外说明信息", color = Color.Gray)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 7. 交互式宽高切换 - 媒体播放器演示
            item {
                var containerWidth by remember { mutableStateOf(350.dp) }
                var containerHeight by remember { mutableStateOf(200.dp) }
                
                Column {
                    Text("7. 交互式宽高切换 - 媒体播放器演示:")
                    Text("当前尺寸: $containerWidth x $containerHeight", color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { 
                            containerWidth = 250.dp
                            containerHeight = 300.dp
                        }) {
                            Text("竖屏")
                        }
                        Button(onClick = { 
                            containerWidth = 350.dp
                            containerHeight = 200.dp
                        }) {
                            Text("正常")
                        }
                        Button(onClick = { 
                            containerWidth = 500.dp
                            containerHeight = 180.dp
                        }) {
                            Text("横屏")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .width(containerWidth)
                            .height(containerHeight)
                            .background(Color.Black)
                            .padding(4.dp)
                    ) {
                        val currentMaxWidth = maxWidth
                        val currentMaxHeight = maxHeight
                        val isLandscape = currentMaxWidth > currentMaxHeight
                        
                        if (isLandscape) {
                            // 横屏：左右布局
                            Row {
                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .fillMaxHeight()
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("视频播放", color = Color.White)
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(Color.Gray)
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text("播放控制", color = Color.White)
                                    Text("音量", color = Color.White)
                                    Text("列表", color = Color.White)
                                }
                            }
                        } else {
                            // 竖屏：上下布局
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(2f)
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("视频播放区域", color = Color.White)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.Gray)
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("播放", color = Color.White)
                                    Text("音量", color = Color.White)
                                    Text("列表", color = Color.White)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 8. 交互式网格列数演示
            item {
                var containerWidth by remember { mutableStateOf(300.dp) }
                
                Column {
                    Text("8. 交互式网格列数演示:")
                    Text("当前宽度: $containerWidth", color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { containerWidth = 200.dp }) {
                            Text("窄(200dp)")
                        }
                        Button(onClick = { containerWidth = 350.dp }) {
                            Text("中(350dp)")
                        }
                        Button(onClick = { containerWidth = 500.dp }) {
                            Text("宽(500dp)")
                        }
                        Button(onClick = { containerWidth = 700.dp }) {
                            Text("超宽(700dp)")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .width(containerWidth)
                            .height(200.dp)
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        val currentMaxWidth = maxWidth
                        
                        val columns = when {
                            currentMaxWidth < 250.dp -> 1
                            currentMaxWidth < 360.dp -> 2
                            currentMaxWidth < 600.dp -> 3
                            else -> 4
                        }
                        
                        Column {
                            Text("仪表板 ($columns 列)", color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val totalCards = 6
                            val rows = (totalCards + columns - 1) / columns
                            
                            repeat(rows) { rowIndex ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    repeat(columns) { colIndex ->
                                        val cardIndex = rowIndex * columns + colIndex
                                        if (cardIndex < totalCards) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .background(
                                                        when (cardIndex % 4) {
                                                            0 -> Color.Red
                                                            1 -> Color.Green
                                                            2 -> Color.Blue
                                                            else -> Color.Magenta
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("$cardIndex", color = Color.White)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                if (rowIndex < rows - 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // 9. 交互式字体大小演示
            item {
                var containerWidth by remember { mutableStateOf(250.dp) }
                
                Column {
                    Text("9. 交互式字体大小演示:")
                    Text("当前宽度: $containerWidth", color = Color.Gray)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { containerWidth = 150.dp }) {
                            Text("很窄")
                        }
                        Button(onClick = { containerWidth = 250.dp }) {
                            Text("窄")
                        }
                        Button(onClick = { containerWidth = 350.dp }) {
                            Text("中")
                        }
                        Button(onClick = { containerWidth = 500.dp }) {
                            Text("宽")
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    BoxWithConstraints(
                        modifier = Modifier
                            .width(containerWidth)
                            .background(Color.Cyan)
                            .padding(16.dp)
                    ) {
                        val currentMaxWidth = maxWidth
                        
                        val fontSize = when {
                            currentMaxWidth < 200.dp -> 10.sp
                            currentMaxWidth < 300.dp -> 14.sp
                            currentMaxWidth < 400.dp -> 18.sp
                            else -> 24.sp
                        }
                        
                        Column {
                            Text(
                                text = "响应式文字大小",
                                fontSize = fontSize,
                                color = Color.Black
                            )
                            Text(
                                text = "当前字体: $fontSize",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "这段文字会根据容器宽度自动调整大小，让你看到不同约束下的视觉效果变化。",
                                fontSize = fontSize,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
} 
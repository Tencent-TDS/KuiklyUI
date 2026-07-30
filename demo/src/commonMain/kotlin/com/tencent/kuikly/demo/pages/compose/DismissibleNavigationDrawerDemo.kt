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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.material3.*
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("DismissibleDrawerDemo")
class DismissibleNavigationDrawerDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            DismissibleDrawerDemoContent()
        }
    }

    @Composable
    fun DismissibleDrawerDemoContent() {
        val drawerState = rememberDrawerState()
        val scope = rememberCoroutineScope()
        var selectedItem by remember { mutableIntStateOf(0) }
        val drawerWidth by remember { mutableStateOf(280.dp) }

        val menuItems = listOf(
            "🏠 首页" to "Home",
            "👤 个人中心" to "Profile",
            "⚙️ 设置" to "Settings",
            "📧 消息" to "Messages",
            "❤️ 收藏" to "Favorites",
        )

        DismissibleNavigationDrawer(
            drawerState = drawerState,
            drawerWidth = drawerWidth,
            gesturesEnabled = true,
            drawerContent = {
                // Drawer content
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .background(Color.White)
                ) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF2E7D32)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "DismissibleNavigationDrawer",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "内容跟随 Drawer 一起平移",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menu Items
                    menuItems.forEachIndexed { index, (title, _) ->
                        val isSelected = selectedItem == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .background(
                                    if (isSelected) Color(0xFFC8E6C9)
                                    else Color.Transparent,
                                )
                                .clickable {
                                    selectedItem = index
                                    scope.launch { drawerState.close() }
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontSize = 16.sp,
                                color = if (isSelected) {
                                    Color(0xFF1B5E20)
                                } else {
                                    Color(0xFF333333)
                                }
                            )
                        }
                    }
                }
            },
        ) {
            // Main Content - 会被 Drawer 推开
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                // Top Bar - 绿色调区分 ModalDrawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable {
                                    scope.launch { drawerState.open() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "☰",
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "DismissibleNavigationDrawer Demo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Content Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = menuItems[selectedItem].first,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "当前页面: ${menuItems[selectedItem].second}",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Text("打开 Drawer (代码调用)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💡 注意观察：打开 Drawer 时主内容会被推向右侧",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🏗️ DismissibleNavigationDrawer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("✅ Drawer 打开时主内容被推开", fontSize = 13.sp)
                            Text("✅ 没有 scrim 遮罩层", fontSize = 13.sp)
                            Text("✅ 主内容和 Drawer 同步平移动画", fontSize = 13.sp)
                            Text("✅ 使用 Layout + placeRelative 自定义布局", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "对比: ModalNavigationDrawer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Text("• Modal: 主内容不动 + scrim 遮罩", fontSize = 13.sp)
                            Text("• Dismissible: 主内容被推开 + 无遮罩", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // State info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Drawer 状态信息",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("isOpen: ${drawerState.isOpen}")
                            Text("isClosed: ${drawerState.isClosed}")
                            Text("isAnimating: ${drawerState.isAnimationRunning}")
                            Text("progress: ${drawerState.progress}")
                            Text("offsetX: ${if (drawerState.currentOffset.isNaN()) -1f else drawerState.currentOffset}")
                        }
                    }
                }
            }
        }
    }
}

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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
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

@Page("NavigationDrawerDemo")
class ModalNavigationDrawerDemo : ComposeContainer() {

    companion object {
        private const val TAG = "NavigationDrawerDemo"
    }

    override fun willInit() {
        super.willInit()
        setContent {
            NavigationDrawerDemoContent()
        }
    }

    @Composable
    fun NavigationDrawerDemoContent() {
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
            "📝 笔记" to "Notes",
            "🔔 通知" to "Notifications",
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerWidth = drawerWidth,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerWidth = drawerWidth
                ) {
                    // 整个 Drawer 内容作为一个垂直滚动区域
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Drawer Header
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(Color(0xFF1565C0)),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                            Text(
                                text = "Modal Navigation Drawer",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "anchoredDraggable on Root Box",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(8.dp)) }

                        // Menu Items
                        items(menuItems.size) { index ->
                            val (title, _) = menuItems[index]
                            val isSelected = selectedItem == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                                    .background(
                                        if (isSelected) Color(0xFFBBDEFB)
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
                                        Color(0xFF0D47A1)
                                    } else {
                                        Color(0xFF333333)
                                    }
                                )
                            }
                        }

                        // ========== 内嵌水平滚动列表（嵌套滚动测试） ==========
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "📱 内嵌水平滚动列表",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "← 左右滑动（嵌套在垂直列表中） →",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(20) { index ->
                                    val colors = listOf(
                                        Color(0xFFE3F2FD), Color(0xFFFCE4EC),
                                        Color(0xFFF3E5F5), Color(0xFFE8F5E9),
                                        Color(0xFFFFF8E1), Color(0xFFE0F7FA)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(60.dp)
                                            .background(colors[index % colors.size]),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "H-$index",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // ========== 内嵌垂直滚动列表（嵌套滚动测试） ==========
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "📜 内嵌垂直滚动列表",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "↑ 上下滑动（嵌套在垂直列表中） ↓",
                                modifier = Modifier.padding(horizontal = 16.dp),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(30) { index ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .background(
                                                if (index % 2 == 0) Color(0xFFF5F5F5) else Color.White
                                            )
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = "嵌套列表项 #$index",
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 底部间距
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            },
        ) {
            // Main Content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar - 蓝色调区分
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(0xFF1565C0)),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Menu button
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
                            text = "ModalNavigationDrawer Demo",
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

                    // Action buttons
                    Button(
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Text("打开 Drawer (代码调用)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💡 可以从屏幕任意位置水平右滑来打开 Drawer",
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Drawer state info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Drawer 状态信息",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("isOpen: ${drawerState.isOpen}")
                            Text("isClosed: ${drawerState.isClosed}")
                            Text("isAnimating: ${drawerState.isAnimationRunning}")
                            Text("progress: ${drawerState.progress}")
                            Text("offsetX: ${if (drawerState.currentOffset.isNaN()) -1f else drawerState.currentOffset}")
                            Text("currentValue: ${drawerState.currentValue}")
                            Text("targetValue: ${drawerState.targetValue}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Architecture comparison card
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
                                text = "🏗️ 架构对比说明",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "本页面使用 ModalNavigationDrawer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("✅ anchoredDraggable 在根容器 Box 上", fontSize = 13.sp)
                            Text("✅ 与官方 Compose Material3 架构一致", fontSize = 13.sp)
                            Text("✅ 系统自然处理手势冲突", fontSize = 13.sp)
                            Text("❌ 不支持 backPressBoundary 边界排除", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "对比: DrawerLegacyDemo 使用 ModalNavigationDrawerLegacy",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("✅ anchoredDraggable 仅在 Drawer 面板上", fontSize = 13.sp)
                            Text("✅ 自定义 pointerInput 边缘检测", fontSize = 13.sp)
                            Text("✅ 支持 backPressBoundary 边界排除", fontSize = 13.sp)
                            Text("❌ 更复杂的手势管理逻辑", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Test checklist
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
                                text = "🧪 测试检查清单",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("1. ☰ 按钮点击 → 应打开 Drawer", fontSize = 13.sp)
                            Text("2. 屏幕任意位置右滑 → 应打开 Drawer", fontSize = 13.sp)
                            Text("3. 遮罩层点击 → 应关闭 Drawer", fontSize = 13.sp)
                            Text("4. Drawer 面板左滑 → 应关闭 Drawer", fontSize = 13.sp)
                            Text("5. 菜单项点击 → 应切换页面并关闭", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚡ 关键对比测试:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Text("  - 屏幕中间区域右滑能否打开?（应能）", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Text("  - 系统返回手势是否被 Drawer 覆盖?", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Text("  - 与 DrawerLegacyDemo 对比手势行为差异", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📋 Drawer 内列表手势测试（当前有冲突）:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                            Text("  - 水平列表左右滑 → 可能与 Drawer 冲突", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Text("  - 垂直列表上下滑 → 应正常滚动", fontSize = 12.sp, color = Color(0xFF1B5E20))
                            Text("  - 等待方案H实现解决冲突", fontSize = 12.sp, color = Color(0xFFE65100))
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.drawer.MoveableDrawer
import com.tencent.kuikly.compose.foundation.drawer.MoveableDrawerState
import com.tencent.kuikly.compose.foundation.drawer.rememberMoveableDrawerState
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * MoveableDrawer Demo — 展示侧边栏组件的全屏与非全屏两种使用方式。
 *
 * 使用 key() 在模式/宽度切换时彻底重建 Drawer，配合组件层预设 contentOffset
 * 确保原生 ScrollView 重建后立即处于关闭位置，避免闪烁。
 */
@Page("MoveableDrawerDemo")
internal class MoveableDrawerDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                MoveableDrawerDemoScreen()
            }
        }
    }
}

// ======================== 主界面 ========================

@Composable
private fun MoveableDrawerDemoScreen() {
    var isFullScreen by remember { mutableStateOf(false) }
    var drawerWidthValue by remember { mutableIntStateOf(300) }
    val scope = rememberCoroutineScope()

    // 使用 key(isFullScreen, drawerWidthValue) 让模式/宽度切换时整个 Drawer
    // （包括 state）彻底重建，避免旧 state 残留导致侧边栏自动打开。
    key(isFullScreen, drawerWidthValue) {
        val drawerState = rememberMoveableDrawerState(
            fullScreen = isFullScreen,
            drawerWidth = drawerWidthValue.dp,
        )

        val onItemClick: (String) -> Unit = {
            scope.launch {
                drawerState.close()
            }
        }

        MoveableDrawer(
            state = drawerState,
            drawerContent = { DrawerMenuContent(onItemClick) },
            content = {
                MainContent(
                    drawerState = drawerState,
                    isFullScreen = isFullScreen,
                    drawerWidthValue = drawerWidthValue,
                    scope = scope,
                    onToggleFullScreen = { isFullScreen = it },
                    onChangeWidth = { drawerWidthValue = it },
                )
            },
        )
    }
}

// ======================== 主内容区域 ========================

@Composable
private fun MainContent(
    drawerState: MoveableDrawerState,
    isFullScreen: Boolean,
    drawerWidthValue: Int,
    scope: CoroutineScope,
    onToggleFullScreen: (Boolean) -> Unit,
    onChangeWidth: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFFF8F8F8))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillButton(
                text = if (drawerState.isOpen) "关闭 Drawer" else "打开 Drawer",
                color = Color(0xFF2196F3),
                onClick = {
                    scope.launch {
                        drawerState.toggle()
                    }
                },
            )
            Spacer(Modifier.padding(start = 12.dp))
            Text("MoveableDrawer Demo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(12.dp),
        ) {
            Text(
                "Touch Passthrough Test",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Drawer 状态: ${if (drawerState.isOpen) "已打开" else "已关闭"}",
                fontSize = 13.sp,
                color = if (drawerState.isOpen) Color(0xFF4CAF50) else Color.Gray,
            )
            Text(
                "触摸验证: 先点击下方按钮改变计数；打开 Drawer 后点击遮罩层，计数不应变化。",
                fontSize = 13.sp,
                color = Color(0xFF333333),
            )
            Spacer(Modifier.height(8.dp))
            Text("Drawer 模式", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    text = "普通模式",
                    color = if (!isFullScreen) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                    onClick = { onToggleFullScreen(false) },
                )
                PillButton(
                    text = "全屏模式",
                    color = if (isFullScreen) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                    onClick = { onToggleFullScreen(true) },
                )
            }

            if (!isFullScreen) {
                Spacer(Modifier.height(12.dp))
                Text("Drawer 宽度: ${drawerWidthValue}dp", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(200, 250, 300, 350).forEach { width ->
                        PillButton(
                            text = "${width}dp",
                            color = if (drawerWidthValue == width) Color(0xFF2196F3) else Color(0xFFBDBDBD),
                            onClick = { onChangeWidth(width) },
                        )
                    }
                }
            }
        }

        // ---- 点击穿透验证区域 ----
        ClickPassthroughTestArea()
    }
}

// ======================== 点击穿透验证区域 ========================

@Composable
private fun ClickPassthroughTestArea() {
    var clickCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFFE3F2FD))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "点击计数: $clickCount",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                text = "+1",
                color = Color(0xFF4CAF50),
                onClick = { clickCount++ },
            )
            PillButton(
                text = "-1",
                color = Color(0xFFF44336),
                onClick = { if (clickCount > 0) clickCount-- },
            )
            PillButton(
                text = "重置",
                color = Color(0xFF9E9E9E),
                onClick = { clickCount = 0 },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "打开 Drawer 后点击遮罩层，上方计数不应变化",
            fontSize = 13.sp,
            color = Color(0xFF333333),
        )
    }
}

// ======================== Drawer 菜单内容 ========================

@Composable
private fun DrawerMenuContent(onItemClick: (String) -> Unit) {
    val menuItems = remember {
        listOf(
            "首页", "消息", "收藏",
            "历史记录", "设置", "关于",
            "帮助与反馈", "深色模式", "退出登录",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(top = 48.dp),
    ) {
        Text(
            "菜单",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(menuItems) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onItemClick(item) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item, fontSize = 16.sp)
                }
            }
        }
    }
}

// ======================== 通用胶囊按钮 ========================

@Composable
private fun PillButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

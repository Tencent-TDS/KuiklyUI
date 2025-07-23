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
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.ExperimentalMaterial3Api
import com.tencent.kuikly.compose.material3.LeadingIconTab
import com.tencent.kuikly.compose.material3.PrimaryTabRow
import com.tencent.kuikly.compose.material3.ScrollableTabRow
import com.tencent.kuikly.compose.material3.SecondaryTabRow
import com.tencent.kuikly.compose.material3.Tab
import com.tencent.kuikly.compose.material3.TabRow
import com.tencent.kuikly.compose.material3.TabRowDefaults
import com.tencent.kuikly.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TabRowDemo")
class TabRowDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                TabRowDemoContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TabRowDemoContent() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                // 基础参数示例
                TabParametersExample()
            }

            item {
                // LeadingIconTab示例
                LeadingIconTabExample()
            }

            item {
                // 标准TabRow示例
                StandardTabRowExample()
            }

            item {
                // PrimaryTabRow示例
                PrimaryTabRowExample()
            }

            item {
                // SecondaryTabRow示例
                SecondaryTabRowExample()
            }

            item {
                // ScrollableTabRow示例
                ScrollableTabRowExample()
            }

            item {
                // PrimaryScrollableTabRow示例
                PrimaryScrollableTabRowExample()
            }

            item {
                // SecondaryScrollableTabRow示例
                SecondaryScrollableTabRowExample()
            }

            // 底部留白
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun TabParametersExample() {
        var tabParamsIndex by remember { mutableStateOf(0) }

        Text("1. Tab参数展示示例:")
        TabRow(
            selectedTabIndex = tabParamsIndex,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            // 1. 基础Tab - 仅文本
            Tab(
                selected = tabParamsIndex == 0,
                onClick = { tabParamsIndex = 0 },
                text = { Text("基础Tab") },
            )

            // 2. 禁用状态的Tab
            Tab(
                selected = tabParamsIndex == 1,
                onClick = { tabParamsIndex = 1 },
                enabled = false,
                text = { Text("禁用Tab") },
            )

            // 3. 带图标的Tab
            Tab(
                selected = tabParamsIndex == 2,
                onClick = { tabParamsIndex = 2 },
                icon = {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .background(if (tabParamsIndex == 2) Color.Red else Color.Gray),
                    )
                },
                text = { Text("图标Tab") },
            )

            // 4. 自定义颜色的Tab
            Tab(
                selected = tabParamsIndex == 3,
                onClick = { tabParamsIndex = 3 },
                selectedContentColor = Color.Red,
                unselectedContentColor = Color.Gray,
                text = {
                    Text(
                        "彩色Tab",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                },
            )

            // 5. 只有图标的Tab
            Tab(
                selected = tabParamsIndex == 4,
                onClick = { tabParamsIndex = 4 },
                icon = {
                    Box(
                        modifier =
                            Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (tabParamsIndex == 4) Color.Green else Color.LightGray,
                                ),
                    )
                },
            )

            // 6. 复杂内容的Tab
            Tab(
                selected = tabParamsIndex == 5,
                onClick = { tabParamsIndex = 5 },
                selectedContentColor = Color.Blue,
                unselectedContentColor = Color.Gray,
                enabled = true,
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "主标题",
                            fontSize = 14.sp,
                            color = if (tabParamsIndex == 5) Color.Blue else Color.Gray,
                        )
                        Text(
                            "副标题",
                            fontSize = 10.sp,
                            color =
                                if (tabParamsIndex == 5) {
                                    Color.Blue.copy(alpha = 0.7f)
                                } else {
                                    Color.Gray.copy(alpha = 0.7f)
                                },
                        )
                    }
                },
                icon = {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (tabParamsIndex == 5) Color.Blue else Color.Gray,
                                ),
                    )
                },
            )
        }

        // 显示当前选中的Tab的说明
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            when (tabParamsIndex) {
                0 -> "基础Tab：最简单的Tab实现，只包含文本"
                1 -> "禁用Tab：enabled = false，显示禁用状态"
                2 -> "图标Tab：同时包含图标和文本"
                3 -> "彩色Tab：自定义选中和未选中状态的颜色"
                4 -> "纯图标Tab：只显示图标，不显示文本"
                5 -> "复杂Tab：包含图标、主标题、副标题，展示复杂布局"
                else -> ""
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @Composable
    private fun StandardTabRowExample() {
        var standardTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("标签1", "标签2", "标签3", "标签4", "标签5")

        Text("3. 标准TabRow示例 (主导航):")
        // 默认TabRow
        Text(
            "3.1 默认TabRow:",
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        TabRow(
            selectedTabIndex = standardTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = standardTabIndex == index,
                    onClick = { standardTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 自定义TabRow
        Text(
            "3.2 自定义TabRow:",
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        TabRow(
            selectedTabIndex = standardTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
            contentColor = Color.Cyan,
            containerColor = Color.Yellow,
            indicator = { tabPositions ->
                if (standardTabIndex < tabPositions.size) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[standardTabIndex])
                            .height(3.dp)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(Color.Blue),
                    )
                }
            },
            divider = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(color = Color.Red),
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = standardTabIndex == index,
                    onClick = { standardTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        // 显示说明文本
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "TabRow 特点：\n" +
                "1. 通用标签页组件，高度可定制\n" +
                "2. 默认使用主要颜色主题\n" +
                "3. 支持自定义指示器和分割线\n" +
                "4. 适合作为主要导航组件",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @Composable
    private fun LeadingIconTabExample() {
        var primaryTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("标签1", "标签2", "标签3")

        Text("2. LeadingIconTab示例 (图标在文字前):")
        TabRow(
            selectedTabIndex = primaryTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                LeadingIconTab(
                    selected = primaryTabIndex == index,
                    onClick = { primaryTabIndex = index },
                    text = { Text(title) },
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .background(if (primaryTabIndex == index) Color.Red else Color.Gray),
                        )
                    },
                )
            }
        }

        // 显示说明文本
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "LeadingIconTab 与普通 Tab 的区别：\n" +
                "1. 图标始终在文字左侧\n" +
                "2. 使用水平布局，图标和文字在同一行\n" +
                "3. 适合需要固定图标位置的导航场景",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PrimaryTabRowExample() {
        var primaryTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("标签1", "标签2", "标签3")

        Text("4. PrimaryTabRow示例:")
        PrimaryTabRow(
            selectedTabIndex = primaryTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(primaryTabIndex, matchContentSize = true),
                    width = Dp.Unspecified,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = primaryTabIndex == index,
                    onClick = { primaryTabIndex = index },
                    text = { Text(title) },
                    icon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .background(if (primaryTabIndex == index) Color.Red else Color.Gray),
                        )
                    },
                )
            }
        }

        // 显示说明文本
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "PrimaryTabRow：\n" +
                "1. 主要导航标签页\n" +
                "2. 使用主要颜色主题\n" +
                "3. 指示器样式更突出",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SecondaryTabRowExample() {
        var secondaryTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("标签1", "标签2", "标签3", "标签4", "标签5")

        Text("5. SecondaryTabRow示例 (次级导航):")
        // 默认 SecondaryTabRow
        Text(
            "5.1 默认SecondaryTabRow:",
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        SecondaryTabRow(
            selectedTabIndex = secondaryTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = secondaryTabIndex == index,
                    onClick = { secondaryTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 自定义颜色的 SecondaryTabRow
        Text(
            "5.2 自定义颜色的SecondaryTabRow:",
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )
        SecondaryTabRow(
            selectedTabIndex = secondaryTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
            containerColor = Color(0xFFE3F2FD),
            contentColor = Color(0xFF1976D2),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = secondaryTabIndex == index,
                    onClick = { secondaryTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        // 显示说明文本
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "SecondaryTabRow 特点：\n" +
                "1. 专门用于次要导航场景\n" +
                "2. 默认使用次要颜色主题\n" +
                "3. 指示器样式更简单、更轻量\n" +
                "4. 适合内容区域的二级导航",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @Composable
    private fun ScrollableTabRowExample() {
        var scrollableTabIndex by remember { mutableStateOf(0) }
        val longTabs = List(10) { "选项 ${it + 1} 的长文本标签" }

        Text("6. ScrollableTabRow示例:")
        ScrollableTabRow(
            selectedTabIndex = scrollableTabIndex,
            modifier = Modifier.padding(vertical = 8.dp),
            edgePadding = 16.dp,
        ) {
            longTabs.forEachIndexed { index, title ->
                Tab(
                    selected = scrollableTabIndex == index,
                    onClick = { scrollableTabIndex = index },
                    text = { Text(title) },
                )
            }
        }

        // 显示说明文本
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "ScrollableTabRow：\n" +
                "1. 可横向滚动的标签页\n" +
                "2. 适用于标签数量较多的场景\n" +
                "3. 支持长文本标签",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PrimaryScrollableTabRowExample() {
//        var primaryScrollableTabIndex by remember { mutableStateOf(0) }
//        val longTabs = List(10) { "选项 ${it + 1} 的长文本标签" }
//
//        Text("7. PrimaryScrollableTabRow示例:")
//        PrimaryScrollableTabRow(
//            selectedTabIndex = primaryScrollableTabIndex,
//            modifier = Modifier.padding(vertical = 8.dp),
//            edgePadding = 4.dp,
//            indicator = {
//                TabRowDefaults.PrimaryIndicator(
//                    modifier = Modifier.tabIndicatorOffset(primaryScrollableTabIndex, matchContentSize = true),
//                    width = Dp.Unspecified
//                )
//            }
//        ) {
//            longTabs.forEachIndexed { index, title ->
//                Tab(
//                    selected = primaryScrollableTabIndex == index,
//                    onClick = { primaryScrollableTabIndex = index },
//                    text = { Text(title) }
//                )
//            }
//        }
//
//        // 显示说明文本
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            "PrimaryScrollableTabRow：\n" +
//            "1. 可滚动的主要导航标签页\n" +
//            "2. 使用主要颜色主题\n" +
//            "3. 支持自定义边距和指示器",
//            modifier = Modifier.padding(horizontal = 16.dp)
//        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SecondaryScrollableTabRowExample() {
//        var secondaryScrollableTabIndex by remember { mutableStateOf(0) }
//        val longTabs = List(10) { "选项 ${it + 1} 的长文本标签" }
//
//        Text("8. SecondaryScrollableTabRow示例:")
//        SecondaryScrollableTabRow(
//            selectedTabIndex = secondaryScrollableTabIndex,
//            modifier = Modifier.padding(vertical = 8.dp),
//            containerColor = Color(0xFFE3F2FD),
//            contentColor = Color(0xFF1976D2),
//            edgePadding = 8.dp
//        ) {
//            longTabs.forEachIndexed { index, title ->
//                Tab(
//                    selected = secondaryScrollableTabIndex == index,
//                    onClick = { secondaryScrollableTabIndex = index },
//                    enabled = index % 2 == 0,  // 偶数选项可用，奇数选项禁用
//                    text = { Text(title) }
//                )
//            }
//        }
//
//        // 显示说明文本
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            "SecondaryScrollableTabRow：\n" +
//            "1. 可滚动的次要导航标签页\n" +
//            "2. 使用次要颜色主题\n" +
//            "3. 支持禁用特定选项",
//            modifier = Modifier.padding(horizontal = 16.dp)
//        )
    }
}

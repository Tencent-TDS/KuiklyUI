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

@file:OptIn(ExperimentalFoundationApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.combinedClickable
import com.tencent.kuikly.compose.foundation.focusable
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
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.Tab
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TabRow
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.semantics.CustomAccessibilityAction
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.clearAndSetSemantics
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.customActions
import com.tencent.kuikly.compose.ui.semantics.heading
import com.tencent.kuikly.compose.ui.semantics.role
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.stateDescription
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

/**
 * 无障碍 Demo，需要APP全新启动才能 生效。
 * 进程已经启动情况下打开无障碍则无效。
 */
@Page("AccessibilityDemo")
internal class AccessibilityDemoPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        // 这里可以修改一些基本配置
        layoutDirection = LayoutDirection.Ltr
        setContent {
            AccessibilityDemo()
        }
    }
}

@Composable
fun AccessibilityDemo() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                "无障碍功能演示",
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // 1. 基础语义标签示例
        item {
            DemoCard(title = "基础语义标签") {
                BasicSemanticsDemo()
            }
        }

        // 2. 无障碍描述示例
        item {
            DemoCard(title = "无障碍描述") {
                AccessibilityDescriptionDemo()
            }
        }

        // 5. 无障碍组示例
        item {
            DemoCard(title = "无障碍组 进来首个聚焦位置") {
                AccessibilityGroupDemo()
            }
        }

        // 3. 无障碍动作示例
        item {
            DemoCard(title = "无障碍动作") {
                AccessibilityActionDemo()
            }
        }

        // 4. 无障碍状态示例
        item {
            DemoCard(title = "无障碍状态") {
                AccessibilityStateDemo()
            }
        }

        // 6. 无障碍标题示例
        item {
            DemoCard(title = "无障碍标题") {
                AccessibilityHeadingDemo()
            }
        }

        // 7. 状态描述示例
        item {
            DemoCard(title = "状态描述") {
                StateDescriptionDemo()
            }
        }

        // 8. LazyColumn Merge 示例
        item {
            DemoCard(title = "LazyColumn Merge(列表合并，以及 长按 无障碍描述)") {
                LazyColumnMergeDemo()
            }
        }

        // 9. 多层级 Merge 示例
        item {
            DemoCard(title = "多层级 Merge") {
                MultiLevelMergeDemo()
            }
        }
    }
}

@Composable
private fun BasicSemanticsDemo() {
    Column {
        // 使用 semantics 修饰符添加语义标签
        // 声明clickable时，mergeDescendants = true
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .semantics {
                    contentDescription = "这是一个可点击的卡片"
                    role = Role.Button
                }
                .clickable { /* 处理点击事件 */ }
        ) {
            Text(
                "点击我",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun AccessibilityDescriptionDemo() {
    Column {
        // 为图片添加无障碍描述
        Box(
            modifier = Modifier
                .size(100.dp)
                .semantics {
                    contentDescription = "这是一张示例图片，展示了一个红色的圆形"
                }
//                .background(MaterialTheme.colors.primary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 为文本添加无障碍描述
        Text(
            "重要通知",
            modifier = Modifier.semantics {
                contentDescription = "这是一条重要通知，请仔细阅读"
            }
        )
    }
}

@Composable
private fun AccessibilityActionDemo() {
    var count by remember { mutableStateOf(0) }

    Column {
        Text("当前计数: $count")

        // 添加无障碍动作
        Button(
            onClick = { count++ },
            modifier = Modifier.semantics {
                contentDescription = "增加计数按钮"
                // 添加自定义无障碍动作
                customActions = listOf(
                    CustomAccessibilityAction("重置计数") {
                        count = 0
                        true
                    }
                )
            }
        ) {
            Text("增加")
        }
    }
}

@Composable
private fun AccessibilityStateDemo() {
//    var isChecked by remember { mutableStateOf(false) }
//
//    Column {
//        // 添加无障碍状态
//        Switch(
//            checked = isChecked,
//            onCheckedChange = { isChecked = it },
//            modifier = Modifier.semantics {
//                contentDescription = "通知开关"
//                stateDescription = if (isChecked) "已开启" else "已关闭"
//            }
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // 添加进度条的无障碍状态
//        LinearProgressIndicator(
//            progress = 0.7f,
//            modifier = Modifier
//                .fillMaxWidth()
//                .semantics {
//                    contentDescription = "下载进度"
//                    progressBarRangeInfo = ProgressBarRangeInfo(
//                        current = 0.7f,
//                        range = 0f..1f
//                    )
//                }
//        )
//    }
}

@Composable
private fun AccessibilityGroupDemo() {
    // 使用无障碍组组织相关元素
    val focusRequester = remember { FocusRequester() }
    Column(
        modifier = Modifier.focusRequester(focusRequester)
            .focusable()
            .semantics(mergeDescendants = true)

            {
            contentDescription = "用户信息卡片组"
        }
    ) {
        Column() {
            Column() {
                Text("用户信息")
                Text("姓名: 张三", modifier = Modifier.clearAndSetSemantics {
                    contentDescription = "你好 我的名字叫张三"
                })
                Text("年龄: 25")
                Text("职业: 工程师")
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(500)
        focusRequester.requestFocus()
    }
}

@Composable
private fun AccessibilityHeadingDemo() {
    Column {
        // 添加无障碍标题
        Text(
            "章节标题",
//            style = MaterialTheme.typography.h6,
            modifier = Modifier.semantics {
//                heading()
            }
        )

        Text("这是章节内容...")

        Spacer(modifier = Modifier.height(16.dp))

        // 另一个无障碍标题
        Text(
            "子章节标题",
//            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.semantics {
                heading()
            }
        )

        Text("这是子章节内容...")
    }
}

@Composable
private fun StateDescriptionDemo() {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var isEnabled by remember { mutableStateOf(true) }

    Column {
        // 1. 展开/折叠状态
        Button(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.semantics {
                contentDescription = "展开/折叠按钮"
                stateDescription = if (isExpanded) "当前已展开" else "当前已折叠"
            }
        ) {
            Text(if (isExpanded) "折叠" else "展开")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 标签页状态
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.semantics {
                    contentDescription = "第一个标签页"
                    stateDescription = if (selectedTab == 0) "当前选中" else "未选中"
                }
            ) {
                Text("标签1")
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.semantics {
                    contentDescription = "第二个标签页"
                    stateDescription = if (selectedTab == 1) "当前选中" else "未选中"
                }
            ) {
                Text("标签2")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 启用/禁用状态
        Button(
            onClick = { isEnabled = !isEnabled },
//            enabled = true,
            modifier = Modifier.semantics {
                contentDescription = "切换启用状态按钮"
                stateDescription = if (isEnabled) "当前已启用" else "当前已禁用"
            }
        ) {
            Text(if (isEnabled) "禁用" else "启用")
        }

        // 4. 自定义状态组件
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
//                .background(if (isEnabled) MaterialTheme.colors.primary else MaterialTheme.colors.error)
                .semantics {
                    contentDescription = "状态指示器"
                    stateDescription = if (isEnabled) "系统正常运行中" else "系统已停止"
                }
        )
    }
}

@Composable
private fun LazyColumnMergeDemo() {
    val items = remember { List(5) { "列表项 ${it + 1}" } }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    // 触发点击长按无障碍描述
                    println("触发点击长按无障碍描述 ")
                }
            ) {
                // 触发点击无障碍描述
                println("触发点击无障碍描述 ")
            }
            .semantics(mergeDescendants = true) {
                contentDescription = "这是一个可滚动的列表区域，包含 ${items.size} 个项目"
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics {
                            contentDescription = item
                        }
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiLevelMergeDemo() {
    // 第一层：最外层容器，设置 mergeDescendants = true
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "这是一个多层级嵌套的容器，包含多个子元素"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 第二层：标题区域，也设置 mergeDescendants = true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "标题区域"
                    }
            ) {
                Text(
                    "多层级示例",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 第三层：内容区域，使用 clearAndSetSemantics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {
                        contentDescription = "这是独立的内容区域，不会被合并到父级语义中"
                    }
            ) {
                Column {
                    Text("独立内容1")
                    Text("独立内容2")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 第四层：底部区域，继续使用 mergeDescendants = true
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = "底部区域"
                    }
            ) {
                Column {
                    Text("底部内容1")
                    Text("底部内容2")
                }
            }
        }
    }
}

@Composable
private fun DemoCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
//        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
//                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
} 

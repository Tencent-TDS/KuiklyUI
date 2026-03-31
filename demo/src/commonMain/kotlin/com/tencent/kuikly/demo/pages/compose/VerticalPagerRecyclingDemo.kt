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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.demo.pages.demo.MyDemoCustomView
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.VerticalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * VerticalPager 复用测试 Demo
 * 
 * 此 Demo 用于测试 VerticalPager 的页面复用机制：
 * 1. 当 key 变化时，页面是否被正确复用
 * 2. 通过日志追踪每个页面的创建、销毁、复用过程
 * 3. 展示如何正确使用 key 参数实现高效的页面复用
 */
@Page("vprd")
class VerticalPagerRecyclingDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                VerticalPagerRecyclingTest()
            }
        }
    }
}

/**
 * 数据项，包含稳定的 key 和内容
 */
data class RecycleItem(
    val key: String,           // 用于 VerticalPager 的 key 参数
    val title: String,         // 显示标题
    val color: Color,          // 背景色（帮助视觉上区分不同页面）
    val instanceId: Int = 0    // 实例ID（用于追踪复用情况）
)

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: Long,
    val message: String,
    val type: LogType
)

enum class LogType {
    CREATE,      // 创建新页面
    DISPOSE,     // 销毁页面
    RECYCLE,     // 复用页面
    REFRESH,     // 刷新数据
    INFO         // 普通信息
}

@Composable
fun VerticalPagerRecyclingTest() {
    // 日志列表
    var logs by remember { mutableStateOf(listOf<LogEntry>()) }
    var logCounter by remember { mutableIntStateOf(0) }
    
    // 添加日志的辅助函数
    fun addLog(message: String, type: LogType = LogType.INFO) {
        logs = logs + LogEntry(getTimeMillis(), "[${logCounter++}] $message", type)
    }
    
    // 初始数据：5条数据
    val initialData = remember {
        listOf(
            RecycleItem("key_A", "页面 A", Color(0xFFE53935)),  // 红
            RecycleItem("key_B", "页面 B", Color(0xFFD81B60)),  // 粉红
            RecycleItem("key_C", "页面 C", Color(0xFF8E24AA)),  // 紫
            RecycleItem("key_D", "页面 D", Color(0xFF5E35B1)),  // 深紫
            RecycleItem("key_E", "页面 E", Color(0xFF3949AB)),  // 靛蓝
        )
    }
    
    // 当前数据列表
    var dataList by remember { mutableStateOf(initialData) }
    
    // 创建 PagerState
    val pagerState = rememberPagerState { dataList.size }
    
    // 当前选中的方案
    var selectedScenario by remember { mutableIntStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "VerticalPager 复用测试",
                fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }
        
        // 测试场景选择
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "测试场景",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 场景1：key不变，数据刷新（应该触发复用）
                ScenarioButton(
                    text = "场景1：Key不变，数据刷新（复用）",
                    isSelected = selectedScenario == 1,
                    onClick = {
                        selectedScenario = 1
                        addLog("执行场景1：key不变，仅刷新数据", LogType.REFRESH)
                        dataList = dataList.mapIndexed { index, item ->
                            item.copy(title = "${item.title.split(" ")[0]} (刷新#${logCounter})")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 场景2：key变化（应该触发页面重建）
                ScenarioButton(
                    text = "场景2：Key变更（重新创建）",
                    isSelected = selectedScenario == 2,
                    onClick = {
                        selectedScenario = 2
                        addLog("执行场景2：key互换 A↔B, C↔D", LogType.REFRESH)
                        // 交换 key，模拟数据重组
                        val newList = dataList.toMutableList()
                        if (newList.size >= 4) {
                            // 交换 A 和 B 的 key
                            newList[0] = newList[0].copy(key = "key_B", title = "页面 A→B")
                            newList[1] = newList[1].copy(key = "key_A", title = "页面 B→A")
                            // 交换 C 和 D 的 key
                            newList[2] = newList[2].copy(key = "key_D", title = "页面 C→D")
                            newList[3] = newList[3].copy(key = "key_C", title = "页面 D→C")
                        }
                        dataList = newList
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 场景3：新增数据
                ScenarioButton(
                    text = "场景3：新增数据项",
                    isSelected = selectedScenario == 3,
                    onClick = {
                        selectedScenario = 3
                        val newKey = "key_F${logCounter}"
                        val newItem = RecycleItem(
                            newKey,
                            "新增页面 F",
                            Color(0xFF1E88E5)
                        )
                        dataList = dataList + newItem
                        addLog("执行场景3：新增数据 key=$newKey", LogType.REFRESH)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 场景4：删除数据
                ScenarioButton(
                    text = "场景4：删除末尾数据",
                    isSelected = selectedScenario == 4,
                    onClick = {
                        selectedScenario = 4
                        if (dataList.isNotEmpty()) {
                            val removed = dataList.last()
                            dataList = dataList.dropLast(1)
                            addLog("执行场景4：删除 key=${removed.key}", LogType.REFRESH)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 重置按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResetButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedScenario = 0
                            addLog("重置数据列表", LogType.REFRESH)
                            dataList = initialData
                            logs = emptyList()
                            logCounter = 0
                        }
                    )
                }
            }
        }
        
        // VerticalPager 区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSize = PageSize.Fill,
                key = { index -> dataList[index].key },  // 关键：使用稳定的 key
            ) { page ->
                val item = dataList[page]
                
                // 追踪页面的创建和销毁
                DisposableEffect(item.key) {
                    addLog("🆕 创建/复用页面: key=${item.key}, page=$page", LogType.CREATE)
                    onDispose {
                        addLog("❌ 销毁页面: key=${item.key}, page=$page", LogType.DISPOSE)
                    }
                }
                
                // 记录复用情况
                LaunchedEffect(item.key) {
                    addLog("♻️ 页面进入组合: key=${item.key}, title=${item.title}", LogType.RECYCLE)
                }
                
                PagerPage(
                    item = item,
                    page = page,
                    totalPages = dataList.size,
                    onLog = { msg, type -> addLog(msg, type) }
                )
            }
        }
        
        // 日志区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "运行日志（最新在上）",
                    fontSize = 12.sp,
                    color = Color(0xFF80CBC4),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(logs.size) { index ->
                        val log = logs[logs.size - 1 - index]
                        val color = when (log.type) {
                            LogType.CREATE -> Color(0xFF81C784)   // 绿色
                            LogType.DISPOSE -> Color(0xFFE57373) // 红色
                            LogType.RECYCLE -> Color(0xFF64B5F6) // 蓝色
                            LogType.REFRESH -> Color(0xFFFFB74D) // 橙色
                            LogType.INFO -> Color(0xFFB0BEC5)    // 灰色
                        }
                        Text(
                            text = log.message,
                            fontSize = 11.sp,
                            color = color,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 自定义组件包装：MyDemoCustomView
 * 
 * 使用 MakeKuiklyComposeNode 将自定义 View 桥接到 Compose 中
 * 这个组件会测试：
 * 1. 自定义 View 的复用能力
 * 2. 事件回调（点击）是否正常工作
 * 3. 状态传递（message）是否正常
 */
@Composable
private fun MyDemoCustomViewWrapper(
    itemKey: String,
    pageIndex: Int,
    onLog: (String, LogType) -> Unit
) {
    var tapCount by remember { mutableIntStateOf(0) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 自定义组件 MyDemoCustomView",
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 显示当前页面的点击次数
            Text(
                text = "页面点击次数: $tapCount",
                fontSize = 16.sp,
                color = Color(0xFFFFCDD2)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 使用 MakeKuiklyComposeNode 渲染自定义 View
            MakeKuiklyComposeNode<MyDemoCustomView>(
                factory = { MyDemoCustomView() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                viewUpdate = { view ->
                    // 设置属性 - 显示当前 key 和点击次数
                    view.getViewAttr().message("Key: $itemKey\\n点击次数: $tapCount")
                    
                    // 注册点击事件
                    view.getViewEvent().onMyViewTapped {
                        tapCount++
                        onLog("👆 MyDemoCustomView[$itemKey] 被点击 (count=$tapCount)", LogType.INFO)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 说明文字
            Text(
                text = "测试: 自定义View复用、事件回调、状态保持",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PagerPage(
    item: RecycleItem,
    page: Int,
    totalPages: Int,
    onLog: (String, LogType) -> Unit = { _, _ -> }
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(item.color)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Key 显示
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "Key: ${item.key}",
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // 标题
            Text(
                text = item.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            // 页码指示器
            Text(
                text = "${page + 1} / $totalPages",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            // ===== 自定义组件：用于测试复用 =====
            // 替换 CounterCard 为 MyDemoCustomViewWrapper
            MyDemoCustomViewWrapper(
                itemKey = item.key,
                pageIndex = page,
                onLog = onLog
            )
            
            // 提示信息
            Text(
                text = "上下滑动切换页面",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ScenarioButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Color(0xFF1976D2) else Color(0xFFECEFF1),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = if (isSelected) Color.White else Color(0xFF455A64),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun ResetButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF757575),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "重置数据",
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 获取当前时间戳（跨平台兼容）
 */
private fun getTimeMillis(): Long {
    return java.lang.System.currentTimeMillis()
}
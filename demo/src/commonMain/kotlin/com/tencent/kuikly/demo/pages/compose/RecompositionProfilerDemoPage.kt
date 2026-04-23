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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyVerticalGrid
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.profiler.RecompositionProfiler
import com.tencent.kuikly.compose.profiler.filter.ComposableFilter
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.FileModule
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.lifecycle.ViewModel
import com.tencent.kuikly.lifecycle.viewModelScope
import com.tencent.kuikly.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * RecompositionProfiler 示例页面。
 *
 * 演示：
 * 1. 使用 RecompositionProfiler API 启停追踪
 * 2. CompositionTracer 自动追踪所有 Composable 重组（无需手动包装）
 * 3. 三种输出模式的切换（Log / Overlay / JSON）
 * 4. 获取并展示 JSON 报告
 * 5. 滚动场景重组追踪（LazyColumn 内嵌 LazyColumn）
 * 6. 外部 ViewModel 状态变更追踪
 * 7. 自定义 data class 作为 State 类型追踪
 */
@Page("RecompositionProfilerDemo")
internal class RecompositionProfilerDemoPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Recomposition Profiler") {
                RecompositionProfilerDemo()
            }
        }
    }
}

@Composable
private fun RecompositionProfilerDemo() {
    // 初始状态从 Profiler 单例读取，避免退出再进后按钮状态重置
    var profilerEnabled by remember { mutableStateOf(RecompositionProfiler.isEnabled) }
    var overlayEnabled by remember { mutableStateOf(false) }
    var reportJson by remember { mutableStateOf("") }

    // 外层用 LazyColumn，支持滚动
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // === 控制面板 ===
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Controls", fontSize = 18.sp, color = Color.Black)
        }

        item {
            // Profiler 启停
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = if (profilerEnabled) "Stop Profiler" else "Start Profiler",
                    color = if (profilerEnabled) Color(0xFFF44336.toInt()) else Color(0xFF4CAF50.toInt())
                ) {
                    if (profilerEnabled) {
                        RecompositionProfiler.stop()
                        profilerEnabled = false
                    } else {
                        RecompositionProfiler.configure {
                            sampleRate = 1.0f
                            hotspotThreshold = 5
                            enableLog = true      // 默认开，打印日志
                            enableFile = true     // 默认开，写文件
                            enableOverlay = overlayEnabled
                            overlayTopCount = 10
                        }
                        RecompositionProfiler.start()
                        profilerEnabled = true
                    }
                }

                ActionButton(text = "Reset", color = Color(0xFF2196F3.toInt())) {
                    RecompositionProfiler.reset()
                    reportJson = ""
                }

                ActionButton(text = "Get Report", color = Color(0xFF9C27B0.toInt())) {
                    val report = RecompositionProfiler.getReport(saveToFile = true)
                    reportJson = report.toJson()
                }
            }
        }

        item {
            // TC-01: 基础过滤名称测试
            Text("[TC-01] Filter by Name", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Exclude CounterSection", color = Color(0xFFFF5722.toInt())) {
                    RecompositionProfiler.excludeByName("CounterSection", "StatusBar")
                    println("[TC01] excludeByName called — check RCProfiler log for: names: [CounterSection, StatusBar], prefixes: []")
                }
                ActionButton(text = "Clear Filters", color = Color(0xFF607D8B.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    println("[TC01] clearCustomFilters called — check RCProfiler log for: Custom filter cleared")
                }
            }
        }

        item {
            // TC-02: 追加语义 + TC-05: 空白字符串 + TC-06: 幂等
            Text("[TC-02/05/06] Append + Blank + Idempotent", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-02/05/06", color = Color(0xFF3F51B5.toInt())) {
                    // 先清空
                    RecompositionProfiler.clearCustomFilters()
                    // TC-02: 追加语义
                    RecompositionProfiler.excludeByName("A")
                    RecompositionProfiler.excludeByName("B")
                    println("[TC02] After two excludeByName calls — expect names: [A, B] (not just [B])")
                    // TC-05: 空白字符串
                    RecompositionProfiler.excludeByName("", "  ", "ValidName")
                    println("[TC05] After blank strings — expect names: [A, B, ValidName] (no empty strings)")
                    // TC-06: 幂等
                    RecompositionProfiler.excludeByName("ValidName")
                    println("[TC06] After duplicate add — expect names still: [A, B, ValidName] (Set, no dups)")
                }
            }
        }

        item {
            // TC-03: 前缀过滤
            Text("[TC-03] Prefix Filter", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Exclude by Prefix", color = Color(0xFF009688.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    RecompositionProfiler.excludeByPrefix("com.tencent.kuikly.demo.pages.compose.Counter")
                    println("[TC03] excludeByPrefix called — expect prefixes: [com.tencent.kuikly.demo.pages.compose.Counter]")
                    println("[TC03] CounterSection should no longer appear in logs")
                }
            }
        }

        item {
            // TC-04: vararg / List 两种方式
            Text("[TC-04] vararg vs List", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-04", color = Color(0xFF795548.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    RecompositionProfiler.excludeByName("Foo", "Bar")       // vararg
                    RecompositionProfiler.excludeByName(listOf("Baz"))      // List
                    println("[TC04] vararg + List — expect names: [Bar, Baz, Foo] (sorted)")
                }
            }
        }

        item {
            // TC-08: 清空过滤 + 连续清空幂等
            Text("[TC-08] Clear Filters", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-08", color = Color(0xFFE91E63.toInt())) {
                    // 先加规则
                    RecompositionProfiler.excludeByName("Foo")
                    println("[TC08] Added Foo — expect 'Custom filter updated' log")
                    // 清空
                    RecompositionProfiler.clearCustomFilters()
                    println("[TC08] Cleared — expect 'Custom filter cleared' log")
                    // 连续清空（应无日志输出）
                    RecompositionProfiler.clearCustomFilters()
                    println("[TC08] Cleared again — expect NO 'Custom filter cleared' log (already empty)")
                }
            }
        }

        item {
            // TC-10: Report 过滤快照 + TC-11: saveToFile=false 无日志
            Text("[TC-10/11] Report Snapshot", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-10/11", color = Color(0xFF9C27B0.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    RecompositionProfiler.excludeByName("Widget1")
                    RecompositionProfiler.excludeByPrefix("com.myapp.")
                    // TC-10: Report 快照
                    val report = RecompositionProfiler.getReport(saveToFile = false)
                    println("[TC10] report.filteredNames=${report.filteredNames}")
                    println("[TC10] report.filteredPrefixes=${report.filteredPrefixes}")
                    println("[TC10] JSON snippet: ${report.toJson().let { json ->
                        val start = json.indexOf("\"filteredNames\"")
                        if (start >= 0) json.substring(start, minOf(start + 120, json.length)) else "NOT FOUND"
                    }}")
                    // TC-11: saveToFile=false 应无 === Recomposition Report === 日志
                    println("[TC11] Above getReport used saveToFile=false — check NO '=== Recomposition Report ===' in RCProfiler logs")
                    // 对比：saveToFile=true
                    val report2 = RecompositionProfiler.getReport(saveToFile = true)
                    println("[TC11] This getReport used saveToFile=true — check '=== Recomposition Report ===' APPEARS in RCProfiler logs")
                }
            }
        }

        item {
            // TC-07: configure 不覆盖 excludeByName
            Text("[TC-07] configure vs excludeByName", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-07", color = Color(0xFF00BCD4.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    // 先用 configure 设置一个 static filter
                    RecompositionProfiler.configure {
                        customFilters = listOf(object : ComposableFilter {
                            override fun shouldFilter(composableName: String, info: String): Boolean {
                                return composableName == "StaticTarget"
                            }
                        })
                    }
                    // 再用 excludeByName 添加动态规则
                    RecompositionProfiler.excludeByName("DynamicWidget")
                    println("[TC07] configure(StaticTarget) + excludeByName(DynamicWidget) — both should coexist")
                    println("[TC07] Check: names: [DynamicWidget] in log (StaticFilter is separate, not shown in names)")
                    // 再次 configure 修改其他配置，不设置 customFilters
                    RecompositionProfiler.configure { hotspotThreshold = 20 }
                    println("[TC07] After configure{hotspotThreshold=20} — DynamicWidget should STILL be in names")
                }
            }
        }

        item {
            // TC-09: start 前配置生效
            Text("[TC-09] Config before start", fontSize = 14.sp, color = Color.Gray)
            Text("  ⚠️ 需先 Stop Profiler，再点此按钮", fontSize = 11.sp, color = Color.Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-09", color = Color(0xFFFF9800.toInt())) {
                    // 确保 stop 状态
                    RecompositionProfiler.stop()
                    RecompositionProfiler.clearCustomFilters()
                    // 先配置过滤，再启动
                    RecompositionProfiler.excludeByName("CounterSection")
                    println("[TC09] excludeByName(CounterSection) called BEFORE start")
                    RecompositionProfiler.configure { enableOverlay = true; enableLog = true }
                    RecompositionProfiler.start()
                    println("[TC09] Profiler started — now tap '+' to trigger CounterSection recomposition")
                    println("[TC09] CounterSection should NOT appear in logs (pre-start config)")
                }
            }
        }

        item {
            // TC-19: 动态修改规则 + TC-20: stop/start 保留 + TC-21: configure 多次不丢规则
            Text("[TC-19/20/21] Stability Tests", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-19/20/21", color = Color(0xFF673AB7.toInt())) {
                    // TC-19: 动态修改规则
                    RecompositionProfiler.clearCustomFilters()
                    RecompositionProfiler.excludeByName("W1")
                    println("[TC19] Step 1: excludeByName(W1)")
                    RecompositionProfiler.excludeByName("W2")
                    println("[TC19] Step 2: excludeByName(W2) — expect names: [W1, W2]")
                    RecompositionProfiler.clearCustomFilters()
                    println("[TC19] Step 3: clearCustomFilters — expect 'Custom filter cleared'")
                    RecompositionProfiler.excludeByPrefix("com.test.")
                    println("[TC19] Step 4: excludeByPrefix — expect prefixes: [com.test.]")

                    // TC-20: stop/start 规则保留
                    RecompositionProfiler.clearCustomFilters()
                    RecompositionProfiler.excludeByName("MyWidget")
                    println("[TC20] Added MyWidget, now stop+start...")
                    RecompositionProfiler.stop()
                    RecompositionProfiler.configure { enableOverlay = true; enableLog = true }
                    RecompositionProfiler.start()
                    // 检查规则是否保留
                    val report20 = RecompositionProfiler.getReport(saveToFile = false)
                    println("[TC20] After stop+start: filteredNames=${report20.filteredNames} — expect [MyWidget]")

                    // TC-21: configure 多次不丢动态规则
                    RecompositionProfiler.configure { hotspotThreshold = 15 }
                    val report21 = RecompositionProfiler.getReport(saveToFile = false)
                    println("[TC21] After configure{hotspotThreshold=15}: filteredNames=${report21.filteredNames} — expect [MyWidget] still")
                    RecompositionProfiler.configure { hotspotThreshold = 30 }
                    val report21b = RecompositionProfiler.getReport(saveToFile = false)
                    println("[TC21] After configure{hotspotThreshold=30}: filteredNames=${report21b.filteredNames} — expect [MyWidget] still")
                }
            }
        }

        item {
            // TC-22: 大量规则性能
            Text("[TC-22] Bulk Rules Performance", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(text = "Run TC-22", color = Color(0xFF455A64.toInt())) {
                    RecompositionProfiler.clearCustomFilters()
                    val names = (1..200).map { "Widget$it" }
                    val mark = kotlin.time.TimeSource.Monotonic.markNow()
                    RecompositionProfiler.excludeByName(names)
                    val elapsed = mark.elapsedNow().inWholeMilliseconds
                    println("[TC22] Added 200 rules in ${elapsed}ms — expect <50ms, no crash")
                    val report = RecompositionProfiler.getReport(saveToFile = false)
                    println("[TC22] filteredNames.size=${report.filteredNames.size} — expect 200")
                }
            }
        }

        item {
            // 输出模式切换
            Text("Output Modes", fontSize = 14.sp, color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton("Overlay", overlayEnabled) {
                    overlayEnabled = it
                }
            }
        }

        // === 示例组件 ===
        item {
            Text("Sample Components", fontSize = 18.sp, color = Color.Black)
        }

        item { CounterSection() }

        item { AutoIncrementSection() }

        item { ParentChildDemo() }

        // === 滚动场景 ===
        item { ScrollSection() }

        // === LazyRow 水平滚动 ===
        item { LazyRowSection() }

        // === LazyVerticalGrid 网格滚动 ===
        item { LazyGridSection() }

        // === HorizontalPager 翻页 ===
        item { PagerSection() }

        // === ViewModel 外部状态 ===
        item { ViewModelSection() }

        // === 自定义 data class State ===
        item { CustomStateSection() }

        // === 对象持有 State 属性 ===
        item { ObjectWithStateSection() }

        // === Filter 截断 scope 复现 ===
        item { FilterTruncatedScopeSection() }

        // === 报告 ===
        if (reportJson.isNotEmpty()) {
            item {
                Text("JSON Report", fontSize = 14.sp, color = Color.Gray)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5.toInt()), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = reportJson,
                        fontSize = 10.sp,
                        color = Color(0xFF333333.toInt())
                    )
                }
            }
        }

        // === FileModule 验证 ===
        item {
            val pager = LocalActivity.current.getPager() as Pager
            ActionButton(text = "Write Test File", color = Color(0xFF607D8B.toInt())) {
                val fileModule = pager.acquireModule<FileModule>(FileModule.MODULE_NAME)
                fileModule.writeFile("profiler_test.json", """{"test":"hello from KuiklyUI"}""") { result ->
                    println("[FILE_TEST] writeFile result=$result")
                }
                fileModule.getFilesDir { result ->
                    println("[FILE_TEST] getFilesDir result=$result")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// ========== 计数器 ==========

@Composable
private fun CounterSection() {
    var count by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("Manual Counter", fontSize = 14.sp, color = Color(0xFF1565C0.toInt()))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Count: $count", fontSize = 24.sp, color = Color.Black)
            ActionButton(text = "+1", color = Color(0xFF1976D2.toInt())) { count++ }
            ActionButton(text = "+10", color = Color(0xFF0D47A1.toInt())) { count += 10 }
        }
    }
}

// ========== 自动递增（热点检测）==========

@Composable
private fun AutoIncrementSection() {
    var autoCount by remember { mutableStateOf(0) }
    var autoRunning by remember { mutableStateOf(false) }

    LaunchedEffect(autoRunning) {
        while (autoRunning) {
            delay(100L)
            autoCount++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFCE4EC.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text("Auto Increment (hotspot demo)", fontSize = 14.sp, color = Color(0xFFC62828.toInt()))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Auto: $autoCount", fontSize = 24.sp, color = Color.Black)
            ActionButton(
                text = if (autoRunning) "Stop" else "Start",
                color = if (autoRunning) Color(0xFFF44336.toInt()) else Color(0xFF4CAF50.toInt())
            ) { autoRunning = !autoRunning }
        }
    }
}

// ========== 参数变更检测 ==========

@Composable
private fun ParentChildDemo() {
    var userName by remember { mutableStateOf("Alice") }
    var age by remember { mutableStateOf(20) }
    var clickCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8F5E9.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Param Change Detection Tests", fontSize = 14.sp, color = Color(0xFF2E7D32.toInt()))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(text = "Name Only", color = Color(0xFF388E3C.toInt())) {
                clickCount++
                userName = if (clickCount % 2 == 0) "Alice" else "Bob"
            }
            ActionButton(text = "Age Only", color = Color(0xFF1B5E20.toInt())) { age++ }
            ActionButton(text = "Both", color = Color(0xFF4CAF50.toInt())) {
                clickCount++
                userName = if (clickCount % 2 == 0) "Alice" else "Bob"
                age++
            }
        }
        TwoParamChild(name = userName, age = age)
        ThreeParamChild(title = "Profile", name = userName, score = age * 10)
        MiddleLayer(name = userName, count = clickCount)
        NoParamChild()
        LambdaChild(label = "Clicks: $clickCount") { clickCount++ }
    }
}

@Composable private fun TwoParamChild(name: String, age: Int) {
    Text(text = "[2-param] name=$name, age=$age", fontSize = 12.sp, color = Color(0xFF333333.toInt()))
}

@Composable private fun ThreeParamChild(title: String, name: String, score: Int) {
    Text(text = "[3-param] $title: $name (score=$score)", fontSize = 12.sp, color = Color(0xFF333333.toInt()))
}

@Composable private fun MiddleLayer(name: String, count: Int) {
    GrandChild(displayName = name)
}

@Composable private fun GrandChild(displayName: String) {
    Text(text = "[grandchild] $displayName", fontSize = 12.sp, color = Color(0xFF666666.toInt()))
}

@Composable private fun NoParamChild() {
    Text(text = "[no-param] static content", fontSize = 12.sp, color = Color(0xFF999999.toInt()))
}

@Composable private fun LambdaChild(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFFA5D6A7.toInt()), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = "[lambda] $label", fontSize = 12.sp, color = Color.Black)
    }
}

// ========== 滚动场景 ==========

/**
 * 滚动场景 — 内层独立 LazyColumn（固定高度），外层页面本身可滚动。
 * 点击列表项高亮：只有被点中和上一个选中项重组，其余项 skip。
 */
@Composable
private fun ScrollSection() {
    var selectedIndex by remember { mutableStateOf(-1) }
    val scrollItems = remember {
        (1..30).map { i -> "Item #$i — tap to highlight" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E5F5.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Scroll Scene (LazyColumn)", fontSize = 14.sp, color = Color(0xFF6A1B9A.toInt()))
        Text(
            text = "Tap item to select — only 2 items recompose per tap",
            fontSize = 11.sp, color = Color.Gray
        )
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(scrollItems.size) { index ->
                    ScrollListItem(
                        text = scrollItems[index],
                        isSelected = index == selectedIndex,
                        onClick = { selectedIndex = if (selectedIndex == index) -1 else index }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollListItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFFCE93D8.toInt()) else Color(0xFFEDE7F6.toInt())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = if (isSelected) "✓ $text" else text,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF4A148C.toInt()) else Color(0xFF333333.toInt())
        )
    }
}

// ========== LazyRow 水平滚动 ==========

@Composable
private fun LazyRowSection() {
    val items = remember { (1..20).map { "H-Item #$it" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0F2F1.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Horizontal Scroll (LazyRow)", fontSize = 14.sp, color = Color(0xFF004D40.toInt()))
        Text("Swipe left/right to scroll", fontSize = 11.sp, color = Color.Gray)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items.size) { index ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF80CBC4.toInt()), RoundedCornerShape(4.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = items[index], fontSize = 12.sp, color = Color(0xFF004D40.toInt()))
                }
            }
        }
    }
}

// ========== LazyVerticalGrid 网格滚动 ==========

@Composable
private fun LazyGridSection() {
    val gridItems = remember { (1..30).map { "Grid #$it" } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFBE9E7.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Grid Scroll (LazyVerticalGrid)", fontSize = 14.sp, color = Color(0xFFBF360C.toInt()))
        Text("Scroll up/down in the grid", fontSize = 11.sp, color = Color.Gray)
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(gridItems.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(Color(0xFFFFAB91.toInt()), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = gridItems[index], fontSize = 11.sp, color = Color(0xFF3E2723.toInt()))
                    }
                }
            }
        }
    }
}

// ========== HorizontalPager 翻页 ==========

@Composable
private fun PagerSection() {
    val pagerState = rememberPagerState { 5 }
    val pageColors = listOf(
        Color(0xFFBBDEFB.toInt()),
        Color(0xFFC8E6C9.toInt()),
        Color(0xFFFFE0B2.toInt()),
        Color(0xFFF8BBD0.toInt()),
        Color(0xFFD1C4E9.toInt()),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E5F5.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Pager (HorizontalPager)", fontSize = 14.sp, color = Color(0xFF4A148C.toInt()))
        Text("Swipe left/right to flip pages", fontSize = 11.sp, color = Color.Gray)
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .background(pageColors[page], RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Page ${page + 1}", fontSize = 24.sp, color = Color(0xFF333333.toInt()))
                }
            }
        }
    }
}

// ========== ViewModel 外部状态 ==========

/**
 * ViewModel 外部状态场景：
 * - counter / userName / isProcessing 独立 StateFlow
 * - ViewModelCounterDisplay 只订阅 counter，userName 变化时不重组
 * - ViewModelUserDisplay 只订阅 userName，counter 变化时不重组
 */
@Composable
private fun ViewModelSection() {
    val vm: ProfilerDemoViewModel = viewModel { ProfilerDemoViewModel() }
    val counter by vm.counter.collectAsState()
    val userName by vm.userName.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8EAF6.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("ViewModel External State", fontSize = 14.sp, color = Color(0xFF283593.toInt()))
        Text(
            text = "CounterDisplay vs UserDisplay recompose independently",
            fontSize = 11.sp, color = Color.Gray
        )
        ViewModelCounterDisplay(counter = counter, isProcessing = isProcessing)
        ViewModelUserDisplay(userName = userName)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(text = "+1", color = Color(0xFF3949AB.toInt())) { vm.increment() }
            ActionButton(text = "+10", color = Color(0xFF1A237E.toInt())) { vm.incrementBy(10) }
            ActionButton(text = "Reset", color = Color(0xFF5C6BC0.toInt())) { vm.reset() }
            ActionButton(
                text = if (isProcessing) "Stop Auto" else "Auto +1",
                color = if (isProcessing) Color(0xFFF44336.toInt()) else Color(0xFF4CAF50.toInt())
            ) { vm.toggleAutoIncrement() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(text = "→ Alice", color = Color(0xFF7986CB.toInt())) { vm.setUserName("Alice") }
            ActionButton(text = "→ Bob", color = Color(0xFF7986CB.toInt())) { vm.setUserName("Bob") }
            ActionButton(text = "→ Charlie", color = Color(0xFF7986CB.toInt())) { vm.setUserName("Charlie") }
        }
    }
}

/** 只读 counter + isProcessing，userName 变化时 Compose 会 skip 此组件 */
@Composable
private fun ViewModelCounterDisplay(counter: Int, isProcessing: Boolean) {
    Text(
        text = "Counter: $counter${if (isProcessing) " (auto)" else ""}",
        fontSize = 20.sp, color = Color(0xFF1A237E.toInt())
    )
}

/** 只读 userName，counter 变化时 Compose 会 skip 此组件 */
@Composable
private fun ViewModelUserDisplay(userName: String) {
    Text(text = "User: $userName", fontSize = 14.sp, color = Color(0xFF3949AB.toInt()))
}

// ========== 自定义 data class State ==========

/**
 * 自定义类型 State 场景。
 *
 * 用一个 data class [UserProfile] 作为整体 State，
 * 演示当只修改其中一个字段时，Profiler 如何追踪 State 变更。
 *
 * 与 ViewModel 场景的区别：
 * - ViewModel：多个独立 StateFlow，各字段变化互不影响
 * - 此处：整体替换 data class，任何字段变化都触发同一个 State 变更
 *   → 可观察到父组件每次都重组，但子组件因参数未变而 skip
 */
@Composable
private fun CustomStateSection() {
    var profile by remember { mutableStateOf(UserProfile(name = "Alice", level = 1, score = 0)) }
    var logCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Custom data class State", fontSize = 14.sp, color = Color(0xFFE65100.toInt()))
        Text(
            text = "One State holds the whole object — partial field change still triggers full State update",
            fontSize = 11.sp, color = Color.Gray
        )

        // 三个子组件各只读一个字段
        ProfileNameCard(name = profile.name)
        ProfileLevelCard(level = profile.level)
        ProfileScoreCard(score = profile.score)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // 只改 name — level/score 子组件应 skip
            ActionButton(text = "Rename", color = Color(0xFFF57C00.toInt())) {
                logCount++
                profile = profile.copy(name = if (logCount % 2 == 0) "Alice" else "Bob")
            }
            // 只改 level — name/score 子组件应 skip
            ActionButton(text = "Level+1", color = Color(0xFFEF6C00.toInt())) {
                profile = profile.copy(level = profile.level + 1)
            }
            // 只改 score — name/level 子组件应 skip
            ActionButton(text = "Score+10", color = Color(0xFFE65100.toInt())) {
                profile = profile.copy(score = profile.score + 10)
            }
            // 全部改
            ActionButton(text = "All", color = Color(0xFFBF360C.toInt())) {
                logCount++
                profile = UserProfile(
                    name = if (logCount % 2 == 0) "Alice" else "Bob",
                    level = profile.level + 1,
                    score = profile.score + 10
                )
            }
        }
    }
}

/** 只读取 name 字段 */
@Composable
private fun ProfileNameCard(name: String) {
    Text(text = "Name: $name", fontSize = 13.sp, color = Color(0xFF4E342E.toInt()))
}

/** 只读取 level 字段 */
@Composable
private fun ProfileLevelCard(level: Int) {
    Text(text = "Level: $level", fontSize = 13.sp, color = Color(0xFF6D4C41.toInt()))
}

/** 只读取 score 字段 */
@Composable
private fun ProfileScoreCard(score: Int) {
    Text(text = "Score: $score", fontSize = 13.sp, color = Color(0xFF795548.toInt()))
}

/**
 * 自定义 State 类型 — data class 自动生成 equals/hashCode，
 * copy() 只改一个字段时，其他字段 == 不变，子组件参数未变则 skip。
 */
data class UserProfile(
    val name: String,
    val level: Int,
    val score: Int
)

// ========== 通用按钮 ==========

@Composable
private fun ActionButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
private fun ToggleButton(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val bgColor = if (enabled) Color(0xFF4CAF50.toInt()) else Color(0xFFBDBDBD.toInt())
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.White)
    }
}

// ========== ViewModel ==========

class ProfilerDemoViewModel : ViewModel() {
    private val _counter = MutableStateFlow(0)
    val counter: StateFlow<Int> = _counter.asStateFlow()

    private val _userName = MutableStateFlow("Alice")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun increment() { _counter.value++ }
    fun incrementBy(amount: Int) { _counter.value += amount }
    fun reset() { _counter.value = 0 }
    fun setUserName(name: String) { _userName.value = name }

    fun toggleAutoIncrement() {
        _isProcessing.value = !_isProcessing.value
        if (_isProcessing.value) {
            viewModelScope.launch {
                while (_isProcessing.value) {
                    delay(200L)
                    _counter.value++
                }
            }
        }
    }
}

// ========== 对象持有 State 属性 ==========

/**
 * 普通 class，内部属性是 MutableState。
 * 演示"对象包 State"模式：直接修改属性就能触发重组，
 * 且各属性的 State 互相独立，只有对应 scope 重组。
 */
class AppCounter {
    var count by mutableStateOf(0)
    var label by mutableStateOf("Counter")
}

/**
 * 演示"对象持有 State 属性"场景。
 *
 * 与 data class State 的对比：
 * - data class State：整体替换，所有依赖该 State 的 scope 全部重组
 * - 对象持有 State 属性：每个属性是独立 State，只有读取该属性的 scope 重组
 */
@Composable
private fun ObjectWithStateSection() {
    val counter = remember { AppCounter() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE8EAF6.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Object with State Properties", fontSize = 14.sp, color = Color(0xFF283593.toInt()))
        Text(
            text = "Plain class holds mutableStateOf fields — each field is an independent State",
            fontSize = 11.sp, color = Color.Gray
        )

        // 各子组件只读一个属性，修改 count 时 LabelDisplay 应 skip
        CountDisplay(count = counter.count)
        LabelDisplay(label = counter.label)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionButton(text = "Count++", color = Color(0xFF3F51B5.toInt())) {
                counter.count++
            }
            ActionButton(text = "Change Label", color = Color(0xFF5C6BC0.toInt())) {
                counter.label = if (counter.label == "Counter") "Clicked!" else "Counter"
            }
            ActionButton(text = "Both", color = Color(0xFF7986CB.toInt())) {
                counter.count++
                counter.label = if (counter.label == "Counter") "Clicked!" else "Counter"
            }
        }
    }
}

@Composable
private fun CountDisplay(count: Int) {
    Text(
        text = "count = $count",
        fontSize = 13.sp,
        color = Color(0xFF283593.toInt())
    )
}

@Composable
private fun LabelDisplay(label: String) {
    Text(
        text = "label = $label",
        fontSize = 13.sp,
        color = Color(0xFF3949AB.toInt())
    )
}

// ========== Filter 截断 scope 复现 ==========

/**
 * CompositionLocal 用于演示 filter 截断问题。
 * CompositionLocalProvider 是 androidx.compose.runtime 包下的框架组件，
 * 会被 builtin filter 过滤掉。当它是 scope 的直接持有者时，
 * 修复前：子节点 scope=none（filter 截断导致链路断裂）
 * 修复后：子节点应该有正确的 scope 值
 */
private val LocalThemeColor = compositionLocalOf { Color(0xFF333333.toInt()) }

@Composable
private fun FilterTruncatedScopeSection() {
    var themeToggle by remember { mutableStateOf(false) }
    val currentColor = if (themeToggle) Color(0xFFE91E63.toInt()) else Color(0xFF333333.toInt())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0F7FA.toInt()), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Filter Truncated Scope (CompositionLocalProvider)",
            fontSize = 14.sp,
            color = Color(0xFF00695C.toInt())
        )
        Text(
            text = "State → CompositionLocalProvider (filtered) → children. " +
                "Fix: children should show scope != none after toggle.",
            fontSize = 11.sp,
            color = Color.Gray
        )

        // CompositionLocalProvider 会被 builtin filter 过滤（属于 androidx.compose.runtime）
        // 点击 Toggle 后，themeToggle 变化触发 FilterTruncatedScopeSection 重组，
        // CompositionLocalProvider 被 filter，子节点 ThemedTitle / ThemedSubtitle 应继承 scope
        CompositionLocalProvider(LocalThemeColor provides currentColor) {
            ThemedTitle(title = "Hello Profiler")
            ThemedSubtitle(subtitle = if (themeToggle) "Pink Mode" else "Default Mode")
        }

        ActionButton(text = "Toggle Theme", color = Color(0xFF00897B.toInt())) {
            themeToggle = !themeToggle
        }
    }
}

@Composable
private fun ThemedTitle(title: String) {
    val color = LocalThemeColor.current
    Text(text = title, fontSize = 16.sp, color = color)
}

@Composable
private fun ThemedSubtitle(subtitle: String) {
    val color = LocalThemeColor.current
    Text(text = subtitle, fontSize = 12.sp, color = color)
}

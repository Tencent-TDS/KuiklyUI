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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.profiler.RecompositionProfiler
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Compose 稳定性规则验证页面
 *
 * 对应测试计划：compose-stability-test-plan.md
 *
 * 每个 Section 对应一个测试用例（T1-T9）。
 * 使用方式：
 * 1. 启动 Profiler（Start Profiler 按钮）
 * 2. 在目标 Section 点击「触发父重组」按钮 5 次
 * 3. 停止 Profiler，查看 adb logcat / profiler_report.json
 * 4. 验证对应子组件的 recompositionCount 是否符合预期
 */
@Page("ComposeStabilityTest")
internal class ComposeStabilityTestPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Stability Test") {
                StabilityTestMain()
            }
        }
    }
}

// ============================================================
// 测试数据类型定义
// ============================================================

// T1：全 val + 稳定类型 → 自动稳定，无需注解
data class T1StableData(val id: Int, val name: String)

// T2：含 var → 不稳定
data class T2UnstableData(val id: Int, var name: String)

// T3：含 List → 不稳定
data class T3DataWithList(val id: Int, val items: List<String>)

// T4：含 ImmutableList → 稳定
data class T4DataWithImmutableList(val id: Int, val items: ImmutableList<String>)

// T5：含 List 但加 @Immutable → 覆盖编译器推断
@Immutable
data class T5ImmutableDataWithList(val id: Int, val items: List<String>)

// T6：含 var 但加 @Stable → 承诺无法兑现（危险用法）
@Stable
data class T6FakeStableData(val id: Int, var name: String)

// T7：data class copy 相同内容 → equals() 返回 true，可 skip
data class T7CopiedData(val id: Int, val name: String)

// T8：普通 @Stable class，无 equals() → 每次新实例引用不等，无法 skip
@Stable
class T8StableClass(val id: Int, val name: String)

// T9：@Stable 接口 + 稳定 data class 实现 → 可 skip
@Stable
interface T9StableItem {
    val id: Int
    val name: String
}
data class T9StableItemImpl(override val id: Int, override val name: String) : T9StableItem

// ============================================================
// 主页面
// ============================================================

@Composable
private fun StabilityTestMain() {
    var profilerEnabled by remember { mutableStateOf(RecompositionProfiler.isEnabled) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Profiler 控制
        item {
            StabilityActionButton(
                text = if (profilerEnabled) "Stop Profiler" else "Start Profiler",
                color = if (profilerEnabled) Color(0xFFF44336.toInt()) else Color(0xFF4CAF50.toInt())
            ) {
                if (profilerEnabled) {
                    RecompositionProfiler.stop()
                    profilerEnabled = false
                } else {
                    RecompositionProfiler.configure {
                        enableLog = true
                        enableFile = true
                    }
                    RecompositionProfiler.start()
                    profilerEnabled = true
                }
            }
        }

        item {
            Text(
                "启动 Profiler 后，在各 Section 点击按钮 5 次，再 Stop，\n查看 recompositionCount 是否符合预期。",
                fontSize = 12.sp, color = Color.Gray
            )
        }

        item { SectionDivider("T1：全 val data class — 预期 skip（recompositionCount=0）") }
        item { T1Section() }

        item { SectionDivider("T2：含 var data class — 预期不 skip（recompositionCount=5）") }
        item { T2Section() }

        item { SectionDivider("T3：含 List data class — 预期不 skip（recompositionCount=5）") }
        item { T3Section() }

        item { SectionDivider("T4：含 ImmutableList data class — 预期 skip（recompositionCount=0）") }
        item { T4Section() }

        item { SectionDivider("T5：@Immutable + List — 预期 skip（recompositionCount=0）") }
        item { T5Section() }

        item { SectionDivider("T6：@Stable + var 直接赋值 — 预期 skip 但界面过时（危险）") }
        item { T6Section() }

        item { SectionDivider("T7：data class copy 相同内容 — 预期 skip（recompositionCount=0）") }
        item { T7Section() }

        item { SectionDivider("T8：@Stable 普通 class 每次新实例 — 预期不 skip（recompositionCount=5）") }
        item { T8Section() }

        item { SectionDivider("T9：@Stable 接口 + 稳定实现 — 预期 skip（recompositionCount=0）") }
        item { T9Section() }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

// ============================================================
// T1：全 val + 稳定类型 → 自动稳定
// 预期：子组件 skip，recompositionCount=0
// 设计：每次父重组 new 新实例（内容相同），验证 stable 类型因 equals() 可以 skip
// ============================================================
@Composable
private fun T1Section() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "子组件参数类型：T1StableData（全 val + String/Int）\n每次父重组 new 新实例但内容相同 → 应 skip",
            fontSize = 11.sp, color = Color(0xFF555555.toInt())
        )
        T1Parent()
    }
}

@Composable
private fun T1Parent() {
    var trigger by remember { mutableStateOf(0) }
    val data = T1StableData(1, "Alice") // 不用 remember，每次新实例但内容相同
    StabilityActionButton("触发重组（$trigger）", Color(0xFF2196F3.toInt())) { trigger++ }
    T1Child(data)
}

@Composable
private fun T1Child(data: T1StableData) {
    ChildDisplay("T1Child", "id=${data.id} name=${data.name}")
}

// ============================================================
// T2：含 var → 不稳定
// 预期：子组件无法 skip，recompositionCount=5
// 设计：父组件（T2Parent）持有 trigger state，子组件（T2Child）只接收 data
// T2Parent 和 T2Child 是完全分离的 Composable，没有共享 scope
// ============================================================
@Composable
private fun T2Section() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "子组件参数类型：T2UnstableData（含 var name）\n每次父重组传新实例，类型不稳定 → 应重组 5 次",
            fontSize = 11.sp, color = Color(0xFF555555.toInt())
        )
        T2Parent()
    }
}

@Composable
private fun T2Parent() {
    var trigger by remember { mutableStateOf(0) }
    val data = T2UnstableData(trigger, "Alice_$trigger") // trigger 变化 → 新实例且内容不同
    StabilityActionButton("触发重组（$trigger）", Color(0xFF2196F3.toInt())) { trigger++ }
    T2Child(data)
}

@Composable
private fun T2Child(data: T2UnstableData) {
    ChildDisplay("T2Child", "id=${data.id} name=${data.name}")
}

// ============================================================
// T3：含 List → 不稳定
// 预期：子组件无法 skip，recompositionCount=5
// ============================================================
@Composable
private fun T3Section() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "子组件参数类型：T3DataWithList（含 List<String>）\nList 接口不稳定，每次传新实例 → 应重组 5 次",
            fontSize = 11.sp, color = Color(0xFF555555.toInt())
        )
        T3Parent()
    }
}

@Composable
private fun T3Parent() {
    var trigger by remember { mutableStateOf(0) }
    val data = T3DataWithList(trigger, listOf("a_$trigger", "b", "c")) // 每次新实例且内容不同
    StabilityActionButton("触发重组（$trigger）", Color(0xFF2196F3.toInt())) { trigger++ }
    T3Child(data)
}

@Composable
private fun T3Child(data: T3DataWithList) {
    ChildDisplay("T3Child", "id=${data.id} items=${data.items}")
}

// ============================================================
// T4：含 ImmutableList → 稳定
// 预期：子组件 skip，recompositionCount=0
// ============================================================
@Composable
private fun T4Section() {
    var trigger by remember { mutableStateOf(0) }
    val data = remember { T4DataWithImmutableList(1, persistentListOf("a", "b", "c")) }

    TestSectionContent(
        trigger = trigger,
        onTrigger = { trigger++ },
        description = "子组件参数类型：T4DataWithImmutableList（含 ImmutableList）\nImmutableList 稳定，应 skip → 应重组 0 次"
    ) {
        T4Child(data)
    }
}

@Composable
private fun T4Child(data: T4DataWithImmutableList) {
    ChildDisplay("T4Child", "id=${data.id} items=${data.items}")
}

// ============================================================
// T5：含 List 但加 @Immutable → 覆盖编译器推断
// 预期：子组件 skip，recompositionCount=0（调用方复用同一实例）
// ============================================================
@Composable
private fun T5Section() {
    var trigger by remember { mutableStateOf(0) }
    val data = remember { T5ImmutableDataWithList(1, listOf("a", "b", "c")) } // 固定实例

    TestSectionContent(
        trigger = trigger,
        onTrigger = { trigger++ },
        description = "子组件参数类型：T5ImmutableDataWithList（@Immutable + List）\n注解覆盖推断，调用方复用同一实例 → 应 skip"
    ) {
        T5Child(data)
    }
}

@Composable
private fun T5Child(data: T5ImmutableDataWithList) {
    ChildDisplay("T5Child", "id=${data.id} items=${data.items}")
}

// ============================================================
// T6：@Stable + var 直接赋值 → 承诺无法兑现（危险）
// 预期：T6Child skip（recompositionCount=0），但 name 显示不更新（界面 bug）
// ============================================================
@Composable
private fun T6Section() {
    var trigger by remember { mutableStateOf(0) }
    val data = remember { T6FakeStableData(1, "Alice") }

    TestSectionContent(
        trigger = trigger,
        onTrigger = {
            trigger++
            data.name = "Bob_$trigger" // 直接赋值 var，不通过 MutableState
        },
        description = "子组件参数类型：T6FakeStableData（@Stable + var name）\n点击会修改 var，但 Compose 不知道 → skip 发生但 name 不更新\n【验证 @Stable + var 的危险性】"
    ) {
        T6Child(data)
    }
}

@Composable
private fun T6Child(data: T6FakeStableData) {
    ChildDisplay("T6Child", "id=${data.id} name=${data.name}（不应更新为 Bob）")
}

// ============================================================
// T7：data class copy 相同内容 → equals() 返回 true，可以 skip
// 预期：子组件 skip，recompositionCount=0
// 设计：每次父重组 new 新实例但内容相同，对比 T8（普通 class 无 equals()）
// ============================================================
@Composable
private fun T7Section() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "子组件参数类型：T7CopiedData（全 val，每次 new 新实例但内容相同）\ndata class equals() 比较内容 → 应 skip（对比 T8）",
            fontSize = 11.sp, color = Color(0xFF555555.toInt())
        )
        T7Parent()
    }
}

@Composable
private fun T7Parent() {
    var trigger by remember { mutableStateOf(0) }
    val data = T7CopiedData(1, "Alice") // 不用 remember，每次新实例但内容相同
    StabilityActionButton("触发重组（$trigger）", Color(0xFF2196F3.toInt())) { trigger++ }
    T7Child(data)
}

@Composable
private fun T7Child(data: T7CopiedData) {
    ChildDisplay("T7Child", "id=${data.id} name=${data.name}")
}

// ============================================================
// T8：@Stable 普通 class，无 equals()，每次新实例 → 引用不等，无法 skip
// 预期：子组件无法 skip，recompositionCount=5
// ============================================================
@Composable
private fun T8Section() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "子组件参数类型：T8StableClass（@Stable 普通 class，每次 new 新实例）\n无 equals()，引用不等 → 无法 skip（对比 T7）",
            fontSize = 11.sp, color = Color(0xFF555555.toInt())
        )
        T8Parent()
    }
}

@Composable
private fun T8Parent() {
    var trigger by remember { mutableStateOf(0) }
    val data = T8StableClass(trigger, "Alice_$trigger") // 每次新实例且内容不同
    StabilityActionButton("触发重组（$trigger）", Color(0xFF2196F3.toInt())) { trigger++ }
    T8Child(data)
}

@Composable
private fun T8Child(data: T8StableClass) {
    ChildDisplay("T8Child", "id=${data.id} name=${data.name}")
}

// ============================================================
// T9：@Stable 接口 + 稳定 data class 实现 → 可 skip
// 预期：子组件 skip，recompositionCount=0
// ============================================================
@Composable
private fun T9Section() {
    var trigger by remember { mutableStateOf(0) }
    val item: T9StableItem = remember { T9StableItemImpl(1, "Alice") } // 固定实例

    TestSectionContent(
        trigger = trigger,
        onTrigger = { trigger++ },
        description = "子组件参数类型：T9StableItem（@Stable 接口，实现为 data class）\n接口加 @Stable，实现类稳定 → 应 skip"
    ) {
        T9Child(item)
    }
}

@Composable
private fun T9Child(item: T9StableItem) {
    ChildDisplay("T9Child", "id=${item.id} name=${item.name}")
}

// ============================================================
// 通用 UI 组件
// ============================================================

@Composable
private fun TestSectionContent(
    trigger: Int,
    onTrigger: () -> Unit,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5.toInt()))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(description, fontSize = 11.sp, color = Color(0xFF555555.toInt()))
        // 按钮单独放一个 Composable，读取 trigger 的 scope 与 content() 隔离
        TriggerButton(trigger = trigger, onTrigger = onTrigger)
        // content 在独立 scope 里，不会因 trigger 被标记为 reader
        content()
    }
}

// 独立的按钮组件，只有它读取 trigger，与子组件 scope 隔离
@Composable
private fun TriggerButton(trigger: Int, onTrigger: () -> Unit) {
    StabilityActionButton(
        text = "触发父重组（$trigger）",
        color = Color(0xFF2196F3.toInt()),
        onClick = onTrigger
    )
}

@Composable
private fun ChildDisplay(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE3F2FD.toInt()))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("[$label]", fontSize = 12.sp, color = Color(0xFF1565C0.toInt()))
        Text(value, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
private fun SectionDivider(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            title,
            fontSize = 13.sp,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF37474F.toInt()))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StabilityActionButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 12.sp, color = Color.White)
    }
}

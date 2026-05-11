package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.core.annotations.Page

/**
 * 复用进阶测试：嵌套滚动 在跨类型复用场景下的行为验证。
 *
 * 外层 LazyColumn 按 rowIndex % 3 交替展示三种 item：
 *   0: 嵌套滚动 LazyColumn（纵向，SELF_FIRST / SELF_FIRST — 内层优先）
 *   1: 嵌套滚动 LazyColumn（纵向，PARENT_FIRST / PARENT_FIRST — 外层优先）
 *   2: 普通 LazyRow（横向）
 *
 * 不设 contentType，三种类型共享同一个 ScrollerView 复用池。
 * 验证点：
 *   - SELF_FIRST 嵌套：内层可独立滚动，到底后外层接管
 *   - PARENT_FIRST 嵌套：外层优先消费，内层基本不动
 *   - 纵向/横向切换复用后滚动方向正确
 *   - 嵌套滚动模式在复用后保持正确
 */
@Page("ScrollReuseAdvancedDemo")
class ScrollReuseAdvancedDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                AdvancedReuseTest()
            }
        }
    }
}

private enum class ItemType { NESTED_SELF, NESTED_PARENT, LAZY_ROW }

private fun itemTypeOf(index: Int): ItemType = when (index % 3) {
    0 -> ItemType.NESTED_SELF
    1 -> ItemType.NESTED_PARENT
    else -> ItemType.LAZY_ROW
}

private val colors = listOf(
    Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
)

@Composable
private fun AdvancedReuseTest() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                text = "NestedScroll(SELF_FIRST) / NestedScroll(PARENT_FIRST) / LazyRow 交替，不设 contentType",
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = Color.Gray,
            )
        }

        items(30) { rowIndex ->
            val type = itemTypeOf(rowIndex)
            val bg = if (rowIndex % 2 == 0) Color.White else Color(0xFFF5F5F5)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bg)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 行号 + 类型标签
                Column(
                    modifier = Modifier.width(60.dp).padding(start = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = "$rowIndex", fontSize = 20.sp, color = Color(0xFF333333))
                    Text(text = type.name, fontSize = 8.sp, color = Color(0xFF999999))
                }

                when (type) {
                    ItemType.NESTED_SELF -> NestedSelfFirstSection(rowIndex)
                    ItemType.NESTED_PARENT -> NestedParentFirstSection(rowIndex)
                    ItemType.LAZY_ROW -> LazyRowSection(rowIndex)
                }
            }
        }
    }
}

// ——————————————— 嵌套滚动 SELF_FIRST（内层优先） ———————————————

@Composable
private fun NestedSelfFirstSection(rowIndex: Int) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .bouncesEnable(false)
            .nestedScroll(
                scrollUp = NestedScrollMode.SELF_FIRST,
                scrollDown = NestedScrollMode.SELF_FIRST,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items(15) { idx ->
            Card(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors[rowIndex % colors.size].copy(alpha = 0.15f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "  Row$rowIndex - SelfFirst $idx",
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

// ——————————————— 嵌套滚动 PARENT_FIRST（外层优先） ———————————————

@Composable
private fun NestedParentFirstSection(rowIndex: Int) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .bouncesEnable(false)
            .nestedScroll(
                scrollUp = NestedScrollMode.PARENT_FIRST,
                scrollDown = NestedScrollMode.PARENT_FIRST,
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items(15) { idx ->
            Card(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors[(rowIndex + 2) % colors.size].copy(alpha = 0.15f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "  Row$rowIndex - ParentFirst $idx",
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

// ——————————————— 普通 LazyRow ———————————————

@Composable
private fun LazyRowSection(rowIndex: Int) {
    val listState = rememberLazyListState()
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .bouncesEnable(false),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        items(20) { idx ->
            Card(
                modifier = Modifier.width(100.dp).height(170.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors[(rowIndex + 4) % colors.size].copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$rowIndex.$idx",
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

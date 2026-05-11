package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
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
import com.tencent.kuikly.compose.material3.pullToRefreshItem
import com.tencent.kuikly.compose.material3.rememberPullToRefreshState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PullToRefresh 复用测试。
 *
 * 外层 LazyRow（横向滚动），内层按 colIndex % 2 交替展示：
 *   偶数列: 纵向 LazyColumn + PullToRefresh（下拉刷新）
 *   奇数列: 横向 LazyRow
 *
 * 外层横向，内层纵向 PullToRefresh 不会同向冲突，下拉手势可正常触发。
 * 不设 contentType，两种类型共享同一个 ScrollerView 复用池。
 * 验证点：
 *   - PullToRefresh 复用后下拉刷新功能正常
 *   - 纵向/横向切换复用后滚动方向正确
 *   - 滚动位置在复用后保持
 */
@Page("PullToRefreshReuseDemo")
class PullToRefreshReuseDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                PullRefreshReuseTest()
            }
        }
    }
}

private val colors = listOf(
    Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00ACC1),
)

@Composable
private fun PullRefreshReuseTest() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "外层 LazyRow，内层交替: PullToRefresh(纵向) / LazyRow(横向)，验证跨类型复用",
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            color = Color.Gray,
        )

        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .bouncesEnable(false),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            items(30) { colIndex ->
                val isPull = colIndex % 2 == 0
                val bg = if (colIndex % 2 == 0) Color(0xFFF0F8FF) else Color(0xFFFFF8F0)

                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(bg)
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$colIndex - ${if (isPull) "PULL" else "ROW"}",
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    if (isPull) {
                        PullRefreshColumn(colIndex)
                    } else {
                        VerticalRow(colIndex)
                    }
                }
            }
        }
    }
}

// ——————————————— 纵向 LazyColumn + PullToRefresh ———————————————

@Composable
private fun PullRefreshColumn(colIndex: Int) {
    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState(isRefreshing)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var itemCount by rememberSaveable { mutableStateOf(8) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        pullToRefreshItem(
            state = pullState,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    delay(1500)
                    itemCount += 2
                    isRefreshing = false
                }
            },
            scrollState = listState,
        )

        items(itemCount) { idx ->
            Card(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors[colIndex % colors.size].copy(alpha = 0.15f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "  Col$colIndex - Pull $idx",
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

// ——————————————— 纵向 LazyColumn（普通，无 PullToRefresh） ———————————————

@Composable
private fun VerticalRow(colIndex: Int) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .bouncesEnable(false),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        items(20) { idx ->
            Card(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors[(colIndex + 4) % colors.size].copy(alpha = 0.2f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "  Col$colIndex - Item $idx",
                        fontSize = 13.sp,
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

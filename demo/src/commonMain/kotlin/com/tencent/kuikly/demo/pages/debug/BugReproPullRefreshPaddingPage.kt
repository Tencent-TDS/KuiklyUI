/*
 * Issue #1325 repro: HeaderBar overlay + pullToRefreshItem padding(top=84.dp)
 */
package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.pullToRefreshItem
import com.tencent.kuikly.compose.material3.rememberPullToRefreshState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Page("BugReproPullRefreshPaddingPage")
internal class BugReproPullRefreshPaddingPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            BugReproContent()
        }
    }
}

@Composable
private fun BugReproContent() {
    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var itemCount by remember { mutableStateOf(30) }
    val pullToRefreshState = rememberPullToRefreshState(isRefreshing)
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Issue #1325: 慢滑列表观察顶部留白与 index",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = Color.Gray,
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.White),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pullToRefreshItem(
                    state = pullToRefreshState,
                    onRefresh = {
                        scope.launch {
                            isRefreshing = true
                            delay(2000)
                            itemCount += 5
                            isRefreshing = false
                        }
                    },
                    scrollState = listState,
                    topInset = 84.dp,
                    content = { progress, refreshing, threshold ->
                        SimpleRefreshIndicator(progress, refreshing, threshold)
                    },
                )

                items(itemCount) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFFECEFF1))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("Item ${index + 1}", fontSize = 15.sp)
                    }
                }
            }

            DemoHeaderBar(listState = listState)
        }
    }
}

@Composable
private fun DemoHeaderBar(listState: LazyListState) {
    val topBarHeightDp = 48.dp
    val topBarHeightPx = with(LocalDensity.current) { topBarHeightDp.toPx() }

    val scrollProgress by remember(listState, topBarHeightPx) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            KLog.i("scrollProgress", "index=$index offset=$offset")
            when {
                topBarHeightPx <= 0f -> 0f
                index <= 0 -> (offset.toFloat() / topBarHeightPx).coerceIn(0f, 1f)
                else -> 1f
            }
        }
    }

    val currentTopBarHeightDp = with(LocalDensity.current) {
        (topBarHeightPx * (1f - scrollProgress)).toDp()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentTopBarHeightDp)
                .alpha(1f - scrollProgress),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("DEMO LOGO", color = Color.White, fontSize = 18.sp)
                Text("按钮", color = Color.White, fontSize = 14.sp)
            }
        }

        val customerWidthDp = 60.dp
        val searchEndPadding = customerWidthDp * scrollProgress

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(end = searchEndPadding)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "不压缩部分...",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .alpha(scrollProgress)
                    .padding(end = 4.dp),
            ) {
                Text("按钮", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SimpleRefreshIndicator(
    pullProgress: Float,
    isRefreshing: Boolean,
    refreshThreshold: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(refreshThreshold),
        contentAlignment = Alignment.Center,
    ) {
        val text = when {
            isRefreshing -> "Refreshing..."
            pullProgress >= 1f -> "Release to refresh"
            pullProgress > 0f -> "Pull to refresh"
            else -> ""
        }
        if (text.isNotEmpty()) {
            Text(text, fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

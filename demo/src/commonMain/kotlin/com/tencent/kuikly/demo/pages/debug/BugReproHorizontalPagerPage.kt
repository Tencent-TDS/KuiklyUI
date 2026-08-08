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

package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("BugReproHorizontalPagerPage")
internal class BugReproHorizontalPagerPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            HorizontalPagerReproScreen(
                topInset = pagerData.safeAreaInsets.top,
                bottomInset = pagerData.safeAreaInsets.bottom,
            )
        }
    }
}

private data class PagerDemoItem(
    val title: String,
    val description: String,
    val color: Color,
)

private val PagerDemoItems = listOf(
    PagerDemoItem("Page 1", "从第一页开始滑动", Color(0xFF6750A4)),
    PagerDemoItem("Page 2", "currentPage 会跟随最近页面变化", Color(0xFF006A6A)),
    PagerDemoItem("Page 3", "settledPage 会等待滚动结束", Color(0xFF9C4146)),
    PagerDemoItem("Page 4", "慢慢拖动，观察两者差异", Color(0xFF3F6374)),
    PagerDemoItem("Page 5", "快速滑动，观察日志顺序", Color(0xFF795548)),
    PagerDemoItem("Page 6", "最后一个演示页面", Color(0xFF386A20)),
)

@Composable
private fun HorizontalPagerReproScreen(
    topInset: Float,
    bottomInset: Float,
) {
    val pagerState = rememberPagerState(pageCount = { PagerDemoItems.size })
    val currentPage = pagerState.currentPage
    val settledPage = pagerState.settledPage
    val isScrollInProgress = pagerState.isScrollInProgress

    LaunchedEffect(currentPage) {
        logPageChange(
            changedState = "currentPage",
            changedValue = currentPage.toString(),
            currentPage = pagerState.currentPage,
            settledPage = pagerState.settledPage,
            isScrollInProgress = pagerState.isScrollInProgress,
        )
    }
    LaunchedEffect(settledPage) {
        logPageChange(
            changedState = "settledPage",
            changedValue = settledPage.toString(),
            currentPage = pagerState.currentPage,
            settledPage = pagerState.settledPage,
            isScrollInProgress = pagerState.isScrollInProgress,
        )
    }
    LaunchedEffect(isScrollInProgress) {
        logPageChange(
            changedState = "isScrollInProgress",
            changedValue = isScrollInProgress.toString(),
            currentPage = pagerState.currentPage,
            settledPage = pagerState.settledPage,
            isScrollInProgress = pagerState.isScrollInProgress,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7FA))
            .padding(
                top = topInset.dp + 20.dp,
                bottom = bottomInset.dp + 20.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "HorizontalPager Bug #1560",
            color = Color(0xFF1B1B1F),
            fontSize = 24.sp,
            fontWeight = FontWeight(600),
        )
        Text(
            text = "左右滑动页面，查看控制台日志",
            modifier = Modifier.padding(top = 8.dp),
            color = Color(0xFF74747B),
            fontSize = 14.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PagerStateCard(
                label = "currentPage",
                value = currentPage.toString(),
                modifier = Modifier.weight(1f),
            )
            PagerStateCard(
                label = "settledPage",
                value = settledPage.toString(),
                modifier = Modifier.weight(1f),
            )
            PagerStateCard(
                label = "isScrollInProgress",
                value = isScrollInProgress.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            val item = PagerDemoItems[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(item.color),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight(700),
                    )
                    Text(
                        text = item.description,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .padding(horizontal = 24.dp),
                        color = Color(0xD9FFFFFF),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PagerDemoItems.indices.forEach { page ->
                Box(
                    modifier = Modifier
                        .size(if (page == currentPage) 10.dp else 8.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (page == currentPage) Color(0xFF6750A4)
                            else Color(0xFFD0CDD5),
                        ),
                )
            }
        }
    }
}

@Composable
private fun PagerStateCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = Color(0xFF74747B),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 4.dp),
            color = Color(0xFF1B1B1F),
            fontSize = 18.sp,
            fontWeight = FontWeight(600),
            textAlign = TextAlign.Center,
        )
    }
}

private fun logPageChange(
    changedState: String,
    changedValue: String,
    currentPage: Int,
    settledPage: Int,
    isScrollInProgress: Boolean,
) {
    val message = "[BugReproPager] $changedState changed to $changedValue; " +
        "currentPage=$currentPage, settledPage=$settledPage, isScrollInProgress=$isScrollInProgress"
    println(message)
}
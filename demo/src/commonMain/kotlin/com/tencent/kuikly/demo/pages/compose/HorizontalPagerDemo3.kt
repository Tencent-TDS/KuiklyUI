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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("HorizontalPagerDemo3")
class HorizontalPagerDemo3 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                HorizontalPagerTest3()
            }
        }
    }

    @Composable
    fun HorizontalPagerTest3() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            item {

                val pagerState =
                    rememberPagerState(
                        initialPage = 0,
                        initialPageOffsetFraction = 0f,
                    ) { 10 }

                // 测试用的 Pager
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .background(Color.LightGray),
                    pageSize = PageSize.Fixed(250.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    pageSpacing = 8.dp,
                    beyondViewportPageCount = 2,
                ) { page ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .width(250.dp)
                                .background(
                                    when (page % 3) {
                                        0 -> Color.Blue
                                        1 -> Color.Red
                                        else -> Color.Green
                                    },
                                ).padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Page $page",
                            color = Color.White,
                        )
                    }
                }

                // 使用derivedStateOf监听所有状态变化
                val pagerInfo by remember {
                    derivedStateOf {
                        buildString {
                            // 基本信息
                            appendLine("基本信息:")
                            appendLine("总页数: ${pagerState.pageCount} 当前页面: ${pagerState.currentPage}")
                            appendLine("目标页面: ${pagerState.targetPage} 已停止页面: ${pagerState.settledPage}")
                            appendLine("当前页面偏移: ${pagerState.currentPageOffsetFraction}")
                            appendLine()

                            // 布局信息
                            appendLine("布局信息:")
                            with(pagerState.layoutInfo) {
                                appendLine("可见页面数量: ${visiblePagesInfo.size}")
                                appendLine("页面大小: $pageSize")
                                appendLine("页面间距: ${pageSpacing}dp")
                                appendLine("视窗起始偏移: $viewportStartOffset")
                                appendLine("视窗结束偏移: $viewportEndOffset")
                                appendLine("内容前填充: ${beforeContentPadding}dp")
                                appendLine("内容后填充: ${afterContentPadding}dp")
                                appendLine("视窗大小: $viewportSize")
                                appendLine("方向: $orientation")
                                appendLine("预加载页面数: $beyondViewportPageCount")
                                appendLine("对齐位置: $snapPosition")
                            }
                            appendLine()

                            // 滚动状态信息
                            appendLine("滚动状态:")
                            appendLine("是否正在滚动: ${pagerState.isScrollInProgress}")
                            appendLine("可向前滚动: ${pagerState.canScrollForward}")
                            appendLine("可向后滚动: ${pagerState.canScrollBackward}")
                            appendLine("上次是否向前滚动: ${pagerState.lastScrolledForward}")
                            appendLine("上次是否向后滚动: ${pagerState.lastScrolledBackward}")
                            appendLine()

                            // 可见页面详情
                            appendLine("可见页面详情:")
                            pagerState.layoutInfo.visiblePagesInfo.forEach { pageInfo ->
                                appendLine("页面 ${pageInfo.index}:")
                                appendLine("  偏移: ${pageInfo.offset}")
                                appendLine("  key: ${pageInfo.key}")
                            }
                        }
                    }
                }

                // 显示状态信息
                Text("Pager 状态信息:")
                Text(pagerInfo)
            }
        }
    }
} 

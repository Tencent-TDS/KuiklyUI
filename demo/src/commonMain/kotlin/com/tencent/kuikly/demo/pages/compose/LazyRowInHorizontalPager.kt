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
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.compose.setContent
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
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.Alignment

@Page("LazyRowInHorizontalPager")
class LazyRowInHorizontalPager : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                lazyRowInPagerImpl()
            }
        }
    }

    @Composable
    private fun lazyRowInPagerImpl() {
        val pagerState: PagerState = rememberPagerState(pageCount = { 3 })
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().bouncesEnable(false).height(220.dp),
                beyondViewportPageCount = 3
            ) { page ->
                val items = List(20) { idx -> "Page ${page + 1} - Item ${idx + 1}" }
                LazyRow(
                    modifier = Modifier.fillMaxSize().background(
                        when (page % 3) {
                            0 -> Color(0xFFE3F2FD)
                            1 -> Color(0xFFE8F5E9)
                            else -> Color(0xFFFFF3E0)
                        }
                    )
                        .nestedScroll(NestedScrollMode.SELF_FIRST, NestedScrollMode.SELF_FIRST)
                        .bouncesEnable(false),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(items) { label ->
                        Box(
                            modifier = Modifier
                                .height(120.dp)
                                .width(160.dp)
                                .background(Color(0xFF90CAF9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("当前页: ${pagerState.currentPage + 1}", modifier = Modifier.padding(12.dp))
        }
    }
} 
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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PagerState
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * HorizontalPager 嵌套多个 LazyRow 的可复现 Demo
 * 
 * 场景：用于复现双指触摸问题
 * - 外层是 HorizontalPager（可横向滑动）
 * - 每个页面内部包含两个独立的 LazyRow（A 和 B）
 * - 单指落在 A 或 B 横向滑动时会出现 bounce 效果
 * - 双指（一个指头在 A，一个指头在 B）同时滑动时可能触发父容器 HorizontalPager 的滑动
 */
@Page("mlr")
class MultiLazyRowInHorizontalPager : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                multiLazyRowInPagerDemo()
            }
        }
    }

    @Composable
    private fun multiLazyRowInPagerDemo() {
        // 外层 HorizontalPager：3 个页面
        val pagerState: PagerState = rememberPagerState(pageCount = { 3 })
        
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            
            // 页面指示器
            Text(
                text = "双指触摸测试: Page ${pagerState.currentPage + 1}/3",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            Text(
                text = "说明：单指在 A 或 B 中滑动只有bounce效果；双指同时在 A 和 B 中滑动可能触发父容器滑动",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                beyondViewportPageCount = 3
            ) { page ->
                MultiLazyRowPage(pageIndex = page)
            }
            
            // 底部页码指示器
            RowIndicator(
                pageCount = 3,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Composable
    private fun MultiLazyRowPage(pageIndex: Int) {
        // 页面背景色根据索引变化
        val pageBackgroundColor = when (pageIndex % 3) {
            0 -> Color(0xFFE3F2FD) // 浅蓝
            1 -> Color(0xFFE8F5E9) // 浅绿
            else -> Color(0xFFFFF3E0) // 浅橙
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBackgroundColor)
                .padding(16.dp)
        ) {
            Text(
                text = "Page ${pageIndex + 1}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // LazyRow A
            LazyRowSection(
                title = "LazyRow A (上部)",
                rowId = "A-${pageIndex}",
                backgroundColor = Color(0xFF90CAF9), // 蓝色卡片
                itemCount = 20
            )

            Spacer(modifier = Modifier.height(24.dp))

            // LazyRow B
            LazyRowSection(
                title = "LazyRow B (下部)",
                rowId = "B-${pageIndex}",
                backgroundColor = Color(0xFFA5D6A7), // 绿色卡片
                itemCount = 20
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 说明文字
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "测试步骤：\n" +
                           "1. 单指在 A 或 B 中横向滑动 → 应该看到 bounce 效果\n" +
                           "2. 双指同时在 A 和 B 中横向滑动 → 观察是否会触发父 Pager 滑动",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }
        }
    }

    @Composable
    private fun LazyRowSection(
        title: String,
        rowId: String,
        backgroundColor: Color,
        itemCount: Int
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // LazyRow：嵌套滚动配置
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White)
//                    .nestedScroll(
//                        NestedScrollMode.SELF_ONLY, NestedScrollMode.SELF_ONLY
//                    )
//                    .bouncesEnable(false), // 启用 bounce 效果
                ,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(12.dp)
            ) {
                items(itemCount) { index ->
                    LazyRowItem(
                        label = "$rowId-${index + 1}",
                        backgroundColor = backgroundColor
                    )
                }
            }
        }
    }

    @Composable
    private fun LazyRowItem(label: String, backgroundColor: Color) {
        Box(
            modifier = Modifier
                .height(120.dp)
                .width(140.dp)
                .background(backgroundColor)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun RowIndicator(
        pageCount: Int,
        currentPage: Int,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { index ->
                val isSelected = index == currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .height(8.dp)
                        .background(
                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFBDBDBD),
                        )
                )
            }
        }
    }
}

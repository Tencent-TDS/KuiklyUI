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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.rememberScrollState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("HorizontalPagerDemo1")
class HorizontalPagerDemo1 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                HorizontalPagerTest1()
            }
        }
    }

    @Composable
    fun HorizontalPagerTest1() {
        val state = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 基本使用
            Text("1. 基本 HorizontalPager:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .bouncesEnable(false)
                        .background(Color.LightGray),
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(150.dp)
                            .background(Color.Blue)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 2. 测试 PageSize
            Text("2. PageSize.Fixed, pageSpacing 测试:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.LightGray),
                pageSize = PageSize.Fixed(200.dp),
                pageSpacing = 5.dp,
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Red)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Fixed Width Page $page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. 测试 contentPadding
            Text("3. contentPadding 测试:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.LightGray),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 5.dp),
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Green)
                            .border(2.dp, Color.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. 测试 verticalAlignment
            Text("5. verticalAlignment 测试:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(150.dp)
                        .background(Color.LightGray),
                verticalAlignment = Alignment.Bottom,
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .height(100.dp)
                            .width(150.dp)
                            .background(Color.Magenta)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 6. 测试 beyondViewportPageCount
            Text("6. beyondViewportPageCount = 2:")
            HorizontalPager(
                state = rememberPagerState { 20 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.LightGray),
                beyondViewportPageCount = 2,
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(150.dp)
                            .background(Color.Cyan)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Page $page")
                }
            }

            Spacer(Modifier.height(20.dp))

            // 7. 测试 userScrollEnabled
            var scrollEnabled by remember { mutableStateOf(true) }
            Box(
                modifier =
                    Modifier.clickable {
                        scrollEnabled = !scrollEnabled
                    },
            ) {
                Text("7. 点击切换滚动状态 (userScrollEnabled = $scrollEnabled):")
            }
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.LightGray),
                userScrollEnabled = scrollEnabled,
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(150.dp)
                            .background(Color.Gray)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
} 

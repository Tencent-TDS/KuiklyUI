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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
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
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("HorizontalPagerDemo2")
class HorizontalPagerDemo2 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                HorizontalPagerTest2()
            }
        }
    }

    @Composable
    fun HorizontalPagerTest2() {
        Column(modifier = Modifier.fillMaxSize()) {
            // 2. 测试 key 参数 - 使用自定义key
            Text("2. key = 'item_index':")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                pageSize = PageSize.Fixed(200.dp),
                key = { index -> "item_$index" },
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                            .background(Color.Red)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page\nKey: item_$page",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 3. 测试 SnapPosition.Start
            Text("3. SnapPosition.Start:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                pageSize = PageSize.Fixed(200.dp),
//                snapPosition = SnapPosition.Start
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                            .background(Color.Green)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page\nSnap: Start",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 4. 测试 SnapPosition.Center
            Text("4. SnapPosition.Center:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                pageSize = PageSize.Fixed(200.dp),
//                snapPosition = SnapPosition.Center
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                            .background(Color.Magenta)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page\nSnap: Center",
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. 测试 SnapPosition.End
            Text("5. SnapPosition.End:")
            HorizontalPager(
                state = rememberPagerState { 5 },
                modifier =
                    Modifier
                        .height(100.dp)
                        .fillMaxWidth()
                        .background(Color.LightGray),
                pageSize = PageSize.Fixed(200.dp),
//                snapPosition = SnapPosition.End
            ) { page ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(200.dp)
                            .background(Color.Cyan)
                            .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Page $page\nSnap: End",
                        color = Color.White,
                    )
                }
            }
        }
    }
}

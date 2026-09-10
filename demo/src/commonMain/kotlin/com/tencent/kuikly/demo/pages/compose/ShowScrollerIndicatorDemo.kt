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
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.extension.showScrollerIndicator
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

/**
 * 滚动条 showScrollerIndicator 能力验证页（Compose DSL）
 *
 * 同屏四种对照：
 * 1. 默认：不设置任何属性 —— 滚动条隐藏（框架默认）
 * 2. showScrollerIndicator(true) —— 滚动条显示（滚动时出现、停止后淡出）
 * 3. setProp("showScrollerIndicator", 0) —— 显式关闭，等同默认
 * 4. 横向 LazyRow + showScrollerIndicator(true) —— 横向滚动条显示
 */
@Page("ShowScrollerIndicatorDemo")
class ShowScrollerIndicatorDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ShowScrollerIndicatorTest()
            }
        }
    }

    @Composable
    fun ShowScrollerIndicatorTest() {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White),
        ) {
            Text(
                "滚动条 showScrollerIndicator 验证页",
                modifier = Modifier.padding(12.dp),
            )

            // ===== 1. 默认（不设置）→ 滚动条隐藏 =====
            Text(
                "1. 默认（不设置）→ 滚动条隐藏",
                modifier = Modifier.padding(12.dp),
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF2F2F2)),
            ) {
                items(30) { index ->
                    Text(
                        "Default Item $index",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) Color(0xFFE8F0FE) else Color(0xFFFDE8E8)
                                )
                                .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== 2. showScrollerIndicator(true) → 滚动条显示 =====
            Text(
                "2. showScrollerIndicator(true) → 滚动条显示",
                modifier = Modifier.padding(12.dp),
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF2F2F2))
                        .showScrollerIndicator(true),
            ) {
                items(30) { index ->
                    Text(
                        "Visible Item $index",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) Color(0xFFE8F0FE) else Color(0xFFFDE8E8)
                                )
                                .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== 3. setProp("showScrollerIndicator", 0) → 显式关闭 =====
            Text(
                "3. setProp(..., 0) → 显式关闭（等同默认）",
                modifier = Modifier.padding(12.dp),
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF2F2F2))
                        .setProp("showScrollerIndicator", 0),
            ) {
                items(30) { index ->
                    Text(
                        "Hidden Item $index",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) Color(0xFFE8F0FE) else Color(0xFFFDE8E8)
                                )
                                .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ===== 4. 横向 LazyRow + showScrollerIndicator(true) =====
            Text(
                "4. LazyRow + showScrollerIndicator(true) → 横向滚动条显示",
                modifier = Modifier.padding(12.dp),
            )
            LazyRow(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(Color(0xFFF2F2F2))
                        .showScrollerIndicator(true),
            ) {
                items(30) { index ->
                    Text(
                        "H-$index",
                        modifier =
                            Modifier
                                .width(72.dp)
                                .background(Color(0xFFE8F0FE))
                                .padding(12.dp),
                    )
                }
            }
        }
    }
}

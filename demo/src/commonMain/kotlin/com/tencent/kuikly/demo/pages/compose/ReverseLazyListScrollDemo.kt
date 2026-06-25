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
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.semantics.testTag
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog

private const val ReverseLazyListScrollTag = "ReverseLazyListScrollDemo"

@Page("ReverseLazyListScrollDemo")
class ReverseLazyListScrollDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Reverse Lazy Scroll") {
                ReverseLazyListScrollContent()
            }
        }
    }

    @Composable
    private fun ReverseLazyListScrollContent() {
        val normalState = rememberLazyListState()
        val reverseState = rememberLazyListState()

        LogScrollState("normal", normalState)
        LogScrollState("reverse", reverseState)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                "ADB native scroll check",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF202124),
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                LazyListPanel(
                    title = "NORMAL",
                    state = normalState,
                    reverseLayout = false,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                LazyListPanel(
                    title = "REVERSE",
                    state = reverseState,
                    reverseLayout = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    @Composable
    private fun LogScrollState(label: String, state: LazyListState) {
        val firstIndex = state.firstVisibleItemIndex
        val firstOffset = state.firstVisibleItemScrollOffset
        val visible = state.layoutInfo.visibleItemsInfo.joinToString(limit = 4) { it.index.toString() }
        LaunchedEffect(label, firstIndex, firstOffset, visible) {
            KLog.i(
                ReverseLazyListScrollTag,
                "$label first=$firstIndex offset=$firstOffset visible=$visible",
            )
        }
    }

    @Composable
    private fun LazyListPanel(
        title: String,
        state: LazyListState,
        reverseLayout: Boolean,
        modifier: Modifier = Modifier,
    ) {
        val panelColor = if (reverseLayout) Color(0xFFE8F0FE) else Color(0xFFE6F4EA)
        val itemColor = if (reverseLayout) Color(0xFF1967D2) else Color(0xFF188038)
        Column(
            modifier =
                modifier
                    .background(panelColor)
                    .border(1.dp, Color(0xFF5F6368))
                    .padding(8.dp),
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF202124))
            Text(
                "first=${state.firstVisibleItemIndex} offset=${state.firstVisibleItemScrollOffset}",
                modifier = Modifier.testTag("${title.lowercase()}_scroll_state"),
                fontSize = 13.sp,
                color = Color(0xFF3C4043),
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                state = state,
                reverseLayout = reverseLayout,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .background(Color.White)
                        .testTag("${title.lowercase()}_lazy_column"),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(80) { index ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(itemColor)
                                .padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$title item $index",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

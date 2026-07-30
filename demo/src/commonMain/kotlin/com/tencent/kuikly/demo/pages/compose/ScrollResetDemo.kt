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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("ScrollResetDemo")
class ScrollResetDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ResetCases()
            }
        }
    }

    @Composable
    private fun ResetCases() {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // State for forcing scrollView recreation via key
            var listKey by remember { mutableIntStateOf(0) }
            // State for container size change
            var tall by remember { mutableStateOf(false) }
            var wide by remember { mutableStateOf(true) }

            // Row (horizontal) test: force recreate
            var rowKey by remember { mutableIntStateOf(0) }

            val columnState = rememberLazyListState()
            val rowState = rememberLazyListState()
            // Item count to simulate content size shrinking/expansion
            var colItems by remember { mutableIntStateOf(50) }

            // Capture layout info for Column and Row
            var colLayoutInfo by remember { mutableStateOf<com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo?>(null) }
            var rowLayoutInfo by remember { mutableStateOf<com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo?>(null) }

            val scope = rememberCoroutineScope()

            // Controls (row 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color.Blue).clickable {
                        // Toggle key to recreate scrollView
                        listKey += 1
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Recreate scrollView (key++)", color = Color.White) }

                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFF00897B)).clickable {
                        // Toggle container size
                        tall = !tall
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Toggle container size", color = Color.White) }

                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFF546E7A)).clickable {
                        // Toggle container width (fractional)
                        wide = !wide
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Toggle container width", color = Color.White) }

                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFF455A64)).clickable {
                        // Recreate LazyRow (horizontal) scrollView
                        rowKey += 1
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Recreate Row", color = Color.White) }
            }

            Spacer(Modifier.height(12.dp))

            // Controls (row 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFF6A1B9A)).clickable {
                        scope.launch { columnState.scrollToItem(0) }
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("ScrollToItem 0", color = Color.White) }

                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFFD32F2F)).clickable {
                        // Decrease items to simulate content size shrink
                        colItems = maxOf(5, colItems - 10)
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Shrink items -10", color = Color.White) }

                Box(
                    modifier = Modifier.size(140.dp, 40.dp).background(Color(0xFF2E7D32)).clickable {
                        // Increase items
                        colItems += 10
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("Grow items +10", color = Color.White) }
            }

            // Column test area with key to force recreate
            Text("LazyColumn test (recreate + size change)")
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (wide) 1f else 0.6f)
                    .height(if (tall) 260.dp else 140.dp)
                    .background(Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Use key to force SubcomposeLayout/ScrollerView recreation
                    androidx.compose.runtime.key(listKey) {
                        LazyColumn(state = columnState,
                            modifier = Modifier.fillMaxSize(),
                            content = {
                                items((0 until colItems).toList()) { idx ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 8.dp)
                                            .background(if (idx % 2 == 0) Color(0xFF90CAF9) else Color(0xFF64B5F6)),
                                        contentAlignment = Alignment.Center
                                    ) { Text("C-Item $idx", color = Color.White) }
                                }
                            })
                    }
                }
            }

            // Update and show column layout/state info
            LaunchedEffect(columnState.layoutInfo) { colLayoutInfo = columnState.layoutInfo }
            InfoPanel(title = "Column state/layout", info = buildString {
                appendLine("isScrollInProgress=${columnState.isScrollInProgress}")
                appendLine("firstVisibleItemIndex=${columnState.firstVisibleItemIndex}")
                appendLine("canScrollForward=${columnState.canScrollForward}")
                appendLine("canScrollBackward=${columnState.canScrollBackward}")
                colLayoutInfo?.let { info ->
                    appendLine("viewportStartOffset=${info.viewportStartOffset}")
                    appendLine("viewportEndOffset=${info.viewportEndOffset}")
                    appendLine("viewportSize=${info.viewportSize}")
                    appendLine("totalItemsCount=${info.totalItemsCount}")
                    appendLine("orientation=${info.orientation}")
                }
            })

            Spacer(Modifier.height(16.dp))

            // Row test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) { Text("LazyRow test") }
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (wide) 1f else 0.7f)
                    .height(if (tall) 160.dp else 120.dp)
                    .background(Color(0xFFF5F5F5))
            ) {
                androidx.compose.runtime.key(rowKey) {
                    LazyRow(state = rowState,
                        modifier = Modifier.fillMaxSize(),
                        content = {
                            items((0 until 40).toList()) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .padding(4.dp)
                                        .background(if (idx % 2 == 0) Color(0xFFA5D6A7) else Color(0xFF81C784)),
                                    contentAlignment = Alignment.Center
                                ) { Text("R-$idx", color = Color.White) }
                            }
                        })
                }
            }

            // Update and show row layout/state info
            LaunchedEffect(rowState.layoutInfo) { rowLayoutInfo = rowState.layoutInfo }
            InfoPanel(title = "Row state/layout", info = buildString {
                appendLine("isScrollInProgress=${rowState.isScrollInProgress}")
                appendLine("firstVisibleItemIndex=${rowState.firstVisibleItemIndex}")
                appendLine("canScrollForward=${rowState.canScrollForward}")
                appendLine("canScrollBackward=${rowState.canScrollBackward}")
                rowLayoutInfo?.let { info ->
                    appendLine("viewportStartOffset=${info.viewportStartOffset}")
                    appendLine("viewportEndOffset=${info.viewportEndOffset}")
                    appendLine("viewportSize=${info.viewportSize}")
                    appendLine("totalItemsCount=${info.totalItemsCount}")
                    appendLine("orientation=${info.orientation}")
                }
            })
        }
    }

    @Composable
    private fun InfoPanel(title: String, info: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x11000000))
                .padding(8.dp)
        ) {
            Text(title)
            Text(info)
        }
    }
}



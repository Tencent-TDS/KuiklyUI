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

@file:OptIn(ExperimentalFoundationApi::class)

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

/**
 * Custom Snackbar Component
 * Used to display bottom-popping alert messages
 */
@Composable
fun CustomSnackbar(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        LaunchedEffect(message) {
            delay(3000) // Auto-dismiss after 3 seconds
            onDismiss()
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Page("LazyColumnStickyHeader")
class LazyColumnStickyHeader : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumnStickyHeaderImpl()
            }
        }
    }
}

@Composable
fun LazyColumnStickyHeaderImpl() {
    var showAdvancedDemo by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        // Simple top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sticky Header 演示",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Button(
                onClick = { showAdvancedDemo = !showAdvancedDemo }
            ) {
                Text(
                    text = if (showAdvancedDemo) "Basic Demo" else "Advanced Demo"
                )
            }
        }

        // Content area
        if (showAdvancedDemo) {
            AdvancedStickyHeaderDemo { message ->
                snackbarMessage = message
                showSnackbar = true
            }
        } else {
            BasicStickyHeaderDemo { message ->
                snackbarMessage = message
                showSnackbar = true
            }
        }
        }

        // Snackbar overlay
        CustomSnackbar(
            message = snackbarMessage,
            isVisible = showSnackbar,
            onDismiss = { showSnackbar = false },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun BasicStickyHeaderDemo(onShowSnackbar: (String) -> Unit = {}) {
    val listState = rememberLazyListState()

    // Simulated grouped data
    val groups = remember {
        listOf(
            "A" to (1..5).map { "Apple $it" },
            "B" to (1..8).map { "Banana $it" },
            "C" to (1..6).map { "Cherry $it" },
            "D" to (1..4).map { "Date $it" },
            "E" to (1..7).map { "Elderberry $it" },
            "F" to (1..9).map { "Fig $it" },
            "G" to (1..5).map { "Grape $it" },
            "H" to (1..6).map { "Honeydew $it" }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Description text
        Text(
            text = "Basic Demo: Simple sticky header (Click headers for snackbar)",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Main LazyColumn
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Top description area
            item {
                Text(
                    text = "Using stickyHeader to implement simple sticky effect",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Grouped content
            groups.forEach { (letter, items) ->
                // Sticky Header - Alphabet group title
                stickyHeader(
                    key = "header_$letter"
                ) { headerIndex ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                onShowSnackbar("Clicked on Group $letter sticky header!")
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Group $letter (Click for info)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                }

                // Items within the group
                items(
                    items = items,
                    key = { item -> "item_${letter}_$item" }
                ) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Item icon
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Item content
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "This is detailed description for $item",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                        }
                    }
                }
            }

            // Bottom description
            item {
                Text(
                    text = "Scroll to see sticky effect",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvancedStickyHeaderDemo(onShowSnackbar: (String) -> Unit = {}) {
    val listState = rememberLazyListState()

    // Simulated more complex grouped data
    val advancedGroups = remember {
        listOf(
            "Hot Recommendations" to (1..3).map { "Hot Item $it" },
            "Latest Releases" to (1..5).map { "New Item $it" },
            "Featured Content" to (1..4).map { "Featured $it" },
            "Recommended Reading" to (1..6).map { "Recommended $it" }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top description
        item {
            Text(
                text = "Advanced Demo: Sticky header with margins (Click headers for snackbar)",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Grouped content
        advancedGroups.forEach { (category, items) ->
            // Sticky Header with margins
            stickyHeaderWithMarginTop(
                key = "advanced_header_$category",
                listState = listState,
                hoverMarginTop = 56.dp
            ) { headerIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable {
                            onShowSnackbar("Clicked on $category sticky header with margins!")
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        text = "$category (Click for info)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }
            }


            // Items within the group
            items(
                items = items,
                key = { item -> "advanced_item_${category}_$item" }
            ) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.first().toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Item details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "This is detailed description for $item",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 右侧箭头
                    }
                }
            }
        }

        // 底部说明
        item {
            Text(
                text = "使用 stickyHeaderWithMarginTop 实现带边距的吸顶效果",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/*
 * 使用示例：
 * 
 * 1. 基础 Sticky Header 使用：
 * 
 * LazyColumn {
 *     stickyHeader(key = "header_1") { headerIndex ->
 *         Text("分组标题", modifier = Modifier.background(Color.Gray))
 *     }
 *     items(items) { item ->
 *         Text(item)
 *     }
 * }
 * 
 * 2. 带边距的 Sticky Header 使用：
 * 
 * val listState = rememberLazyListState()
 * LazyColumn(state = listState) {
 *     stickyHeaderWithMarginTop(
 *         key = "header_1",
 *         listState = listState,
 *         hoverMarginTop = 56.dp
 *     ) { headerIndex ->
 *         Text("带边距的分组标题")
 *     }
 *     items(items) { item ->
 *         Text(item)
 *     }
 * }
 * 
 * 3. 分组列表的完整示例：
 * 
 * val groups = listOf(
 *     "A" to listOf("Apple", "Ant"),
 *     "B" to listOf("Banana", "Bear")
 * )
 * 
 * LazyColumn {
 *     groups.forEach { (letter, items) ->
 *         stickyHeader(key = "header_$letter") {
 *             Text("分组 $letter")
 *         }
 *         items(items) { item ->
 *             Text(item)
 *         }
 *     }
 * }
 */
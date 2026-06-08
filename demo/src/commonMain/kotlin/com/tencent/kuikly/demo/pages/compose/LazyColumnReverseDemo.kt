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
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
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
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.launch

@Page("LazyColumnReverseDemo")
class LazyColumnReverseDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ReverseLazyColumnDemo()
            }
        }
    }
}

@Composable
private fun ReverseLazyColumnDemo() {
    val itemCount = 20
    val columnState = rememberLazyListState()
    val rowState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(12.dp),
    ) {
        Text("reverseLayout 纵向示例：初始会从底部开始显示，手势仍然是正常上下滑动。")
        Text(
            "Column firstVisibleItemIndex=${columnState.firstVisibleItemIndex}，Row firstVisibleItemIndex=${rowState.firstVisibleItemIndex}。",
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DemoButton(
                text = "滚到视觉顶部",
                modifier = Modifier.weight(1f),
            ) {
                scope.launch {
                    columnState.animateScrollToItem(itemCount - 1)
                }
            }
            DemoButton(
                text = "滚到尾部(index 0)",
                modifier = Modifier.weight(1f),
            ) {
                scope.launch {
                    columnState.animateScrollToItem(0)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            state = columnState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(itemCount) { index ->
                ReverseDemoCard(index, modifier = Modifier.fillMaxWidth().height(56.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("reverseLayout 横向示例：左右滑动应与纵向同样稳定。")
        LazyRow(
            state = rowState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color.LightGray),
            reverseLayout = true,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            items(itemCount) { index ->
                ReverseDemoCard(index, modifier = Modifier.width(120.dp).height(88.dp))
            }
        }
    }
}

@Composable
private fun ReverseDemoCard(index: Int, modifier: Modifier) {
    val cardColor =
        when (index % 4) {
            0 -> Color(0xFF4C6EF5)
            1 -> Color(0xFF12B886)
            2 -> Color(0xFFF59F00)
            else -> Color(0xFFE03131)
        }
    Box(
        modifier =
            modifier
                .background(cardColor)
                .border(1.dp, Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text("Message $index", color = Color.White)
    }
}

@Composable
private fun DemoButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(40.dp)
                .background(Color.Black)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White)
    }
}

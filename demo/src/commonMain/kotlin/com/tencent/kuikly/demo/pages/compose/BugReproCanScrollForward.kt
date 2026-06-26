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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("5555")
internal class BugReproCanScrollForward : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            CanScrollForwardBugDemo()
        }
    }
}

@Composable
private fun CanScrollForwardBugDemo() {
    val listState = rememberLazyListState()
    var showFloatBall by remember { mutableStateOf(false) }
    var canScrollForwardValue by remember { mutableStateOf(false) }
    var lastScrolledBackwardValue by remember { mutableStateOf(false) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.canScrollForward to listState.lastScrolledBackward
        }.collect { (canFwd, scrolledBwd) ->
            canScrollForwardValue = canFwd
            lastScrolledBackwardValue = scrolledBwd

            if (!canFwd) {
                showFloatBall = false
            } else if (scrolledBwd) {
                showFloatBall = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF333333))
                    .padding(16.dp)
            ) {
                Text(
                    text = "canScrollForward: $canScrollForwardValue\nlastScrolledBackward: $lastScrolledBackwardValue\nshowFloatBall: $showFloatBall",
                    color = Color.White,
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(50) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(if (index % 2 == 0) Color(0xFFEEEEEE) else Color.White)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(text = "Item $index")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showFloatBall) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-16).dp, y = (-16).dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Blue)
                    .clickable {
                        // 点击回底
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("↑", color = Color.White)
            }
        }
    }
}

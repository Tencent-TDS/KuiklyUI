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
import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.combinedClickable
import com.tencent.kuikly.compose.foundation.gestures.draggable2D
import com.tencent.kuikly.compose.foundation.gestures.rememberDraggable2DState
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("ComposeEventPage")
class ComposeEventPage : ComposeContainer() {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun composeEventPage() {
        var clickCount by remember { mutableStateOf(0) }
        var doubleTapCount by remember { mutableStateOf(0) }
        var longPressCount by remember { mutableStateOf(0) }

        val max = 300.dp
        val min = 0.dp
        val (minPx, maxPx) = with(LocalDensity.current) { min.toPx() to max.toPx() }
        // this is the offset we will update while dragging
        var offsetPositionX by remember { mutableStateOf(0f) }
        var offsetPositionY by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier =
                    Modifier.height(50.dp).background(Color.Yellow).combinedClickable(onDoubleClick = {
                        doubleTapCount += 1
                    }, onLongClick = {
                        longPressCount += 1
                    }) {
                        clickCount += 1
                    },
            ) {
                Text("Tap or Double Tap or Long Press Me", fontSize = 20.sp, color = Color.Red)
            }

//            Spacer(Modifier.height(20.dp))
            Text("tap Count: $clickCount", fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp))
            // todo: jonas
//            Spacer(Modifier.height(20.dp))
//            Text("double tap Count: $doubleTapCount")
            Text("long press Count: $longPressCount", fontSize = 16.sp, modifier = Modifier.padding(top = 20.dp))

            Spacer(modifier = Modifier.height(50.dp))
            Text("Drag The Red Rect", fontSize = 20.sp, modifier = Modifier.padding(bottom = 20.dp))
            Column(
                modifier =
                    Modifier
                        .size(max, max)
                        .draggable2D(
                            state =
                                rememberDraggable2DState { delta ->
                                    val newValueX = offsetPositionX + delta.x
                                    val newValueY = offsetPositionY + delta.y
                                    offsetPositionX = newValueX.coerceIn(minPx, maxPx)
                                    offsetPositionY = newValueY.coerceIn(minPx, maxPx)
                                },
                        ).background(Color.Yellow),
            ) {
                Box(
                    Modifier
                        .offset(offsetPositionX.dp, offsetPositionY.dp)
                        .size(50.dp)
                        .background(Color.Red),
                )
            }
        }
    }

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                composeEventPage()
            }
        }
    }
}

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
import com.tencent.kuikly.compose.BackHandler
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextButton
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.RectangleShape
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.tencent.kuikly.core.annotations.Page

private var expandedWidthInit = false
private var expandedHeightInit = false
private var usePlatformDefaultWidthInit = false

@Page("DialogDemo")
internal class DialogDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                DialogDemo()
            }
        }
    }
}

@Composable
internal fun DialogDemo() {
    val roundedRectangleShape = RoundedCornerShape(percent = 15)
    var shape by remember { mutableStateOf(RectangleShape) }
    var elevation by remember { mutableStateOf(8.dp) }
    var openDialog by remember { mutableStateOf(true) }
    var openDialog2 by remember { mutableStateOf(false) }
    var expandedWidth by remember { mutableStateOf(expandedWidthInit) }
    var expandedHeight by remember { mutableStateOf(expandedHeightInit) }
    var usePlatformDefaultWidth by remember { mutableStateOf(usePlatformDefaultWidthInit) }

    Column(modifier = Modifier.padding(50.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = { openDialog = !openDialog }) {
            Text("Tap anywhere to reopen dialog")
        }
        if (openDialog) {
            Dialog(
                onDismissRequest = { openDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = usePlatformDefaultWidth),
            ) {
                val width =
                    if (expandedWidth) {
                        1500.dp
                    } else {
                        300.dp
                    }
                val height =
                    if (expandedHeight) {
                        600.dp
                    } else {
                        400.dp
                    }
                Card(
                    modifier = Modifier.size(width, height).padding(10.dp),
                    elevation = CardDefaults.cardElevation(elevation),
                    shape = shape,
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Text("Dialog")
                        TextButton(
                            onClick = {
                                shape =
                                    if (shape == roundedRectangleShape) {
                                        RectangleShape
                                    } else {
                                        roundedRectangleShape
                                    }
                            },
                        ) {
                            Text("Toggle corners")
                        }
                        TextButton(
                            onClick = {
                                expandedWidth = !expandedWidth
                                expandedWidthInit = expandedWidth
                            },
                        ) {
                            Text("Toggle width")
                        }
                        TextButton(
                            onClick = {
                                expandedHeight = !expandedHeight
                                expandedHeightInit = expandedHeight
                            },
                        ) {
                            Text("Toggle height")
                        }
                        TextButton(
                            onClick = {
                                usePlatformDefaultWidth = !usePlatformDefaultWidth
                                usePlatformDefaultWidthInit = usePlatformDefaultWidthInit
                            },
                        ) {
                            Text("Toggle widthlock")
                        }
                        var inEditing by remember { mutableStateOf(false) }
                        if (inEditing) {
                            BackHandler {
                                inEditing = false
                            }
                        }
                        TextButton(
                            onClick = {
                                inEditing = !inEditing
                            },
                        ) {
                            Text("Toggle Edit ${ if(inEditing) "编辑态拦截返回键中" else "点击编辑" }")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { elevation -= 1.dp }) {
                                Text("-1")
                            }
                            Text("Elevation: $elevation")
                            TextButton(onClick = { elevation += 1.dp }) {
                                Text("+1")
                            }
                        }
                        Text("Current size: [$width, $height]")
                        Text("usePlatformDefaultWidth = $usePlatformDefaultWidth")
                    }
                }
            }
        }
        Button(onClick = { openDialog2 = !openDialog2 }) {
            Text("Tap anywhere to reopen dialog2")
        }
    }

    if (openDialog2) {
        Dialog(
            onDismissRequest = { openDialog2 = false },
            properties = DialogProperties(scrimColor = Color.Transparent),
        ) {
            Box(Modifier.size(100.dp).alpha(0.5f).background(Color.Red)) {}
            Box(Modifier.size(200.dp).alpha(0.5f).background(Color.Blue)) {}
        }
    }
}

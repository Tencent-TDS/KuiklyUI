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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.text.selection.LocalTextSelectionColors
import com.tencent.kuikly.compose.foundation.text.selection.TextSelectionColors
import com.tencent.kuikly.compose.material3.OutlinedTextField
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TextFieldSelectionDemo")
class TextFieldSelectionDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("TextField Selection Fix") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = "TextField Selection Color Fix (#1378)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    // 1. Using explicit selectionColors in colors()
                    item {
                        Text("1. Explicit selectionColors (Green highlight, Red cursor)")
                        var text1 by remember { mutableStateOf("Select me to see green highlight and red handles/cursor.") }
                        val customColors1 = TextFieldDefaults.colors(
                            selectionColors = TextSelectionColors(
                                handleColor = Color.Red,
                                backgroundColor = Color.Green.copy(alpha = 0.4f)
                            )
                        )
                        TextField(
                            value = text1,
                            onValueChange = { text1 = it },
                            colors = customColors1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // 2. OutlinedTextField with custom colors
                    item {
                        Text("2. OutlinedTextField (Yellow highlight, Blue cursor)")
                        var text2 by remember { mutableStateOf("Testing the newly added OutlinedTextField component.") }
                        val customColors2 = TextFieldDefaults.colors(
                            selectionColors = TextSelectionColors(
                                handleColor = Color.Blue,
                                backgroundColor = Color.Yellow.copy(alpha = 0.4f)
                            )
                        )
                        OutlinedTextField(
                            value = text2,
                            onValueChange = { text2 = it },
                            colors = customColors2,
                            label = { Text("Outlined Label") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // 3. Global selection colors via LocalTextSelectionColors
                    item {
                        Text("3. Global colors via CompositionLocalProvider (Magenta)")
                        val globalSelectionColors = TextSelectionColors(
                            handleColor = Color.Magenta,
                            backgroundColor = Color.Magenta.copy(alpha = 0.4f)
                        )
                        CompositionLocalProvider(LocalTextSelectionColors provides globalSelectionColors) {
                            Column {
                                var text3 by remember { mutableStateOf("This field inherits magenta colors from its parent provider.") }
                                TextField(
                                    value = text3,
                                    onValueChange = { text3 = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                var text4 by remember { mutableStateOf("Outlined field also inheriting magenta.") }
                                OutlinedTextField(
                                    value = text4,
                                    onValueChange = { text4 = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }

                    item {
                        Text(
                            text = "Verification Steps:\n" +
                                    "1. Tap a text field to focus it (cursor color check).\n" +
                                    "2. Long press to select text (highlight color check).\n" +
                                    "3. Verify OutlinedTextField is rendered with a border.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

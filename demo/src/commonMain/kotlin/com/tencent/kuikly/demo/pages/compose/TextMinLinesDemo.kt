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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TextMinLinesDemo")
class TextMinLinesDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Text minLines Support") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Text minLines Verification",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 1. Single line text with minLines = 3
                    item {
                        Column {
                            Text("1. Short text, minLines = 3 (Should have 3-line height)")
                            Text(
                                text = "This is short text.",
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                            )
                        }
                    }

                    // 2. Multi-line text with minLines = 2
                    item {
                        Column {
                            Text("2. Multi-line text, minLines = 2 (Should grow naturally)")
                            Text(
                                text = "This is a longer text that will definitely span more than one line to see if it grows correctly beyond minLines.",
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Yellow)
                            )
                        }
                    }

                    // 3. minLines and maxLines combined
                    item {
                        Column {
                            Text("3. minLines = 2, maxLines = 4")
                            var text by remember { mutableStateOf("Initial text.") }
                            Text(
                                text = text,
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE3F2FD))
                                    .clickable {
                                        text += " Adding more text to trigger more lines."
                                    }
                            )
                            Text("Click text to add content", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // 4. Comparison
                    item {
                        Column {
                            Text("4. Comparison: Top (minLines=1), Bottom (minLines=3)")
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "No minLines",
                                    modifier = Modifier.background(Color.Green.copy(alpha = 0.2f))
                                )
                                Text(
                                    "With minLines=3",
                                    minLines = 3,
                                    modifier = Modifier.background(Color.Green.copy(alpha = 0.2f)).fillMaxWidth()
                                )
                            }
                        }
                    }
                    
                    // 5. Dynamic minLines
                    item {
                        Column {
                            Text("5. Dynamic minLines")
                            var minLines by remember { mutableStateOf(1) }
                            Text(
                                text = "Current minLines: $minLines. Click to increase.",
                                minLines = minLines,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F8E9))
                                    .clickable {
                                        minLines = if (minLines >= 5) 1 else minLines + 1
                                    }
                                    .padding(4.dp)
                            )
                            Text("Click to cycle minLines (1-5)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // 6. Empty text with minLines
                    item {
                        Column {
                            Text("6. Empty text, minLines = 2")
                            Text(
                                text = "",
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFFF3E0))
                            )
                            Text("Should still occupy 2 lines of height", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    // 7. minLines with custom lineHeight
                    item {
                        Column {
                            Text("7. minLines = 2 with large lineHeight (30.sp)")
                            Text(
                                text = "Line height test.",
                                minLines = 2,
                                lineHeight = 30.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3E5F5))
                            )
                        }
                    }

                    // 8. minLines with different font sizes
                    item {
                        Column {
                            Text("8. minLines = 2 with large fontSize (24.sp)")
                            Text(
                                text = "Large font.",
                                minLines = 2,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE0F7FA))
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

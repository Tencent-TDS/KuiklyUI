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

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("KeyboardHeightDemo")
internal class KeyboardHeightDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            var input by remember { mutableStateOf("") }
            val messages = remember { mutableStateListOf(
                "你好！我是KuiklyBot。",
                "你好，有什么可以帮你？",
                "你可以在下方输入消息。"
            ) }

            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    items(messages) { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = msg,
                                color = Color.Black,
                                modifier = Modifier.background(Color.White).padding(12.dp)
                            )
                        }
                    }
                }
                var keyboardHeight by remember { mutableStateOf(0f) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = keyboardHeight.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f).keyboardHeightChange {
                            println("keyboardHeightChange " + it.height)
                            keyboardHeight = it.height
                        },
                        placeholder = { Text("输入消息...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (input.isNotBlank()) {
                            messages.add(input)
                            input = ""
                        }
                    }) {
                        Text("发送")
                    }
                }
            }
        }
    }
} 
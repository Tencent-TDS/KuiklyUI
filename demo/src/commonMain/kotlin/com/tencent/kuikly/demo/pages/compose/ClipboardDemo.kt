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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.*
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalClipboardManager
import com.tencent.kuikly.compose.ui.text.AnnotatedString
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.compose.material3.TextField

@Page("ClipboardDemo")
internal class ClipboardDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ClipboardDemoContent()
        }
    }

    @Composable
    fun ClipboardDemoContent() {
        val clipboardManager = LocalClipboardManager.current
        var inputText by remember { mutableStateOf("") }
        var clipboardText by remember { mutableStateOf("") }
        var showToast by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("剪贴板测试", color = Color.Black)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.width(200.dp),
                    placeholder = { Text("输入要复制的内容") }
                )
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(inputText))
                    showToast = "已复制到剪贴板"
                }) {
                    Text("复制")
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val text = clipboardManager.getText()?.text ?: ""
                clipboardText = text
                showToast = if (text.isNotEmpty()) "已读取剪贴板内容" else "剪贴板为空"
            }) {
                Text("粘贴")
            }
            Spacer(Modifier.height(16.dp))
            Text("剪贴板内容：$clipboardText", color = Color.DarkGray)
            if (showToast.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(showToast, color = Color(0xFF2196F3))
                LaunchedEffect(showToast) {
                    kotlinx.coroutines.delay(1200)
                    showToast = ""
                }
            }
        }
    }
} 
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
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.foundation.layout.asPaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.ime
import com.tencent.kuikly.compose.foundation.layout.imePadding
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("ImeInsetDemo")
internal class ImeInsetDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("IME Insets - imePadding") {
                ImeInsetDemoContent()
            }
        }
    }
}

private data class ImeInsetMessage(
    val text: String,
    val fromUser: Boolean,
)

private val initialImeInsetMessages = listOf(
    ImeInsetMessage("这是 phase1 页面级 IME inset demo。", false),
    ImeInsetMessage("聚焦底部输入框后，输入栏应由 imePadding 顶离键盘。", false),
    ImeInsetMessage("上方信息会展示 WindowInsets.ime 与旧 keyboardHeightChange 回调值，用于验证兼容性。", false),
)

@Composable
private fun ImeInsetDemoContent() {
    var input by remember { mutableStateOf("") }
    var legacyKeyboardHeight by remember { mutableStateOf(0f) }
    var messages by remember { mutableStateOf(initialImeInsetMessages) }
    val configuration = LocalConfiguration.current
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "WindowInsets.ime(bottom) = $imeBottomPadding | current = ${configuration.imeBottomDp.dp}",
            color = Color(0xFF333333),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "legacy keyboardHeightChange = ${legacyKeyboardHeight.dp}",
            color = Color(0xFF666666),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "当前页面只验证 page-level IME height 是否能驱动 imePadding，不验证逐帧动画或同步事件。",
            color = Color(0xFF8A5200),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF4E5))
                .padding(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (message.fromUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Text(
                        text = message.text,
                        color = Color.Black,
                        modifier = Modifier
                            .background(if (message.fromUser) Color(0xFFD7F5D0) else Color.White)
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .imePadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .keyboardHeightChange { params ->
                        legacyKeyboardHeight = params.height
                    },
                placeholder = { Text("输入一条消息，观察输入栏是否自动避让键盘") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (input.isBlank()) {
                    return@Button
                }
                val content = input
                messages = messages + ImeInsetMessage(content, true) + ImeInsetMessage("已发送：$content", false)
                input = ""
            }) {
                Text("发送")
            }
        }
    }
}

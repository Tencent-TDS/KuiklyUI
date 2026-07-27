/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.WindowInsets
import com.tencent.kuikly.compose.foundation.layout.asPaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.ime
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.ExperimentalMaterial3Api
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TopAppBar
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("ScaffoldDemo")
internal class ScaffoldDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                ScaffoldDemoImpl()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldDemoImpl() {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val configuration = LocalConfiguration.current
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scaffold IME Insets") }
            )
        },
        containerColor = Color(0xFFF7F8FA),
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF7F8FA))
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Scaffold WindowInsets.ime = $imeBottomPadding | current = ${configuration.imeBottomDp.dp}",
                    color = Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("聚焦底部表单项后，Scaffold 默认 contentWindowInsets 应自动包含 IME，并把内容顶离键盘。")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "这是 phase1 基础避让 demo，用来验证声明式 API 入口保持不变。",
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "边界说明：phase1 只保证基础避让，不会自动把更深层被遮挡的输入框滚到可见区。",
                    color = Color(0xFF8A5200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF4E5))
                        .padding(12.dp)
                )
                // 用 Box + BottomCenter 把三个表单整体锚定到 Column 底部，
                // 避免 Spacer(weight=1f) 单独使用时把后续表单推出可视区
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("姓名") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = phone,
                            onValueChange = { phone = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("手机号") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = address,
                            onValueChange = { address = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("详细地址（聚焦这里观察默认键盘避让）") }
                        )
                    }
                }
            }
        }
    )
}

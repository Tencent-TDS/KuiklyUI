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
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.interaction.collectIsFocusedAsState
import com.tencent.kuikly.compose.foundation.interaction.collectIsPressedAsState
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("InteractionSourceDemo")
class TextFieldInteractionSourceDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                Column(
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 70.dp, horizontal = 30.dp),
                ) {
                    InteractionSourceDemo()
                }
            }
        }
    }
}

@Composable
fun InteractionSourceDemo() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "InteractionSource 示例",
//                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        // 1. 基础交互状态监听
        item {
            DemoCard(title = "基础交互状态") {
                BasicInteractionDemo()
            }
        }

        // 2. 自定义焦点颜色
        item {
            DemoCard(title = "自定义焦点效果") {
                CustomFocusDemo()
            }
        }

        // 3. 按压状态示例
        item {
            DemoCard(title = "按压状态") {
                PressedStateDemo()
            }
        }

        // 4. 组合交互状态
        item {
            DemoCard(title = "组合交互状态") {
                CombinedInteractionDemo()
            }
        }
    }
}

@Composable
private fun BasicInteractionDemo() {
    var text by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("基础交互") },
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "输入框状态: ${if (isFocused) "已获取焦点" else "未获取焦点"}",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CustomFocusDemo() {
    var text by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    TextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("自定义焦点颜色") },
        interactionSource = interactionSource,
        colors =
            TextFieldDefaults.colors().copy(
                focusedIndicatorColor = Color.Green,
                focusedLabelColor = Color.Green,
//            backgroundColor = if (isFocused) Color.LightGray.copy(alpha = 0.1f) else Color.Transparent
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun PressedStateDemo() {
    var text by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("按压状态") },
            interactionSource = interactionSource,
            colors =
                TextFieldDefaults.colors().copy(
//                backgroundColor = if (isPressed) Color.LightGray else Color.Transparent
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "按压状态: ${if (isPressed) "已按压" else "未按压"}",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CombinedInteractionDemo() {
    var text by remember { mutableStateOf("") }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("组合交互状态") },
            interactionSource = interactionSource,
            colors =
                TextFieldDefaults.colors().copy(
//                backgroundColor = when {
//                    isPressed -> Color.LightGray.copy(alpha = 0.2f)
//                    isFocused -> Color.LightGray.copy(alpha = 0.1f)
//                    else -> Color.Transparent
//                },
                    focusedIndicatorColor = Color.Blue,
                    focusedLabelColor = Color.Blue,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text =
                buildString {
                    append("当前状态: ")
                    if (isFocused) append("已获取焦点 ")
                    if (isPressed) append("已按压 ")
                    if (!isFocused && !isPressed) append("空闲")
                },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DemoCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
//        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
//                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            content()
        }
    }
}

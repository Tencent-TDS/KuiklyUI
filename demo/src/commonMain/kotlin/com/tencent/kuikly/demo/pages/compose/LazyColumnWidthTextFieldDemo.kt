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
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.onFocusChanged
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 测试 TextField 在 LazyColumn 中的表现
 *
 * 覆盖场景：
 * 1. 多个 TextField 在 LazyColumn 中的焦点切换
 * 2. 键盘弹出时的滚动行为
 * 3. 滚动后状态保持
 * 4. 动态新增/删除输入框
 * 5. 输入框获取焦点时自动滚动到可见区域
 */
@Page("LazyColumnWidthTextFieldDemo")
class LazyColumnWidthTextFieldDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            LazyColumnTextFieldDemoContent()
        }
    }
}

@Composable
fun LazyColumnTextFieldDemoContent() {
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    var dynamicItems by remember { mutableStateOf(listOf("")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { focusManager.clearFocus() },
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .bouncesEnable(false),
            state = listState,
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TextField in LazyColumn 测试",
                    fontSize = 20.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "场景1：多个 TextField 焦点切换",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            items(5, key = { "xxxx$it" }) { index ->
                var text by remember { mutableStateOf("") }
                var isFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, if (isFocused) Color.Blue else Color(0xFFDDDDDD))
                        .padding(8.dp)
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("输入框 $index") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (index < 4) ImeAction.Next else ImeAction.Done
                        )
                    )
                }
            }

            item {
                Text(
                    text = "场景2：键盘弹出时自动滚动",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            item {
                var text by remember { mutableStateOf("") }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, Color(0xFFDDDDDD))
                        .padding(8.dp)
                ) {
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("底部输入框（键盘弹出测试）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            item {
                Text(
                    text = "场景3：BasicTextField 在 LazyColumn 中",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(3, key = { "basic_$it" }) { index ->
                var text by remember { mutableStateOf("") }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, Color(0xFFDDDDDD))
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth(),
                        cursorBrush = SolidColor(Color.Blue),
                        textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333))
                    )
                }
            }

            item {
                Text(
                    text = "场景4：动态增减输入框",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { dynamicItems = dynamicItems + "" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("添加输入框")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Button(
                        onClick = { if (dynamicItems.isNotEmpty()) dynamicItems = dynamicItems.dropLast(1) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("删除输入框")
                    }
                }
            }

            items(dynamicItems.size, key = { it }) { index ->
                var text by remember { mutableStateOf(dynamicItems[index]) }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .border(1.dp, Color(0xFFDDDDDD))
                        .padding(8.dp)
                ) {
                    TextField(
                        value = text,
                        onValueChange = {
                            text = it
                            dynamicItems = dynamicItems.toMutableList().also { list -> list[index] = text }
                        },
                        label = { Text("动态输入框 $index") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(400.dp))
            }
        }
    }
}

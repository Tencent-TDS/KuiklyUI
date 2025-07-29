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
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardActions
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.extension.nativeRef
import com.tencent.kuikly.compose.foundation.gestures.scrollBy
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.focus.onFocusChanged
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.text.input.KeyboardType
import com.tencent.kuikly.compose.ui.text.input.PasswordVisualTransformation
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.coroutines.GlobalScope
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.launch

@Composable
fun OneItemLazyColumn(
    modifier: Modifier,
    listState: LazyListState,
    content: @Composable () -> Unit,
) {
    LazyColumn(modifier.bouncesEnable(false), state = listState) {
        item {
            content()
        }
    }
}

@Page("TextFieldDemo")
class TextFieldDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            val focusManager = LocalFocusManager.current;
            ComposeNavigationBar {
                var keyboardHeight by remember { mutableStateOf(0f) }
                var awareKeyboardHeight by remember { mutableStateOf(false) }
                val listState = rememberLazyListState()
                OneItemLazyColumn(
                    listState = listState,
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 30.dp, end = 30.dp, bottom = if (awareKeyboardHeight) keyboardHeight.dp else 0.dp).clickable {
                            focusManager.clearFocus()
                        },
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "0 推荐使用输入框 响应键盘高度 自动聚焦")
                    var text7 by remember { mutableStateOf("") }
                    Box(modifier = Modifier.border(1.dp, Color.Black)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = text7,
                                onValueChange = {
                                    text7 = it
                                },
                                textStyle = TextStyle(
                                    fontSize = 30.sp,
                                    color = Color.Blue,
                                ),
                                maxLines = 2,
                                modifier = Modifier.width(150.dp),
                                onTextLayout = {},
                                placeholderColor = Color.Red,
                                autoFocus = false,
                                placeholder = "占位文本",
                                keyboardHeightChange = {
                                    println("TextFieldDemo.keyboardHeightChange" + it)
                                },
                                onBlur = {
                                    println("TextFieldDemo.onBlur")
                                },
                                onFocus = {
                                    println("TextFieldDemo.onFocus")
                                },
                            )
                            Box(
                                modifier = Modifier.height(45.dp).wrapContentWidth()
                                    .background(Color.Red).padding(horizontal = 8.dp).clickable {
                                        text7 = ""
                                    }, contentAlignment = Alignment.Center
                            ) {
                                Text("清除")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "1 BasicTextField Box居中 红色游标")
                    Box(
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                            .border(1.dp, color = Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(modifier = Modifier) {
                            var text2 by remember { mutableStateOf("你好") }
                            BasicTextField(
                                cursorBrush = SolidColor(Color.Red),
                                value = text2,
                                onValueChange = {
                                    text2 = it
                                },
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            )
                            Box(
                                modifier = Modifier.size(50.dp).clickable {
                                    text2 = ""
                                },
                            )
                        }
                    }

                    // 1. 基础输入框
                    Spacer(modifier = Modifier.height(16.dp))
                    var text0 by remember { mutableStateOf("") }
                    Text(text = "2 右侧图标 输入框带：$text0")
                    TextField(
                        modifier = Modifier.fillMaxWidth().nativeRef(ref = {
                            println("nativeRef " + it.nativeRef)
                        }),
                        trailingIcon = {
                            Image(
                                modifier = Modifier.size(50.dp).alpha(0.5f).border(1.dp, Color.Red)
                                    .background(Color.Green),
                                painter = rememberAsyncImagePainter("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59ef6918.gif"),
                                contentDescription = null,
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Crop,
                            )
                        },
                        prefix = {
                            Text("name：")
                        },
                        suffix = {
                            Text("少将")
                        },
                        value = text0,
                        onValueChange = { text0 = it },
                        label = { Text("基础输入框") },
                        placeholder = { Text("请输入内容...") },
                    )


                    Spacer(modifier = Modifier.height(16.dp))
                    var text1 by remember { mutableStateOf("") }
                    Text(text = "3 单行输入不换行：$text1")
                    TextField(
                        singleLine = true,
                        value = text1,
                        onValueChange = { text1 = it },
                        label = {
                            Text(
                                "基础输入框", modifier = Modifier.border(1.dp, Color.Black)
                            )
                        },
                        placeholder = { Text("请输入内容...") },
                        shape = RoundedCornerShape(16.dp),
                    )
//
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "4 密码输入无法换行，高度36dp，响应键盘高度")
                    Box(modifier = Modifier.border(1.dp, Color.Black)) {
                        TextField(
                            value = "",
                            onValueChange = {

                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.fillMaxWidth().height(36.dp).onFocusChanged {
                                awareKeyboardHeight = it.isFocused
                            }.keyboardHeightChange {
                                if (awareKeyboardHeight) {
                                    keyboardHeight = it.height
                                    GlobalScope.launch {
                                        delay(500)
                                        listState.scrollBy(keyboardHeight * 2)
                                    }
                                }
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. 数字输入框
                    Text(text = "5 数字输入 固定宽度100dp ")
                    var number by remember { mutableStateOf("") }
                    Box(modifier = Modifier.border(1.dp, Color.Black)) {
                        BasicTextField(
                            modifier = Modifier.width(100.dp),
                            value = number,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    number = it
                                }
                            },
                            cursorBrush = SolidColor(Color.Red),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                            ),
                        )
                    }

                    // 5. 只读状态的输入框
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = " 6 只读状态的输入框:")
                    TextField(
                        value = "只读状态",
                        onValueChange = { },
                        readOnly = true,
                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            color = Color.Red,
                        ),
                        label = { Text("禁用输入框") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

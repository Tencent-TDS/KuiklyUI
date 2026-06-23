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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.pager.PageSize
import com.tencent.kuikly.compose.foundation.pager.VerticalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardActions
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.foundation.text.maxLength
import com.tencent.kuikly.compose.foundation.text.onLimitChange
import com.tencent.kuikly.compose.extension.nativeRef
import com.tencent.kuikly.compose.foundation.gestures.scrollBy
import com.tencent.kuikly.compose.foundation.layout.wrapContentWidth
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.text.autoHideKeyboardOnImeAction
import com.tencent.kuikly.compose.material3.Button
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
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.text.input.KeyboardType
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.views.LengthLimitType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                    val scope = rememberCoroutineScope()
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
                                modifier = Modifier.weight(1f),
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
                    var doneState by remember { mutableStateOf("") }
                    Text(text = "4 密码输入无法换行，高度80dp，响应键盘高度 ${doneState}")
                    val keyboardController = LocalSoftwareKeyboardController.current
                    Box(modifier = Modifier.border(1.dp, Color.Black)) {
                        TextField(
                            value = "",
                            onValueChange = {

                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // 处理回车Done逻辑
                                    doneState = "已点击Done"
                                    keyboardController?.hide()
                                }
                            ),
                            modifier = Modifier.height(80.dp).onFocusChanged {
                                awareKeyboardHeight = it.isFocused
                            }.keyboardHeightChange {
                                if (awareKeyboardHeight) {
                                    keyboardHeight = it.height
                                    scope.launch {
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

                    // 6. 最大长度限制与超限回调
                    Spacer(modifier = Modifier.height(16.dp))
                    var textMaxLength by remember { mutableStateOf("") }
                    val maxLength = 10
                    var currentLength by remember { mutableStateOf(0) }
                    var notify by remember { mutableStateOf(false) }
                    TextField(
                        value = textMaxLength,
                        onValueChange = { textMaxLength = it },
                        modifier = Modifier.fillMaxWidth()
                            .maxLength(length = 10, type = LengthLimitType.CHARACTER)
                            .onLimitChange { length, limit ->
                                currentLength = length
                                if (limit) {
                                    notify = true
                                    scope.launch {
                                        delay(250)
                                        notify = false
                                        delay(250)
                                        notify = true
                                        delay(250)
                                        notify = false
                                    }
                                }
                            },
                        label = { Text(
                            "最多10个字符($currentLength/$maxLength)",
                            color = if (notify) Color.Red else Color.Black
                        ) },
                        placeholder = { Text("请输入，超过10字将无法继续输入") },
                    )

                    // 7. 禁用状态的输入框
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = " 禁用状态的输入框:")
                    TextField(
                        value = "禁用状态",
                        enabled = false,
                        onValueChange = { },
                        textStyle = TextStyle(
                            fontSize = 36.sp,
                            color = Color.Red,
                        ),
                        label = { Text("禁用输入框") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // 8. KeyboardActions
                    Spacer(modifier = Modifier.height(16.dp))
                    var singleLine by remember { mutableStateOf(true) }
                    key(singleLine) {
                        Button(
                            onClick = { singleLine = !singleLine }
                        ) {
                            Text(if (singleLine) "切换为多行输入" else "切换为单行输入")
                        }
                        var actionsEvent by remember { mutableStateOf("") }
                        val modifier = Modifier
                            .autoHideKeyboardOnImeAction(true)
                            .onFocusChanged {
                            if (it.isFocused) {
                                actionsEvent = ""
                            }

                        }
                        val keyboardActions = remember {
                            KeyboardActions(
                                onDone = {
                                    actionsEvent = "onDone"
                                },
                                onGo = {
                                    actionsEvent = "onGo"
                                },
                                onNext = {
                                    actionsEvent = "onNext"
                                },
                                onPrevious = {
                                    actionsEvent = "onPrevious"
                                },
                                onSearch = {
                                    actionsEvent = "onSearch"
                                },
                                onSend = {
                                    actionsEvent = "onSend"
                                }
                            )
                        }
                        Text("KeyboardActions 事件：$actionsEvent")
                        var textNotSet by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction Not Set") },
                            value = textNotSet,
                            onValueChange = { textNotSet = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textDefault by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Default") },
                            value = textDefault,
                            onValueChange = { textDefault = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Default
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textUnspecified by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Unspecified") },
                            value = textUnspecified,
                            onValueChange = { textUnspecified = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Unspecified
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textNone by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.None") },
                            value = textNone,
                            onValueChange = { textNone = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.None
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textGo by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Go") },
                            value = textGo,
                            onValueChange = { textGo = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textSearch by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Search") },
                            value = textSearch,
                            onValueChange = { textSearch = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textSend by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Send") },
                            value = textSend,
                            onValueChange = { textSend = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textPrevious by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Previous") },
                            value = textPrevious,
                            onValueChange = { textPrevious = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Previous
                            ),
                            keyboardActions = keyboardActions,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textNext by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Next") },
                            value = textNext,
                            onValueChange = { textNext = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = keyboardActions,

                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        var textDone by remember { mutableStateOf("") }
                        TextField(
                            label = { Text("ImeAction.Done") },
                            value = textDone,
                            onValueChange = { textDone = it },
                            modifier = modifier,
                            singleLine = singleLine,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = keyboardActions,
                        )
                    }

                    // 9. 多行文本框 KeyboardActions 测试
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "9. 多行文本框 KeyboardActions 测试 (numLines > 1)")
                    var multiLineNumLines by remember { mutableStateOf(3) }
                    var multiLineActionsEvent by remember { mutableStateOf("") }
                    val multiLineModifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Blue)
                        .padding(8.dp)
                        .autoHideKeyboardOnImeAction(true)
                        .onFocusChanged {
                            if (it.isFocused) {
                                multiLineActionsEvent = ""
                            }
                        }

                    val multiLineKeyboardActions = remember {
                        KeyboardActions(
                            onDone = {
                                multiLineActionsEvent = "onDone"
                            },
                            onGo = {
                                multiLineActionsEvent = "onGo"
                            },
                            onNext = {
                                multiLineActionsEvent = "onNext"
                            },
                            onPrevious = {
                                multiLineActionsEvent = "onPrevious"
                            },
                            onSearch = {
                                multiLineActionsEvent = "onSearch"
                            },
                            onSend = {
                                multiLineActionsEvent = "onSend"
                            }
                        )
                    }

                    // 切换行数按钮
                    Button(
                        onClick = { multiLineNumLines = if (multiLineNumLines == 3) 5 else 3 }
                    ) {
                        Text("切换行数: 当前 $multiLineNumLines 行")
                    }
                    Text("多行文本框 KeyboardActions 事件：$multiLineActionsEvent")

                    // 使用 BasicTextField 测试各种 imeAction
                    key(multiLineNumLines) {
                        var textMLDefault by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLDefault,
                            onValueChange = { textMLDefault = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Default
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLDefault.isEmpty()) {
                                        Text("Default (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLGo by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLGo,
                            onValueChange = { textMLGo = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLGo.isEmpty()) {
                                        Text("Go (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLSearch by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLSearch,
                            onValueChange = { textMLSearch = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLSearch.isEmpty()) {
                                        Text("Search (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLSend by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLSend,
                            onValueChange = { textMLSend = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLSend.isEmpty()) {
                                        Text("Send (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLNext by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLNext,
                            onValueChange = { textMLNext = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLNext.isEmpty()) {
                                        Text("Next (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLDone by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLDone,
                            onValueChange = { textMLDone = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLDone.isEmpty()) {
                                        Text("Done (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLPrevious by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLPrevious,
                            onValueChange = { textMLPrevious = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Previous
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLPrevious.isEmpty()) {
                                        Text("Previous (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLNone by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLNone,
                            onValueChange = { textMLNone = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.None
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLNone.isEmpty()) {
                                        Text("None (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var textMLUnspecified by remember { mutableStateOf("") }
                        BasicTextField(
                            value = textMLUnspecified,
                            onValueChange = { textMLUnspecified = it },
                            modifier = multiLineModifier,
                            textStyle = TextStyle(fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Unspecified
                            ),
                            keyboardActions = multiLineKeyboardActions,
                            maxLines = multiLineNumLines,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (textMLUnspecified.isEmpty()) {
                                        Text("Unspecified (多行)", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    // space for keyboard coverage
                    Spacer(modifier = Modifier.height(300.dp))
                }
            }
        }
    }
}

@Page("88888")
internal class VerticalPagerRotateJitterReproPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            VerticalPagerRotateJitterRepro()
        }
    }
}

@Composable
private fun VerticalPagerRotateJitterRepro() {
    val configuration = LocalConfiguration.current
    val pages = remember { listOf(5, 6, 7, 8, 9) }
    val pagerState = rememberPagerState { pages.size }

    var sizeChangeCount by remember { mutableIntStateOf(0) }
    var lastSize by remember { mutableStateOf("-") }

    LaunchedEffect(configuration.pageViewWidth, configuration.pageViewHeight) {
        sizeChangeCount += 1
        lastSize = "${configuration.pageViewWidth.toInt()} x ${configuration.pageViewHeight.toInt()}"
    }

    val orientation = if (configuration.pageViewWidth >= configuration.pageViewHeight) "横屏" else "竖屏"
    val visiblePages = pagerState.layoutInfo.visiblePagesInfo
        .take(3)
        .joinToString { "${it.index}@${it.offset}" }
        .ifEmpty { "无" }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().bouncesEnable(false),
            pageSize = PageSize.Fill,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageColor(page))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Only Page ${page + 1}",
                    fontSize = 42.sp,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.48f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "VerticalPager 旋转抖动复现页",
                color = Color.White,
                fontSize = 18.sp,
            )
            Text(
                text = "条件：仅 1 个元素 + bouncesEnable=false",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "步骤：1. 竖屏进入 2. 旋转到横屏 3. 首次上下滑动观察是否抖动",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "方向=$orientation  pageView=$lastSize  sizeChangeCount=$sizeChangeCount",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "pageCount=${pages.size}  currentPage=${pagerState.currentPage}  offset=${pagerState.currentPageOffsetFraction}",
                color = Color.White,
                fontSize = 12.sp,
            )
            Text(
                text = "bouncesEnable=false  pageSize=${pagerState.layoutInfo.pageSize}  visible=$visiblePages",
                color = Color.White,
                fontSize = 12.sp,
            )
        }
    }
}

private fun pageColor(page: Int): Color {
    return when (page % 6) {
        0 -> Color(0xFF5B8FF9)
        1 -> Color(0xFF61DDAA)
        2 -> Color(0xFF65789B)
        3 -> Color(0xFFF6BD16)
        4 -> Color(0xFF7262FD)
        else -> Color(0xFFF6903D)
    }
}

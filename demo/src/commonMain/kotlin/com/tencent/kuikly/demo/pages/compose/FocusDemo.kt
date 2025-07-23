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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.focusable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusDirection
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.focus.onFocusChanged
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalFocusManager
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("focusDemo")
internal class FocusDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        layoutDirection = LayoutDirection.Ltr
        setContent {
            ComposeNavigationBar {
                FocusDemo()
            }
        }
    }
}

@Composable
fun FocusableBox(modifier: Modifier) {
    val focusRequester = remember { FocusRequester() }
    // 1. 定义焦点状态变量
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                // 添加焦点监听
                .onFocusChanged { isFocused = it.isFocused }
                // 设置可聚焦属性
                .focusable()
                .clickable {
                    focusRequester.requestFocus() // 点击时请求焦点
                }
                // 根据焦点状态切换背景色
                .background(if (isFocused) Color.Blue else Color.Gray),
    )
}

@Composable
fun HighlightTextField(modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val (text, setText) = remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = setText,
        modifier =
            modifier
                .onFocusChanged { isFocused = it.hasFocus }
                .background(if (isFocused) Color.Yellow else Color.Gray),
    )
}

@Composable
fun FocusDemo() {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        LaunchedEffect(Unit) {
            // autofocus
            focusRequester.requestFocus()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            HighlightTextField(Modifier.size(100.dp, 50.dp))
            HighlightTextField(Modifier.size(100.dp, 50.dp).focusRequester(focusRequester))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            FocusableBox(Modifier.size(100.dp, 50.dp))
            FocusableBox(Modifier.size(100.dp, 50.dp))
        }
        Row {
            Column(Modifier.weight(1f)) {
                Button(onClick = { focusManager.moveFocus(FocusDirection.Right) }) { Text("Right") }
                Button(onClick = { focusManager.moveFocus(FocusDirection.Left) }) { Text("Left") }
                Button(onClick = { focusManager.moveFocus(FocusDirection.Up) }) { Text("Up") }
                Button(onClick = { focusManager.moveFocus(FocusDirection.Down) }) { Text("Down") }
                Button(onClick = { focusManager.moveFocus(FocusDirection.Previous) }) { Text("Previous") }
                Button(onClick = { focusManager.moveFocus(FocusDirection.Next) }) { Text("Next") }
            }
            Column(Modifier.weight(1f)) {
                Button(onClick = { focusRequester.requestFocus() }) { Text("Focus TextField") }
                Button(onClick = { focusManager.clearFocus() }) { Text("Clear Focus") }
                Button(onClick = { keyboardController?.show() }) { Text("Show Keyboard") }
                Button(onClick = { keyboardController?.hide() }) { Text("Hide Keyboard") }
            }
        }
    }
}

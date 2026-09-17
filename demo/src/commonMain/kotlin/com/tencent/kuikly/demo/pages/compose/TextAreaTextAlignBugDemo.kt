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
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * 复现 iOS BasicTextField 设置 TextAlign.Right/Center 后，获焦或输入内容时对齐被重置为左对齐的问题。
 * 验证：打开页面，点击「居中」「右对齐」输入框并输入，观察对齐是否始终保持。
 */
@Page("TextAreaTextAlignBugDemo")
class TextAreaTextAlignBugDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Left 对齐（预期：点击/输入后始终左对齐）")
                    var textLeft by remember { mutableStateOf("Left 123456") }
                    BasicTextField(
                        value = textLeft,
                        onValueChange = { textLeft = it },
                        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Left),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                            .border(1.dp, Color.Black).padding(8.dp),
                    )

                    Text("Center 对齐（预期：点击/输入后始终居中）")
                    var textCenter by remember { mutableStateOf("Center 123456") }
                    BasicTextField(
                        value = textCenter,
                        onValueChange = { textCenter = it },
                        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                            .border(1.dp, Color.Black).padding(8.dp),
                    )

                    Text("Right 对齐（预期：点击/输入后始终右对齐，iOS 实际会掉回左对齐）")
                    var textRight by remember { mutableStateOf("Right 123456") }
                    BasicTextField(
                        value = textRight,
                        onValueChange = { textRight = it },
                        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Right),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                            .border(1.dp, Color.Black).padding(8.dp),
                    )
                }
            }
        }
    }
}

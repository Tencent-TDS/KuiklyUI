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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.textPostProcessor
import com.tencent.kuikly.compose.foundation.background
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
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.input.TextPostProcessorOutputTransformation
import com.tencent.kuikly.compose.foundation.text.input.rememberTextFieldState
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TextFieldEmojiDemo")
class TextFieldEmojiDemo : ComposeContainer() {

    private val emojiList = listOf(
        "[smile]" to "微笑",
        "[heart]" to "爱心",
        "[thumbup]" to "点赞",
        "[star]" to "星星",
        "[fire]" to "火焰",
    )

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                TextFieldEmojiDemoImpl()
            }
        }
    }

    @Composable
    fun TextFieldEmojiDemoImpl() {
        val state = rememberTextFieldState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Text(
                text = "自定义表情输入 Demo",
                fontSize = 20.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "演示 BasicTextField + TextPostProcessor 实现表情输入\n注意：iOS 上 singleLine=true 时 UITextField 不支持 NSTextAttachment 图片渲染，仅多行模式支持表情预览",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "① BasicTextField（多行输入框 + 表情预览）",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White)
                    .padding(12.dp)
                    .textPostProcessor("input"),
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "② EmojiGrid（自定义表情面板）",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                EmojiGrid(emojiList) { shortCode ->
                    state.edit {
                        replace(selection.start, selection.end, shortCode)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "③ 原始文本 State.text（短码格式）",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = state.text.ifEmpty { "（暂无内容）" },
                fontSize = 14.sp,
                color = Color(0xFF333333),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "④ 表情预览 Text.composable（textPostProcessor 修饰符）",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Text(
                    text = state.text.ifEmpty { "（请在上方输入框或表情面板输入）" },
                    fontSize = 16.sp,
                    color = if (state.text.isEmpty()) Color(0xFFCCCCCC) else Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth()
                        .textPostProcessor("input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { state.clearText() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空内容")
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun EmojiGrid(
    emojis: List<Pair<String, String>>,
    onEmojiClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        emojis.forEach { (shortCode, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onEmojiClick(shortCode) }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFF0F0F0), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.first().toString(),
                        fontSize = 20.sp,
                        color = Color(0xFF666666)
                    )
                }
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

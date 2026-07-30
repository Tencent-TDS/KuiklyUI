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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.input.TextPostProcessorOutputTransformation
import com.tencent.kuikly.compose.foundation.text.input.rememberTextFieldState
import com.tencent.kuikly.compose.foundation.text.maxLength
import com.tencent.kuikly.compose.foundation.text.onLimitChange
import com.tencent.kuikly.core.views.LengthLimitType
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextRange
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.foundation.text.input.TextFieldState
import com.tencent.kuikly.core.annotations.Page
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "自定义表情输入 Demo",
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "每个输入框配套独立表情面板与快捷预置，覆盖中间插入、选区替换、超限拒绝，以及 CHARACTER / BYTE / VISUAL_WIDTH / singleLine 回归",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // 测试用例 1: CHARACTER 模式
            item { CharacterModeCard() }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 测试用例 2: BYTE 模式
            item { ByteModeCard() }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 测试用例 3: VISUAL_WIDTH 模式
            item { VisualWidthModeCard() }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 测试用例 4: SingleLine + BYTE 模式（iOS 回归）
            item { SingleLineByteModeCard() }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // 测试用例 6: 无限制模式（表情预览）
            item { PreviewModeCard() }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    @Composable
    fun CharacterModeCard() {
        val state = rememberTextFieldState()
        val focusRequester = remember { FocusRequester() }
        val maxLength = 140
        var currentLength by remember { mutableStateOf(0) }
        var isLimit by remember { mutableStateOf(false) }

        DemoCard(title = "CHARACTER 模式", description = "[smile] 渲染为 ImageSpan 后算 1 个字符") {
            // 输入框
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(4.dp))
                    .padding(12.dp)
                    .textPostProcessor("input")
                    .maxLength(maxLength, LengthLimitType.CHARACTER)
                    .onLimitChange { length, limit ->
                        currentLength = length
                        isLimit = limit
                    },
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            // 长度提示（使用渲染层计算的长度）
            Text(
                text = "字数: $currentLength / $maxLength ${if (isLimit) "【已达上限】" else ""}",
                fontSize = 12.sp,
                color = if (isLimit) Color.Red else Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )

            EmojiScenarioShortcuts(
                state = state,
                focusRequester = focusRequester,
                middlePreset = EmojiScenarioPreset("1234abcd5678", 4),
                replacePreset = EmojiScenarioPreset("1234abcd5678", 4, 8),
                rejectPreset = EmojiScenarioPreset("12345678901234567890", 10),
                rejectHint = "CHARACTER：先点“超限”，再点任意表情，应该整段拒绝且保留当前 raw 选区。"
            )

            EmojiGrid(emojiList) { shortCode ->
                state.insertEmojiShortCode(shortCode)
            }
        }
    }

    @Composable
    fun ByteModeCard() {
        val state = rememberTextFieldState()
        val focusRequester = remember { FocusRequester() }
        val maxBytes = 21
        var currentLength by remember { mutableStateOf(0) }
        var isLimit by remember { mutableStateOf(false) }

        DemoCard(title = "BYTE 模式", description = "[smile] 按原始短码 UTF-8 字节数统计；ASCII=1 字节，中文≈3 字节") {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(4.dp))
                    .padding(12.dp)
                    .textPostProcessor("input")
                    .maxLength(maxBytes, LengthLimitType.BYTE)
                    .onLimitChange { length, limit ->
                        currentLength = length
                        isLimit = limit
                    },
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            Text(
                text = "字节数: $currentLength / $maxBytes ${if (isLimit) "【已达上限】" else ""}",
                fontSize = 12.sp,
                color = if (isLimit) Color.Red else Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )

            EmojiScenarioShortcuts(
                state = state,
                focusRequester = focusRequester,
                middlePreset = EmojiScenarioPreset("hello-world", 5),
                replacePreset = EmojiScenarioPreset("hello-world", 5, 10),
                rejectPreset = EmojiScenarioPreset("12345678901234567890", 10),
                rejectHint = "BYTE：先点“超限”，再点任意表情，应该拒绝整段短码，不出现半个 token。"
            )

            EmojiGrid(emojiList) { shortCode ->
                state.insertEmojiShortCode(shortCode)
            }
        }
    }

    @Composable
    fun VisualWidthModeCard() {
        val state = rememberTextFieldState()
        val focusRequester = remember { FocusRequester() }
        val maxVisualWidth = 12
        var currentLength by remember { mutableStateOf(0) }
        var isLimit by remember { mutableStateOf(false) }

        DemoCard(title = "VISUAL_WIDTH 模式", description = "[smile] 渲染为 ImageSpan 后按 2 个视觉宽度统计，半角=1，全角/Emoji=2") {
            BasicTextField(
                state = state,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(4.dp))
                    .padding(12.dp)
                    .textPostProcessor("input")
                    .maxLength(maxVisualWidth, LengthLimitType.VISUAL_WIDTH)
                    .onLimitChange { length, limit ->
                        currentLength = length
                        isLimit = limit
                    },
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            Text(
                text = "视觉宽度: $currentLength / $maxVisualWidth ${if (isLimit) "【已达上限】" else ""}",
                fontSize = 12.sp,
                color = if (isLimit) Color.Red else Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )

            EmojiScenarioShortcuts(
                state = state,
                focusRequester = focusRequester,
                middlePreset = EmojiScenarioPreset("ab中文cd", 2),
                replacePreset = EmojiScenarioPreset("ab中文cd", 2, 4),
                rejectPreset = EmojiScenarioPreset("中文中文中文", 3),
                rejectHint = "VISUAL_WIDTH：先点“超限”，再点任意表情，应该按渲染后的视觉宽度整段拒绝。"
            )

            EmojiGrid(emojiList) { shortCode ->
                state.insertEmojiShortCode(shortCode)
            }
        }
    }

    @Composable
    fun SingleLineByteModeCard() {
        val state = rememberTextFieldState()
        val focusRequester = remember { FocusRequester() }
        val maxBytes = 21
        var currentLength by remember { mutableStateOf(0) }
        var isLimit by remember { mutableStateOf(false) }

        DemoCard(
            title = "SingleLine + BYTE 模式（iOS 回归）",
            description = "用于回归单行输入的表情插入、超限整段拒绝后的光标恢复，以及纯光标移动时 selection 同步"
        ) {
            BasicTextField(
                state = state,
                singleLine = true,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(24.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .textPostProcessor("input")
                    .maxLength(maxBytes, LengthLimitType.BYTE)
                    .onLimitChange { length, limit ->
                        currentLength = length
                        isLimit = limit
                    },
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            Text(
                text = "字节数: $currentLength / $maxBytes ${if (isLimit) "【已达上限】" else ""}",
                fontSize = 12.sp,
                color = if (isLimit) Color.Red else Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )

            EmojiScenarioShortcuts(
                state = state,
                focusRequester = focusRequester,
                middlePreset = EmojiScenarioPreset("single-line-demo", 6),
                replacePreset = EmojiScenarioPreset("single-line-demo", 7, 11),
                rejectPreset = EmojiScenarioPreset("12345678901234567890", 8),
                rejectHint = "singleLine：先点“超限”，再点任意表情，重点观察 iOS 拒绝后光标是否回到合法 raw 位置。"
            )

            EmojiGrid(emojiList) { shortCode ->
                state.insertEmojiShortCode(shortCode)
            }
        }
    }

    @Composable
    fun PreviewModeCard() {
        val state = rememberTextFieldState()

        DemoCard(title = "表情预览（无限制）", description = "演示 textPostProcessor 渲染效果") {
            // 输入框（无长度限制）
            BasicTextField(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFF8F8F8), RoundedCornerShape(4.dp))
                    .padding(12.dp)
                    .textPostProcessor("input"),
                outputTransformation = TextPostProcessorOutputTransformation("input"),
                textStyle = TextStyle(fontSize = 16.sp, color = Color(0xFF333333)),
            )

            // 原始文本显示
            Text(
                text = "原始文本: ${state.text.ifEmpty { "（空）" }}",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 8.dp)
            )

            // 配套表情面板
            EmojiGrid(emojiList) { shortCode ->
                state.insertEmojiShortCode(shortCode)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 渲染预览
            Text(
                text = "渲染预览:",
                fontSize = 12.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = state.text.ifEmpty { "（请在上方输入）" },
                    fontSize = 16.sp,
                    color = if (state.text.isEmpty()) Color(0xFFCCCCCC) else Color(0xFF333333),
                    modifier = Modifier
                        .fillMaxWidth()
                        .textPostProcessor("input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { state.clearText() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("清空内容", fontSize = 14.sp)
            }
        }
    }

    private data class EmojiScenarioPreset(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int = selectionStart,
    )

    private fun TextFieldState.insertEmojiShortCode(shortCode: String) {
        val rawSelection = selection
        edit {
            replace(rawSelection.start, rawSelection.end, shortCode)
        }
    }

    private fun TextFieldState.applyScenarioPreset(preset: EmojiScenarioPreset) {
        edit {
            replace(0, length, preset.text)
            selection = TextRange(preset.selectionStart, preset.selectionEnd)
        }
    }

    @Composable
    private fun EmojiScenarioShortcuts(
        state: TextFieldState,
        focusRequester: FocusRequester? = null,
        middlePreset: EmojiScenarioPreset,
        replacePreset: EmojiScenarioPreset,
        rejectPreset: EmojiScenarioPreset,
        rejectHint: String,
    ) {
        Text(
            text = "快捷预置：先点“中间 / 替换 / 超限”，再点任意表情，验证选区替换和 maxLength 拒绝。",
            fontSize = 11.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                state.applyScenarioPreset(middlePreset)
                focusRequester?.requestFocus()
            }) {
                Text("中间", fontSize = 12.sp)
            }
            Button(onClick = {
                state.applyScenarioPreset(replacePreset)
                focusRequester?.requestFocus()
            }) {
                Text("替换", fontSize = 12.sp)
            }
            Button(onClick = {
                state.applyScenarioPreset(rejectPreset)
                focusRequester?.requestFocus()
            }) {
                Text("超限", fontSize = 12.sp)
            }
        }
        Text(
            text = "当前 raw: ${state.text.ifEmpty { "（空）" }}",
            fontSize = 11.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "当前选区: ${state.selection.start} ~ ${state.selection.end}",
            fontSize = 11.sp,
            color = Color(0xFF999999),
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = rejectHint,
            fontSize = 11.sp,
            color = Color(0xFFCC6A00),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }

    @Composable
    fun DemoCard(
        title: String,
        description: String,
        content: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
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
                        .size(40.dp)
                        .background(Color(0xFFF0F0F0), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.first().toString(),
                        fontSize = 18.sp,
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

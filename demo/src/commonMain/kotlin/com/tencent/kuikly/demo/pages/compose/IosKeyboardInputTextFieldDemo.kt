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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.extension.placeHolder
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
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardActions
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.foundation.text.maxLength
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.text.TextLayoutResult
import com.tencent.kuikly.compose.ui.text.TextRange
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.text.input.KeyboardType
import com.tencent.kuikly.compose.ui.text.input.TextFieldValue
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.views.KeyboardParams
import com.tencent.kuikly.core.views.LengthLimitType
import kotlinx.coroutines.delay

/**
 * 业务侧 iOS 键盘问题复现页：集成业务提供的 [InputTextField] + [MeetingName] 组件写法。
 *
 * 复现步骤：点击会议名称输入框 → 观察键盘弹出/收起、光标位置、清空按钮行为。
 */
@Page("IosKeyboardInputTextFieldDemo")
internal class IosKeyboardInputTextFieldDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("iOS键盘InputTextField复现") {
                Content()
            }
        }
    }

    @Composable
    private fun Content() {
        var meetingName by remember { mutableStateOf("张三的测试会议") }
        var keyboardHeight by remember { mutableStateOf(0f) }
        var keyboardDuration by remember { mutableStateOf(0f) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "业务 InputTextField 复现",
                fontSize = 18.sp,
                color = Color.Black,
            )
            Text(
                text = "默认不传 autoFocusOnTextInputState：带预填文本进页不应自动弹键盘。",
                fontSize = 13.sp,
                color = Color(0xFF666666),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                MeetingName(
                    text = meetingName,
                    hint = "请输入会议名称",
                    onTextChange = { meetingName = it },
                    keyboardHeightChange = { params ->
                        keyboardHeight = params.height
                        keyboardDuration = params.duration
                        println("[IosKeyboardInputTextFieldDemo] keyboard height=${params.height}, duration=${params.duration}")
                    },
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("键盘状态", fontSize = 14.sp, color = Color(0xFF1565C0))
                    Spacer(Modifier.height(4.dp))
                    Text("高度: ${keyboardHeight.toInt()} dp", fontSize = 13.sp, color = Color(0xFF424242))
                    Text("动画时长: ${keyboardDuration.toInt()} ms", fontSize = 13.sp, color = Color(0xFF424242))
                    Text("当前文本: $meetingName", fontSize = 13.sp, color = Color(0xFF424242))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InputTextField(
    inputValue: String?,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    hint: String? = null,
    hintColor: Color = Color(0xFF999999),
    autoFocus: Boolean = false,
    keyboardHeightChange: (KeyboardParams) -> Unit = {},
    cursorBrush: Brush = SolidColor(Color(0xFF1976D2)),
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int = Int.MAX_VALUE,
    lengthLimitType: LengthLimitType = LengthLimitType.CHARACTER,
    keyboardType: KeyboardType = KeyboardType.Text,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val currentKeyboardHeightChange by rememberUpdatedState(keyboardHeightChange)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnTextLayout by rememberUpdatedState(onTextLayout)

    val updatedModifier = modifier
        .keyboardHeightChange(currentKeyboardHeightChange)
        .focusRequester(focusRequester)
        .maxLength(maxLength, type = lengthLimitType)
        .let {
            if (!hint.isNullOrEmpty()) {
                it.placeHolder(hint, hintColor)
            } else it
        }

    val textFieldValueState = remember {
        mutableStateOf(
            TextFieldValue(
                text = inputValue ?: "",
                selection = TextRange(inputValue?.length ?: 0),
            ),
        )
    }

    val currentText = inputValue ?: ""
    if (textFieldValueState.value.text != currentText) {
        textFieldValueState.value = TextFieldValue(
            text = currentText,
            selection = TextRange(currentText.length),
        )
    }

    BasicTextField(
        modifier = updatedModifier,
        value = textFieldValueState.value,
        onValueChange = { newValue ->
            textFieldValueState.value = newValue
            currentOnValueChange(newValue)
        },
        textStyle = textStyle,
        maxLines = maxLines,
        onTextLayout = currentOnTextLayout,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(onAny = {
            // 键盘右下角点击事件
        }),
        cursorBrush = cursorBrush,
    )

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(50)
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun MeetingName(
    text: String?,
    hint: String?,
    onTextChange: (String) -> Unit,
    keyboardHeightChange: (KeyboardParams) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 6.dp),
        ) {
            InputTextField(
                inputValue = text,
                onValueChange = { newValue ->
                    onTextChange(newValue.text)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color(0xFF212121),
                    fontSize = 16.sp,
                ),
                hint = hint,
                maxLength = 50,
                keyboardHeightChange = keyboardHeightChange,
            )
        }

        Box(
            modifier = Modifier
                .size(33.dp)
                .padding(end = 11.dp)
                .clickable { onTextChange("") },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "×",
                fontSize = 22.sp,
                color = Color(0xFF9E9E9E),
            )
        }
    }
}

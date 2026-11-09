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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.input.KeyboardType
import com.tencent.kuikly.compose.ui.text.input.TextFieldValue
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("KeyboardTypeDemo")
class KeyboardTypeDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("KeyboardType 键盘类型示例") {
                KeyboardTypeList()
            }
        }
    }
}

@Composable
private fun KeyboardTypeList() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            KeyboardTypeSectionTitle("单行 TextField（singleLine = true）")
            KeyboardTypeField("Text（默认文本键盘）", KeyboardType.Text, singleLine = true)
            KeyboardTypeField("Ascii（ASCII 键盘）", KeyboardType.Ascii, singleLine = true)
            KeyboardTypeField("Number（数字键盘）", KeyboardType.Number, singleLine = true)
            KeyboardTypeField("Phone（电话键盘）", KeyboardType.Phone, singleLine = true)
            KeyboardTypeField("Uri（网址键盘）", KeyboardType.Uri, singleLine = true)
            KeyboardTypeField("Email（邮箱键盘）", KeyboardType.Email, singleLine = true)
            KeyboardTypeField("Password（密码键盘）", KeyboardType.Password, singleLine = true)
            KeyboardTypeField("NumberPassword（数字密码键盘）", KeyboardType.NumberPassword, singleLine = true)
            KeyboardTypeField("Decimal（小数键盘）", KeyboardType.Decimal, singleLine = true)

            Spacer(modifier = Modifier.height(12.dp))
            KeyboardTypeSectionTitle("多行 TextField（singleLine = false，默认）")
            KeyboardTypeField("Text（默认文本键盘）", KeyboardType.Text, singleLine = false)
            KeyboardTypeField("Ascii（ASCII 键盘）", KeyboardType.Ascii, singleLine = false)
            KeyboardTypeField("Number（数字键盘）", KeyboardType.Number, singleLine = false)
            KeyboardTypeField("Phone（电话键盘）", KeyboardType.Phone, singleLine = false)
            KeyboardTypeField("Uri（网址键盘）", KeyboardType.Uri, singleLine = false)
            KeyboardTypeField("Email（邮箱键盘）", KeyboardType.Email, singleLine = false)
            KeyboardTypeField("Password（密码键盘）", KeyboardType.Password, singleLine = false)
            KeyboardTypeField("NumberPassword（数字密码键盘）", KeyboardType.NumberPassword, singleLine = false)
            KeyboardTypeField("Decimal（小数键盘）", KeyboardType.Decimal, singleLine = false)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KeyboardTypeSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        color = Color(0xFF333333),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun KeyboardTypeField(
    label: String,
    keyboardType: KeyboardType,
    singleLine: Boolean,
) {
    // 须用 TextFieldValue 受控：String 重载每次重组会包成 TextFieldValue(text) 且 selection=0，光标会钉在开头
    var value by remember(keyboardType, singleLine) { mutableStateOf(TextFieldValue("")) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = label, fontSize = 14.sp, color = Color(0xFF666666))
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = { value = it },
            modifier = if (singleLine) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().height(88.dp)
            },
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            shape = RoundedCornerShape(8.dp),
            placeholder = {
                val mode = if (singleLine) "单行" else "多行"
                Text("[$mode] 点击弹出「$keyboardType」键盘")
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

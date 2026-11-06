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

package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.font.KuiklyFont
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * iOS 字体解析验证页：generic family 映射、字体加载失败时回落系统字体并保留字重与斜体
 */
@Page("BugReproIosGenericFontWeightPage")
internal class BugReproIosGenericFontWeightPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            GenericFontWeightContent()
        }
    }
}

private class FontCase(
    val title: String,
    val expect: String,
    val style: TextStyle,
)

private const val SAMPLE_TEXT = "Kuikly 字重 Aa 123"

@Composable
private fun fontCases(): List<FontCase> = listOf(
    FontCase("Default · Normal", "Regular 基准", TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal)),
    FontCase("Default · Bold", "Bold 基准", TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)),
    FontCase("SansSerif · Bold", "与 Default·Bold 一致", TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold)),
    FontCase("SansSerif · Medium", "Medium", TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)),
    FontCase("SansSerif · Bold Italic", "Bold 斜体", TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)),
    FontCase("Serif · Bold", "iOS: New York Bold", TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)),
    FontCase("Monospace · Bold", "iOS: SF Mono Bold", TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)),
    FontCase("Cursive · Bold", "iOS: Snell Roundhand 手写体 Bold", TextStyle(fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold)),
    FontCase(
        "不存在的自定义字体 · Bold Italic",
        "回落系统字体并保留 Bold 斜体",
        TextStyle(
            fontFamily = FontFamily(KuiklyFont("NotExistFont", FontWeight.Bold, FontStyle.Italic)),
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
        ),
    ),
    FontCase(
        "真实字体名 Satisfy-Regular（静态注册）",
        "Satisfy 手写体",
        TextStyle(fontFamily = FontFamily(KuiklyFont("Satisfy-Regular", FontWeight.Normal, FontStyle.Normal))),
    ),
    FontCase(
        "未注册字体名 UnknownFont · Bold",
        "回落系统字体 Bold",
        TextStyle(
            fontFamily = FontFamily(KuiklyFont("UnknownFont", FontWeight.Bold, FontStyle.Normal)),
            fontWeight = FontWeight.Bold,
        ),
    ),
    FontCase("Material typography.titleMedium", "SansSerif + Medium", MaterialTheme.typography.titleMedium),
    FontCase("Material typography.titleLarge", "SansSerif + Normal", MaterialTheme.typography.titleLarge),
)

@Composable
private fun GenericFontWeightContent() {
    val cases = fontCases()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
    ) {
        item(key = "header") {
            Text(
                text = "iOS generic font family 字重 / 斜体 / 字体别名",
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
        }
        cases.forEachIndexed { index, case ->
            item(key = "case-$index") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(Color.White)
                        .padding(12.dp),
                ) {
                    Text(case.title, fontSize = 12.sp, fontFamily = FontFamily.Default, color = Color(0xFF333333))
                    Text("预期：${case.expect}", fontSize = 11.sp, fontFamily = FontFamily.Default, color = Color(0xFF999999))
                    Text(
                        text = SAMPLE_TEXT,
                        modifier = Modifier.padding(top = 6.dp),
                        style = case.style.copy(fontSize = 22.sp, color = Color.Black),
                    )
                }
            }
        }
    }
}

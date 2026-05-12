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
import com.tencent.kuikly.compose.extension.setProp
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextButton
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusRequester
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay

/**
 * 横屏键盘复现：主界面仅按钮；点击后弹出带输入框的 Dialog，通过 [FocusRequester.requestFocus] 尝试拉起键盘。
 *
 * Android 一键横屏：`adb shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity
 * --es pageName ComposeLandscapeKeyboardReproDemo --ez forceLandscape true`
 */
@Page("ComposeLandscapeKeyboardReproDemo")
internal class ComposeLandscapeKeyboardReproDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            Content()
        }
    }

    @Composable
    private fun Content() {
        var showOverlay by remember { mutableStateOf(false) }
        var overlayText by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "横屏键盘复现（浮层 + requestFocus）",
                fontSize = 18.sp,
                color = Color.Black,
            )
            Text(
                text = "点击按钮展开浮层；浮层内 TextField 会在展示后调用 focusRequester.requestFocus()。\n" +
                    "横屏下观察软键盘是否出现。",
                fontSize = 14.sp,
                color = Color(0xFF424242),
            )
            Button(onClick = { showOverlay = true }) {
                Text("打开带输入框的浮层")
            }
        }

        if (showOverlay) {
            Dialog(onDismissRequest = { showOverlay = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("浮层输入", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        TextField(
                            value = overlayText,
                            onValueChange = { overlayText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                // Android 横屏 + Dialog 浮层下，默认全屏 IME 会让软键盘无法弹出；
                                // 关闭 fullscreen IME 即可，同时会触发 restartInput 重启输入连接。
                                .setProp("imeNoFullscreen", true)
                                .focusRequester(focusRequester),
                            placeholder = { Text("焦点由代码请求") },
                        )
                        LaunchedEffect(Unit) {
                            delay(50)
                            focusRequester.requestFocus()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { showOverlay = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}

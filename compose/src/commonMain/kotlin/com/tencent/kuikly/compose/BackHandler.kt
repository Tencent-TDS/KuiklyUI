/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.tencent.kuikly.compose.foundation.event.OnBackPressedCallback
import com.tencent.kuikly.compose.ui.platform.LocalOnBackPressedDispatcherOwner

@Composable
fun BackHandler(onBack: () -> Unit) {
    // 安全地更新当前的onBack lambda，确保使用最新的回调函数
    val currentOnBack = rememberUpdatedState(onBack)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current.onBackPressedDispatcher

    // 在Composition中记住返回键回调
    // 注意: remember {} 中返回匿名对象时，需要指定泛型<>类型
    // 否则在js编译时会报错
    val backCallback = remember<OnBackPressedCallback> {
        object : OnBackPressedCallback() {
            override fun handleOnBackPressed() {
                currentOnBack.value()
            }
        }
    }

    DisposableEffect(Unit) {
        // 将回调添加到返回键分发器
        backDispatcher.addCallback(backCallback)
        // 当效果离开Composition时，移除回调
        onDispose { backDispatcher.removeCallback(backCallback) }
    }
}
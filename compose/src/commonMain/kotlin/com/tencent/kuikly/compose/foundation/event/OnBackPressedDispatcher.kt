/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.event

/**
 * 返回键事件分发器
 * 负责管理和分发系统返回键事件
 */
class OnBackPressedDispatcher() {

    /**
     * 存储所有返回键回调的列表
     */
    val onBackPressedCallbacks = mutableListOf<OnBackPressedCallback>()

    /**
     * 添加返回键回调
     * @param onBackPressedCallback 需要添加的返回键回调
     */
    fun addCallback(onBackPressedCallback: OnBackPressedCallback) {
        onBackPressedCallbacks.add(onBackPressedCallback)
    }

    /**
     * 移除返回键回调
     * @param onBackPressedCallback 需要移除的返回键回调
     */
    fun removeCallback(onBackPressedCallback: OnBackPressedCallback) {
        onBackPressedCallbacks.remove(onBackPressedCallback)
    }

    /**
     * 分发返回键事件，从栈顶往下处理Back回调
     */
    fun dispatchOnBackEvent() {
        if (onBackPressedCallbacks.isNotEmpty()) {
            val callback = onBackPressedCallbacks.last()
            callback.handleOnBackPressed()
        }
    }
}
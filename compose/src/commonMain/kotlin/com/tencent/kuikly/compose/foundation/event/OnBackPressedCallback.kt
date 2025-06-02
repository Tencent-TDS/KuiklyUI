/*
 * Copyright 2018 The Android Open Source Project
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
 * 返回键回调抽象类
 * 用于处理系统返回键事件的回调接口
 */
abstract class OnBackPressedCallback {

    /**
     * 处理返回键按下事件
     * 子类需要实现此方法来处理具体的返回逻辑
     */
    abstract fun handleOnBackPressed()
}
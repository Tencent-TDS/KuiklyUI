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
 * 返回键分发器所有者接口
 * 定义了拥有返回键分发能力的组件接口
 *
 * Created by zhenhuachen on 2025/4/19
 */
interface OnBackPressedDispatcherOwner {
    /**
     * 返回键事件分发器
     * 用于处理系统返回按钮事件
     */
    val onBackPressedDispatcher: OnBackPressedDispatcher
}
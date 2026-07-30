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

package com.tencent.kuikly.core.base

/**
 * 返回键事件拦截分发器负责管理和分发系统返回键事件.
 *
 * 原理：内部维护一个返回键回调列表，当返回键事件发生时，从栈顶往下遍历回调列表，找到第一个能处理返回键事件的回调，执行回调。
 * 如果列表为空，则不拦截返回键事件。如果列表不为空，则拦截返回键事件。
 */
open class BackPressHandler() {

    /**
     * 存储所有返回键回调的列表
     */
    internal val backPressCallbackList = mutableListOf<BackPressCallback>()

    /**
     * 添加返回键回调，开始拦截返回键事件
     * @param backPressCallback 需要添加的返回键回调
     */
    fun addCallback(onBackPressedCallback: BackPressCallback) {
        backPressCallbackList.add(onBackPressedCallback)
    }

    /**
     * 判断返回键回调是否存在
     */
    fun containsCallback(onBackPressedCallback: BackPressCallback): Boolean {
        return backPressCallbackList.contains(onBackPressedCallback)
    }

    /**
     * 移除返回键回调，停止拦截返回键事件
     * @param onBackPressedCallback 需要移除的返回键回调
     */
    fun removeCallback(onBackPressedCallback: BackPressCallback) {
        backPressCallbackList.remove(onBackPressedCallback)
    }

    /**
     * 分发返回键事件，从栈顶往下处理Back回调
     */
    fun dispatchOnBackEvent() {
        if (backPressCallbackList.isNotEmpty()) {
            val callback = backPressCallbackList.last()
            callback.handleOnBackPressed()
        }
    }
}
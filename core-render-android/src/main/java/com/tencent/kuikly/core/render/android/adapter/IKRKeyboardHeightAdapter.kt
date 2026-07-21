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

package com.tencent.kuikly.core.render.android.adapter

import android.app.Activity

/**
 * 键盘高度适配器，供业务按机型/ROM 修正框架上报的键盘高度（单位：px）。
 * 未注册时框架直接透传原始高度。
 */
interface IKRKeyboardHeightAdapter {

    /**
     * @param context 包含 Activity 与框架原始键盘高度
     * @return 修正后的键盘高度（px）
     */
    fun adaptKeyboardHeight(context: KRKeyboardHeightContext): Int {
        return context.rawHeightPx
    }
}

data class KRKeyboardHeightContext(
    val activity: Activity,
    val rawHeightPx: Int,
)

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
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets

/**
 * 框架默认键盘高度适配器，修正部分 ROM 上框架计算的键盘高度偏差。
 * 业务可通过注册 [KuiklyRenderAdapterManager.krKeyboardHeightAdapter] 完全覆盖。
 */
object KRDefaultKeyboardHeightAdapter : IKRKeyboardHeightAdapter {

    override fun adaptKeyboardHeight(context: KRKeyboardHeightContext): Int {
        if (!isSamsung()) {
            return context.rawHeightPx
        }
        return adaptSamsung(context)
    }

    private fun adaptSamsung(context: KRKeyboardHeightContext): Int {
        val activity = context.activity
        val rawHeightPx = context.rawHeightPx

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rootView = activity.findViewById<View>(android.R.id.content) ?: return rawHeightPx
            val insets = rootView.rootWindowInsets ?: return rawHeightPx
            val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
            if (imeHeight > 0) {
                return imeHeight
            }
        }

        val fallback = visibleFrameKeyboardHeight(activity)
        return if (fallback > 0) fallback else rawHeightPx
    }

    private fun visibleFrameKeyboardHeight(activity: Activity): Int {
        val decorView = activity.window.decorView
        val rect = Rect()
        decorView.getWindowVisibleDisplayFrame(rect)
        return (decorView.height - rect.bottom).coerceAtLeast(0)
    }

    private fun isSamsung(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    }
}

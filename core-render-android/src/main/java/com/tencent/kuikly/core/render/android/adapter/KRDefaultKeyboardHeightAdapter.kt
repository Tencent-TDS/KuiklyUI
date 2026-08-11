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
import android.content.res.Resources
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.WindowInsets

/** Android 16 (API 36)，compileSdk 未定义对应 VERSION_CODES 时的兜底常量 */
private const val ANDROID_16_API_LEVEL = 36

/** [Settings.Secure.navigation_mode]：0 = 三键虚拟导航栏 */
private const val NAVIGATION_MODE_THREE_BUTTON = 0

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
        var height = context.rawHeightPx

        if (height > 0) {
            return applySamsungGestureNavCompensation(activity, height)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rootView = activity.findViewById<View>(android.R.id.content) ?: return height
            val insets = rootView.rootWindowInsets ?: return height
            val imeHeight = insets.getInsets(WindowInsets.Type.ime()).bottom
            if (imeHeight > 0) {
                return applySamsungGestureNavCompensation(activity, imeHeight)
            }
        }

        val fallback = visibleFrameKeyboardHeight(activity)
        return if (fallback > 0) {
            applySamsungGestureNavCompensation(activity, fallback)
        } else {
            height
        }
    }

    /** Android 16 + 三星 + 全面屏手势：补上系统静态虚拟导航栏高度 */
    private fun applySamsungGestureNavCompensation(activity: Activity, heightPx: Int): Int {
        if (!shouldAddSamsungGestureNavCompensation(activity)) {
            return heightPx
        }
        return heightPx + getStaticNavigationBarHeightPx()
    }

    private fun shouldAddSamsungGestureNavCompensation(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= ANDROID_16_API_LEVEL
            && isFullScreenGestureNavigation(activity)
    }

    private fun isFullScreenGestureNavigation(activity: Activity): Boolean {
        return try {
            Settings.Secure.getInt(
                activity.contentResolver,
                "navigation_mode",
                NAVIGATION_MODE_THREE_BUTTON,
            ) != NAVIGATION_MODE_THREE_BUTTON
        } catch (_: Exception) {
            false
        }
    }

    /** 系统配置的导航栏高度（与当前 WindowInsets 可见性无关） */
    private fun getStaticNavigationBarHeightPx(): Int {
        val resources = Resources.getSystem()
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
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

/*
 * Copyright 2020 The Android Open Source Project
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

package com.tencent.kuikly.compose.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.core.base.EdgeInsets
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.pager.PageData

/**
 * 页面配置信息，如屏幕信息，页面宽度，设备，版本, 平台等信息
 */
class Configuration(private val pager: IPager) {
    // 当前页面的数据
    val pageData: PageData
        get() {
            return pager.pageData
        }

    // 设备屏幕的宽度（以dp为单位）
    val screenWidthDp: Float
        get() = pageData.deviceWidth

    // 设备屏幕的高度（以dp为单位）
    val screenHeightDp: Float
        get() = pageData.deviceHeight

    private var _pageViewWidth = mutableStateOf(pageData.pageViewWidth)
    private var _activityWidth = mutableStateOf(pageData.activityWidth)

    // 当前活动（Activity/Pager）的宽度
    val activityWidth: Float by _activityWidth
    // 当前活动（Activity/Pager）的宽度
    val pageViewWidth: Float by _pageViewWidth

    private var _pageViewHeight = mutableStateOf(pageData.pageViewHeight)
    private var _activityHeight = mutableStateOf(pageData.activityHeight)

    // 当前活动（Activity/Pager）的高度
    val activityHeight: Float by _activityHeight
    // 当前活动（Activity/Pager）的高度
    val pageViewHeight: Float by _pageViewHeight

    // 是否是 iOS 设备
    val isIOS: Boolean
        get() = pageData.isIOS

    // 是否是 macOS 设备
    val isMacOS: Boolean
        get() = pageData.isMacOS

    // 是否是 Android 设备
    val isAndroid: Boolean
        get() = pageData.isAndroid

    // 是否是 iPhone (刘海屏)
    val isIphoneX: Boolean
        get() = pageData.isIphoneX

    // 状态栏的高度（dp）
    val statusBarHeight: Float
        get() = pageData.statusBarHeight

    // 导航栏的高度（dp）
    val navigationBarHeight: Float
        get() = pageData.navigationBarHeight

    /** 安全区域是指不被系统界面（如状态栏、导航栏、工具栏或底部 Home 指示器、刘海屏底部边距）遮挡的视图区域 */
    val safeAreaInsets: EdgeInsets
        get() = pageData.safeAreaInsets

    // 设备的操作系统版本
    val osVersion: String
        get() = pageData.osVersion

    // 设备的平台（iOS 或 Android）
    val platform: String
        get() = pageData.platform

    // 应用程序的版本
    val appVersion: String
        get() = pageData.appVersion

    // native render 版本号
    val nativeBuild: Int
        get() = pageData.nativeBuild

    // 是否为debug包
    val isDebug: Boolean
       get() = pageData.isDebug()

    private var _fontSizeScale = mutableStateOf(1f)
    private var _fontWeightScale = mutableStateOf(1f)
    private var _imeBottomDp = mutableStateOf(0f)
    private var _imeAnimationDuration = mutableStateOf(DEFAULT_IME_ANIMATION_DURATION_MILLIS.toFloat())
    private var _imeAnimationCurve = mutableStateOf(DEFAULT_IME_ANIMATION_CURVE)

    // 当前活动（Activity/Pager）的宽度
    val fontSizeScale: Float by _fontSizeScale
    // 当前活动（Activity/Pager）的宽度
    val fontWeightScale: Float by _fontWeightScale
    // 当前页面的软件键盘占位高度（dp）
    val imeBottomDp: Float by _imeBottomDp
    // 当前页面的软件键盘动画时长（毫秒，phase1 仅内部预留）
    val imeAnimationDuration: Float by _imeAnimationDuration
    // 当前页面的软件键盘动画曲线（内部标准化值，phase1 仅内部预留）
    val imeAnimationCurve: Int by _imeAnimationCurve

    fun onRootViewSizeChanged(width: Double, height: Double) {
        _pageViewWidth.value = width.toFloat()
        _pageViewHeight.value = height.toFloat()
        _activityWidth.value = width.toFloat()
        _activityHeight.value = height.toFloat()
    }

    fun onWindowSizeChanged(width: Double, height: Double) {
        _activityWidth.value = width.toFloat()
        _activityHeight.value = height.toFloat()
    }

    // 页面级 IME 状态统一落在 Configuration，供 WindowInsets.ime 等 API 复用。
    fun onImeInsetsChanged(
        height: Double,
        duration: Double,
        curve: Int
    ) {
        val oldHeight = _imeBottomDp.value
        val oldDuration = _imeAnimationDuration.value
        val oldCurve = _imeAnimationCurve.value
        _imeBottomDp.value = height.toFloat().coerceAtLeast(0f)
        _imeAnimationDuration.value = normalizeImeAnimationDuration(duration).toFloat()
        _imeAnimationCurve.value = normalizeImeAnimationCurve(curve)
        KLog.i(
            "Kuikly.ComposeIME",
            "[IME_EVENT][Configuration] pageName=${pager.pageName}, platform=${pageData.platform}, " +
                "heightDp=$oldHeight->${_imeBottomDp.value}, durationMs=$oldDuration->${_imeAnimationDuration.value}, " +
                "curve=$oldCurve->${_imeAnimationCurve.value}"
        )
    }

    fun onFontConfigChange(
        fontSizeScale: Double, fontWeightScale: Double
    ) {
        _fontSizeScale.value = fontSizeScale.toFloat()
        _fontWeightScale.value = fontWeightScale.toFloat()
    }

    private fun normalizeImeAnimationDuration(duration: Double): Int {
        if (!duration.isFinite() || duration <= 0.0) {
            return DEFAULT_IME_ANIMATION_DURATION_MILLIS
        }
        val durationValue = duration.toFloat()
        val durationMillis = if (durationValue >= 10f) {
            durationValue
        } else {
            durationValue * 1000f
        }
        return durationMillis.toInt().coerceIn(MIN_IME_ANIMATION_DURATION_MILLIS, MAX_IME_ANIMATION_DURATION_MILLIS)
    }

    private fun normalizeImeAnimationCurve(curve: Int): Int {
        return when (curve) {
            IME_ANIMATION_CURVE_EASE_IN_OUT,
            IME_ANIMATION_CURVE_EASE_IN,
            IME_ANIMATION_CURVE_EASE_OUT,
            IME_ANIMATION_CURVE_LINEAR -> curve
            else -> DEFAULT_IME_ANIMATION_CURVE
        }
    }

    companion object {
        private const val DEFAULT_IME_ANIMATION_DURATION_MILLIS = 250
        private const val MIN_IME_ANIMATION_DURATION_MILLIS = 80
        private const val MAX_IME_ANIMATION_DURATION_MILLIS = 1000
        private const val IME_ANIMATION_CURVE_EASE_IN_OUT = 0
        private const val IME_ANIMATION_CURVE_EASE_IN = 1
        private const val IME_ANIMATION_CURVE_EASE_OUT = 2
        private const val IME_ANIMATION_CURVE_LINEAR = 3
        private const val DEFAULT_IME_ANIMATION_CURVE = IME_ANIMATION_CURVE_EASE_IN_OUT
    }
}

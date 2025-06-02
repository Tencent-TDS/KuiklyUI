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
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.pager.PageData
/**
 * 页面配置信息，如屏幕信息，页面宽度，设备，版本, 平台等信息
 */
class Configuration {
    // 当前页面的数据
    val pageData: PageData
        get() {
            return PagerManager.getCurrentPager().pageData
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

    fun onRootViewSizeChanged(width: Double, height: Double) {
        _pageViewWidth.value = width.toFloat()
        _pageViewHeight.value = height.toFloat()
        _activityWidth.value = width.toFloat()
        _activityHeight.value = height.toFloat()
    }
}
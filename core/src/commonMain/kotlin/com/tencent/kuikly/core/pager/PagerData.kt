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

package com.tencent.kuikly.core.pager

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.EdgeInsets
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.urlParams

/*
 * 页面数据（包含设备信息和平台以及根视图宽高，以及页面传参params）
 */
class PageData() {
    private lateinit var rawPageData : JSONObject
    // 页面参数（页面携带参数&扩展参数都在该数据里，可打印查看所有数据）
    val params: JSONObject
        get() {
            return rawPageData.optJSONObject(KEY_PARAM) ?: JSONObject()
        }
    var pageViewWidth: Float by observable(0.0f)
    var pageViewHeight: Float by observable(0.0f)
    var statusBarHeight: Float = 0.0f
        private set
    var deviceHeight: Float by observable(0.0f)
        private set
    var deviceWidth: Float by observable(0.0f)
        private set
    var appVersion: String = ""
        private set
    var platform: String = ""
        private set
    var isIOS: Boolean = false
        private set
    var isAndroid: Boolean = false
        private set
    var isOhOs: Boolean = false
        private set
    var isIphoneX: Boolean = false
        private set
    var navigationBarHeight: Float = 0.0f
        private set
    var nativeBuild: Int = 0
        private set
    var activityWidth: Float by observable(0.0f)
        internal set
    var activityHeight: Float by observable(0.0f)
        internal set
    /** 安全区域是指不被系统界面（如状态栏、导航栏、工具栏或底部 Home 指示器、刘海屏底部边距）遮挡的视图区域 */
    var safeAreaInsets: EdgeInsets by observable(EdgeInsets.default)

    var density: Float = 3f
        internal set
	
    var osVersion: String = ""
        private set
    /* 是否处于无障碍化模式 */
    var isAccessibilityRunning: Boolean = false
        private set
    /**
     * Android 底部导航栏高度
     */
    var androidBottomBavBarHeight: Float = 0f
        private set

    fun init(pageData: JSONObject) {
        this.rawPageData = pageData
        pageViewWidth = pageData.optDouble(ROOT_VIEW_WIDTH).toFloat()
        pageViewHeight = pageData.optDouble(ROOT_VIEW_HEIGHT).toFloat()
        statusBarHeight = pageData.optDouble(STATUS_BAR_HEIGHT).toFloat()
        deviceHeight = pageData.optDouble(DEVICE_HEIGHT).toFloat()
        deviceWidth = pageData.optDouble(DEVICE_WIDTH).toFloat()
        appVersion = pageData.optString(APP_VERSION, "")
        platform =  pageData.optString(PLATFORM, "")
        nativeBuild = pageData.optInt(NATIVE_BUILD, 0)
        isIOS = platform == PLATFORM_IOS
        isAndroid = platform == PLATFORM_ANDROID
        isOhOs = platform == PLATFORM_OHOS
        navigationBarHeight = statusBarHeight + 44
        isIphoneX = isIOS && statusBarHeight > 30
        activityWidth = pageData.optDouble(ACTIVITY_WIDTH, if (isMoreThan8()) deviceWidth.toDouble() else 0.0).toFloat()
        activityHeight = pageData.optDouble(ACTIVITY_HEIGHT, if (isMoreThan8()) deviceHeight.toDouble() else 0.0).toFloat()

        val safeAreaInsetsString = pageData.optString(SAFE_AREA_INSETS, "")
        if (safeAreaInsetsString.isNotEmpty()) {
            safeAreaInsets = EdgeInsets.decodeWithString(safeAreaInsetsString)
        }
        osVersion = pageData.optString(OS_VERSION)
        androidBottomBavBarHeight = pageData.optDouble(ANDROID_BOTTOM_NAV_BAR_HEIGHT, 0.0).toFloat()
        density = pageData.optDouble(DENSITY, 3.0).toFloat() // use 3(xxhdpi) for backwards compatibility
        mergeUrlParamsToPageParams()
    }

    private fun mergeUrlParamsToPageParams() {
        if (rawPageData.optJSONObject(KEY_PARAM) == null) {
            rawPageData.put(KEY_PARAM, JSONObject())
        }
        rawPageData.optJSONObject(KEY_PARAM)?.also { params ->
            urlParams(rawPageData.optString(URL) ?: "").forEach { (k, v) ->
                params.put(k, v)
            }
        }
    }

    fun isDebug(): Boolean = params.optBoolean("isDebug", false)

    internal fun updateRootViewSize(data: JSONObject, width: Double, height: Double) {
         this.pageViewWidth = width.toFloat()
         this.pageViewHeight = height.toFloat()
         if (data.has(DEVICE_WIDTH) && data.has(DEVICE_HEIGHT)) { // 更新设备宽度/高度
             val dWidth = data.optDouble(DEVICE_WIDTH).toFloat()
             val dHeight = data.optDouble(DEVICE_HEIGHT).toFloat()
             if (dWidth != this.deviceWidth || dHeight != this.deviceHeight) {
                 this.deviceWidth = dWidth
                 this.deviceHeight = dHeight
             }
         }
        // 更新activityWidth/Height
        if (data.has(ACTIVITY_WIDTH) && data.has(ACTIVITY_HEIGHT)) { // 更新设备宽度/高度
            val aWidth = data.optDouble(ACTIVITY_WIDTH).toFloat()
            val aHeight = data.optDouble(ACTIVITY_HEIGHT).toFloat()
            if (aWidth != this.activityWidth || aHeight != this.activityHeight) {
                this.activityWidth = aWidth
                this.activityHeight = aHeight
            }
        } else { // activityWidth/Height兜底为deviceWidth/Height
            if (isMoreThan8()) {
                if (this.activityWidth != this.deviceWidth) {
                    this.activityWidth = this.deviceWidth
                }
                if (this.activityHeight != this.deviceHeight) {
                    this.activityHeight = this.deviceHeight
                }
            }
        }
    }

    private fun isMoreThan8(): Boolean {
        return nativeBuild >= 8
    }

    companion object {
        private const val KEY_PARAM = "param"
        private const val URL = "url"
        private const val ROOT_VIEW_WIDTH = "rootViewWidth"
        private const val ROOT_VIEW_HEIGHT = "rootViewHeight"
        private const val STATUS_BAR_HEIGHT = "statusBarHeight"
        private const val DEVICE_WIDTH = "deviceWidth"
        private const val DEVICE_HEIGHT = "deviceHeight"
        private const val APP_VERSION = "appVersion"
        const val PLATFORM = "platform"
        const val NATIVE_BUILD = "nativeBuild"
        private const val SAFE_AREA_INSETS = "safeAreaInsets"
        private const val ACTIVITY_WIDTH = "activityWidth"
        private const val ACTIVITY_HEIGHT = "activityHeight"
        private const val ACCESSIBILITY_RUNNING = "isAccessibilityRunning"
        private const val OS_VERSION = "osVersion"
        private const val ANDROID_BOTTOM_NAV_BAR_HEIGHT = "androidBottomNavBarHeight"
        private const val DENSITY = "density"
        const val PLATFORM_ANDROID = "android"
        const val PLATFORM_IOS = "iOS"
        const val PLATFORM_OHOS = "ohos"
    }
}
/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.core.module.wx

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 微信小程序 系统 / 设备 / 启动信息 类 API 封装。
 *
 * 对应微信 API:
 * - wx.getWindowInfo()        同步
 * - wx.getDeviceInfo()        同步
 * - wx.getAppBaseInfo()       同步
 * - wx.getSystemInfoSync()    同步（老 API，一次返回以上信息的合集）
 * - wx.getLaunchOptionsSync() 同步（小程序启动参数）
 * - wx.getSystemInfo()        异步
 */
class WXSystemModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /** wx.getWindowInfo —— 窗口信息（safeArea / statusBarHeight 等），小程序平台返回 JSON，否则 null */
    fun getWindowInfoSync(): JSONObject? = syncJson(METHOD_GET_WINDOW_INFO)

    /** wx.getDeviceInfo —— 设备信息（brand / model / system 等） */
    fun getDeviceInfoSync(): JSONObject? = syncJson(METHOD_GET_DEVICE_INFO)

    /** wx.getAppBaseInfo —— 宿主 App 基础信息（SDKVersion / language / theme 等） */
    fun getAppBaseInfoSync(): JSONObject? = syncJson(METHOD_GET_APP_BASE_INFO)

    /** wx.getSystemInfoSync —— 老 API，合集信息 */
    fun getSystemInfoSync(): JSONObject? = syncJson(METHOD_GET_SYSTEM_INFO_SYNC)

    /** wx.getLaunchOptionsSync —— 小程序启动参数（path / query / scene / referrerInfo 等） */
    fun getLaunchOptionsSync(): JSONObject? = syncJson(METHOD_GET_LAUNCH_OPTIONS_SYNC)

    /** wx.getEnterOptionsSync —— 小程序进入信息（每次回前台刷新） */
    fun getEnterOptionsSync(): JSONObject? = syncJson(METHOD_GET_ENTER_OPTIONS_SYNC)

    /** wx.getSystemInfo —— 异步版本 */
    fun getSystemInfo(
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, JSONObject())
        }
        asyncToNativeMethod(METHOD_GET_SYSTEM_INFO, wrapped) { data ->
            val ok = data?.optInt(WXApiModule.KEY_OK, 0) == 1
            val payload = data?.optJSONObject(WXApiModule.KEY_DATA)
            if (ok) onSuccess?.invoke(payload) else onFail?.invoke(payload)
        }
    }

    private fun syncJson(method: String): JSONObject? {
        val raw = syncToNativeMethod(method, JSONObject(), null)
        return if (raw.isNullOrEmpty()) null else JSONObject(raw)
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.SYSTEM

        private const val METHOD_GET_WINDOW_INFO = "getWindowInfo"
        private const val METHOD_GET_DEVICE_INFO = "getDeviceInfo"
        private const val METHOD_GET_APP_BASE_INFO = "getAppBaseInfo"
        private const val METHOD_GET_SYSTEM_INFO = "getSystemInfo"
        private const val METHOD_GET_SYSTEM_INFO_SYNC = "getSystemInfoSync"
        private const val METHOD_GET_LAUNCH_OPTIONS_SYNC = "getLaunchOptionsSync"
        private const val METHOD_GET_ENTER_OPTIONS_SYNC = "getEnterOptionsSync"
    }
}

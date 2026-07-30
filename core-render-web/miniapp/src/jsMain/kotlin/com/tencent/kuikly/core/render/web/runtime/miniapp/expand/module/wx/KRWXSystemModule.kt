/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand.module.wx

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi

/**
 * 系统 / 设备 / 启动信息 类 API 渲染层实现。
 */
class KRWXSystemModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val paramsJson = params.toJSONObjectSafely()
        val args = paramsJson.optJSONObject("args")
        return when (method) {
            METHOD_GET_WINDOW_INFO        -> syncCall { NativeApi.plat?.getWindowInfo() }
            METHOD_GET_DEVICE_INFO        -> syncCall { NativeApi.plat?.getDeviceInfo() }
            METHOD_GET_APP_BASE_INFO      -> syncCall { NativeApi.plat?.getAppBaseInfo() }
            METHOD_GET_SYSTEM_INFO_SYNC   -> syncCall { NativeApi.plat?.getSystemInfoSync() }
            METHOD_GET_LAUNCH_OPTIONS_SYNC -> syncCall { NativeApi.plat?.getLaunchOptionsSync() }
            METHOD_GET_ENTER_OPTIONS_SYNC  -> syncCall { NativeApi.plat?.getEnterOptionsSync() }

            METHOD_GET_SYSTEM_INFO -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }

            else -> super.call(method, params, callback)
        }
    }

    private inline fun syncCall(block: () -> dynamic): String? {
        val ret = block()
        return if (ret == null || ret == undefined) null
        else KRWXApiBridge.stringifyJsObject(ret)
    }

    companion object {
        const val MODULE_NAME = "KRWXSystemModule"

        private const val METHOD_GET_WINDOW_INFO = "getWindowInfo"
        private const val METHOD_GET_DEVICE_INFO = "getDeviceInfo"
        private const val METHOD_GET_APP_BASE_INFO = "getAppBaseInfo"
        private const val METHOD_GET_SYSTEM_INFO = "getSystemInfo"
        private const val METHOD_GET_SYSTEM_INFO_SYNC = "getSystemInfoSync"
        private const val METHOD_GET_LAUNCH_OPTIONS_SYNC = "getLaunchOptionsSync"
        private const val METHOD_GET_ENTER_OPTIONS_SYNC = "getEnterOptionsSync"
    }
}

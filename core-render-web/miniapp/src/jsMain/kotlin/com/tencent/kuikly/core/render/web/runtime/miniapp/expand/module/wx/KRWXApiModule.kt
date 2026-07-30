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
 * 账号 / 登录 / 用户信息 类 API 渲染层实现（小程序专用）。
 *
 * - login / checkSession / getUserProfile / getUserInfo  走异步；
 * - getAccountInfoSync                                    走同步，直接返回 wx 结果 JSON 字符串。
 */
class KRWXApiModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val paramsJson = params.toJSONObjectSafely()
        val args = paramsJson.optJSONObject("args")
        return when (method) {
            METHOD_LOGIN,
            METHOD_CHECK_SESSION,
            METHOD_GET_USER_PROFILE,
            METHOD_GET_USER_INFO -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }

            METHOD_GET_ACCOUNT_INFO_SYNC -> {
                // wx.getAccountInfoSync 是零参同步方法
                val plat = NativeApi.plat
                if (plat == null || plat == undefined) {
                    null
                } else {
                    KRWXApiBridge.stringifyJsObject(plat["getAccountInfoSync"]())
                }
            }

            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXApiModule"

        private const val METHOD_LOGIN = "login"
        private const val METHOD_CHECK_SESSION = "checkSession"
        private const val METHOD_GET_USER_PROFILE = "getUserProfile"
        private const val METHOD_GET_USER_INFO = "getUserInfo"
        private const val METHOD_GET_ACCOUNT_INFO_SYNC = "getAccountInfoSync"
    }
}

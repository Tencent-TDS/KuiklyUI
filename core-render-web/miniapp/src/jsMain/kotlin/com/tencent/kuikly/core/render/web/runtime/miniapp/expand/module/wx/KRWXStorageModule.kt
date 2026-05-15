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
 * 微信小程序 Storage 相关 API 渲染层实现。
 */
class KRWXStorageModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val paramsJson = params.toJSONObjectSafely()
        val args = paramsJson.optJSONObject("args")
        return when (method) {
            METHOD_SET_STORAGE,
            METHOD_GET_STORAGE,
            METHOD_REMOVE_STORAGE,
            METHOD_CLEAR_STORAGE -> {
                KRWXApiBridge.invokeAsync(method, args, callback)
                null
            }

            METHOD_SET_STORAGE_SYNC -> {
                val plat = NativeApi.plat
                val key = args?.optString("key") ?: ""
                // 业务侧以 put("data", value) 的形式塞进来，这里根据运行时类型分发
                val rawData: Any? = args?.opt("data")
                val data: dynamic = when (rawData) {
                    null -> null
                    is String -> rawData
                    is Int, is Long, is Double, is Float, is Boolean -> rawData
                    else -> {
                        // JSONObject / JSONArray 等复杂结构：先序列化再用 JS 原生 JSON.parse 还原成 plain object
                        val text = rawData.toString()
                        js("JSON.parse(text)")
                    }
                }
                plat?.setStorageSync(key, data)
                null
            }

            METHOD_GET_STORAGE_SYNC -> {
                val plat = NativeApi.plat ?: return null
                val key = args?.optString("key") ?: ""
                val ret: dynamic = plat.getStorageSync(key)
                // 简单场景直接返回字符串；对象类型返回其 JSON 序列化
                return when (jsTypeOf(ret)) {
                    "undefined" -> ""
                    "string" -> ret.unsafeCast<String>()
                    "number", "boolean" -> ret.toString()
                    "object" -> {
                        if (ret == null) "" else js("JSON.stringify(ret)").unsafeCast<String>()
                    }
                    else -> ""
                }
            }

            METHOD_REMOVE_STORAGE_SYNC -> {
                NativeApi.plat?.removeStorageSync(args?.optString("key") ?: "")
                null
            }

            METHOD_CLEAR_STORAGE_SYNC -> {
                NativeApi.plat?.clearStorageSync()
                null
            }

            METHOD_GET_STORAGE_INFO_SYNC -> {
                val plat = NativeApi.plat ?: return null
                KRWXApiBridge.stringifyJsObject(plat.getStorageInfoSync())
            }

            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXStorageModule"

        private const val METHOD_SET_STORAGE = "setStorage"
        private const val METHOD_SET_STORAGE_SYNC = "setStorageSync"
        private const val METHOD_GET_STORAGE = "getStorage"
        private const val METHOD_GET_STORAGE_SYNC = "getStorageSync"
        private const val METHOD_REMOVE_STORAGE = "removeStorage"
        private const val METHOD_REMOVE_STORAGE_SYNC = "removeStorageSync"
        private const val METHOD_CLEAR_STORAGE = "clearStorage"
        private const val METHOD_CLEAR_STORAGE_SYNC = "clearStorageSync"
        private const val METHOD_GET_STORAGE_INFO_SYNC = "getStorageInfoSync"
    }
}

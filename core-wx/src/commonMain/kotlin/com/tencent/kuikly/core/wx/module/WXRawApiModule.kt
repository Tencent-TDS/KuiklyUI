/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.core.wx.module

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 微信小程序 API 兜底通用桥。
 *
 * **用途**：当 Kuikly 尚未封装某个 `wx.xxx` API、或业务需要快速接入一个不常用 API 时，
 * 可以不新建 Module，直接通过本 Module 的 [call] / [callSync] 调用任意 `wx.xxx`。
 *
 * **工作原理**：渲染层 `KRWXRawApiModule` 将 params 透传给 `wx[apiName](params)`，
 * 并在 success/fail 回调中回包给业务。
 *
 * **使用示例**：
 * ```kotlin
 * // 异步调用任意 wx.xxx：
 * acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME).call(
 *     apiName = "vibrateShort",
 *     args = JSONObject().apply { put("type", "heavy") },
 *     onSuccess = { res -> KLog.i(TAG, "vibrate ok") },
 *     onFail = { err -> KLog.e(TAG, "vibrate fail: $err") }
 * )
 *
 * // 同步调用任意 wx.xxxSync：
 * val info = acquireModule<WXRawApiModule>(WXRawApiModule.MODULE_NAME)
 *     .callSync("getStorageSync", JSONObject().apply { put("args", arrayOf("key1")) })
 * ```
 *
 * **约束**：
 * 1. 仅支持小程序平台，非小程序平台调用 [call] 会触发 fail 回调（`errMsg = "not supported"`）；
 * 2. `args` 里不要传 success/fail/complete 字段，回调由 Kuikly 统一注入；
 * 3. 参数只支持 JSON 可序列化类型（string / number / bool / array / JSONObject）；
 * 4. 不做编译期类型校验，请自查微信官方文档。
 */
class WXRawApiModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * 调用任意 wx.xxx 异步 API。
     * @param apiName  wx 上的方法名，如 "scanCode"、"getLocation"、"vibrateShort"
     * @param args     调用参数（**不要**包含 success / fail / complete 字段）
     * @param onSuccess success 回调，data 为微信 API 返回的 `res`
     * @param onFail    fail 回调，data 为微信 API 返回的 `err`
     */
    fun call(
        apiName: String,
        args: JSONObject? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val wrapped = JSONObject().apply {
            put(KEY_API_NAME, apiName)
            put(WXApiModule.KEY_ARGS, args ?: JSONObject())
        }
        asyncToNativeMethod(METHOD_CALL, wrapped) { data ->
            val ok = data?.optInt(WXApiModule.KEY_OK, 0) == 1
            val payload = data?.optJSONObject(WXApiModule.KEY_DATA)
            if (ok) onSuccess?.invoke(payload) else onFail?.invoke(payload)
        }
    }

    /**
     * 调用任意 wx.xxxSync 同步 API。
     * @param apiName 微信同步方法名，如 "getStorageSync"、"getSystemInfoSync"
     * @param args 参数，常见是 `{ args: [key] }` 这样的位置参数数组；
     *             也可以直接传 wx 对应同步 API 所需的对象参数。
     * @return JSONObject（若 wx 返回对象）；其它平台或调用失败返回 null
     */
    fun callSync(apiName: String, args: JSONObject? = null): JSONObject? {
        val wrapped = JSONObject().apply {
            put(KEY_API_NAME, apiName)
            put(WXApiModule.KEY_ARGS, args ?: JSONObject())
        }
        val raw = syncToNativeMethod(METHOD_CALL_SYNC, wrapped, null)
        return if (raw.isNullOrEmpty()) null else JSONObject(raw)
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.RAW

        private const val METHOD_CALL = "call"
        private const val METHOD_CALL_SYNC = "callSync"

        internal const val KEY_API_NAME = "apiName"
    }
}

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

import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * 共享工具：将 `wx[apiName](rawArgs)` 调用统一包装为 Kuikly 的 success/fail 回调。
 *
 * 约定回包结构（与业务层 WX*Module 对齐）：
 * - 成功：{ ok: 1, data: <微信 API 回调的 res 对象> }
 * - 失败：{ ok: 0, data: <微信 API 回调的 err 对象> }
 *
 * 非小程序平台（`NativeApi.plat === undefined`）也会通过 fail 通道回包。
 */
internal object KRWXApiBridge {

    private const val TAG = "KRWXApi"

    /**
     * 异步调用 wx[apiName](args)。
     *
     * @param apiName  wx 对象上的方法名
     * @param rawArgs  业务侧 JSONObject 参数（即 WX*Module.invoke 时放进 `args` 字段的对象）
     * @param callback Kuikly 渲染回调（success/fail 统一回）
     */
    fun invokeAsync(apiName: String, rawArgs: JSONObject?, callback: KuiklyRenderCallback?) {
        val cb = callback
        val plat = NativeApi.plat
        if (plat == null || plat == undefined) {
            Log.error("$TAG invokeAsync called on non-miniapp platform, apiName=$apiName")
            cb?.invoke(buildResult(false, mapOf("errMsg" to "not supported: not in mini-app")))
            return
        }
        val method = plat[apiName]
        if (method == null || jsTypeOf(method) != "function") {
            Log.error("$TAG wx.$apiName is not a function")
            cb?.invoke(buildResult(false, mapOf("errMsg" to "wx.$apiName is not a function")))
            return
        }

        // 把 rawArgs 转成纯 JS 对象，避免 JSONObject 被直接传递
        val jsArgs: dynamic = if (rawArgs == null) js("{}") else js("JSON.parse(rawArgs.toString())")
        jsArgs.success = { res: dynamic ->
            val json = stringifyJsObject(res)
            cb?.invoke(buildResultJson(true, json))
        }
        jsArgs.fail = { err: dynamic ->
            val json = stringifyJsObject(err)
            cb?.invoke(buildResultJson(false, json))
        }
        try {
            method(jsArgs)
        } catch (e: dynamic) {
            Log.error("$TAG invokeAsync exception on wx.$apiName: $e")
            cb?.invoke(buildResult(false, mapOf("errMsg" to "exception: $e")))
        }
    }

    /**
     * 同步调用 wx[apiName](args)。直接返回 wx API 的 JSON 字符串结果；非小程序 / 调用异常返回 null。
     */
    fun invokeSync(apiName: String, rawArgs: JSONObject?): String? {
        val plat = NativeApi.plat
        if (plat == null || plat == undefined) {
            Log.error("$TAG invokeSync called on non-miniapp platform, apiName=$apiName")
            return null
        }
        val method = plat[apiName]
        if (method == null || jsTypeOf(method) != "function") {
            Log.error("$TAG wx.$apiName is not a function")
            return null
        }
        val jsArgs: dynamic = if (rawArgs == null) js("{}") else js("JSON.parse(rawArgs.toString())")
        return try {
            val ret: dynamic = method(jsArgs)
            stringifyJsObject(ret)
        } catch (e: dynamic) {
            Log.error("$TAG invokeSync exception on wx.$apiName: $e")
            null
        }
    }

    /** 将任意 JS 对象安全序列化为 JSON 字符串（primitive 直接包一层 { value: ... }） */
    fun stringifyJsObject(any: dynamic): String {
        if (any == null || any == undefined) return "{}"
        val t = jsTypeOf(any)
        return when (t) {
            "object" -> js("JSON.stringify(any)").unsafeCast<String>()
            "string" -> js("JSON.stringify({ value: any })").unsafeCast<String>()
            "number", "boolean" -> js("JSON.stringify({ value: any })").unsafeCast<String>()
            else -> "{}"
        }
    }

    /** 构造 { ok: 1, data: {...} } 的回包 JSON 字符串 */
    private fun buildResult(ok: Boolean, data: Map<String, Any>): String {
        val dataStr = mapToJsonString(data)
        val okInt = if (ok) 1 else 0
        return """{"ok":$okInt,"data":$dataStr}"""
    }

    private fun buildResultJson(ok: Boolean, dataJson: String): String {
        val okInt = if (ok) 1 else 0
        return """{"ok":$okInt,"data":$dataJson}"""
    }

    private fun mapToJsonString(data: Map<String, Any>): String {
        val obj: dynamic = js("{}")
        data.forEach { (k, v) -> obj[k] = v }
        return js("JSON.stringify(obj)").unsafeCast<String>()
    }
}

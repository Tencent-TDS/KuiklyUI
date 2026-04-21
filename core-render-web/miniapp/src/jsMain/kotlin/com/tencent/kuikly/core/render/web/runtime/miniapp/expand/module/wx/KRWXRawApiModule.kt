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

/**
 * 微信小程序 API 兜底通用桥渲染层实现：
 * - 异步：`call`      ——  params 中带 `apiName` + `args`，透传给 `wx[apiName](args)`
 * - 同步：`callSync`  ——  同上，直接返回 `wx[apiName](args)` 的 JSON 字符串
 */
class KRWXRawApiModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        val paramsJson = params.toJSONObjectSafely()
        val apiName = paramsJson.optString("apiName")
        val args = paramsJson.optJSONObject("args")
        return when (method) {
            METHOD_CALL -> {
                KRWXApiBridge.invokeAsync(apiName, args, callback)
                null
            }
            METHOD_CALL_SYNC -> {
                KRWXApiBridge.invokeSync(apiName, args)
            }
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val MODULE_NAME = "KRWXRawApiModule"
        private const val METHOD_CALL = "call"
        private const val METHOD_CALL_SYNC = "callSync"
    }
}

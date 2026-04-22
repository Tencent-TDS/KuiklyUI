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
 * 微信小程序 剪贴板 API 封装。
 *
 * 对应微信 API:
 * - wx.setClipboardData
 * - wx.getClipboardData
 */
class WXClipboardModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /** wx.setClipboardData */
    fun setClipboardData(
        data: String,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        val params = JSONObject().apply { put("data", data) }
        invoke(METHOD_SET_CLIPBOARD_DATA, params, onSuccess, onFail)
    }

    /**
     * wx.getClipboardData
     * 回调 data 字段：data: String
     */
    fun getClipboardData(
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null
    ) {
        invoke(METHOD_GET_CLIPBOARD_DATA, null, onSuccess, onFail)
    }

    private fun invoke(
        method: String,
        params: JSONObject?,
        onSuccess: CallbackFn?,
        onFail: CallbackFn?
    ) {
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, params ?: JSONObject())
        }
        asyncToNativeMethod(method, wrapped) { data ->
            val ok = data?.optInt(WXApiModule.KEY_OK, 0) == 1
            val payload = data?.optJSONObject(WXApiModule.KEY_DATA)
            if (ok) onSuccess?.invoke(payload) else onFail?.invoke(payload)
        }
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.CLIPBOARD
        private const val METHOD_SET_CLIPBOARD_DATA = "setClipboardData"
        private const val METHOD_GET_CLIPBOARD_DATA = "getClipboardData"
    }
}

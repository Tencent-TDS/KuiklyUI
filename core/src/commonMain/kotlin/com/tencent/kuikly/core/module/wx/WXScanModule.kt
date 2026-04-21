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
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * 微信小程序 扫码 API 封装。
 *
 * 对应微信 API:
 * - wx.scanCode
 */
class WXScanModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.scanCode
     * @param onlyFromCamera 仅允许摄像头扫码
     * @param scanType 识别码类型：["barCode", "qrCode", "datamatrix", "pdf417"]
     */
    fun scanCode(
        onlyFromCamera: Boolean = false,
        scanType: List<String>? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("onlyFromCamera", onlyFromCamera)
            scanType?.also { list ->
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                put("scanType", arr)
            }
        }
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, params)
        }
        asyncToNativeMethod(METHOD_SCAN_CODE, wrapped) { data ->
            val ok = data?.optInt(WXApiModule.KEY_OK, 0) == 1
            val payload = data?.optJSONObject(WXApiModule.KEY_DATA)
            if (ok) onSuccess?.invoke(payload) else onFail?.invoke(payload)
        }
    }

    companion object {
        const val MODULE_NAME = WXModuleConst.SCAN
        private const val METHOD_SCAN_CODE = "scanCode"
    }
}

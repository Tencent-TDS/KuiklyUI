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
 * 微信小程序 定位 / 地图 API 封装。
 *
 * 对应微信 API:
 * - wx.getLocation
 * - wx.chooseLocation（需要 scope.userLocation 授权）
 * - wx.openLocation（使用地图查看位置）
 */
class WXLocationModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /**
     * wx.getLocation
     * @param type "wgs84" | "gcj02"
     * @param altitude 是否返回海拔
     * @param isHighAccuracy 是否高精度
     */
    fun getLocation(
        type: String = "wgs84",
        altitude: Boolean = false,
        isHighAccuracy: Boolean = false,
        highAccuracyExpireTime: Int? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("type", type)
            put("altitude", altitude)
            put("isHighAccuracy", isHighAccuracy)
            highAccuracyExpireTime?.also { put("highAccuracyExpireTime", it) }
        }
        invoke(METHOD_GET_LOCATION, params, onSuccess, onFail)
    }

    /** wx.chooseLocation —— 打开地图选点 */
    fun chooseLocation(
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            latitude?.also { put("latitude", it) }
            longitude?.also { put("longitude", it) }
        }
        invoke(METHOD_CHOOSE_LOCATION, params, onSuccess, onFail)
    }

    /** wx.openLocation —— 地图查看位置 */
    fun openLocation(
        latitude: Double,
        longitude: Double,
        scale: Int = 18,
        name: String? = null,
        address: String? = null,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("scale", scale)
            name?.also { put("name", it) }
            address?.also { put("address", it) }
        }
        invoke(METHOD_OPEN_LOCATION, params, onSuccess, onFail)
    }

    private fun invoke(
        method: String,
        params: JSONObject?,
        onSuccess: CallbackFn?,
        onFail: CallbackFn?,
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
        const val MODULE_NAME = WXModuleConst.LOCATION
        private const val METHOD_GET_LOCATION = "getLocation"
        private const val METHOD_CHOOSE_LOCATION = "chooseLocation"
        private const val METHOD_OPEN_LOCATION = "openLocation"
    }
}

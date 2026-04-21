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
 * 微信小程序数据缓存 API 的封装。
 *
 * 对应微信 API:
 * - wx.setStorage / wx.setStorageSync
 * - wx.getStorage / wx.getStorageSync
 * - wx.removeStorage / wx.removeStorageSync
 * - wx.clearStorage / wx.clearStorageSync
 * - wx.getStorageInfoSync
 *
 * 仅在微信小程序平台生效。
 *
 * 与 Kuikly `SharedPreferencesModule` 的差异：
 * - `SharedPreferencesModule` 底层在小程序上走的是 `localStorage`（由 Kuikly 运行时注入，
 *   实际也是 wx.setStorage，但只支持字符串 KV）；
 * - `WXStorageModule` 直接透传 wx API，支持任意 JSON 值、支持失败回调、支持一次性清空。
 */
class WXStorageModule : Module() {

    override fun moduleName(): String = MODULE_NAME

    /** wx.setStorage —— 异步写入任意 JSON 值 */
    fun setStorage(
        key: String,
        data: Any?,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply {
            put("key", key)
            data?.also { put("data", it) }
        }
        invoke(METHOD_SET_STORAGE, params, onSuccess, onFail)
    }

    /** wx.setStorageSync —— 同步写入 */
    fun setStorageSync(key: String, data: Any?) {
        val params = JSONObject().apply {
            put("key", key)
            data?.also { put("data", it) }
        }
        val wrapped = JSONObject().put(WXApiModule.KEY_ARGS, params)
        syncToNativeMethod(METHOD_SET_STORAGE_SYNC, wrapped, null)
    }

    /** wx.getStorage —— 异步读取 */
    fun getStorage(
        key: String,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply { put("key", key) }
        invoke(METHOD_GET_STORAGE, params, onSuccess, onFail)
    }

    /** wx.getStorageSync —— 同步读取。返回字符串（其它平台/不存在时返回空串） */
    fun getStorageSync(key: String): String {
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, JSONObject().put("key", key))
        }
        return syncToNativeMethod(METHOD_GET_STORAGE_SYNC, wrapped, null) ?: ""
    }

    /** wx.removeStorage */
    fun removeStorage(
        key: String,
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        val params = JSONObject().apply { put("key", key) }
        invoke(METHOD_REMOVE_STORAGE, params, onSuccess, onFail)
    }

    /** wx.removeStorageSync */
    fun removeStorageSync(key: String) {
        val wrapped = JSONObject().apply {
            put(WXApiModule.KEY_ARGS, JSONObject().put("key", key))
        }
        syncToNativeMethod(METHOD_REMOVE_STORAGE_SYNC, wrapped, null)
    }

    /** wx.clearStorage */
    fun clearStorage(
        onSuccess: CallbackFn? = null,
        onFail: CallbackFn? = null,
    ) {
        invoke(METHOD_CLEAR_STORAGE, null, onSuccess, onFail)
    }

    /** wx.clearStorageSync */
    fun clearStorageSync() {
        syncToNativeMethod(METHOD_CLEAR_STORAGE_SYNC, JSONObject(), null)
    }

    /** wx.getStorageInfoSync —— { keys: [...], currentSize, limitSize } */
    fun getStorageInfoSync(): JSONObject? {
        val raw = syncToNativeMethod(METHOD_GET_STORAGE_INFO_SYNC, JSONObject(), null)
        return if (raw.isNullOrEmpty()) null else JSONObject(raw)
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
        const val MODULE_NAME = WXModuleConst.STORAGE

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

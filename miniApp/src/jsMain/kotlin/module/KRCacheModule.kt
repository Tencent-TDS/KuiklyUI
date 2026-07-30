package com.tencent.kuikly.miniapp.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.runtime.miniapp.LocalStorage
import com.tencent.kuikly.core.render.web.utils.Log

class KRCacheModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            GET_ITEM -> getItem(params)
            SET_ITEM -> {
                setItem(params)
                Unit
            }
            else -> {
                Log.error("$method not found")
                callback?.invoke("{}")
                Unit
            }
        }
    }

    private fun getItem(key: String?): String? {
        if (key == null) {
            return null
        }
        return try {
            LocalStorage.getItem(key)
        } catch (e: Exception) {
            ""
        }
    }

    private fun setItem(params: String?) {
        try {
            val json = params.toJSONObjectSafely()
            val key = json.optString("key")
            val value = json.optString("value")
            LocalStorage.setItem(key, value)
        } catch (e: Exception) {
            Log.error("HRCacheModule setItem error")
        }
    }

    companion object {
        const val MODULE_NAME = "HRCacheModule"
        private const val GET_ITEM = "getItem"
        private const val SET_ITEM = "setItem"
    }
}
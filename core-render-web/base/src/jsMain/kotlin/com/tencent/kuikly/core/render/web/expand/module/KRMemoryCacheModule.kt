package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject

/**
 * Kuikly memory cache module
 */
class KRMemoryCacheModule : KuiklyRenderBaseModule() {
    private val cacheMap = mutableMapOf<String, Any>()

    /**
     * Handle method calls
     */
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        when (method) {
            // Method for caching objects in memory cache module
            SET_OBJECT -> {
                val json = JSONObject(params ?: "{}")
                // Get value to cache
                val value = json.opt("value") ?: return null
                val key = json.optString("key")
                // Associate key with value
                set(key, value)
            }
        }
        return null
    }

    /**
     * Get memory cache value by key
     */
    fun <T> get(key: String): T? = cacheMap[key].unsafeCast<T?>()

    /**
     * Associate key with value
     */
    fun set(key: String, value: Any) {
        cacheMap[key] = value
    }

    companion object {
        const val MODULE_NAME = "KRMemoryCacheModule"
        const val SET_OBJECT = "setObject"
    }
}

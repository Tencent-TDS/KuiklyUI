package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Kuikly disk cache module, web uses localStorage for simulation, but localStorage has size limit,
 * could also use indexDB as an alternative
 */
class KRSharedPreferencesModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            GET_ITEM -> this.getItem(params)
            SET_ITEM -> this.setItem(params)
            else -> super.call(method, params, callback)
        }
    }

    /**
     * Check if localStorage is available
     * Some browsers in private mode or with strict security settings may disable localStorage
     */
    private fun isLocalStorageAvailable(): Boolean {
        return try {
            val testKey = "__kuikly_storage_test__"
            kuiklyWindow.localStorage.setItem(testKey, testKey)
            kuiklyWindow.localStorage.removeItem(testKey)
            true
        } catch (e: dynamic) {
            Log.warn("localStorage is not available: ${e.message}")
            false
        }
    }

    /**
     * Get content from localStorage cache
     *
     * @param key Cache key
     */
    private fun getItem(key: String?): String? {
        if (key == null) {
            return null
        }
        return try {
            if (!isLocalStorageAvailable()) {
                return null
            }
            // Get localStorage cache content
            kuiklyWindow.localStorage.getItem(key)
        } catch (e: dynamic) {
            Log.error("Failed to get item from localStorage: ${e.message}")
            null
        }
    }

    /**
     * Set localStorage cache
     * 
     * @return true if successful, false if failed (e.g., storage full or not available)
     */
    private fun setItem(params: String?): Boolean {
        val json = params.toJSONObjectSafely()
        val key = json.optString("key")
        val value = json.optString("value")
        
        if (key.isEmpty()) {
            Log.warn("setItem called with empty key")
            return false
        }
        
        return try {
            if (!isLocalStorageAvailable()) {
                return false
            }
            // Set localStorage cache
            kuiklyWindow.localStorage.setItem(key, value)
            true
        } catch (e: dynamic) {
            // Handle quota exceeded error or other localStorage errors
            val errorMessage = e.message?.toString() ?: "Unknown error"
            when {
                errorMessage.contains("QuotaExceeded", ignoreCase = true) ||
                errorMessage.contains("quota", ignoreCase = true) -> {
                    Log.error("localStorage quota exceeded. Consider clearing old data or using IndexedDB.")
                }
                else -> {
                    Log.error("Failed to set item in localStorage: $errorMessage")
                }
            }
            false
        }
    }

    companion object {
        const val MODULE_NAME = "KRSharedPreferencesModule"
        private const val GET_ITEM = "getItem"
        private const val SET_ITEM = "setItem"
    }
}

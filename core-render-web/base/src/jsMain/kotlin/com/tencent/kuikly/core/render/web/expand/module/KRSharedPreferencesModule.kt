package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely

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
     * Get content from localStorage cache
     *
     * @param key Cache key
     */
    private fun getItem(key: String?): String? {
        if (key == null) {
            return null
        }
        // Get localStorage cache content
        return kuiklyWindow.localStorage.getItem(key)
    }

    /**
     * Set localStorage cache
     */
    private fun setItem(params: String?) {
        val json = params.toJSONObjectSafely()
        val key = json.optString("key")
        val value = json.optString("value")
        // Set localStorage cache
        kuiklyWindow.localStorage.setItem(key, value)
    }

    companion object {
        const val MODULE_NAME = "KRSharedPreferencesModule"
        private const val GET_ITEM = "getItem"
        private const val SET_ITEM = "setItem"
    }
}

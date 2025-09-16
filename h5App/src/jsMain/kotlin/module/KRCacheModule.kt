package module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import kotlinx.browser.window

/**
 * QQ Cache Module
 */
class KRCacheModule : KuiklyRenderBaseModule() {
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
     * @param key
     */
    private fun getItem(key: String?): String? {
        if (key == null) {
            return null
        }
        return try {
            window.localStorage.getItem(key)
        } catch (e: dynamic) {
            // localStorage parsing error
            console.error("localStorage get error", e)
            ""
        }
    }

    /**
     * Set localStorage cache
     */
    private fun setItem(params: String?) {
        val json = params.toJSONObjectSafely()
        val key = json.optString("key")
        val value = json.optString("value")
        try {
            window.localStorage.setItem(key, value)
        } catch (e: dynamic) {
            // localStorage parsing error
            console.error("localStorage set error", e)
        }
    }

    companion object {
        const val MODULE_NAME = "HRCacheModule"
        private const val GET_ITEM = "getItem"
        private const val SET_ITEM = "setItem"
    }
}

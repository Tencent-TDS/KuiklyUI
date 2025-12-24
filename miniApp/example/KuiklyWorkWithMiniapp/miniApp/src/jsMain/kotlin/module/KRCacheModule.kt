package module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.runtime.miniapp.LocalStorage
import com.tencent.kuikly.core.render.web.utils.Log

class KRCacheModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "getItem" -> {
                if (params == null) {
                    return null
                }
                return try {
                    LocalStorage.getItem(params)
                } catch (e: Exception) {
                    ""
                }
            }

            "setItem" -> {
                try {
                    val json = params.toJSONObjectSafely()
                    val key = json.optString("key")
                    val value = json.optString("value")
                    LocalStorage.setItem(key, value)
                } catch (e: Exception) {
                    Log.error("HRCacheModule setItem error")
                }
            }

            else -> {
                Log.error("$method not found")
                callback?.invoke("{}")
            }
        }
    }

    companion object {
        const val MODULE_NAME = "HRCacheModule"
    }
}
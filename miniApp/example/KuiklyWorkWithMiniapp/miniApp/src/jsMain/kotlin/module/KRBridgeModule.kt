package module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONException
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.json

/**
 * Bridge interface module used by business side
 */
class KRBridgeModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "toast" -> {
                showToast(params)
            }

            else -> {
                Log.error("$method not found")
                callback?.invoke("{}")
            }
        }
    }

    private fun showToast(params: String?) {
        if (params == null) {
            return
        }
        try {
            val data = JSONObject(params)

            val icon = mapOf(
                1 to "success",
                2 to "error",
                3 to "none"
            )[data.optInt("mode")] ?: "none"

            // NativeApi.plat.showToast = wx.showToast
            NativeApi.plat.showToast(
                json(
                    "title" to data.optString("content"),
                    "icon" to icon
                )
            )
        } catch (e: JSONException) {
            console.error("toast json parse error", e)
        }
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
    }
}
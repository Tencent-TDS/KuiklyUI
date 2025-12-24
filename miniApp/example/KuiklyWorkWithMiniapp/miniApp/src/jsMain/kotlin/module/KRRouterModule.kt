package module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.json

/**
 * Web render routing module, uses default web API for handling. For routing in other scenarios like other APPs,
 * page routing can be implemented through bridgeModule interface
 */
class KRRouterModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            OPEN_PAGE -> openPage(params)
            CLOSE_PAGE -> closePage()
            else -> super.call(method, params, callback)
        }
    }

    /**
     * Open new page
     */
    private fun openPage(param: String?) {
        if (param == null) {
            return
        }
        val params = param.toJSONObjectSafely()
        val pageName = params.optString("pageName")
        if (pageName.isEmpty()) {
            return
        }
        
        // url data
        val pageData: MutableMap<String, Any> =
            (params.optJSONObject("pageData") ?: JSONObject()).toMap()
        
        // Check if it's a native miniapp page
        val isMiniNativePage = pageData["isMiniNativePage"]?.toString() == "1"
        
        if (isMiniNativePage) {
            // For native miniapp pages, use pageName directly as the URL
            // Remove isMiniNativePage from pageData to avoid passing it as URL param
            pageData.remove("isMiniNativePage")
            
            // Generate URL params from remaining pageData
            val urlParamsString = if (pageData.isNotEmpty()) {
                pageData.entries.joinToString("&") { (key, value) ->
                    "${key}=${value}"
                }
            } else {
                ""
            }
            
            // Navigate to native page: /pages/list/list?param1=value1
            val targetUrl = if (urlParamsString.isNotEmpty()) {
                "/pages/$pageName?$urlParamsString"
            } else {
                "/pages/$pageName"
            }
            
            console.log("navigating to native page: $targetUrl")

            NativeApi.plat.navigateTo(json("url" to targetUrl))
        } else {
            // For KuiklyUI pages, use the original logic
            val urlPrefix = if (jsTypeOf(kuiklyWindow.asDynamic().URL) != "undefined") {
                // web page url instance
                val urlInstance = js("new URL(kuiklyWindow.location.href)")
                // return url prefix
                "${urlInstance.origin}${urlInstance.pathname}"
            } else {
                ""
            }
            
            // Set new page name
            pageData["page_name"] = pageName
            // generate url params
            val urlParamsString = pageData.entries.joinToString("&") { (key, value) ->
                "${key}=${value}"
            }

            // Open new URL
            kuiklyWindow.open("$urlPrefix?${urlParamsString}")
        }
    }

    /**
     * Close page opened by click
     */
    private fun closePage() {
        try {
            kuiklyWindow.close()
        } catch (e: dynamic) {
            // Cannot close pages not opened by self
            Log.error("close page error: $e")
        }
    }

    companion object {
        const val MODULE_NAME = "KRRouterModule"
        private const val OPEN_PAGE = "openPage"
        private const val CLOSE_PAGE = "closePage"
    }
}

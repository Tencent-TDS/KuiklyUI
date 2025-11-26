package module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.utils.Log

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
        // Page parameters, need to be appended to page url
        val urlPrefix = if (jsTypeOf(kuiklyWindow.asDynamic().URL) != "undefined") {
            // web page url instance
            val urlInstance = js("new URL(kuiklyWindow.location.href)")
            // return url prefix
            "${urlInstance.origin}${urlInstance.pathname}"
        } else {
            ""
        }
        // url data
        val pageData: MutableMap<String, Any> =
            (params.optJSONObject("pageData") ?: JSONObject()).toMap()
        // Set new page name
        pageData["page_name"] = pageName
        // generate url params
        val urlParamsString = pageData.entries.joinToString("&") { (key, value) ->
            "${key}=${value}"
        }

        // Open new URL
        val url = "$urlPrefix?${urlParamsString}"
        if (globalNavigationHandler?.invoke(url) == true) {
            return
        }
        kuiklyWindow.open(url)
    }

    /**
     * Close page opened by click
     */
    private fun closePage() {
        if (globalClosePageHandler?.invoke() == true) {
            return
        }
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

        /**
         * Global navigation interception handler
         * Returns true to intercept the default behavior, false to continue
         */
        var globalNavigationHandler: ((String) -> Boolean)? = null
        
        /**
         * Global close page interception handler
         * Returns true to intercept the default behavior, false to continue
         */
        var globalClosePageHandler: (() -> Boolean)? = null
    }
}

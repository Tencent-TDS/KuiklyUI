/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.web.runtime.miniapp.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import kotlin.js.json

/**
 * Mini program routing module, uses wx.navigateTo/navigateBack for page navigation
 */
class MiniRouterModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            OPEN_PAGE -> openPage(params)
            CLOSE_PAGE -> closePage()
            else -> super.call(method, params, callback)
        }
    }

    /**
     * Open new page using wx.navigateTo
     */
    private fun openPage(param: String?) {
        console.log("[MiniRouterModule] openPage called with param: $param")
        if (param == null) {
            console.log("[MiniRouterModule] param is null, returning")
            return
        }
        val params = param.toJSONObjectSafely()
        val pageName = params.optString("pageName")
        console.log("[MiniRouterModule] pageName: $pageName")
        if (pageName.isEmpty()) {
            console.log("[MiniRouterModule] pageName is empty, returning")
            return
        }
        
        // Page parameters, need to be appended to page url
        val pageData: MutableMap<String, Any> =
            (params.optJSONObject("pageData") ?: JSONObject()).toMap()
        // Set new page name
        pageData["page_name"] = pageName
        // generate url params
        val urlParamsString = pageData.entries.joinToString("&") { (key, value) ->
            "${key}=${value}"
        }
        
        val url = "/pages/index/index?${urlParamsString}"
        console.log("[MiniRouterModule] navigating to: $url")
        
        // Use mini program navigateTo API to open new page
        // This preserves page history so user can navigate back
        NativeApi.plat.navigateTo(
            json(
                "url" to url,
                "success" to { _: dynamic ->
                    console.log("[MiniRouterModule] navigateTo success")
                },
                "fail" to { err: dynamic ->
                    console.log("[MiniRouterModule] navigateTo failed: ", err)
                    // If navigateTo fails (e.g., page stack limit), try reLaunch as fallback
                    console.log("[MiniRouterModule] trying reLaunch as fallback")
                    NativeApi.plat.reLaunch(
                        json(
                            "url" to url,
                            "success" to { _: dynamic ->
                                console.log("[MiniRouterModule] reLaunch success")
                            },
                            "fail" to { err2: dynamic ->
                                console.log("[MiniRouterModule] reLaunch also failed: ", err2)
                            }
                        )
                    )
                }
            )
        )
    }

    /**
     * Close current page using wx.navigateBack
     */
    private fun closePage() {
        NativeApi.plat.navigateBack()
    }

    companion object {
        const val MODULE_NAME = "KRRouterModule"
        private const val OPEN_PAGE = "openPage"
        private const val CLOSE_PAGE = "closePage"
    }
}

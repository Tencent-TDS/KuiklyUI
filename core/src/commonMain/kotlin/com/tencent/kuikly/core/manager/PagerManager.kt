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

package com.tencent.kuikly.core.manager

import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.exception.PagerNotFoundException
import com.tencent.kuikly.core.exception.ReactiveObserverNotFoundException
import com.tencent.kuikly.core.global.GlobalFunctionRef
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.pager.PageCreateTrace
import com.tencent.kuikly.core.pager.PageEventTrace
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.utils.getParamFromUrl

object PagerManager {
    private const val TAG = "PagerManager"
    private val pagerMap = fastHashMapOf<String, IPager>()
    private val pagerNameMap = fastHashMapOf<String, () -> IPager>()
    private val reactiveObserverMap = fastHashMapOf<String, ReactiveObserver>()

    fun getPager(pagerId: String): IPager {
        return pagerMap[pagerId] ?: throw PagerNotFoundException("pager not found: $pagerId")
    }

    fun getPagerEventTrace(pagerId: String) : PageEventTrace? {
        try {
            return getPager(pagerId).getPageTrace()?.pageEventTrace
        }catch (e: Throwable){
            // noop
        }
        return null
    }

    fun isPagerCreatorExist(pageName: String): Boolean {
        return pagerNameMap.containsKey(pageName.lowercase())
    }
    fun getCurrentPager(): IPager {
        return pagerMap[BridgeManager.currentPageId] ?: throw PagerNotFoundException("pager not found: ${BridgeManager.currentPageId}")
    }

    internal fun getReactiveObserver(pagerId: String): ReactiveObserver? = reactiveObserverMap[pagerId]

    fun getCurrentReactiveObserver() : ReactiveObserver {
        return reactiveObserverMap[BridgeManager.currentPageId] ?: throw ReactiveObserverNotFoundException("ReactiveObserver not found: ${BridgeManager.currentPageId}")
    }

    fun createPager(
        pagerId: String,
        url: String,
        pagerData: Any?
    ) {
        val pageTrace = PageCreateTrace()
        val pagerName = pageNameFromUrl(url)
        pageTrace.pageName = pagerName
        pageTrace.pageId = pagerId
        reactiveObserverMap[pagerId] = ReactiveObserver()

        pageTrace.onNewPageStart()
        val pager: IPager? = pagerCreator(pagerName)?.invoke()
        pageTrace.onNewPageEnd()

        if (pager != null) {
            pagerMap[pagerId] = pager
            pager.pageName = pagerName
            pager.setPageTrace(pageTrace)
            pager.onCreatePager(pagerId, toBridgeJSONObject(pagerData) ?: JSONObject())
        } else {
            reactiveObserverMap.remove(pagerId)
            throw PagerNotFoundException("[createPager]: pager 未注册. pagerName: $pagerName")
        }
    }

    fun firePagerEvent(pagerId: String, event: String, data: Any?) {
        pagerMap[pagerId]?.onReceivePagerEvent(event, toBridgeJSONObject(data) ?: JSONObject())
    }

    fun destroyPager(pagerId: String) {
        pagerMap[pagerId]?.onDestroyPager()
        pagerMap.remove(pagerId)
        reactiveObserverMap[pagerId]?.destroy()
        reactiveObserverMap.remove(pagerId)
    }

    /**
     * [data] is a JSON [String] or [JSONObject] (OHOS NATIVE_JSON / iOS
     * NSDictionary already wrapped at the platform callKotlin entry).
     */
    fun fireViewEvent(pagerId: String, viewRef: Int, event: String, data: Any?) {
        pagerMap[pagerId]?.onViewEvent(viewRef, event, toBridgeJSONObject(data))
    }

    /**
     * Normalize Native/Kotlin bridge payload to [JSONObject].
     * Platform collections are converted before BridgeManager
     * (OHOS toAny / iOS toKotlinBridgeArg).
     */
    private fun toBridgeJSONObject(data: Any?): JSONObject? {
        return when (data) {
            null -> null
            is JSONObject -> data
            is String -> if (data.isEmpty()) null else JSONObject(data) // TODO: what if it's NOT a JSON string?
            else -> null
        }
    }

    fun fireCallBack(pagerId: String, functionRef: GlobalFunctionRef, data: Any? = null) {
        // Structured payloads arrive as JSONObject (lazy) or legacy JSON String.
        GlobalFunctions.invokeFunction(pagerId, functionRef, data)
    }

    fun fireLayoutView(pagerId: String) {
        pagerMap[pagerId]?.onLayoutView()
    }

    fun registerPageRouter(pageName: String, creator: () -> IPager) {
        // need support forward compatible, so use toLowerCase
        pagerNameMap[pageName.lowercase()] = creator
    }

    private fun pagerCreator(pageName: String): (() -> IPager)? {
        // need support forward compatible, so use toLowerCase
        return pagerNameMap[pageName.lowercase()]
    }

    private fun pageNameFromUrl(url: String): String {
        if (url.startsWith("http")) {
            if (url.contains("v_bundleName")) {
                return getParamFromUrl(url, "v_bundleName")
            }
            if (url.contains("pageName")) {
                return getParamFromUrl(url, "pageName")
            }
            if (url.contains("pagerName")) {
                return getParamFromUrl(url, "pagerName")
            }
        }
        return url
    }

}

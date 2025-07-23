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

package com.tencent.kuikly.demo.pages.app.model

import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.demo.pages.base.BridgeModule

enum class AppFeedsType(val value: String) {
    Follow("follow"),      // 关注
    Recommend("recommend"), // 推荐
    Nearby("nearby"),      // 附近
    Top("top"),         // 榜单
    Star("star"),       // 明星
    Laugh("laugh"),      // 搞笑
    Society("society"),    // 社会
    Test("test"),       // 测试
}

internal object AppFeedsManager {
    private var callbackMap: HashMap<Pair<AppFeedsType, Int>, Any> = hashMapOf()

    private fun genKey(type: AppFeedsType, page: Int): Pair<AppFeedsType, Int> {
        return type to page
    }

    internal fun requestFeeds(type: AppFeedsType, page: Int, callback: (List<AppFeedModel>, String) -> Unit) {
        val key = genKey(type, page)
        // 已经在请求中了
        if (callbackMap.containsKey(key)) {
            callback(listOf(), "in request")
            return
        }
        if (page >= 3) {
            callback(listOf(), "")
            return
        }

        callbackMap[key] = callback
        val pathName = getFileName(type, page)
        val bridgeModule = PagerManager.getCurrentPager().getModule<BridgeModule>(BridgeModule.MODULE_NAME)

        if (bridgeModule != null) {
            val startMills = DateTime.currentTimestamp()
            KLog.d("AppDemo", "requestFeeds startMills $startMills")
            bridgeModule.readAssetFile(pathName) { json ->
                val cost = DateTime.currentTimestamp() - startMills
                KLog.d("AppDemo", "requestFeeds readAssetPath cost: $cost")
                if (json == null || json.optString("error").isNotEmpty()) {
                    callback(listOf(), "error")
                } else {
                    val feeds = parseJson(json)
                    callback(feeds, "")
                }
                callbackMap.remove(key)
            }
        } else {
            callbackMap.remove(key)
        }
    }
    
    private fun parseJson(json: JSONObject): List<AppFeedModel> {
        val jsonArray = json.optJSONArray("result")
        val feeds = mutableListOf<AppFeedModel>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                it.optJSONObject(i)?.let { itemJson ->
                    AppFeedModel.fromJson(itemJson)?.let { model->
                        feeds.add(model)
                    }
                }
            }
        }
        return feeds
    }

    private fun getFileName(type: AppFeedsType, page: Int) : String {
        return "AppTabPage/json/" + type.value + "_" + page.toString() + ".json"
    }

}
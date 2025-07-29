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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * Kuikly默认的路由模块，用于打开Kuikly页面和关闭Kuikly页面
 */
class RouterModule : Module() {
    /*
     * @brief 打开kuikly页面
     * @param pageName kuikly页面名，为@Page注解名
     * @param pageData 页面的传参，数据类型为JsonObject, 目标页面可通过PageData.params获得
     */
    fun openPage(pageName: String, pageData: JSONObject?= null) {
        val params = JSONObject().apply {
            put("pageName", pageName)
            pageData?.also {
                put("pageData", it)
            }
        }
        toNative(
            false,
            METHOD_OPEN_PAGE,
            params.toString()
        )
    }

    /*
     * 关闭当前页面
     */
    fun closePage() {
        toNative(
            false,
             METHOD_CLOSE_PAGE,
            null
        )
    }

    override fun moduleName(): String {
        return MODULE_NAME
    }


    fun backHandle(isConsumed: Boolean) {
        toNative(
            false,
            METHOD_BACK_HANDLE,
            param = JSONObject().apply {
                put("consumed", isConsumed)
            }.toString(),
            syncCall = true
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.ROUTER
        const val METHOD_OPEN_PAGE = "openPage"
        const val METHOD_CLOSE_PAGE = "closePage"
        const val METHOD_BACK_HANDLE = "backHandle"

    }
}
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

package com.tencent.kuikly.core.render.android.expand.module

import android.content.Context
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback

/**
 * Created by kam on 2023/3/21.
 */
class KRSharedPreferencesModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            GET_ITEM -> getItem(params)
            SET_ITEM -> setItem(params)
            else -> super.call(method, params, callback)
        }
    }

    private fun getItem(params: String?): String {
        val value = ""
        val cacheKey = params ?: return value
        return context?.getSharedPreferences(MODULE_NAME, Context.MODE_PRIVATE)
            ?.getString(cacheKey, value) ?: value
    }

    private fun setItem(params: String?): String {
        val json = params.toJSONObjectSafely()
        val key = json.optString("key")
        val value = json.optString("value")
        context?.getSharedPreferences(MODULE_NAME, Context.MODE_PRIVATE)?.edit()
            ?.putString(key, value)?.apply()
        return ""
    }
    companion object {
        const val MODULE_NAME = "KRSharedPreferencesModule"
        private const val GET_ITEM = "getItem"
        private const val SET_ITEM = "setItem"

    }
}
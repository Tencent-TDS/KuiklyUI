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
 * 一个轻量级的存储（持久化）能力模块，一般用来保存应用程序的各种配置信息
 */
class SharedPreferencesModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    companion object {
        const val MODULE_NAME = ModuleConst.SHARED_PREFERENCES
        const val METHOD_GET_ITEM = "getItem"
        const val METHOD_SET_ITEM = "setItem"
    }

    /*
     * 设置保存字符串
     */
    fun setString(key: String, value: String) {
        setItem(key, value)
    }

    fun setFloat(key: String, value: Float?) {
        if (value == null) {
            setItem(key, "")
        } else {
            setItem(key, value.toString())
        }
    }

    fun setInt(key: String, value: Int?) {
        if (value == null) {
            setItem(key, "")
        } else {
            setItem(key, value.toString())
        }
    }

    fun setObject(key: String, value: JSONObject?) {
        if (value == null) {
            setItem(key, "")
        } else {
            setItem(key, value.toString())
        }
    }

    fun getString(key: String): String {
        return getItem(key)
    }

    fun getInt(key: String): Int? {
        val res = getItem(key)
        if (res.isNotEmpty()) {
            return res.toInt()
        }
        return null
    }

    fun getFloat(key: String): Float? {
        val res = getItem(key)
        if (res.isNotEmpty()) {
            return res.toFloat()
        }
        return null
    }

    fun getObject(key: String): JSONObject? {
        val value = getItem(key)
        if (value.isNotEmpty()) {
            return JSONObject(value)
        }
        return null
    }

    fun getItem(key: String): String {
        return toNative(
            false,
            METHOD_GET_ITEM,
            key,
            null,
            true
        ).toString()
    }

    fun setItem(key: String, value: String) {
        val params = JSONObject()
        params.put("key", key)
        params.put("value", value)
        toNative(
            false,
            METHOD_SET_ITEM,
            params.toString(),
            null,
            true
        )
    }

}

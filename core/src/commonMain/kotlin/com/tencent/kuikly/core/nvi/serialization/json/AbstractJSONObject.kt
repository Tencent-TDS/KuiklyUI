/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.core.nvi.serialization.json

import com.tencent.kuikly.core.log.KLog

/**
 * [JSONObject] 的平台无关实现。
 *
 * [JSONObject] 本身是 expect/actual 类，各平台的差异只在「底层容器怎么来」——
 * Android/JVM/JS 用 Kotlin 集合，Apple 用 Foundation 字典，OHOS 用 cJSON 树。
 * 读写语义（类型选型、异常文案、序列化文本）必须逐字节一致，因此统一放在这里，
 * 由各平台 actual 类继承，避免 5 份实现漂移。
 *
 * 构造函数为 internal：外部只能通过 [JSONObject] 使用。
 */
abstract class AbstractJSONObject internal constructor(nameValuePairs: MutableMap<String, Any?>) {

    internal var nameValuePairs: MutableMap<String, Any?> = nameValuePairs

    fun length(): Int {
        return nameValuePairs.size
    }

    /**
     * Default Kotlin-Map implementation. Platform lazy containers may override
     * this to access a native insertion-indexed key directly.
     */
    open fun keyAt(index: Int): String? {
        if (index < 0 || index >= nameValuePairs.size) {
            return null
        }
        return nameValuePairs.keys.elementAt(index)
    }

    fun put(name: String, value: Boolean): JSONObject {
        nameValuePairs[name] = value
        return self()
    }

    fun put(name: String, value: Int): JSONObject {
        nameValuePairs[name] = value
        return self()
    }

    fun put(name: String, value: Long): JSONObject {
        nameValuePairs[name] = value
        return self()
    }

    fun put(name: String, value: Double): JSONObject {
        nameValuePairs[name] = value
        return self()
    }

    fun put(name: String, value: Any?): JSONObject {
        nameValuePairs[name] = value
        return self()
    }

    fun has(name: String): Boolean {
        return nameValuePairs.containsKey(name)
    }

    fun opt(name: String): Any? {
        return nameValuePairs[name]
    }

    /**
     * Default Kotlin-Map implementation. Platform lazy containers may override
     * this to access a native insertion-indexed value directly.
     */
    open fun opt(index: Int): Any? {
        if (index < 0 || index >= nameValuePairs.size) {
            return null
        }
        return nameValuePairs.values.elementAt(index)
    }

    fun optBoolean(name: String): Boolean {
        return optBoolean(name, false)
    }

    fun optBoolean(name: String, fallback: Boolean): Boolean {
        val o = opt(name)
        val result = JSON.toBoolean(o)
        return result ?: fallback
    }

    fun optDouble(name: String): Double {
        return optDouble(name, 0.0)
    }

    fun optDouble(name: String, fallback: Double): Double {
        val o = opt(name)
        val result = JSON.toDouble(o)
        return result ?: fallback
    }

    fun optInt(name: String): Int {
        return optInt(name, 0)
    }

    fun optInt(name: String, fallback: Int): Int {
        val o = opt(name)
        val result = JSON.toInteger(o)
        return result ?: fallback
    }

    fun optLong(name: String): Long {
        return optLong(name, 0L)
    }

    fun optLong(name: String, fallback: Long): Long {
        val o = opt(name)
        val result = JSON.toLong(o)
        return result ?: fallback
    }

    fun optString(name: String): String {
        return optString(name, "")
    }

    fun optString(name: String, fallback: String): String {
        val o = opt(name)
        val result = JSON.toString(o)
        return result ?: fallback
    }

    fun optJSONArray(name: String): JSONArray? {
        return when (val value = opt(name)) {
            is JSONArray -> {
                value
            }

            is String -> {
                try {
                    JSONArray(value)
                } catch (e: JSONException) {
                    KLog.e(TAG, "$value can not convert to json")
                    null
                }
            }

            else -> {
                null
            }
        }
    }

    fun optJSONObject(name: String): JSONObject? {
        return when (val value = opt(name)) {
            is JSONObject -> {
                value
            }
            is String -> {
                try {
                    JSONObject(value)
                } catch (e: JSONException) {
                    KLog.e(TAG, "$value can not convert to json")
                    null
                }
            }
            else -> {
                null
            }
        }
    }

    fun keys(): Iterator<String> {
        return nameValuePairs.keys.iterator()
    }

    fun keySet(): Set<String> {
        return nameValuePairs.keys
    }

    override fun toString(): String {
        return try {
            JSONEngine.stringify(self())
        } catch (e: JSONException) {
            "{}"
        }
    }

    fun toMap(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = keys()
        for (key in keys) {
            when (val value = opt(key)) {
                is Int -> {
                    map[key] = value
                }
                is Long -> {
                    map[key] = value
                }
                is Double -> {
                    map[key] = value
                }
                is Float -> {
                    map[key] = value
                }
                is String -> {
                    map[key] = value
                }
                is Boolean -> {
                    map[key] = value
                }
                is JSONObject -> {
                    map[key] = value.toMap()
                }
                is JSONArray -> {
                    map[key] = value.toList()
                }
            }
        }
        return map
    }

    @Throws(JSONException::class)
    fun writeTo(stringer: JSONStringer) {
        stringer.startObject()
        for ((key, value) in nameValuePairs) {
            stringer.key(key).value(value)
        }
        stringer.endObject()
    }

    /** 每个 actual 类都是 [JSONObject]，链式 API 需要以 [JSONObject] 身份返回自身。 */
    private fun self(): JSONObject = this as JSONObject

    private companion object {
        private const val TAG = "JSONObject"
    }
}

/** [JSONObject] 的 `String` / [JSONTokener] 构造入口共用的类型校验。 */
@Throws(JSONException::class)
internal fun requireJSONObjectPairs(value: Any?): MutableMap<String, Any?> {
    if (value !is JSONObject) {
        throw JSON.typeMismatch(value, "JSONObject")
    }
    return value.nameValuePairs
}

/** [JSONObject.quote] 的共享实现。 */
internal fun quoteJSONString(data: String?): String {
    if (data == null) {
        return "\"\""
    }
    val stringer = JSONStringer()
    stringer.open(JSONStringer.Scope.NULL_OBJ, "")
    stringer.value(data)
    stringer.close(JSONStringer.Scope.NULL_OBJ, JSONStringer.Scope.NULL_OBJ, "")
    return stringer.toString()
}

package com.tencent.kuikly.core.nvi.serialization.json

import com.tencent.kuikly.core.log.KLog

/**
 * Created by kam on 2022/4/11.
 */
class JSONObject internal constructor(nameValuePairs: MutableMap<String, Any?>) {
    companion object{
        private const val TAG = "JSONObject"

        fun quote(data: String?): String {
            if (data == null) {
                return "\"\""
            }
            val stringer = JSONStringer()
            stringer.open(JSONStringer.Scope.NULL_OBJ, "")
            stringer.value(data)
            stringer.close(JSONStringer.Scope.NULL_OBJ, JSONStringer.Scope.NULL_OBJ, "")
            return stringer.toString()
        }

    }

    internal var nameValuePairs: MutableMap<String, Any?>

    init {
        this.nameValuePairs = nameValuePairs
    }

    constructor(): this(JSONEngine.getMutableMap())

    @Throws(JSONException::class)
    constructor(json: String) : this(
        (JSONEngine.parse(json).also { result ->
            if (result !is JSONObject) {
                throw JSON.typeMismatch(result, "JSONObject")
            }
        } as JSONObject).nameValuePairs
    )

    @Throws(JSONException::class)
    constructor(jsonTokener: JSONTokener) : this(
        (jsonTokener.nextValue().also { result ->
            if (result !is JSONObject) {
                throw JSON.typeMismatch(result, "JSONObject")
            }
        } as JSONObject).nameValuePairs
    )

    fun length(): Int {
        return nameValuePairs.size
    }

    fun put(name: String, value: Boolean): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun put(name: String, value: Int): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun put(name: String, value: Long): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun put(name: String, value: Double): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun put(name: String, value: Any?): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun has(name: String): Boolean {
        return nameValuePairs.containsKey(name)
    }

    fun opt(name: String): Any? {
        return nameValuePairs[name]
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
            JSONEngine.stringify(this)
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
}
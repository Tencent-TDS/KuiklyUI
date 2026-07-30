package com.tencent.kuikly.core.render.web.nvi.serialization.json

import com.tencent.kuikly.core.render.web.nvi.serialization.IDeserialization
import com.tencent.kuikly.core.render.web.nvi.serialization.ISerialization
import com.tencent.kuikly.core.render.web.nvi.serialization.serialization
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * JSON Object handling class
 */
class JSONObject internal constructor(nameValuePairs: MutableMap<String, Any?>) {
    internal var nameValuePairs: MutableMap<String, Any?>

    init {
        this.nameValuePairs = nameValuePairs
    }

    constructor() : this(JSONEngine.getMutableMap())

    constructor(json: String) : this(
        (JSONEngine.parse(json).also { result ->
            if (result !is JSONObject) {
                throw JSON.typeMismatch(result, "JSONObject")
            }
        } as JSONObject).nameValuePairs
    )

    constructor(jsonTokener: JSONTokener) : this(
        (jsonTokener.nextValue().also { result ->
            if (result !is JSONObject) {
                throw JSON.typeMismatch(result, "JSONObject")
            }
        } as JSONObject).nameValuePairs
    )

    constructor(init: JSONObject.() -> Unit) : this() {
        this.init()
    }

    fun length(): Int = nameValuePairs.size

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

    fun <T : ISerialization> put(name: String, value: T) {
        nameValuePairs[name] = value.serialization()
    }

    fun put(name: String, value: Any?): JSONObject {
        nameValuePairs[name] = value
        return this
    }

    fun has(name: String): Boolean = nameValuePairs.containsKey(name)

    fun opt(name: String): Any? = nameValuePairs[name]

    fun optBoolean(name: String): Boolean = optBoolean(name, false)

    fun optBoolean(name: String, fallback: Boolean): Boolean {
        val o = opt(name)
        val result = JSON.toBoolean(o)
        return result ?: fallback
    }

    fun optDouble(name: String): Double = optDouble(name, 0.0)

    fun optDouble(name: String, fallback: Double): Double {
        val o = opt(name)
        val result = JSON.toDouble(o)
        return result ?: fallback
    }

    fun optInt(name: String): Int = optInt(name, 0)

    fun optInt(name: String, fallback: Int): Int {
        val o = opt(name)
        val result = JSON.toInteger(o)
        return result ?: fallback
    }

    fun optLong(name: String): Long = optLong(name, 0L)

    fun optLong(name: String, fallback: Long): Long {
        val o = opt(name)
        val result = JSON.toLong(o)
        return result ?: fallback
    }

    fun optString(name: String): String = optString(name, "")

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
                    Log.log(TAG, "$value can not convert to json")
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
                    Log.log(TAG, "$value can not convert to json")
                    null
                }
            }

            else -> {
                null
            }
        }
    }

    fun keys(): Iterator<String> = nameValuePairs.keys.iterator()

    fun keySet(): Set<String> = nameValuePairs.keys

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

    fun writeTo(stringer: JSONStringer) {
        stringer.startObject()
        for ((key, value) in nameValuePairs) {
            stringer.key(key).value(value)
        }
        stringer.endObject()
    }

    fun <T : IDeserialization> parseTo(target: T) {
        target.deserialization(this)
    }

    infix fun String.with(value: Any?) {
        put(this, value)
    }

    infix fun <T> String.with(list: List<T>) {
        put(this, list.serialization())
    }

    infix fun <T> String.with(set: Set<T>) {
        put(this, set.serialization())
    }

    infix fun <T> String.with(map: Map<String, T>) {
        put(this, map.serialization())
    }

    infix fun <T : ISerialization> String.with(value: T) {
        put(this, value.serialization())
    }

    infix fun String.with(init: JSONObject.() -> Unit) {
        put(this, JSONObject().apply(init))
    }

    infix fun String.withArray(init: JSONArray.() -> Unit) {
        put(this, JSONArray().apply(init))
    }

    companion object {
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
}

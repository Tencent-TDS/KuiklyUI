package com.tencent.kuikly.core.nvi.serialization.json

/**
 * Created by kam on 2022/4/11.
 */
class JSONArray internal constructor(values: MutableList<Any?>) {

    internal var values: MutableList<Any?>

    init {
        this.values = values
    }

    constructor(): this(JSONEngine.getMutableList())

    @Throws(JSONException::class)
    constructor(json: String) : this(
        (JSONEngine.parse(json).also { result ->
            if (result !is JSONArray) {
                throw JSON.typeMismatch(result, "JSONArray")
            }
        } as JSONArray).values
    )

    @Throws(JSONException::class)
    constructor(jsonTokener: JSONTokener) : this(
        (jsonTokener.nextValue().also { result ->
            if (result !is JSONArray) {
                throw JSON.typeMismatch(result, "JSONArray")
            }
        } as JSONArray).values
    )

    /**
     * Returns the number of values in this array.
     */
    fun length(): Int {
        return values.size
    }

    fun put(value: Boolean): JSONArray {
        values.add(value)
        return this
    }

    fun put(value: Double): JSONArray {
        values.add(value)
        return this
    }

    fun put(value: Int): JSONArray {
        values.add(value)
        return this
    }

    fun put(value: Long): JSONArray {
        values.add(value)
        return this
    }

    fun put(value: Any?): JSONArray {
        values.add(value)
        return this
    }

    fun opt(index: Int): Any? {
        return if (index < 0 || index >= values.size) {
            null
        } else {
            values[index]
        }
    }

    fun remove(index: Int): Any? {
        return if (index < 0 || index >= values.size) {
            null
        } else {
            values.removeAt(index)
        }
    }

    fun optBoolean(index: Int): Boolean {
        return optBoolean(index, false)
    }

    fun optBoolean(index: Int, fallback: Boolean): Boolean {
        val o = opt(index)
        val result = JSON.toBoolean(o)
        return result ?: fallback
    }

    fun optDouble(index: Int): Double {
        return optDouble(index, Double.NaN)
    }

    fun optDouble(index: Int, fallback: Double): Double {
        val o = opt(index)
        val result = JSON.toDouble(o)
        return result ?: fallback
    }

    fun optInt(index: Int): Int {
        return optInt(index, 0)
    }

    fun optInt(index: Int, fallback: Int): Int {
        val o = opt(index)
        val result = JSON.toInteger(o)
        return result ?: fallback
    }

    fun optLong(index: Int): Long {
        return optLong(index, 0L)
    }

    fun optLong(index: Int, fallback: Long): Long {
        val o = opt(index)
        val result = JSON.toLong(o)
        return result ?: fallback
    }

    fun optString(index: Int): String? {
        return optString(index, "")
    }

    fun optString(index: Int, fallback: String?): String? {
        val o = opt(index)
        val result = JSON.toString(o)
        return result ?: fallback
    }

    fun optJSONArray(index: Int): JSONArray? {
        val o = opt(index)
        return if (o is JSONArray) {
            o
        } else {
            null
        }
    }

    fun optJSONObject(index: Int): JSONObject? {
        val o = opt(index)
        return if (o is JSONObject) {
            o
        } else {
            null
        }
    }

    override fun toString(): String {
        return try {
            JSONEngine.stringify(this)
        } catch (e: JSONException) {
            "[]"
        }
    }

    fun toList(): MutableList<Any> {
        val list = mutableListOf<Any>()
        val size = length()
        for (i in 0 until size) {
            when (val value = opt(i)) {
                is Int -> {
                    list.add(value)
                }
                is Long -> {
                    list.add(value)
                }
                is Float -> {
                    list.add(value)
                }
                is Double -> {
                    list.add(value)
                }
                is String -> {
                    list.add(value)
                }
                is Boolean -> {
                    list.add(value)
                }
                is JSONObject -> {
                    list.add(value.toMap())
                }
                is JSONArray -> {
                    list.add(value.toList())
                }
            }
        }
        return list
    }

    @Throws(JSONException::class)
    fun writeTo(stringer: JSONStringer) {
        stringer.startArray()
        for (value in values) {
            if (value == null) {
                stringer.value(null)
            } else {
                stringer.value(value)
            }
        }
        stringer.endArray()
    }

    override fun equals(other: Any?): Boolean {
        return other is JSONArray && other.values == values
    }

    override fun hashCode(): Int {
        // diverge from the original, which doesn't implement hashCode
        return values.hashCode()
    }
}
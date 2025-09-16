package com.tencent.kuikly.core.render.web.nvi.serialization.json

/**
 * JSON array handling class
 */
class JSONArray internal constructor(internal var values: MutableList<Any?>) {
    constructor() : this(JSONEngine.getMutableList())

    constructor(json: String) : this(
        (JSONEngine.parse(json).also { result ->
            if (result !is JSONArray) {
                throw JSON.typeMismatch(result, "JSONArray")
            }
        } as JSONArray).values
    )

    constructor(jsonTokener: JSONTokener) : this(
        (jsonTokener.nextValue().also { result ->
            if (result !is JSONArray) {
                throw JSON.typeMismatch(result, "JSONArray")
            }
        } as JSONArray).values
    )

    constructor(init: JSONArray.() -> Unit) : this() {
        this.init()
    }

    /**
     * Returns the number of values in this array.
     */
    fun length(): Int = values.size

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

    fun putJSONObject(init: JSONObject.() -> Unit) {
        values.add(JSONObject(init))
    }

    fun putJSONArray(init: JSONArray.() -> Unit) {
        values.add(JSONArray(init))
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

    fun optBoolean(index: Int): Boolean = optBoolean(index, false)

    fun optBoolean(index: Int, fallback: Boolean): Boolean {
        val o = opt(index)
        val result = JSON.toBoolean(o)
        return result ?: fallback
    }

    fun optDouble(index: Int): Double = optDouble(index, Double.NaN)

    fun optDouble(index: Int, fallback: Double): Double {
        val o = opt(index)
        val result = JSON.toDouble(o)
        return result ?: fallback
    }

    fun optInt(index: Int): Int = optInt(index, 0)

    fun optInt(index: Int, fallback: Int): Int {
        val o = opt(index)
        val result = JSON.toInteger(o)
        return result ?: fallback
    }

    fun optLong(index: Int): Long = optLong(index, 0L)

    fun optLong(index: Int, fallback: Long): Long {
        val o = opt(index)
        val result = JSON.toLong(o)
        return result ?: fallback
    }

    fun optString(index: Int): String? = optString(index, "")

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

    override fun equals(other: Any?): Boolean = other is JSONArray && other.values == values

    /**
     * diverge from the original, which doesn't implement hashCode
     */
    override fun hashCode(): Int = values.hashCode()

    infix fun JSONArray.add(init: JSONObject.() -> Unit) {
        this.put(JSONObject().apply(init))
    }

    infix fun JSONArray.add(value: Any) {
        this.put(value)
    }

    infix fun JSONArray.addArray(init: JSONArray.() -> Unit) {
        this.put(JSONArray().apply(init))
    }
}

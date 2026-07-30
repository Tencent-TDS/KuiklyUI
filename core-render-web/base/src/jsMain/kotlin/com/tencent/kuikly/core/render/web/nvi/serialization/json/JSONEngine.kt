package com.tencent.kuikly.core.render.web.nvi.serialization.json

import com.tencent.kuikly.core.render.web.collection.FastMutableList
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.utils.Log

private const val TAG = "JSONEngine"

object JSONEngine {
    fun parse(jsonStr: String): Any? {
        if (!JSON.useNativeMethod) {
            return JSONTokener(jsonStr).nextValue()
        }
        return parseFromEngine(jsonStr)
    }

    fun stringify(jsonObject: JSONObject): String {
        if (!JSON.useNativeMethod) {
            return commonStringify(jsonObject)
        }
        return stringifyFromEngine(jsonObject)
    }

    fun stringify(jsonArray: JSONArray): String {
        if (!JSON.useNativeMethod) {
            return commonStringify(jsonArray)
        }
        return stringifyFromEngine(jsonArray)
    }

    fun <K, V> getMutableMap(): MutableMap<K, V> {
        if (!JSON.useNativeMethod) {
            return mutableMapOf()
        }
        return FastMutableMap(getEmptyObject())
    }

    fun <E> getMutableList(): MutableList<E> {
        if (!JSON.useNativeMethod) {
            return mutableListOf()
        }
        return FastMutableList(getEmptyArray())
    }
}

private fun stringifyFromEngine(json: Any): String {
    try {
        return kotlin.js.JSON.stringify(json) { _, value ->
            if (jsTypeOf(value) === "object" && value !== null) {
                when (value) {
                    is JSONObject -> {
                        value.nameValuePairs.unsafeCast<FastMutableMap<String, Any?>>().fastMap
                            ?: getStringifyObject(value)
                    }

                    is JSONArray -> {
                        value.values.unsafeCast<FastMutableList<Any?>>().fastList
                            ?: getStringifyArray(value)
                    }

                    is Long -> {
                        parseInt(value.toString(), 10).also {
                            if (!isSafeInteger(it)) {
                                throw JSONException("stringify unsafe integer: $value")
                            }
                        }
                    }

                    else -> {
                        value.toString()
                    }
                }
            } else {
                value
            }
        }
    } catch (error: dynamic) {
        Log.error(TAG, "can not convert to string: $error")
        if (json is JSONObject) {
            return commonStringify(json)
        }
        if (json is JSONArray) {
            return commonStringify(json)
        }
        throw JSONException("unsupported type")
    }
}

private fun getStringifyObject(jsonObject: JSONObject): dynamic {
    val result = getEmptyObject()
    for (key in jsonObject.keys()) {
        result[key] = jsonObject.opt(key)
    }
    return result
}

private fun getStringifyArray(jsonArray: JSONArray): dynamic {
    val result = getEmptyArray()
    for (i in 0 until jsonArray.length()) {
        result[i] = jsonArray.opt(i)
    }
    return result
}

private fun parseFromEngine(jsonStr: String): Any? {
    try {
        return kotlin.js.JSON.parse(jsonStr) { _, value ->
            if (jsTypeOf(value) !== "object" || value === null) {
                if (jsTypeOf(value) === "number" && isInteger(value) && !isSafeInteger(value)) {
                    throw JSONException("parse unsafe number: $value")
                }
                value
            } else if (isArray(value)) {
                JSONArray(FastMutableList(value))
            } else {
                JSONObject(FastMutableMap(value))
            }
        }
    } catch (error: dynamic) {
        Log.error(TAG, "can not convert to json: $error")
        Log.log(TAG, "bad case: $jsonStr")
        return JSONTokener(jsonStr).nextValue()
    }
}

private fun checkPositionIndex(index: Int, size: Int, isIncludeEnd: Boolean = false) {
    val length = if (isIncludeEnd) {
        size + 1
    } else {
        size
    }
    if (index !in 0 until length) {
        throw IndexOutOfBoundsException("index: $index, size: $size")
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isArray(v: Any?): Boolean = js("Array").isArray(v).unsafeCast<Boolean>()

@Suppress("NOTHING_TO_INLINE")
private inline fun getEmptyObject(): dynamic = js("{}")

@Suppress("NOTHING_TO_INLINE")
private inline fun getEmptyArray(): dynamic = js("[]")

@Suppress("NOTHING_TO_INLINE")
private inline fun isSafeInteger(value: dynamic): Boolean =
    value > js("Number").MIN_SAFE_INTEGER && value < js("Number").MAX_SAFE_INTEGER

@Suppress("NOTHING_TO_INLINE")
private inline fun isInteger(value: dynamic): Boolean = value % 1 === 0

external fun parseInt(value: Any, radix: Int): Int

fun commonStringify(jsonObject: JSONObject): String {
    val stringer = JSONStringer()
    jsonObject.writeTo(stringer)
    return stringer.toString()
}

fun commonStringify(jsonObject: JSONArray): String {
    val stringer = JSONStringer()
    jsonObject.writeTo(stringer)
    return stringer.toString()
}

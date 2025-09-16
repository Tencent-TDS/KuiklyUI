package com.tencent.kuikly.core.render.web.nvi.serialization

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject


fun Map<String, Any?>.serialization(): JSONObject {
    val serializationObject = JSONObject()
    forEach { (key, value) ->
        when (value) {
            is Int -> {
                serializationObject.put(key, value)
            }

            is Long -> {
                serializationObject.put(key, value)
            }

            is Double -> {
                serializationObject.put(key, value)
            }

            is Float -> {
                serializationObject.put(key, value)
            }

            is String -> {
                serializationObject.put(key, value)
            }

            is Boolean -> {
                serializationObject.put(key, if (value) 1 else 0)
            }

            is Map<*, *> -> {
                val map = value.unsafeCast<Map<String, Any>>()
                serializationObject.put(key, map.serialization())
            }

            is List<*> -> {
                val list = value.unsafeCast<List<Any>>()
                serializationObject.put(key, list.serialization())
            }

            is ISerialization -> {
                serializationObject.put(key, value.serialization())
            }

            else -> {
                serializationObject.put(key, null)
            }

        }
    }
    return serializationObject
}

internal fun List<Any?>.serialization(): JSONArray {
    val serializationArray = JSONArray()
    forEach { value ->
        when (value) {
            is Int -> {
                serializationArray.put(value)
            }

            is Long -> {
                serializationArray.put(value)
            }

            is Float -> {
                serializationArray.put(value)
            }

            is Double -> {
                serializationArray.put(value)
            }

            is String -> {
                serializationArray.put(value)
            }

            is Boolean -> {
                serializationArray.put(value)
            }

            is Map<*, *> -> {
                val map = value.unsafeCast<Map<String, Any>>()
                serializationArray.put(map.serialization())
            }

            is List<*> -> {
                val list = value.unsafeCast<List<Any>>()
                serializationArray.put(list.serialization())
            }

            is ISerialization -> {
                serializationArray.put((value.serialization()))
            }

            else -> {
                serializationArray.put(null)
            }
        }
    }
    return serializationArray
}

internal fun Set<Any?>.serialization(): JSONArray {
    val serializationArray = JSONArray()
    forEach { value ->
        when (value) {
            is Int -> {
                serializationArray.put(value)
            }

            is Long -> {
                serializationArray.put(value)
            }

            is Float -> {
                serializationArray.put(value)
            }

            is Double -> {
                serializationArray.put(value)
            }

            is String -> {
                serializationArray.put(value)
            }

            is Boolean -> {
                serializationArray.put(value)
            }

            is Map<*, *> -> {
                val map = value.unsafeCast<Map<String, Any>>()
                serializationArray.put(map.serialization())
            }

            is List<*> -> {
                val list = value.unsafeCast<List<Any>>()
                serializationArray.put(list.serialization())
            }

            is ISerialization -> {
                serializationArray.put((value.serialization()))
            }

            else -> {
                serializationArray.put(null)
            }
        }
    }
    return serializationArray
}

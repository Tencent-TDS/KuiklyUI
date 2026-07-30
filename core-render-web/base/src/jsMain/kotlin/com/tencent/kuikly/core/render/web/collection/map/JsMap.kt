package com.tencent.kuikly.core.render.web.collection.map

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.fastMutableMapOf

@JsName("Map")
external class JsMap<K, V> {
    val size: Int

    fun has(key: K): Boolean
    fun delete(key: K): Boolean
    fun forEach(callback: (value: V, key: K) -> Unit)
    fun clear()
}

// Extension functions (similar to Kotlin Map operator overloading)
operator fun <K, V> JsMap<K, V>.get(key: K): V? = this.asDynamic().get(key).unsafeCast<V?>()
operator fun <K, V> JsMap<K, V>.set(key: K, value: V) = this.asDynamic().set(key, value)

// Extensions for converting to Kotlin Map
inline fun <K, V> JsMap<K, V>.toMap(): Map<K, V> {
    val map = fastMutableMapOf<K, V>()
    this.keys().forEach { item ->
        map[item] = this[item]!!
    }
    return map
}

inline fun <K, V> JsMap<K, V>.getOrPut(key: K, defaultValue: () -> V): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        set(key, answer)
        answer
    } else {
        value
    }
}

inline fun <K, V> JsMap<K, V>.remove(key: K): V? {
    val value = get(key)
    delete(key)
    return value
}

// Other utility extensions
inline fun <K, V> JsMap<K, V>.isEmpty(): Boolean = size == 0
inline fun <K, V> JsMap<K, V>.isNotEmpty(): Boolean = size != 0

inline fun <K, V> JsMap<K, V>.keys(): JsArray<K> = this.asDynamic().keys().unsafeCast<JsArray<K>>()
inline fun <K, V> JsMap<K, V>.values(): JsArray<V> =
    this.asDynamic().values().unsafeCast<JsArray<V>>()

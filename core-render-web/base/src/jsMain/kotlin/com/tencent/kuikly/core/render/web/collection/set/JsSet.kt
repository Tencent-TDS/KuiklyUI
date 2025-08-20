package com.tencent.kuikly.core.render.web.collection.set

@JsName("Set")
external class JsSet<E> {
    val size: Int

    // Native method declarations
    fun add(value: E): JsSet<E>
    fun delete(value: E): Boolean
    fun has(value: E): Boolean
    fun forEach(callback: (value: E) -> Unit)
    fun clear()
}

// Utility extensions
inline fun <E> JsSet<E>.isEmpty(): Boolean = size == 0
inline fun <E> JsSet<E>.isNotEmpty(): Boolean = size != 0

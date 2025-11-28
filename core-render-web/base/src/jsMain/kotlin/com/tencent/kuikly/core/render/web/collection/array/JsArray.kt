package com.tencent.kuikly.core.render.web.collection.array

import com.tencent.kuikly.core.render.web.ktx.KRExtConst

fun <T> emptyJsArrayOf() = JsArray<T>()

@JsName("Array")
external class JsArray<T> {
    val length: Int

    constructor()
    constructor(vararg items: T)

    fun push(item: T): Int
    fun pop(): T
    fun includes(item: T): Boolean
    fun indexOf(item: T): Int
    fun join(joinString: String): String
    fun forEach(callback: (item: T, index: Int) -> Unit)
    fun forEach(callback: (item: T) -> Unit)
    fun slice(start: Int, end: Int): JsArray<T>
    fun splice(start: Int, deleteCount: Int, vararg items: T): JsArray<T>
    fun findIndex(callback: (item: T) -> Boolean): Int
    fun findIndex(callback: (item: T, index: Int) -> Boolean): Int
    fun filter(callback: (item: T) -> Boolean): JsArray<T>
}

inline operator fun <T> JsArray<T>.get(index: Int): T = this.asDynamic()[index].unsafeCast<T>()

inline operator fun <T> JsArray<T>.set(index: Int, value: T) {
    this.asDynamic()[index] = value
}

inline fun <T> JsArray<T>.add(element: T) {
    this.asDynamic()[length] = element
}

inline fun <T> JsArray<T>.add(index: Int, element: T) {
    val adjustedIndex = when {
        index < 0 -> 0  // Handle negative index
        index > this.length -> this.length  // Append to end when exceeding length
        else -> index
    }

    // Use splice to implement insertion
    this.asDynamic().splice(adjustedIndex, 0, element)
}


inline fun <T> JsArray<T>.isEmpty(): Boolean = this.length == 0

inline fun <T> JsArray<T>.removeLast(): T {
    val last = this[length - 1]
    this.asDynamic().length = length - 1
    return last
}

inline fun <T> JsArray<T>.clear() {
    this.asDynamic().length = 0
}

inline fun <T> JsArray<T>.remove(element: T): Boolean {
    val index = this.asDynamic().indexOf(element) as Int
    if (index != -1) {
        this.splice(index, 1)
        return true
    }
    return false
}

inline fun <T> JsArray<T>.removeAll() {
    this.clear()
}

inline fun <T : Comparable<T>> JsArray<T>.max(): T? {
    if (this.isEmpty()) return null
    var maxValue = this[0]
    for (i in 1 until this.length) {
        val current = this[i]
        if (current > maxValue) {
            maxValue = current
        }
    }
    return maxValue
}

inline fun <T> JsArray<T>.firstArg(): T =
    this.asDynamic()[KRExtConst.FIRST_ARG_INDEX].unsafeCast<T>()

inline fun <T> JsArray<T>.secondArg(): T =
    this.asDynamic()[KRExtConst.SECOND_ARG_INDEX].unsafeCast<T>()

inline fun <T> JsArray<T>.thirdArg(): T =
    this.asDynamic()[KRExtConst.THIRD_ARG_INDEX].unsafeCast<T>()

inline fun <T> JsArray<T>.fourthArg(): T =
    this.asDynamic()[KRExtConst.FOURTH_ARG_INDEX].unsafeCast<T>()

inline fun <T> JsArray<T>.fifthArg(): T =
    this.asDynamic()[KRExtConst.FIFTH_ARG_INDEX].unsafeCast<T>()

inline fun <T> JsArray<T>.sixthArg(): T =
    this.asDynamic()[KRExtConst.SIXTH_ARG_INDEX].unsafeCast<T>()

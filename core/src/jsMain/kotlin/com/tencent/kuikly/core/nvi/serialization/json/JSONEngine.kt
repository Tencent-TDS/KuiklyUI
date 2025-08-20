package com.tencent.kuikly.core.nvi.serialization.json

import com.tencent.kuikly.core.log.KLog

private const val TAG = "JSONEngine"

actual object JSONEngine {

    actual fun parse(jsonStr: String): Any? {
        if (!JSON.useNativeMethod) {
            return JSONTokener(jsonStr).nextValue()
        }
        return parseFromEngine(jsonStr)
    }

    actual fun stringify(jsonObject: JSONObject): String {
        if (!JSON.useNativeMethod) {
            return commonStringify(jsonObject)
        }
        return stringifyFromEngine(jsonObject)
    }

    actual fun stringify(jsonArray: JSONArray): String {
        if (!JSON.useNativeMethod) {
            return commonStringify(jsonArray)
        }
        return stringifyFromEngine(jsonArray)
    }

    actual fun <K, V> getMutableMap(): MutableMap<K, V> {
        if (!JSON.useNativeMethod) {
            return mutableMapOf()
        }
        return FastMutableMap(getEmptyObject())
    }

    actual fun <E> getMutableList(): MutableList<E> {
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
        KLog.e(TAG, "can not convert to string: $error")
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
        KLog.e(TAG, "can not convert to json: $error")
        KLog.i(TAG, "bad case: $jsonStr")
        return JSONTokener(jsonStr).nextValue()
    }
}

private class FastMutableList<E>(list: dynamic) : MutableList<E> {

    val fastList: dynamic

    init {
        this.fastList = list
    }

    override val size: Int
        get() = fastList.length.unsafeCast<Int>()

    override fun clear() {
        fastList.length = 0
    }

    override fun addAll(elements: Collection<E>): Boolean {
        fastList.push.apply(fastList, elements.toTypedArray())
        return elements.isNotEmpty()
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        checkPositionIndex(index, size)
        fastList.splice(index, 0, elements.toTypedArray())
        return elements.isNotEmpty()
    }

    override fun add(index: Int, element: E) {
        checkPositionIndex(index, size)
        fastList.splice(index, 0, element)
    }

    override fun add(element: E): Boolean {
        fastList.push(element)
        return true
    }

    override fun get(index: Int): E {
        return fastList[index].unsafeCast<E>()
    }

    override fun isEmpty(): Boolean {
        return fastList.length === 0
    }

    override fun iterator(): MutableIterator<E> {
        return listIterator()
    }

    override fun listIterator(): MutableListIterator<E> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): MutableListIterator<E> {
        checkPositionIndex(index, size, true)
        return object : MutableListIterator<E> {
            private var currentIndex = index

            override fun hasNext(): Boolean = currentIndex < size

            override fun hasPrevious(): Boolean = currentIndex > 0

            override fun next(): E {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return fastList[currentIndex++].unsafeCast<E>()
            }

            override fun nextIndex(): Int = currentIndex

            override fun previous(): E {
                if (!hasPrevious()) {
                    throw NoSuchElementException()
                }
                return fastList[--currentIndex].unsafeCast<E>()
            }

            override fun previousIndex(): Int = currentIndex - 1

            override fun add(element: E) {
                this@FastMutableList.add(currentIndex++, element)
            }

            override fun remove() {
                if (currentIndex == 0) {
                    throw IllegalStateException()
                }
                this@FastMutableList.removeAt(--currentIndex)
            }

            override fun set(element: E) {
                if (currentIndex == 0) {
                    throw IllegalStateException()
                }
                this@FastMutableList[currentIndex - 1] = element
            }
        }
    }

    override fun removeAt(index: Int): E {
        checkPositionIndex(index, size)
        return fastList.splice(index, 1)[0].unsafeCast<E>()
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        checkRangeIndexes(fromIndex, toIndex, size)
        return FastMutableList(fastList.slice(fromIndex, toIndex))
    }

    override fun set(index: Int, element: E): E {
        checkPositionIndex(index, size)
        val oldValue = fastList[index].unsafeCast<E>()
        fastList[index] = element
        return oldValue
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return removeAll(this.filter { it !in elements })
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return elements.fold(false) { acc, e ->
            acc or remove(e)
        }
    }

    override fun remove(element: E): Boolean {
        val index = indexOf(element)
        if (index != -1) {
            fastList.splice(index, 1)
            return true
        }
        return false
    }

    override fun lastIndexOf(element: E): Int {
        return fastList.lastIndexOf(element).unsafeCast<Int>()
    }

    override fun indexOf(element: E): Int {
        return fastList.indexOf(element).unsafeCast<Int>()
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return elements.all { contains(it) }
    }

    override fun contains(element: E): Boolean {
        return indexOf(element) != -1
    }
}

private class FastMutableMap<K, V>(map: dynamic) : MutableMap<K, V> {

    var fastMap: dynamic

    init {
        this.fastMap = map
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = keys.map { key ->
            object : MutableMap.MutableEntry<K, V> {
                override val key: K = key
                override val value: V
                    get() = fastMap[key.toString()].unsafeCast<V>()

                override fun setValue(newValue: V): V {
                    val oldValue = fastMap[key.toString()].unsafeCast<V>()
                    fastMap[key.toString()] = newValue
                    return oldValue
                }
            }
        }.toHashSet()
    override val keys: MutableSet<K>
        get() = getObjectKeys(fastMap).unsafeCast<Array<K>>().toHashSet()
    override val size: Int
        get() = getObjectKeys(fastMap).size
    override val values: MutableCollection<V>
        get() = getObjectValue(fastMap).unsafeCast<Array<V>>().toMutableList()

    override fun clear() {
        getObjectKeys(fastMap).forEach { key ->
            deleteObjectValueWithKey(fastMap, key)
        }
    }

    override fun isEmpty(): Boolean {
        return getObjectKeys(fastMap).isEmpty()
    }

    override fun remove(key: K): V? {
        val value = fastMap[key]
        deleteObjectValueWithKey(fastMap, key)
        return value.unsafeCast<V>()
    }

    override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) ->
            put(key, value)
        }
    }

    override fun put(key: K, value: V): V? {
        fastMap[key] = value
        return value
    }

    override fun get(key: K): V? {
        return fastMap[key].unsafeCast<V>()
    }

    override fun containsValue(value: V): Boolean {
        return value in getObjectValue(fastMap)
    }

    override fun containsKey(key: K): Boolean {
        return key.toString() in getObjectKeys(fastMap)
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

private fun checkRangeIndexes(fromIndex: Int, toIndex: Int, size: Int) {
    checkPositionIndex(fromIndex, size)
    checkPositionIndex(toIndex, size)
    if (fromIndex > toIndex) {
        throw IllegalArgumentException("fromIndex: $fromIndex > toIndex: $toIndex")
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isArray(v: Any?): Boolean = js("Array").isArray(v).unsafeCast<Boolean>()

@Suppress("NOTHING_TO_INLINE")
private inline fun getObjectKeys(v: Any?): Array<String> =
    js("Object").keys(v).unsafeCast<Array<String>>()

@Suppress("NOTHING_TO_INLINE")
private inline fun getObjectValue(v: Any?): Array<Any?> =
    js("Object").values(v).unsafeCast<Array<Any?>>()

@Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")
private inline fun deleteObjectValueWithKey(data: dynamic, key: dynamic) {
    js("delete data[key]")
}

@Suppress("NOTHING_TO_INLINE")
private inline fun getEmptyObject(): dynamic {
    return js("{}")
}

@Suppress("NOTHING_TO_INLINE")
private inline fun getEmptyArray(): dynamic {
    return js("[]")
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isSafeInteger(value: dynamic): Boolean {
    return value > js("Number").MIN_SAFE_INTEGER && value < js("Number").MAX_SAFE_INTEGER
}

@Suppress("NOTHING_TO_INLINE")
private inline fun isInteger(value: dynamic): Boolean {
    return value % 1 === 0
}

external fun parseInt(value: Any, radix: Int): Int



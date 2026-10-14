/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.nvi.serialization.json

import kotlin.concurrent.AtomicInt
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

/**
 * KRJSON object 之上的惰性 Map，是 OHOS 平台 [JSONObject] 的底层容器之一。
 *
 * 与 Apple [LazyNSDictionaryMap] 同构：壳只 retain 当前 object 节点；读时按需转换；
 * 写时物化并释放原生引用。子 object/array 各自 retain 自己的值，父壳释放后仍可读。
 */
@OptIn(ExperimentalNativeApi::class)
internal class LazyJsonMap private constructor(
    private var native: Long,
    private var releaseToken: ValueRelease?,
    private var cleaner: Any?,
) : AbstractMutableMap<String, Any?>() {

    private class ValueRelease(private val bits: Long) {
        private val done = AtomicInt(0)

        fun releaseOnce() {
            if (done.compareAndSet(0, 1)) {
                JsonNative.release(bits)
            }
        }
    }

    private var containerCache: MutableMap<String, Any?>? = null

    /** key → 子值 borrowed 字；父壳仍 retain 期间有效。 */
    private var keyIndex: HashMap<String, Long>? = null

    private var orderedKeys: List<String>? = null

    private var materialized: MutableMap<String, Any?>? = null

    companion object {
        fun fromOwner(bits: Long): JSONObject {
            if (bits == 0L || JsonNative.isInvalid(bits)) {
                return JSONObject()
            }
            val held = JsonNative.retain(bits)
            if (JsonNative.type(held) != JSON_KIND_OBJECT) {
                JsonNative.release(held)
                return JSONObject()
            }
            return JSONObject(wrap(held))
        }

        fun fromOwnerAny(bits: Long): Any? {
            if (bits == 0L || JsonNative.isInvalid(bits)) {
                return null
            }
            val held = JsonNative.retain(bits)
            return when (JsonNative.type(held)) {
                JSON_KIND_ARRAY -> {
                    try {
                        LazyJsonList.fromOwner(held)
                    } finally {
                        JsonNative.release(held)
                    }
                }
                JSON_KIND_OBJECT -> JSONObject(wrap(held))
                else -> {
                    JsonNative.release(held)
                    null
                }
            }
        }

        /** 子 object：retain 该节点自身（borrowed 字也可）。 */
        internal fun fromValue(bits: Long): JSONObject {
            if (bits == 0L || JsonNative.isInvalid(bits)) {
                return JSONObject()
            }
            val held = JsonNative.retain(bits)
            if (JsonNative.type(held) != JSON_KIND_OBJECT) {
                JsonNative.release(held)
                return JSONObject()
            }
            return JSONObject(wrap(held))
        }

        private fun wrap(held: Long): LazyJsonMap {
            val token = ValueRelease(held)
            val cleaner = createCleaner(token) { it.releaseOnce() }
            return LazyJsonMap(held, token, cleaner)
        }
    }

    override val size: Int
        get() {
            materialized?.let { return it.size }
            keyIndex?.let { return it.size }
            return JsonNative.size(native)
        }

    override fun containsKey(key: String): Boolean {
        materialized?.let { return it.containsKey(key) }
        // A page normally reads only a handful of launch parameters. Building
        // a Kotlin HashMap for every key in a multi-megabyte payload before
        // the first read makes that cheap path O(all fields). Keep native
        // lookup lazy; keys()/entries() still build the index when traversal
        // actually requires it.
        return !JsonNative.isInvalid(JsonNative.objectGet(native, key))
    }

    override fun containsValue(value: Any?): Boolean {
        materialized?.let { return it.containsValue(value) }
        return entries.any { it.value == value }
    }

    override fun get(key: String): Any? {
        materialized?.let { return it[key] }
        return optFromNative(key)
    }

    private fun ensureKeyIndex(): HashMap<String, Long> {
        keyIndex?.let { return it }
        val index = HashMap<String, Long>()
        val keys = ArrayList<String>()
        val n = JsonNative.size(native)
        for (i in 0 until n) {
            val key = JsonNative.objectKeyAt(native, i) ?: continue
            if (!index.containsKey(key)) {
                index[key] = JsonNative.objectValueAt(native, i)
                keys.add(key)
            }
        }
        keyIndex = index
        orderedKeys = keys
        return index
    }

    private fun keyOrder(): List<String> {
        orderedKeys?.let { return it }
        ensureKeyIndex()
        return orderedKeys ?: emptyList()
    }

    /**
     * Reads a key by native insertion index without building [keyIndex].
     * This is intended for one-pass scans of a large lazy KRJSON object.
     */
    internal fun keyAt(index: Int): String? {
        materialized?.let {
            if (index < 0 || index >= it.size) {
                return null
            }
            return it.keys.elementAt(index)
        }
        if (index < 0 || native == 0L) {
            return null
        }
        return JsonNative.objectKeyAt(native, index)
    }

    /**
     * Reads a value by native insertion index without a secondary ObjectGet
     * lookup or [keyIndex] construction.
     */
    internal fun valueAt(index: Int): Any? {
        materialized?.let {
            if (index < 0 || index >= it.size) {
                return null
            }
            return it.values.elementAt(index)
        }
        if (index < 0 || native == 0L) {
            return null
        }
        val key = JsonNative.objectKeyAt(native, index) ?: return null
        // A handed-out child container may have been mutated; return that instance.
        containerCache?.let {
            if (it.containsKey(key)) {
                return it[key]
            }
        }
        val child = JsonNative.objectValueAt(native, index)
        return convertChild(key, child)
    }

    override fun put(key: String, value: Any?): Any? {
        return ensureMaterialized().put(key, value)
    }

    override fun remove(key: String): Any? {
        return ensureMaterialized().remove(key)
    }

    override fun clear() {
        ensureMaterialized().clear()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<String, Any?>>
        get() {
            materialized?.let { return it.entries }
            return LazyEntries()
        }

    private fun ensureMaterialized(): MutableMap<String, Any?> {
        materialized?.let { return it }
        val map: MutableMap<String, Any?> = JSONEngine.getMutableMap()
        for (key in keyOrder()) {
            map[key] = cachedOrConvert(key)
        }
        releaseNativeOwnership()
        materialized = map
        return map
    }

    private fun releaseNativeOwnership() {
        containerCache = null
        keyIndex = null
        orderedKeys = null
        native = 0L
        val token = releaseToken
        releaseToken = null
        cleaner = null
        token?.releaseOnce()
    }

    /**
     * Returns one additional owning reference while this map still directly
     * represents a KRJSON tree. Once a write materializes the Kotlin map,
     * callers must rebuild a native tree from Map entries instead.
     */
    internal fun retainNativeOrNull(): Long? {
        // Handed-out child containers may have been mutated; the native tree would be stale.
        if (materialized != null || containerCache != null || native == 0L) {
            return null
        }
        return JsonNative.retain(native)
    }

    private fun optFromNative(name: String): Any? {
        containerCache?.let {
            if (it.containsKey(name)) {
                return it[name]
            }
        }
        // Do not eagerly allocate keyIndex here. KRJSON object lookup is
        // native and allocation-free, whereas eagerly copying hundreds of
        // thousands of keys into Kotlin defeats the lazy bridge exactly when
        // a destination Pager only reads a few pageData fields.
        val child = JsonNative.objectGet(native, name)
        if (JsonNative.isInvalid(child)) {
            return null
        }
        return convertChild(name, child)
    }

    private fun cachedOrConvert(name: String): Any? {
        containerCache?.let {
            if (it.containsKey(name)) {
                return it[name]
            }
        }
        val child = ensureKeyIndex()[name] ?: return null
        return convertChild(name, child)
    }

    private fun convertChild(name: String, child: Long): Any? {
        if (JsonNative.isInvalid(child)) {
            return null
        }
        return when (JsonNative.type(child)) {
            JSON_KIND_NULL -> null
            JSON_KIND_BOOL -> JsonNative.asBool(child, false)
            JSON_KIND_INT, JSON_KIND_UINT, JSON_KIND_DOUBLE -> numberFromJson(child)
            JSON_KIND_LONG -> JsonNative.asInt(child)
            JSON_KIND_FLOAT -> JsonNative.asDouble(child, 0.0).toFloat()
            JSON_KIND_STRING, JSON_KIND_U16STRING -> JsonNative.asString(child)
            JSON_KIND_BYTES -> JsonNative.asByteArray(child)
            JSON_KIND_OBJECT -> cacheContainer(name, fromValue(child))
            JSON_KIND_ARRAY -> cacheContainer(name, LazyJsonList.fromValue(child))
            else -> null
        }
    }

    private fun <T> cacheContainer(key: String, value: T): T {
        val cache = containerCache ?: mutableMapOf<String, Any?>().also { containerCache = it }
        cache[key] = value
        return value
    }

    internal fun nativePrintCompactOrNull(): String? {
        if (materialized != null || containerCache != null || native == 0L) {
            return null
        }
        return JsonNative.print(native)
    }

    private inner class LazyEntries : AbstractMutableSet<MutableMap.MutableEntry<String, Any?>>() {
        override val size: Int
            get() = this@LazyJsonMap.size

        override fun add(element: MutableMap.MutableEntry<String, Any?>): Boolean {
            ensureMaterialized()[element.key] = element.value
            return true
        }

        override fun clear() {
            this@LazyJsonMap.clear()
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Any?>> {
            materialized?.let { return it.entries.iterator() }
            val keyIterator = keyOrder().iterator()
            return object : MutableIterator<MutableMap.MutableEntry<String, Any?>> {
                private var lastKey: String? = null

                override fun hasNext(): Boolean = keyIterator.hasNext()

                override fun next(): MutableMap.MutableEntry<String, Any?> {
                    val key = keyIterator.next()
                    lastKey = key
                    return LazyEntry(key)
                }

                override fun remove() {
                    val key = lastKey ?: throw IllegalStateException()
                    this@LazyJsonMap.remove(key)
                    lastKey = null
                }
            }
        }
    }

    private inner class LazyEntry(
        override val key: String,
    ) : MutableMap.MutableEntry<String, Any?> {
        override val value: Any?
            get() = cachedOrConvert(key)

        override fun setValue(newValue: Any?): Any? {
            val old = this@LazyJsonMap[key]
            this@LazyJsonMap[key] = newValue
            return old
        }
    }
}

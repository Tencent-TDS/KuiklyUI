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
 * OHOS-only lazy map backed by a shared_ptr cJSON owner.
 * Passed into the existing internal [JSONObject] map constructor — no commonMain
 * [JSONObject] API changes and no subclassing.
 *
 * Nested objects returned from [get] are themselves [JSONObject]s wrapping a new
 * [LazyCJsonMap] (own retain), so they survive parent materialize / release.
 */
@OptIn(ExperimentalNativeApi::class)
internal class LazyCJsonMap private constructor(
    private var ownerPtr: Long,
    private var nodePtr: Long,
    private var releaseToken: OwnerRelease?,
    /** Kept so Cleaner is not collected early. */
    private var cleaner: Any?,
) : AbstractMutableMap<String, Any?>() {

    private class OwnerRelease(private val ptr: Long) {
        private val done = AtomicInt(0)

        fun releaseOnce() {
            if (done.compareAndSet(0, 1) && ptr != 0L) {
                CJsonNative.release(ptr)
            }
        }
    }

    /** After first mutation (or forced materialize), native tree is released. */
    private var materialized: MutableMap<String, Any?>? = null

    companion object {
        fun fromOwner(ownerPtr: Long): JSONObject {
            if (ownerPtr == 0L) {
                return JSONObject()
            }
            val held = CJsonNative.retain(ownerPtr)
            if (held == 0L) {
                return JSONObject()
            }
            val root = CJsonNative.ownerRoot(held)
            if (root == 0L) {
                CJsonNative.release(held)
                return JSONObject()
            }
            return JSONObject(wrap(held, root))
        }

        private fun fromNode(ownerPtr: Long, nodePtr: Long): JSONObject {
            if (ownerPtr == 0L || nodePtr == 0L) {
                return JSONObject()
            }
            val held = CJsonNative.retain(ownerPtr)
            if (held == 0L) {
                return JSONObject()
            }
            return JSONObject(wrap(held, nodePtr))
        }

        private fun wrap(held: Long, nodePtr: Long): LazyCJsonMap {
            val token = OwnerRelease(held)
            val cleaner = createCleaner(token) { it.releaseOnce() }
            return LazyCJsonMap(held, nodePtr, token, cleaner)
        }
    }

    override val size: Int
        get() {
            materialized?.let { return it.size }
            return if (nodePtr != 0L) CJsonNative.size(nodePtr) else 0
        }

    override fun containsKey(key: String): Boolean {
        materialized?.let { return it.containsKey(key) }
        return nodePtr != 0L && CJsonNative.hasKey(nodePtr, key)
    }

    override fun containsValue(value: Any?): Boolean {
        materialized?.let { return it.containsValue(value) }
        return entries.any { it.value == value }
    }

    override fun get(key: String): Any? {
        materialized?.let { return it[key] }
        if (nodePtr == 0L) {
            return null
        }
        return optFromCJson(key)
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
        if (nodePtr != 0L) {
            val n = CJsonNative.size(nodePtr)
            for (i in 0 until n) {
                val key = CJsonNative.keyAt(nodePtr, i) ?: continue
                map[key] = optFromCJson(key)
            }
        }
        releaseNativeOwnership()
        materialized = map
        return map
    }

    private fun releaseNativeOwnership() {
        nodePtr = 0L
        ownerPtr = 0L
        val token = releaseToken
        releaseToken = null
        cleaner = null
        token?.releaseOnce()
    }

    private fun optFromCJson(name: String): Any? {
        return when (CJsonNative.valueKind(nodePtr, name)) {
            1 -> CJsonNative.getBool(nodePtr, name, false)
            2 -> {
                val d = CJsonNative.getNumber(nodePtr, name, 0.0)
                val asLong = d.toLong()
                if (d == asLong.toDouble() && asLong >= Int.MIN_VALUE && asLong <= Int.MAX_VALUE) {
                    asLong.toInt()
                } else if (d == asLong.toDouble()) {
                    asLong
                } else {
                    d
                }
            }
            3 -> CJsonNative.getString(nodePtr, name)
            4 -> {
                val child = CJsonNative.getObjectPtr(nodePtr, name)
                if (child == 0L || ownerPtr == 0L) {
                    null
                } else {
                    fromNode(ownerPtr, child)
                }
            }
            5 -> {
                val child = CJsonNative.getArrayPtr(nodePtr, name)
                if (child == 0L) {
                    null
                } else {
                    val printed = CJsonNative.print(child)
                    if (printed == null) {
                        null
                    } else {
                        try {
                            JSONArray(printed)
                        } catch (_: JSONException) {
                            null
                        }
                    }
                }
            }
            else -> null
        }
    }

    /**
     * Read-only entry view over cJSON. Any structural mutation materializes.
     */
    private inner class LazyEntries : AbstractMutableSet<MutableMap.MutableEntry<String, Any?>>() {
        override val size: Int
            get() = this@LazyCJsonMap.size

        override fun add(element: MutableMap.MutableEntry<String, Any?>): Boolean {
            ensureMaterialized()[element.key] = element.value
            return true
        }

        override fun clear() {
            this@LazyCJsonMap.clear()
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Any?>> {
            materialized?.let { return it.entries.iterator() }
            if (nodePtr == 0L) {
                return mutableListOf<MutableMap.MutableEntry<String, Any?>>().iterator()
            }
            val keys = ArrayList<String>(CJsonNative.size(nodePtr))
            val n = CJsonNative.size(nodePtr)
            for (i in 0 until n) {
                CJsonNative.keyAt(nodePtr, i)?.let { keys.add(it) }
            }
            val keyIter = keys.iterator()
            return object : MutableIterator<MutableMap.MutableEntry<String, Any?>> {
                private var lastKey: String? = null

                override fun hasNext(): Boolean = keyIter.hasNext()

                override fun next(): MutableMap.MutableEntry<String, Any?> {
                    val key = keyIter.next()
                    lastKey = key
                    return object : MutableMap.MutableEntry<String, Any?> {
                        override val key: String = key
                        override val value: Any?
                            get() = get(key)

                        override fun setValue(newValue: Any?): Any? {
                            val old = get(key)
                            put(key, newValue)
                            return old
                        }
                    }
                }

                override fun remove() {
                    val key = lastKey ?: throw IllegalStateException()
                    this@LazyCJsonMap.remove(key)
                    lastKey = null
                }
            }
        }
    }
}

/**
 * Factory name kept for TypeUtils / tests.
 */
internal object CJsonJSONObject {
    fun fromOwner(ownerPtr: Long): JSONObject {
        return LazyCJsonMap.fromOwner(ownerPtr)
    }
}

/**
 * Nested-object lifecycle checks for the lazy cJSON bridge.
 *
 * [ownerHandle] is a transferred shared_ptr handle from native (caller must not
 * use it after this returns; this function consumes it via retain + release).
 */
fun testOhosNestedCJsonLifecycle(ownerHandle: Long): String {
    if (ownerHandle == 0L) {
        return """{"ok":false,"error":"null owner"}"""
    }
    return try {
        val root = CJsonJSONObject.fromOwner(ownerHandle)
        CJsonNative.release(ownerHandle)

        val a = root.optInt("a")
        if (a != 1) {
            return """{"ok":false,"error":"a=$a"}"""
        }

        val child = root.optJSONObject("child")
            ?: return """{"ok":false,"error":"missing child"}"""
        if (child.nameValuePairs !is LazyCJsonMap) {
            return """{"ok":false,"error":"child not lazy cJSON map"}"""
        }
        val x = child.optString("x")
        if (x != "hello") {
            return """{"ok":false,"error":"x=$x"}"""
        }

        val grand = child.optJSONObject("grand")
            ?: return """{"ok":false,"error":"missing grand"}"""
        val y = grand.optInt("y")
        if (y != 42) {
            return """{"ok":false,"error":"y=$y"}"""
        }

        val blob = root.optString("blob")
        if (blob.length != 4096) {
            return """{"ok":false,"error":"blobLen=${blob.length}"}"""
        }

        root.put("_touch", 1)
        if (root.optInt("_touch") != 1) {
            return """{"ok":false,"error":"touch after materialize"}"""
        }
        val xAfter = child.optString("x")
        if (xAfter != "hello") {
            return """{"ok":false,"error":"child UAF after parent materialize x=$xAfter"}"""
        }
        val yAfter = grand.optInt("y")
        if (yAfter != 42) {
            return """{"ok":false,"error":"grand UAF after parent materialize y=$yAfter"}"""
        }

        child.put("_childTouch", true)
        val yAfterChild = grand.optInt("y")
        if (yAfterChild != 42) {
            return """{"ok":false,"error":"grand UAF after child materialize y=$yAfterChild"}"""
        }

        """{"ok":true,"a":$a,"x":"$x","y":$y,"blob":${blob.length}}"""
    } catch (t: Throwable) {
        """{"ok":false,"error":"${t.message}"}"""
    }
}

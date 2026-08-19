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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSArray
import platform.Foundation.NSDictionary
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSNull
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding

/**
 * Apple-only lazy map backed by [NSDictionary].
 * Passed into the existing internal [JSONObject] map constructor — no commonMain
 * [JSONObject] API changes and no subclassing.
 *
 * Nested dictionaries become [JSONObject]s wrapping a new [LazyNSDictionaryMap].
 */
@OptIn(ExperimentalForeignApi::class)
internal class LazyNSDictionaryMap private constructor(
    private var dict: NSDictionary?,
) : AbstractMutableMap<String, Any?>() {

    private var materialized: MutableMap<String, Any?>? = null

    companion object {
        fun from(dictionary: NSDictionary): LazyNSDictionaryMap {
            return LazyNSDictionaryMap(dictionary)
        }

        fun wrapAsJSONObject(dictionary: NSDictionary): JSONObject {
            return JSONObject(from(dictionary))
        }
    }

    override val size: Int
        get() {
            materialized?.let { return it.size }
            return dict?.count?.toInt() ?: 0
        }

    override fun containsKey(key: String): Boolean {
        materialized?.let { return it.containsKey(key) }
        val d = dict ?: return false
        return d.objectForKey(key) != null
    }

    override fun containsValue(value: Any?): Boolean {
        materialized?.let { return it.containsValue(value) }
        return entries.any { it.value == value }
    }

    override fun get(key: String): Any? {
        materialized?.let { return it[key] }
        val d = dict ?: return null
        return convertOcValue(d.objectForKey(key))
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

    private fun forEachKey(d: NSDictionary, block: (Any) -> Unit) {
        val enumerator = d.keyEnumerator()
        while (true) {
            val keyObj = enumerator.nextObject() ?: break
            block(keyObj)
        }
    }

    private fun ensureMaterialized(): MutableMap<String, Any?> {
        materialized?.let { return it }
        val map: MutableMap<String, Any?> = JSONEngine.getMutableMap()
        val d = dict
        if (d != null) {
            forEachKey(d) { keyObj ->
                val key = keyObj as? String ?: keyObj.toString()
                map[key] = convertOcValue(d.objectForKey(keyObj))
            }
        }
        dict = null
        materialized = map
        return map
    }

    private fun convertOcValue(value: Any?): Any? {
        if (value == null || value is NSNull) {
            return null
        }
        return when (value) {
            is JSONObject, is JSONArray -> value
            is NSDictionary -> wrapAsJSONObject(value)
            is NSArray -> arrayToJSONArray(value)
            is NSString -> value.toString()
            is String -> value
            is NSNumber -> numberToKotlin(value)
            is Boolean -> value
            is Number -> value
            is Map<*, *> -> {
                val mutable: MutableMap<String, Any?> = JSONEngine.getMutableMap()
                for ((k, v) in value) {
                    val key = k as? String ?: continue
                    mutable[key] = convertOcValue(v as Any?)
                }
                JSONObject(mutable)
            }
            else -> value.toString()
        }
    }

    private fun numberToKotlin(num: NSNumber): Any {
        val dbl = num.doubleValue
        val asLong = dbl.toLong()
        if (dbl == asLong.toDouble()) {
            if (asLong >= Int.MIN_VALUE && asLong <= Int.MAX_VALUE) {
                return asLong.toInt()
            }
            return asLong
        }
        return dbl
    }

    private fun arrayToJSONArray(array: NSArray): JSONArray {
        val list: MutableList<Any?> = JSONEngine.getMutableList()
        val n = array.count.toInt()
        for (i in 0 until n) {
            list.add(convertOcValue(array.objectAtIndex(i.toULong())))
        }
        return JSONArray(list)
    }

    private inner class LazyEntries : AbstractMutableSet<MutableMap.MutableEntry<String, Any?>>() {
        override val size: Int
            get() = this@LazyNSDictionaryMap.size

        override fun add(element: MutableMap.MutableEntry<String, Any?>): Boolean {
            ensureMaterialized()[element.key] = element.value
            return true
        }

        override fun clear() {
            this@LazyNSDictionaryMap.clear()
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, Any?>> {
            materialized?.let { return it.entries.iterator() }
            val d = dict
            if (d == null) {
                return mutableListOf<MutableMap.MutableEntry<String, Any?>>().iterator()
            }
            val keyList = ArrayList<String>(d.count.toInt())
            forEachKey(d) { keyObj ->
                keyList.add(keyObj as? String ?: keyObj.toString())
            }
            val keyIter = keyList.iterator()
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
                    this@LazyNSDictionaryMap.remove(key)
                    lastKey = null
                }
            }
        }
    }
}

/**
 * Nested-object lifecycle checks for the lazy NSDictionary bridge (demo/test).
 */
@OptIn(ExperimentalForeignApi::class)
fun testAppleNestedNSDictionaryLifecycle(): String {
    return try {
        val blob = "z".repeat(4096)
        val json = """{"a":1,"child":{"x":"hello","grand":{"y":42}},"blob":"$blob"}"""
        val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return """{"ok":false,"error":"encode failed"}"""
        val parsed = NSJSONSerialization.JSONObjectWithData(data, 0u, null)
            ?: return """{"ok":false,"error":"parse failed"}"""
        val rootDict = parsed as? NSDictionary
            ?: return """{"ok":false,"error":"not dict"}"""
        val root = LazyNSDictionaryMap.wrapAsJSONObject(rootDict)

        val a = root.optInt("a")
        if (a != 1) {
            return """{"ok":false,"error":"a=$a"}"""
        }

        val childObj = root.optJSONObject("child")
            ?: return """{"ok":false,"error":"missing child"}"""
        if (childObj.nameValuePairs !is LazyNSDictionaryMap) {
            return """{"ok":false,"error":"child not lazy nsdict map"}"""
        }
        val x = childObj.optString("x")
        if (x != "hello") {
            return """{"ok":false,"error":"x=$x"}"""
        }

        val grandObj = childObj.optJSONObject("grand")
            ?: return """{"ok":false,"error":"missing grand"}"""
        val y = grandObj.optInt("y")
        if (y != 42) {
            return """{"ok":false,"error":"y=$y"}"""
        }

        val blobStr = root.optString("blob")
        if (blobStr.length != 4096) {
            return """{"ok":false,"error":"blobLen=${blobStr.length}"}"""
        }

        root.put("_touch", 1)
        if (root.optInt("_touch") != 1) {
            return """{"ok":false,"error":"touch after materialize"}"""
        }
        val xAfter = childObj.optString("x")
        if (xAfter != "hello") {
            return """{"ok":false,"error":"child UAF after parent materialize x=$xAfter"}"""
        }
        val yAfter = grandObj.optInt("y")
        if (yAfter != 42) {
            return """{"ok":false,"error":"grand UAF after parent materialize y=$yAfter"}"""
        }

        childObj.put("_childTouch", true)
        val yAfterChild = grandObj.optInt("y")
        if (yAfterChild != 42) {
            return """{"ok":false,"error":"grand UAF after child materialize y=$yAfterChild"}"""
        }

        """{"ok":true,"a":$a,"x":"$x","y":$y,"blob":${blobStr.length}}"""
    } catch (t: Throwable) {
        """{"ok":false,"error":"${t.message}"}"""
    }
}

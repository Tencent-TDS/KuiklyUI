package com.tencent.kuikly.core.collection

class FastLinkedHashMap<K, V> : MutableMap<K, V> {
    var fastMap: dynamic

    constructor() {
        fastMap = js("new Map()")
    }

    constructor(original: Map<out K, V>) {
        fastMap = js("new Map()")
        original.forEach { (key, value) ->
            fastMap.set(key, value).unsafeCast<Unit>()
        }
    }

   override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = keys.map { key ->
            object : MutableMap.MutableEntry<K, V> {
                override val key: K = key
                override val value: V
                    get() = fastMap.get(key).unsafeCast<V>()

                override fun setValue(newValue: V): V {
                    val oldValue = fastMap.get(key).unsafeCast<V>()
                    fastMap.set(key,newValue)
                    return oldValue
                }
            }
        }.toHashSet()
   override val keys: MutableSet<K>
        get() {
            val keySet = FastHashSet<K>()
            keySet.hashSet = js("function() { return new Set(this) }").call(fastMap.keys())
            return keySet
        }

   override val size: Int
        get() = fastMap.size.unsafeCast<Int>()

   override val values: MutableCollection<V>
        get() {
            val mapValues = js("Array.from")(fastMap.values()).unsafeCast<Array<V>>()
            val fastList =  FastArrayList<V>()
            fastList.fastList = mapValues
            return fastList
        }

   override fun clear() {
       fastMap.clear()
   }

   override fun isEmpty(): Boolean {
        return size == 0
   }

   override fun remove(key: K): V? {
        val value = fastMap.get(key).unsafeCast<V?>()
        fastMap.delete(key)
        return value
   }

   override fun putAll(from: Map<out K, V>) {
        from.forEach { (key, value) ->
            put(key, value)
        }
   }

   override fun put(key: K, value: V): V? {
        fastMap.set(key, value)
        return value
   }

   override fun get(key: K): V? {
        return fastMap.get(key).unsafeCast<V?>()
   }

   override fun containsValue(value: V): Boolean {
        val mapValues = js("Array.from")(fastMap.values()).unsafeCast<Array<V>>()
        return mapValues.asDynamic().some { v->
            value === v
        }.unsafeCast<Boolean>()
   }

   override fun containsKey(key: K): Boolean {
        return fastMap.has(key).unsafeCast<Boolean>()
   }

    fun toNewMap(): Map<K, V> {
        val newJsFastMap = js("function() { return new Map(this) }").call(fastMap)
        val newMap = FastLinkedHashMap<K,V>()
        newMap.fastMap = newJsFastMap
        return newMap
    }
}

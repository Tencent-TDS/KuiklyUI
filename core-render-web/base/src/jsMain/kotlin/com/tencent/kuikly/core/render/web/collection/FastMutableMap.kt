package com.tencent.kuikly.core.render.web.collection

fun <K, V> fastMutableMapOf() = FastMutableMap<K, V>(js("{}"))

class FastMutableMap<K, V>(map: dynamic) : MutableMap<K, V> {

    var fastMap: dynamic = map

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
    val jsKeys: dynamic
        get() = getObjectKeys(fastMap)
    override val size: Int
        get() = getObjectKeys(fastMap).size
    override val values: MutableCollection<V>
        get() = getObjectValue(fastMap).unsafeCast<Array<V>>().toMutableList()

    override fun clear() {
        getObjectKeys(fastMap).forEach { key ->
            deleteObjectValueWithKey(fastMap, key)
        }
    }

    override fun isEmpty(): Boolean = getObjectKeys(fastMap).isEmpty()

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

    override fun get(key: K): V? = fastMap[key].unsafeCast<V>()

    override fun containsValue(value: V): Boolean = value in getObjectValue(fastMap)

    override fun containsKey(key: K): Boolean = key.toString() in getObjectKeys(fastMap)
}


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

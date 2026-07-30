package com.tencent.kuikly.core.nvi.serialization.json

expect object JSONEngine {

    fun parse(jsonStr: String): Any?

    fun stringify(jsonObject: JSONObject): String

    fun stringify(jsonArray: JSONArray): String

    internal fun <K, V> getMutableMap(): MutableMap<K, V>

    internal fun <E> getMutableList(): MutableList<E>
}

fun commonStringify(jsonObject: JSONObject): String {
    val stringer = JSONStringer()
    jsonObject.writeTo(stringer)
    return stringer.toString()
}

fun commonStringify(jsonObject: JSONArray): String {
    val stringer = JSONStringer()
    jsonObject.writeTo(stringer)
    return stringer.toString()
}
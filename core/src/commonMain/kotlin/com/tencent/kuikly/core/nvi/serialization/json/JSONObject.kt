package com.tencent.kuikly.core.nvi.serialization.json

/**
 * Created by kam on 2022/4/11.
 *
 * 各平台底层容器不同（Android/JVM/JS：Kotlin 集合；Apple：Foundation 字典；
 * OHOS：cJSON 树），故声明为 expect 类，公共读写实现见 [AbstractJSONObject]。
 */
expect class JSONObject {

    internal constructor(nameValuePairs: MutableMap<String, Any?>)

    constructor()

    @Throws(JSONException::class)
    constructor(json: String)

    @Throws(JSONException::class)
    constructor(jsonTokener: JSONTokener)

    internal var nameValuePairs: MutableMap<String, Any?>

    fun length(): Int

    /** Returns the top-level key at insertion index, or null when out of range. */
    fun keyAt(index: Int): String?

    fun put(name: String, value: Boolean): JSONObject

    fun put(name: String, value: Int): JSONObject

    fun put(name: String, value: Long): JSONObject

    fun put(name: String, value: Double): JSONObject

    fun put(name: String, value: Any?): JSONObject

    fun has(name: String): Boolean

    fun opt(name: String): Any?

    /** Returns the top-level value at insertion index, or null when out of range. */
    fun opt(index: Int): Any?

    fun optBoolean(name: String): Boolean

    fun optBoolean(name: String, fallback: Boolean): Boolean

    fun optDouble(name: String): Double

    fun optDouble(name: String, fallback: Double): Double

    fun optInt(name: String): Int

    fun optInt(name: String, fallback: Int): Int

    fun optLong(name: String): Long

    fun optLong(name: String, fallback: Long): Long

    fun optString(name: String): String

    fun optString(name: String, fallback: String): String

    fun optJSONArray(name: String): JSONArray?

    fun optJSONObject(name: String): JSONObject?

    fun keys(): Iterator<String>

    fun keySet(): Set<String>

    fun toMap(): MutableMap<String, Any>

    @Throws(JSONException::class)
    fun writeTo(stringer: JSONStringer)

    companion object {
        fun quote(data: String?): String
    }
}

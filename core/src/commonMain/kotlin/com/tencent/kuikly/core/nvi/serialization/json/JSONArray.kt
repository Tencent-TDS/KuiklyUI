package com.tencent.kuikly.core.nvi.serialization.json

/**
 * Created by kam on 2022/4/11.
 *
 * 各平台底层容器不同（Android/JVM/JS：Kotlin 集合；Apple：Foundation 数组；
 * OHOS：cJSON 树），故声明为 expect 类，公共读写实现见 [AbstractJSONArray]。
 */
expect class JSONArray {

    internal constructor(values: MutableList<Any?>)

    constructor()

    @Throws(JSONException::class)
    constructor(json: String)

    @Throws(JSONException::class)
    constructor(jsonTokener: JSONTokener)

    internal var values: MutableList<Any?>

    fun length(): Int

    fun put(value: Boolean): JSONArray

    fun put(value: Double): JSONArray

    fun put(value: Int): JSONArray

    fun put(value: Long): JSONArray

    fun put(value: Any?): JSONArray

    fun opt(index: Int): Any?

    fun remove(index: Int): Any?

    fun optBoolean(index: Int): Boolean

    fun optBoolean(index: Int, fallback: Boolean): Boolean

    fun optDouble(index: Int): Double

    fun optDouble(index: Int, fallback: Double): Double

    fun optInt(index: Int): Int

    fun optInt(index: Int, fallback: Int): Int

    fun optLong(index: Int): Long

    fun optLong(index: Int, fallback: Long): Long

    fun optString(index: Int): String?

    fun optString(index: Int, fallback: String?): String?

    fun optJSONArray(index: Int): JSONArray?

    fun optJSONObject(index: Int): JSONObject?

    fun toList(): MutableList<Any>

    @Throws(JSONException::class)
    fun writeTo(stringer: JSONStringer)
}

package com.tencent.kuikly.core.nvi.serialization.json

/**
 * 宽松扫描逻辑在 [AbstractJSONTokener]；声明为 expect 类，使各平台可以在此之上
 * 叠加原生解析快路径（Apple `NSJSONSerialization`、OHOS cJSON），并在原生解析
 * 不支持的历史写法上回退到宽松扫描。
 */
expect class JSONTokener(json: String) {

    @Throws(JSONException::class)
    fun nextValue(): Any?

    @Throws(JSONException::class)
    fun nextString(quote: Char): String
}

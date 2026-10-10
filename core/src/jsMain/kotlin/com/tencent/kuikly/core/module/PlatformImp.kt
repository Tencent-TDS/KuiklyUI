package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject


actual fun Any.toPlatformObject(): Any {
    if (this is List<*>) {
        return this.toTypedArray()
    }
    return this
}

actual fun Any.toKotlinObject(): Any {
    return this
}

internal actual fun platformOpenPageParams(
    pageName: String,
    pageData: JSONObject?,
    routeStartTimestampMs: Long,
): Any {
    return stringifyOpenPageParams(pageName, pageData)
}

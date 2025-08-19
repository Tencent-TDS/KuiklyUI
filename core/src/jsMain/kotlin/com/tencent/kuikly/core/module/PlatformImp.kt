package com.tencent.kuikly.core.module


actual fun Any.toPlatformObject(): Any {
    if (this is List<*>) {
        return this.toTypedArray()
    }
    return this
}

actual fun Any.toKotlinObject(): Any {
    return this
}
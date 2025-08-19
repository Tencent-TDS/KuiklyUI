package com.tencent.kuikly.compose.ui.platform

internal actual fun simpleIdentityToString(obj: Any, name: String?): String {
    val className = name ?: "<object>"
    return "$className@${obj.hashCode()}"
}

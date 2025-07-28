package com.tencent.kuikly.demo.pages.compose.chatDemo

internal actual fun getPlatform(): String = "ohos"

internal actual object NetworkClient {
    actual val client: Any?
        get() = null
}
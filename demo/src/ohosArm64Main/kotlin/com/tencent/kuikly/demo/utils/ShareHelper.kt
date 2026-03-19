package com.tencent.kuikly.demo.utils

actual fun onShareAppMessage(block: (Any?) -> ShareConfig?) {
    // No-op for HarmonyOS
}

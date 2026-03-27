package com.tencent.kuikly.compose.ui.node

// TODO mark internal once https://youtrack.jetbrains.com/issue/KT-36695 is fixed
actual class WeakReference<T : Any> actual constructor(referent: T) {
    private var instance: T? = referent

    actual fun clear() {
        instance = null
    }

    actual fun get(): T? = instance
}

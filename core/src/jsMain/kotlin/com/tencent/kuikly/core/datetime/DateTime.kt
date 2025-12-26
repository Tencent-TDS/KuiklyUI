package com.tencent.kuikly.core.datetime

import kotlin.js.Date

actual object DateTime {
    actual fun currentTimestamp(): Long {
        return Date().getTime().toLong()
    }

    actual fun nanoTime(): Long {
        return Date.now().toLong() * 1_000_000
    }

    internal actual fun threadLocalTimestamp(): Long {
        return currentTimestamp()
    }

}
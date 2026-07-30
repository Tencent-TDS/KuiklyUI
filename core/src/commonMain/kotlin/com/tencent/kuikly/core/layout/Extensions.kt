package com.tencent.kuikly.core.layout

import kotlin.math.abs

inline fun Float.isUndefined(): Boolean {
    return isNaN()
}

inline val Float.Companion.undefined: Float
    get() = NaN

inline fun Float.valueEquals(other: Float): Boolean {
    if (isUndefined() || other.isUndefined()) {
        return isUndefined() && other.isUndefined()
    }
    return abs(other - this) < 0.00001f
}

inline fun Float.layoutSizeEqual(other: Float): Boolean {
    return if (isUndefined() || other.isUndefined()) {
        false
    } else {
        abs(other - this) < 0.00001f
    }
}
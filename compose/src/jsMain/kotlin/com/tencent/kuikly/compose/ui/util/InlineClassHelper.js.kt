/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

@file:NoLiveLiterals

package com.tencent.kuikly.compose.ui.util

import androidx.compose.runtime.NoLiveLiterals
import kotlin.math.floor

/*
 * JS-side implementation of the platform-abstract packed-value helpers.
 *
 * On the JVM/Native the backing of `PackedFloats`/`PackedInts` is `Long`; pack/unpack
 * is a handful of primitive bit operations.
 *
 * On Kotlin/JS, `Long` is a `{ low, high }` object with software-emulated 64-bit
 * arithmetic. Every `new Long(...)`, `Long.shl`, `Long.and`, `Long.equals` etc.
 * adds GC pressure and CPU cost that dominates high-frequency layout/gesture paths.
 *
 * This actual implementation sidesteps Long entirely by reinterpreting bits through
 * shared TypedArray views:
 *
 *   - `Float32Array(2)` + `Float64Array(buffer)`: write two Floats, read one Double back.
 *   - `Int` pairs use a JS safe-integer fast path and only allocate for rare full-range values.
 *
 * Kotlin/JS is single-threaded (each Web Worker has its own realm), so top-level
 * shared views are safe. The pack/unpack functions are purely synchronous and
 * hold no observable reentrant state between calls.
 */

// All TypedArray views are created inside a single js() call and returned as one
// object. This avoids cross-referencing Kotlin `val` names from inside `js("...")`
// strings — the Kotlin/JS compiler mangles top-level property names, so
// `js("new Float32Array(floatBuffer)")` fails at runtime with
// `ReferenceError: Can't find variable: floatBuffer`.
//
// By constructing the ArrayBuffer and all its views in the same js() expression,
// everything stays within one JavaScript scope and no Kotlin names leak in.
private val floatViews: dynamic = js("""
(function() {
    var buf = new ArrayBuffer(8);
    return { f32: new Float32Array(buf), f64: new Float64Array(buf), i32: new Int32Array(buf) };
})()
""")
private val intViews: dynamic = js("""
(function() {
    return {
        pack: function(x, y) { return { x: x | 0, y: y | 0 }; },
        x: function(value) { return value.x | 0; },
        y: function(value) { return value.y | 0; },
        equals: function(a, b) { return (a.x | 0) === (b.x | 0) && (a.y | 0) === (b.y | 0); }
    };
})()
""")

actual fun packFloatsP(val1: Float, val2: Float): PackedFloats {
    val views = floatViews
    val f32 = views.f32
    f32[0] = val1
    f32[1] = val2
    val value = views.f64[0]
    if (value == value) {
        return value.unsafeCast<Double>()
    }
    val i32 = views.i32
    return intViews.pack(i32[0], i32[1]).unsafeCast<Any>()
}

actual fun unpackFloat1P(value: PackedFloats): Float {
    val views = floatViews
    if (value is Double) {
        views.f64[0] = value
    } else {
        views.i32[0] = intViews.x(value)
        views.i32[1] = intViews.y(value)
    }
    return views.f32[0].unsafeCast<Float>()
}

actual fun unpackFloat2P(value: PackedFloats): Float {
    val views = floatViews
    if (value is Double) {
        views.f64[0] = value
    } else {
        views.i32[0] = intViews.x(value)
        views.i32[1] = intViews.y(value)
    }
    return views.f32[1].unsafeCast<Float>()
}

actual fun packedFloatsBitEquals(a: PackedFloats, b: PackedFloats): Boolean {
    if (a is Double && b is Double) {
        if (a != b) {
            return false
        }
        // Bit-level equality that handles NaN correctly.
        // IEEE-754 says `NaN != NaN`, but `Offset.Unspecified` relies on bit equality.
        val views = floatViews
        val f64 = views.f64
        val i32 = views.i32
        f64[0] = a
        val aLo = i32[0].unsafeCast<Int>()
        val aHi = i32[1].unsafeCast<Int>()
        f64[0] = b
        val bLo = i32[0].unsafeCast<Int>()
        val bHi = i32[1].unsafeCast<Int>()
        return aLo == bLo && aHi == bHi
    }
    if (a is Double || b is Double) {
        return false
    }
    return intViews.equals(a, b).unsafeCast<Boolean>()
}

actual fun packIntsP(val1: Int, val2: Int): PackedInts {
    if (val1 >= INT_PACKED_FAST_MIN && val1 <= INT_PACKED_FAST_MAX &&
        val2 >= INT_PACKED_FAST_MIN && val2 <= INT_PACKED_FAST_MAX
    ) {
        return ((val1 - INT_PACKED_FAST_MIN).toDouble() * INT_PACKED_FAST_BASE +
            (val2 - INT_PACKED_FAST_MIN).toDouble()).unsafeCast<Double>()
    }
    return intViews.pack(val1, val2).unsafeCast<Any>()
}

actual fun unpackInt1P(value: PackedInts): Int {
    if (value is Double) {
        return floor(value / INT_PACKED_FAST_BASE).toInt() + INT_PACKED_FAST_MIN
    }
    return intViews.x(value).unsafeCast<Int>()
}

actual fun unpackInt2P(value: PackedInts): Int {
    if (value is Double) {
        return (value % INT_PACKED_FAST_BASE).toInt() + INT_PACKED_FAST_MIN
    }
    return intViews.y(value).unsafeCast<Int>()
}

actual fun packedIntsBitEquals(a: PackedInts, b: PackedInts): Boolean {
    if (a is Double && b is Double) {
        return a == b
    }
    if (a is Double || b is Double) {
        return false
    }
    return intViews.equals(a, b).unsafeCast<Boolean>()
}

private const val INT_PACKED_FAST_BITS = 26
private const val INT_PACKED_FAST_BASE = 67108864.0
private const val INT_PACKED_FAST_MIN = -33554432
private const val INT_PACKED_FAST_MAX = 33554431

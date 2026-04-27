/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.ui.util

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
 *   - `Int32Array(2)` + `Float64Array(buffer)`: write two Ints, read one Double back.
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
    var buf = new ArrayBuffer(8);
    return { i32: new Int32Array(buf), f64: new Float64Array(buf) };
})()
""")

// Convenience accessors — no overhead, these are just property reads on a plain JS object.
private inline val f32: dynamic get() = floatViews.f32
private inline val f64FromF32: dynamic get() = floatViews.f64
private inline val i32FromF32: dynamic get() = floatViews.i32
private inline val i32: dynamic get() = intViews.i32
private inline val f64FromI32: dynamic get() = intViews.f64

actual fun packFloatsP(val1: Float, val2: Float): PackedFloats {
    f32[0] = val1
    f32[1] = val2
    return f64FromF32[0].unsafeCast<Double>()
}

actual fun unpackFloat1P(value: PackedFloats): Float {
    f64FromF32[0] = value
    return f32[0].unsafeCast<Float>()
}

actual fun unpackFloat2P(value: PackedFloats): Float {
    f64FromF32[0] = value
    return f32[1].unsafeCast<Float>()
}

actual fun packedFloatsBitEquals(a: PackedFloats, b: PackedFloats): Boolean {
    // Bit-level equality that handles NaN correctly.
    // IEEE-754 says `NaN != NaN`, but `Offset.Unspecified` relies on bit equality.
    f64FromF32[0] = a
    val aLo = i32FromF32[0].unsafeCast<Int>()
    val aHi = i32FromF32[1].unsafeCast<Int>()
    f64FromF32[0] = b
    val bLo = i32FromF32[0].unsafeCast<Int>()
    val bHi = i32FromF32[1].unsafeCast<Int>()
    return aLo == bLo && aHi == bHi
}

actual fun packIntsP(val1: Int, val2: Int): PackedInts {
    // val1 stored at slot 0, val2 at slot 1. unpackInt1P reads slot 0, unpackInt2P reads slot 1.
    i32[0] = val1
    i32[1] = val2
    return f64FromI32[0].unsafeCast<Double>()
}

actual fun unpackInt1P(value: PackedInts): Int {
    f64FromI32[0] = value
    return i32[0].unsafeCast<Int>()
}

actual fun unpackInt2P(value: PackedInts): Int {
    f64FromI32[0] = value
    return i32[1].unsafeCast<Int>()
}

actual fun packedIntsBitEquals(a: PackedInts, b: PackedInts): Boolean {
    f64FromI32[0] = a
    val aLo = i32[0].unsafeCast<Int>()
    val aHi = i32[1].unsafeCast<Int>()
    f64FromI32[0] = b
    val bLo = i32[0].unsafeCast<Int>()
    val bHi = i32[1].unsafeCast<Int>()
    return aLo == bLo && aHi == bHi
}

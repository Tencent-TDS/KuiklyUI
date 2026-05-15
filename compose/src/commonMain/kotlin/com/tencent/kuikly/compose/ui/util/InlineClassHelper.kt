/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

@file:Suppress("NOTHING_TO_INLINE")

package com.tencent.kuikly.compose.ui.util

// =============================================================================
// Legacy Long-packed helpers.
//
// These functions are preserved verbatim so that existing call-sites in
// `commonMain` (colorspace connectors, SpringSimulation, SliderRange,
// LazyStaggeredGridMeasure.SpanRange, HitTestResult.DistanceAndInLayer, etc.)
// continue to work unchanged.
//
// For high-frequency value classes where JS Long cost matters (Offset, Size,
// IntOffset, ...) use the `packFloatsP`/`packIntsP` variants below, which
// return the platform-abstract `PackedFloats` / `PackedInts` types.
// =============================================================================

/**
 * Packs two Float values into one Long value for use in inline classes.
 */
inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toRawBits().toLong()
    val v2 = val2.toRawBits().toLong()
    return v1.shl(32) or (v2 and 0xFFFFFFFF)
}

/**
 * Unpacks the first Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat1(value: Long): Float {
    return Float.fromBits(value.shr(32).toInt())
}

/**
 * Unpacks the second Float value in [packFloats] from its returned Long.
 */
inline fun unpackFloat2(value: Long): Float {
    return Float.fromBits(value.and(0xFFFFFFFF).toInt())
}

/**
 * Packs two Int values into one Long value for use in inline classes.
 */
inline fun packInts(val1: Int, val2: Int): Long {
    return val1.toLong().shl(32) or (val2.toLong() and 0xFFFFFFFF)
}

/**
 * Unpacks the first Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt1(value: Long): Int {
    return value.shr(32).toInt()
}

/**
 * Unpacks the second Int value in [packInts] from its returned ULong.
 */
inline fun unpackInt2(value: Long): Int {
    return value.and(0xFFFFFFFF).toInt()
}

// =============================================================================
// Platform-abstract packed helpers.
//
// `PackedFloats` / `PackedInts` alias to `Long` on JVM/Native/HarmonyOS (so
// the generated code is bit-identical to the legacy path) and to `Double` on
// Kotlin/JS (which sidesteps the costly Long software emulation).
//
// Use these from value classes whose packedValue lives on the JS hot path.
// =============================================================================

/**
 * Packs two Float values into a [PackedFloats] backed by the most efficient
 * native representation on the current target.
 *
 * On JVM/Native this is literally identical to [packFloats] — `PackedFloats`
 * aliases to `Long` and the Kotlin compiler inlines the bit math.
 * On JS the returned `Double` is produced through shared `Float32Array`/
 * `Float64Array` views with zero `Long` allocations.
 */
expect fun packFloatsP(val1: Float, val2: Float): PackedFloats

/** Unpacks the first Float packed by [packFloatsP]. */
expect fun unpackFloat1P(value: PackedFloats): Float

/** Unpacks the second Float packed by [packFloatsP]. */
expect fun unpackFloat2P(value: PackedFloats): Float

/**
 * Bit-level equality for [PackedFloats].
 *
 * This is **not** `==` semantics: two packed values that encode `(NaN, NaN)`
 * compare equal under [packedFloatsBitEquals] on all platforms, whereas
 * `Double == Double` in JS would return `false` for NaN per IEEE-754.
 *
 * Use this for sentinel comparisons such as `Offset.Unspecified`.
 *
 * Declared as a top-level function (not an extension) so that after typealias
 * expansion on JVM/Native — where both `PackedFloats` and `PackedInts` become
 * `Long` — the overload with [packedIntsBitEquals] doesn't collide.
 */
expect fun packedFloatsBitEquals(a: PackedFloats, b: PackedFloats): Boolean

/** See [packFloatsP]. */
expect fun packIntsP(val1: Int, val2: Int): PackedInts

/** Unpacks the first Int packed by [packIntsP]. */
expect fun unpackInt1P(value: PackedInts): Int

/** Unpacks the second Int packed by [packIntsP]. */
expect fun unpackInt2P(value: PackedInts): Int

/** Bit-level equality for [PackedInts]. See [packedFloatsBitEquals] for rationale. */
expect fun packedIntsBitEquals(a: PackedInts, b: PackedInts): Boolean

/*
 * Copyright (C) Tencent. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

package com.tencent.kuikly.compose.ui.util

/**
 * Platform-abstract container for two packed Float values.
 *
 * On JVM / Native / HarmonyOS: aliased to `Long` (zero-overhead packed bits).
 * On JS: aliased to `Double` (uses Float32Array/Float64Array view to avoid
 * Kotlin/JS Long software emulation, which causes high GC pressure and CPU cost).
 *
 * This type is opaque in common code — interact with it only through
 * [packFloats], [unpackFloat1], [unpackFloat2], [bitEquals] in
 * [InlineClassHelper].
 */
expect class PackedFloats

/**
 * Platform-abstract container for two packed Int values.
 *
 * On JVM / Native / HarmonyOS: aliased to `Long`.
 * On JS: aliased to `Double` (Int32 full range fits in IEEE 754 double's 53-bit mantissa
 * when split into two halves via Int32Array view).
 */
expect class PackedInts

/**
 * Platform-abstract container for a TextUnit-shaped value (Float + 2-bit type flag).
 *
 * On JVM / Native / HarmonyOS: aliased to `Long`.
 * On JS: aliased to `Double`.
 *
 * Reserved for the future TextUnit migration; not used in the first phase of the
 * packed-value-types refactor.
 */
expect class PackedTextUnit

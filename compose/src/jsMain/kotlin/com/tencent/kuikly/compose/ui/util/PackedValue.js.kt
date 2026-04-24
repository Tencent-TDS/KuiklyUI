/*
 * Copyright (C) Tencent. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.ui.util

/**
 * On JS, the backing storage is a plain `Double`. Float packing is performed by
 * reinterpreting the bits of two consecutive `Float32Array` slots as one
 * `Float64Array` slot; this avoids any Kotlin/JS Long allocation or soft-math.
 */
actual typealias PackedFloats = Double

/** Integer packing uses `Double` as well so that a full pair of `Int` survives round-trip. */
actual typealias PackedInts = Double

/** Reserved for future TextUnit migration. */
actual typealias PackedTextUnit = Double

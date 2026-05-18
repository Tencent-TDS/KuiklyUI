/*
 * Copyright (C) Tencent. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package com.tencent.kuikly.compose.ui.util

/**
 * On JS, packed values are stored as lightweight JS values instead of Kotlin/JS `Long`.
 * Most packed float pairs use a `Double` bit reinterpretation fast path; rare bit patterns
 * that would become a JS `NaN` fall back to a small object to avoid NaN canonicalization.
 */
actual typealias PackedFloats = Any

/** Integer packing uses a JS safe-integer fast path and falls back to an object out of range. */
actual typealias PackedInts = Any

/** Reserved for future TextUnit migration. */
actual typealias PackedTextUnit = Double

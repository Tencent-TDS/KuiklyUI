/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.animation.core

import kotlin.math.floor

/*
 * Kotlin/JS emulates Long with objects: every arithmetic op allocates and division is done in
 * software. These functions run every frame for each keyframes/infinite animation, so they do
 * the math in Double and only fall back to Long when a value is outside the range where all
 * intermediate products stay exact integers.
 */

// 2^52: keeps every `quotient * divisor` below 2^53, where Double integers are exact.
private const val MaxExactNanos = 4503599627370496.0
private const val MillisToNanosDouble = 1_000_000.0

internal actual fun clampedPlayTimeMillis(
    playTimeNanos: Long,
    delayMillis: Int,
    durationMillis: Int
): Int {
    val nanos = playTimeNanos.toDouble()
    if (durationMillis < 0 || nanos > MaxExactNanos || nanos < -MaxExactNanos) {
        return clampedPlayTimeMillisLong(playTimeNanos, delayMillis, durationMillis)
    }
    val playTimeMillis = truncatedDivide(nanos, MillisToNanosDouble) - delayMillis
    return when {
        playTimeMillis < 0.0 -> 0
        playTimeMillis > durationMillis -> durationMillis
        else -> playTimeMillis.toInt()
    }
}

internal actual fun repetitionPlayTimeNanos(
    playTimeNanos: Long,
    initialOffsetNanos: Long,
    durationNanos: Long,
    repeatMode: RepeatMode
): Long {
    val playTime = playTimeNanos.toDouble()
    val offset = initialOffsetNanos.toDouble()
    val duration = durationNanos.toDouble()
    if (
        duration <= 0.0 || duration > MaxExactNanos ||
        playTime > MaxExactNanos || playTime < -MaxExactNanos ||
        offset > MaxExactNanos || offset < -MaxExactNanos
    ) {
        return repetitionPlayTimeNanosLong(
            playTimeNanos, initialOffsetNanos, durationNanos, repeatMode
        )
    }
    val postOffsetPlayTime = playTime + offset
    if (postOffsetPlayTime <= 0.0) return 0L
    if (postOffsetPlayTime > MaxExactNanos) {
        return repetitionPlayTimeNanosLong(
            playTimeNanos, initialOffsetNanos, durationNanos, repeatMode
        )
    }
    val repeatsCount = truncatedDivide(postOffsetPlayTime, duration)
    val result = if (repeatMode == RepeatMode.Restart || repeatsCount % 2.0 == 0.0) {
        postOffsetPlayTime - repeatsCount * duration
    } else {
        (repeatsCount + 1.0) * duration - postOffsetPlayTime
    }
    return result.toLong()
}

/**
 * Integer division truncating toward zero, like `Long./`. Requires integral operands with
 * `|dividend| <= MaxExactNanos` and `0 < divisor <= MaxExactNanos`.
 */
private fun truncatedDivide(dividend: Double, divisor: Double): Double {
    val magnitude = if (dividend < 0.0) -dividend else dividend
    var quotient = floor(magnitude / divisor)
    if (quotient * divisor > magnitude) {
        quotient -= 1.0
    } else if ((quotient + 1.0) * divisor <= magnitude) {
        quotient += 1.0
    }
    return if (dividend < 0.0) -quotient else quotient
}

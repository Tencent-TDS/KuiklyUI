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

/**
 * Returns `(playTimeNanos / MillisToNanos - delayMillis).coerceIn(0, durationMillis)`.
 */
internal expect fun clampedPlayTimeMillis(
    playTimeNanos: Long,
    delayMillis: Int,
    durationMillis: Int
): Int

/**
 * Returns the play time within the current iteration of a repeating animation whose single
 * iteration lasts [durationNanos].
 */
internal expect fun repetitionPlayTimeNanos(
    playTimeNanos: Long,
    initialOffsetNanos: Long,
    durationNanos: Long,
    repeatMode: RepeatMode
): Long

internal fun clampedPlayTimeMillisLong(
    playTimeNanos: Long,
    delayMillis: Int,
    durationMillis: Int
): Int = (playTimeNanos / MillisToNanos - delayMillis).coerceIn(0, durationMillis.toLong()).toInt()

// 2^52: every integer and every `quotient * divisor` below this stays exact in Double.
private const val MaxExactNanos = 4503599627370496.0
private const val MillisToNanosDouble = 1_000_000.0

/**
 * [clampedPlayTimeMillis] without allocating a [Long]. Falls back to the Long path when [playTimeNanos]
 * is outside the range where Double integers are exact.
 */
internal fun clampedPlayTimeMillisDouble(
    playTimeNanos: Double,
    delayMillis: Int,
    durationMillis: Int
): Int {
    if (durationMillis < 0 || playTimeNanos > MaxExactNanos || playTimeNanos < -MaxExactNanos) {
        return clampedPlayTimeMillisLong(playTimeNanos.toLong(), delayMillis, durationMillis)
    }
    val playTimeMillis = truncatedDivide(playTimeNanos, MillisToNanosDouble) - delayMillis
    return when {
        playTimeMillis < 0.0 -> 0
        playTimeMillis > durationMillis -> durationMillis
        else -> playTimeMillis.toInt()
    }
}

/**
 * [repetitionPlayTimeNanos] as a [Double] in nanos, so the infinite-animation frame path does not
 * allocate a [Long] per sample. Same fallback as [clampedPlayTimeMillisDouble].
 */
internal fun repetitionPlayTimeDouble(
    playTimeNanos: Double,
    initialOffsetNanos: Double,
    durationNanos: Double,
    repeatMode: RepeatMode
): Double {
    if (
        durationNanos <= 0.0 || durationNanos > MaxExactNanos ||
        playTimeNanos > MaxExactNanos || playTimeNanos < -MaxExactNanos ||
        initialOffsetNanos > MaxExactNanos || initialOffsetNanos < -MaxExactNanos
    ) {
        return repetitionPlayTimeNanosLong(
            playTimeNanos.toLong(),
            initialOffsetNanos.toLong(),
            durationNanos.toLong(),
            repeatMode
        ).toDouble()
    }
    val postOffsetPlayTime = playTimeNanos + initialOffsetNanos
    if (postOffsetPlayTime <= 0.0) return 0.0
    if (postOffsetPlayTime > MaxExactNanos) {
        return repetitionPlayTimeNanosLong(
            playTimeNanos.toLong(),
            initialOffsetNanos.toLong(),
            durationNanos.toLong(),
            repeatMode
        ).toDouble()
    }
    val repeatsCount = truncatedDivide(postOffsetPlayTime, durationNanos)
    return if (repeatMode == RepeatMode.Restart || repeatsCount % 2.0 == 0.0) {
        postOffsetPlayTime - repeatsCount * durationNanos
    } else {
        (repeatsCount + 1.0) * durationNanos - postOffsetPlayTime
    }
}

/**
 * Integer division truncating toward zero, like `Long./`. Requires integral operands with
 * `|dividend| <= MaxExactNanos` and `0 < divisor <= MaxExactNanos`.
 */
private fun truncatedDivide(dividend: Double, divisor: Double): Double {
    val magnitude = if (dividend < 0.0) -dividend else dividend
    var quotient = kotlin.math.floor(magnitude / divisor)
    if (quotient * divisor > magnitude) {
        quotient -= 1.0
    } else if ((quotient + 1.0) * divisor <= magnitude) {
        quotient += 1.0
    }
    return if (dividend < 0.0) -quotient else quotient
}

internal fun repetitionPlayTimeNanosLong(
    playTimeNanos: Long,
    initialOffsetNanos: Long,
    durationNanos: Long,
    repeatMode: RepeatMode
): Long {
    if (playTimeNanos + initialOffsetNanos <= 0) {
        return 0
    } else {
        val postOffsetPlayTimeNanos = playTimeNanos + initialOffsetNanos
        val repeatsCount = postOffsetPlayTimeNanos / durationNanos
        if (repeatMode == RepeatMode.Restart || repeatsCount % 2 == 0L) {
            return postOffsetPlayTimeNanos - repeatsCount * durationNanos
        } else {
            return (repeatsCount + 1) * durationNanos - postOffsetPlayTimeNanos
        }
    }
}

/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.base

private const val NATIVE_CALLBACK_GRACE_MILLIS = 5_000
private const val NATIVE_CALLBACK_MIN_TIMEOUT_MILLIS = 10_000

/**
 * V2 animation protocol extensions live separately from the legacy [Animation] implementation.
 * The serialized prefix remains the original eight fields, followed by one comma-separated token.
 */
fun Animation.Companion.nativeCubic(
    durationS: Float,
    delayS: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    key: String
): Animation = Animation.linear(durationS, key)
    .delay(delayS)
    .withNativeV2Payload("v2,cubic,$x1,$y1,$x2,$y2")

fun Animation.Companion.nativeSnap(delayS: Float, key: String): Animation =
    Animation.linear(0f, key)
        .delay(delayS)
        .withNativeV2Payload("v2,snap")

/** Expected wall-clock time used only for native callback timeout protection. */
fun Animation.nativeCallbackTimeoutMillis(): Int =
    (((duration + delay) * 1000f).toInt() + NATIVE_CALLBACK_GRACE_MILLIS)
        .coerceAtLeast(NATIVE_CALLBACK_MIN_TIMEOUT_MILLIS)

private fun Animation.withNativeV2Payload(payload: String): Animation = apply {
    nativeV2Payload = payload
}

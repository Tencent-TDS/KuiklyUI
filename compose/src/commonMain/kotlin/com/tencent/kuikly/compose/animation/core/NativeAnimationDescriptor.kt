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

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.Animation as CoreAnimation
import com.tencent.kuikly.core.base.nativeCubic
import com.tencent.kuikly.core.base.nativeSnap

/**
 * Converts only the explicitly supported V2 subset. Returning null is the single capability
 * boundary used to select the unchanged Compose animation path.
 */
internal fun <T, V : AnimationVector> AnimationSpec<T>.toNativeAnimationOrNull(
    initialValue: T,
    targetValue: T,
    initialVelocity: T,
    converter: TwoWayConverter<T, V>
): CoreAnimation? {
    val original = nativePreferredOriginalOrNull() ?: run {
        NativeAnimationTrace.log { "descriptor rejected reason=not-preferred spec=$this" }
        return null
    }
    val velocityVector = converter.convertToVector(initialVelocity)
    if ((0 until velocityVector.size).any { velocityVector[it] != 0f }) {
        NativeAnimationTrace.log {
            "descriptor rejected reason=non-zero-velocity spec=$original velocity=$velocityVector"
        }
        return null
    }

    return when (original) {
        is TweenSpec<T> -> {
            val controlPoints = when (val easing = original.easing) {
                LinearEasing -> floatArrayOf(0f, 0f, 1f, 1f)
                is CubicBezierEasing -> floatArrayOf(easing.a, easing.b, easing.c, easing.d)
                else -> {
                    NativeAnimationTrace.log {
                        "descriptor rejected reason=unsupported-easing spec=$original easing=$easing"
                    }
                    return null
                }
            }
            CoreAnimation.nativeCubic(
                durationS = original.durationMillis / 1000f,
                delayS = original.delay / 1000f,
                x1 = controlPoints[0],
                y1 = controlPoints[1],
                x2 = controlPoints[2],
                y2 = controlPoints[3],
                key = ""
            )
        }
        // Native spring thresholds and interruption velocity are platform-specific. Keep the
        // first version deterministic by executing SpringSpec on the existing Compose clock.
        is SpringSpec<T> -> null
        is SnapSpec<T> -> CoreAnimation.nativeSnap(original.delay / 1000f, "")
        else -> {
            NativeAnimationTrace.log { "descriptor rejected reason=unsupported-spec spec=$original" }
            null
        }
    }
}

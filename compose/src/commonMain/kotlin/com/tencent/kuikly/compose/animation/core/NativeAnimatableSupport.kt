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

internal data class NativeAnimatableCandidate(
    val animation: CoreAnimation,
    val coordinator: NativeAnimationCoordinator
)

/**
 * Centralizes the Animatable capability gate. Mutation of Animatable's private state intentionally
 * remains in Animatable so the Native path shares its mutex and completion invariants.
 */
internal fun <T, V : AnimationVector> AnimationSpec<T>.nativeAnimatableCandidateOrNull(
    initialValue: T,
    targetValue: T,
    initialVelocity: T,
    converter: TwoWayConverter<T, V>,
    hasFrameBlock: Boolean,
    hasExplicitBounds: Boolean
): NativeAnimatableCandidate? {
    if (hasFrameBlock || hasExplicitBounds) {
        NativeAnimationTrace.log {
            "animatable candidate rejected frameBlock=$hasFrameBlock bounds=$hasExplicitBounds"
        }
        return null
    }
    val animation = toNativeAnimationOrNull(
        initialValue = initialValue,
        targetValue = targetValue,
        initialVelocity = initialVelocity,
        converter = converter
    ) ?: run {
        NativeAnimationTrace.log { "animatable candidate rejected reason=descriptor" }
        return null
    }
    val coordinator = NativeAnimationCoordinator.currentOrNull() ?: run {
        NativeAnimationTrace.log { "animatable candidate rejected reason=no-coordinator" }
        return null
    }
    NativeAnimationTrace.log {
        "animatable candidate accepted from=$initialValue to=$targetValue"
    }
    return NativeAnimatableCandidate(animation, coordinator)
}

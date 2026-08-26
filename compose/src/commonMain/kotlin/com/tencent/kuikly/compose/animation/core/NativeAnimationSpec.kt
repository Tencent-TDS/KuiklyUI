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

/**
 * Requests native execution for this animation.
 *
 * This wrapper is transparent to the existing animation engine: [AnimationSpec.vectorize] is
 * delegated to the original spec unchanged. If the complete logical animation cannot be executed
 * natively, Kuikly falls back to that original spec.
 */
fun <T> AnimationSpec<T>.preferNative(): AnimationSpec<T> =
    if (this is NativePreferredAnimationSpec<*>) this else NativePreferredAnimationSpec(this)

/**
 * Finite-specialized overload of [preferNative].
 */
fun <T> FiniteAnimationSpec<T>.preferNative(): FiniteAnimationSpec<T> =
    if (this is NativePreferredFiniteAnimationSpec<*>) {
        this
    } else {
        NativePreferredFiniteAnimationSpec(this)
    }

internal sealed interface NativePreferredSpec<T> : AnimationSpec<T> {
    val originalSpec: AnimationSpec<T>
}

private class NativePreferredAnimationSpec<T>(
    override val originalSpec: AnimationSpec<T>
) : NativePreferredSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedAnimationSpec<V> = originalSpec.vectorize(converter)

    override fun equals(other: Any?): Boolean =
        other is NativePreferredAnimationSpec<*> && originalSpec == other.originalSpec

    override fun hashCode(): Int = originalSpec.hashCode()

    override fun toString(): String = "NativePreferredAnimationSpec($originalSpec)"
}

internal class NativePreferredFiniteAnimationSpec<T>(
    override val originalSpec: FiniteAnimationSpec<T>
) : NativePreferredSpec<T>, FiniteAnimationSpec<T> {
    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<T, V>
    ): VectorizedFiniteAnimationSpec<V> = originalSpec.vectorize(converter)

    override fun equals(other: Any?): Boolean =
        other is NativePreferredFiniteAnimationSpec<*> && originalSpec == other.originalSpec

    override fun hashCode(): Int = originalSpec.hashCode()

    override fun toString(): String = "NativePreferredFiniteAnimationSpec($originalSpec)"
}

internal fun <T> AnimationSpec<T>.nativePreferredOriginalOrNull(): AnimationSpec<T>? =
    (this as? NativePreferredSpec<T>)?.originalSpec

/**
 * Copies the curve parameters to another value type. Used when multiple visual properties belong
 * to one logical transition (for example scale and transform origin).
 */
internal fun <T, R> FiniteAnimationSpec<T>.retargetNativeCurve(): FiniteAnimationSpec<R> {
    val original = nativePreferredOriginalOrNull() ?: return spring()
    val copied: FiniteAnimationSpec<R> = when (original) {
        is TweenSpec<*> -> TweenSpec(original.durationMillis, original.delay, original.easing)
        is SpringSpec<*> -> SpringSpec(
            dampingRatio = original.dampingRatio,
            stiffness = original.stiffness,
            visibilityThreshold = null
        )
        is SnapSpec<*> -> SnapSpec(original.delay)
        else -> return spring()
    }
    return NativePreferredFiniteAnimationSpec(copied)
}

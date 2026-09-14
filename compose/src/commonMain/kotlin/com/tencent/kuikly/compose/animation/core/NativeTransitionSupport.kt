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

import com.tencent.kuikly.core.base.Attr

/**
 * Keeps the Native-specific lifecycle out of [Transition]. The transition itself only supplies
 * state mutations that must remain private to its animation state.
 */
internal class NativeTransitionAnimationState {
    var isActive: Boolean = false
        private set

    private var generation = 0L
    private var activeTransitionKey: Any? = null
    private var activeTargetValue: Any? = null

    fun <T, V : AnimationVector> tryStart(
        transitionKey: Any,
        label: String,
        animationSpec: FiniteAnimationSpec<T>,
        initialValue: T,
        targetValue: T,
        initialVelocity: T,
        converter: TwoWayConverter<T, V>,
        isSeeking: Boolean,
        prepare: () -> Unit,
        commitTarget: () -> Unit,
        finish: (Boolean) -> Unit
    ): Boolean {
        val coordinator = NativeAnimationCoordinator.currentOrNull()
        val nativeAnimation = animationSpec.toNativeAnimationOrNull(
            initialValue = initialValue,
            targetValue = targetValue,
            initialVelocity = initialVelocity,
            converter = converter
        )
        if (nativeAnimation == null || coordinator == null || isSeeking) {
            // Transition may install its internal interruption spec after the native target has
            // already been committed. That spec is intentionally not preferNative(), but it does
            // not represent a new segment when the target is unchanged. Keep native ownership;
            // otherwise Compose sampling and Native Render would both advance the same property.
            if (
                nativeAnimation == null &&
                coordinator != null &&
                !isSeeking &&
                isActive &&
                activeTransitionKey === transitionKey &&
                activeTargetValue == targetValue
            ) {
                NativeAnimationTrace.log {
                    "transition internal update ignored key=${transitionKey.hashCode()} " +
                        "label=$label target=$targetValue reason=native-target-unchanged"
                }
                return true
            }
            // AnimatedVisibility may create or recreate a settled deferred animation. Its
            // internal no-op spec is not preferNative(), but equal endpoints are not an active
            // effect and therefore must not participate in all-or-nothing capability validation.
            // This applies both after a native segment and when the page starts already Visible.
            // Rejecting it would poison the following real Exit for the remainder of this pass.
            if (
                nativeAnimation == null &&
                coordinator != null &&
                !isSeeking &&
                initialValue == targetValue
            ) {
                NativeAnimationTrace.log {
                    "transition settled no-op ignored key=${transitionKey.hashCode()} " +
                        "label=$label value=$targetValue"
                }
                return false
            }
            NativeAnimationTrace.log {
                "transition candidate rejected key=${transitionKey.hashCode()} label=$label " +
                    "from=$initialValue to=$targetValue " +
                    "descriptor=${nativeAnimation != null} coordinator=${coordinator != null} " +
                    "seeking=$isSeeking"
            }
            coordinator?.rejectTransition(transitionKey)
            isActive = false
            activeTransitionKey = null
            activeTargetValue = null
            return false
        }

        prepare()
        NativeAnimationTrace.log {
            "transition start key=${transitionKey.hashCode()} label=$label " +
                "from=$initialValue to=$targetValue"
        }
        val currentGeneration = ++generation
        val accepted = coordinator.animateTransition(
            transitionKey = transitionKey,
            propertyHint = label.nativeAnimationPropertyHint(),
            animation = nativeAnimation,
            initialValue = initialValue,
            targetValue = targetValue,
            targetStateCommit = {
                commitTarget()
                isActive = true
            }
        ) { completion ->
            if (currentGeneration != generation) return@animateTransition
            NativeAnimationTrace.log {
                "transition callback key=${transitionKey.hashCode()} label=$label " +
                    "generation=$currentGeneration completion=$completion"
            }
            isActive = false
            activeTransitionKey = null
            activeTargetValue = null
            when (completion) {
                NativeAnimationCoordinator.TransitionCompletion.Finished -> finish(true)
                NativeAnimationCoordinator.TransitionCompletion.Fallback -> finish(false)
                NativeAnimationCoordinator.TransitionCompletion.Superseded -> Unit
            }
        }
        if (accepted) {
            // Pause Compose sampling during the initial-state materialization frame as well.
            // The coordinator commits the target value after that frame has been drawn.
            isActive = true
            activeTransitionKey = transitionKey
            activeTargetValue = targetValue
        } else {
            NativeAnimationTrace.log {
                "transition not accepted key=${transitionKey.hashCode()} label=$label"
            }
            isActive = false
            activeTransitionKey = null
            activeTargetValue = null
        }
        return accepted
    }
}

private fun String.nativeAnimationPropertyHint(): String? {
    val normalizedLabel = lowercase()
    return when {
        "alpha" in normalizedLabel || "crossfade" in normalizedLabel ->
            Attr.StyleConst.OPACITY

        "scale" in normalizedLabel ||
            "transformorigin" in normalizedLabel ||
            "slide" in normalizedLabel ||
            "rotation" in normalizedLabel ||
            "translation" in normalizedLabel ->
            Attr.StyleConst.TRANSFORM

        "background" in normalizedLabel && "color" in normalizedLabel ->
            Attr.StyleConst.BACKGROUND_COLOR

        else -> null
    }
}

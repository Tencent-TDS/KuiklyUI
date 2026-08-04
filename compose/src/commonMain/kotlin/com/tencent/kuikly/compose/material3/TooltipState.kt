/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3

import com.tencent.kuikly.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.MutatePriority
import com.tencent.kuikly.compose.foundation.MutatorMutex
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * The state that is associated with a [TooltipBox]. Each instance of [TooltipBox] should have its
 * own [TooltipState].
 */
@Stable
interface TooltipState {
    /**
     * The current transition state of the tooltip. Used to start the transition of the tooltip when
     * fading in and out.
     */
    val transition: MutableTransitionState<Boolean>

    /** [Boolean] that indicates if the tooltip is currently being shown or not. */
    val isVisible: Boolean

    /**
     * [Boolean] that determines if the tooltip associated with this will be persistent or not. If
     * isPersistent is true, then the tooltip will only be dismissed when the user clicks outside
     * the bounds of the tooltip or if [dismiss] is called. When isPersistent is false, the tooltip
     * will dismiss after a short duration.
     */
    val isPersistent: Boolean

    /**
     * Show the tooltip associated with the current [TooltipState]. When this method is called all
     * of the other tooltips currently being shown will dismiss.
     *
     * @param mutatePriority [MutatePriority] to be used.
     */
    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)

    /** Dismiss the tooltip associated with this [TooltipState] if it's currently being shown. */
    fun dismiss()

    /** Clean up when the this state leaves Composition. */
    fun onDispose()
}

@Stable
internal class TooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
) : TooltipState {
    override val transition: MutableTransitionState<Boolean> =
        MutableTransitionState(initialIsVisible)

    override val isVisible: Boolean
        get() = transition.currentState || transition.targetState

    private var job: (CancellableContinuation<Unit>)? = null

    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                transition.targetState = true
                job = continuation
            }
        }

        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent || mutatePriority == MutatePriority.UserInput) {
                    cancellableShow()
                } else {
                    withTimeout(TooltipDuration) { cancellableShow() }
                }
            } finally {
                if (mutatePriority != MutatePriority.PreventUserInput) {
                    dismiss()
                }
            }
        }
    }

    override fun dismiss() {
        transition.targetState = false
        if (isPersistent) {
            job?.cancel()
        }
    }

    override fun onDispose() {
        job?.cancel()
    }
}

internal const val TooltipDuration = 1500L

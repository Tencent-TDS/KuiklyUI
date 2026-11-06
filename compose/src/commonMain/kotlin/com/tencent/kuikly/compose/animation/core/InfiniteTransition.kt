/*
 * Copyright 2021 The Android Open Source Project
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

package com.tencent.kuikly.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.unit.Dp
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Creates a [InfiniteTransition] that runs infinite child animations. Child animations can be
 * added using [InfiniteTransition.animateColor][androidx.compose.animation.animateColor],
 * [InfiniteTransition.animateFloat], or [InfiniteTransition.animateValue]. Child animations will
 * start running as soon as they enter the composition, and will not stop until they are removed
 * from the composition.
 *
 * @param label A label for differentiating this animation from others in android studio.
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 */
@Composable
fun rememberInfiniteTransition(label: String = "InfiniteTransition"): InfiniteTransition {
    val infiniteTransition = remember { InfiniteTransition(label) }
    infiniteTransition.run()
    return infiniteTransition
}

/**
 * [InfiniteTransition] is responsible for running child animations. Child animations can be
 * added using [InfiniteTransition.animateColor][androidx.compose.animation.animateColor],
 * [InfiniteTransition.animateFloat], or [InfiniteTransition.animateValue]. Child animations will
 * start running as soon as they enter the composition, and will not stop until they are removed
 * from the composition.
 *
 * @param label A label for differentiating this animation from others in android studio.
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 */
class InfiniteTransition internal constructor(val label: String) {

    /**
     * Each animation created using [InfiniteTransition.animateColor][androidx.compose.animation.animateColor],
     * [InfiniteTransition.animateFloat], or [InfiniteTransition.animateValue] is represented as a
     * [TransitionAnimationState] in [InfiniteTransition]. [typeConverter] converts the animation
     * value from/to an [AnimationVector]. [label] differentiates this animation from others in android studio.
     */
    inner class TransitionAnimationState<T, V : AnimationVector> internal constructor(
        internal var initialValue: T,
        internal var targetValue: T,
        val typeConverter: TwoWayConverter<T, V>,
        animationSpec: AnimationSpec<T>,
        val label: String
    ) : State<T> {
        override var value by mutableStateOf(initialValue)
            internal set

        /** [AnimationSpec] that is used for current animation run. */
        var animationSpec: AnimationSpec<T> = animationSpec
            private set

        /**
         * All the animation configurations including initial value/velocity & target value for
         * animating from [initialValue] to [targetValue] are captured in [animation].
         */
        var animation = TargetBasedAnimation(
            this.animationSpec,
            typeConverter,
            initialValue,
            targetValue
        )
            internal set

        // This is used to signal parent for less work in a normal running mode, but in seeking
        // this is ignored since time can go both ways.
        internal var isFinished = false

        // If animation is refreshed during the run, start the new animation in the next frame
        private var startOnTheNextFrame = false

        // When the animation changes, it needs to start from playtime 0 again, offsetting from
        // parent's playtime to achieve that.
        private var playTimeNanosOffset = 0.0

        // This gets called when the initial/target value changes, which should be a rare case.
        internal fun updateValues(
            initialValue: T,
            targetValue: T,
            animationSpec: AnimationSpec<T>
        ) {
            this.initialValue = initialValue
            this.targetValue = targetValue
            this.animationSpec = animationSpec
            // Create a new animation if anything (i.e. initial/target) has changed
            // TODO: Consider providing some continuity maybe?
            animation = TargetBasedAnimation(
                animationSpec,
                typeConverter,
                initialValue,
                targetValue
            )
            refreshChildNeeded = true
            isFinished = false
            startOnTheNextFrame = true
        }

        /** Set play time for the [animation]. */
        internal fun onPlayTimeChanged(playTimeNanos: Double) {
            refreshChildNeeded = false
            if (startOnTheNextFrame) {
                startOnTheNextFrame = false
                playTimeNanosOffset = playTimeNanos
            }
            val playTime = playTimeNanos - playTimeNanosOffset
            value = animation.getValueFromPlayNanos(playTime)
            isFinished = !animation.isInfinite && animation.isFinishedFromNanos(playTime.toLong())
        }

        internal fun skipToEnd() {
            value = animation.targetValue
            startOnTheNextFrame = true
        }

        internal fun reset() {
            startOnTheNextFrame = true
        }
    }

    private val _animations = mutableVectorOf<TransitionAnimationState<*, *>>()
    private var refreshChildNeeded by mutableStateOf(false)
    private var hasStartTime = false
    private var startTimeNanos = 0.0
    private var appliedDurationScale = Float.NaN
    private var toolingOverride: State<Long>? = null
    private var isRunning by mutableStateOf(true)

    /**
     * List of [TransitionAnimationState]s that are in a [InfiniteTransition].
     */
    val animations: List<TransitionAnimationState<*, *>>
        get() = _animations.asMutableList()

    internal fun addAnimation(animation: TransitionAnimationState<*, *>) {
        _animations.add(animation)
        refreshChildNeeded = true
    }

    internal fun removeAnimation(animation: TransitionAnimationState<*, *>) {
        _animations.remove(animation)
    }

    @Suppress("ComposableNaming")
    @Composable
    internal fun run() {
        val override = remember { mutableStateOf<State<Long>?>(null) }
        toolingOverride = override.value
        if (isRunning || refreshChildNeeded) {
            val context = rememberCoroutineScope().coroutineContext
            DisposableEffect(this) {
                InfiniteTransitionClock.register(this@InfiniteTransition, context)
                onDispose { InfiniteTransitionClock.unregister(this@InfiniteTransition) }
            }
        }
    }

    internal fun onSharedFrame(frameTimeNanos: Double, durationScale: Float) {
        if (!isRunning && !refreshChildNeeded) return
        val currentTimeNanos = toolingOverride?.value?.toDouble() ?: frameTimeNanos
        if (!hasStartTime || appliedDurationScale != durationScale) {
            startTimeNanos = currentTimeNanos
            hasStartTime = true
            _animations.forEach { it.reset() }
            appliedDurationScale = durationScale
        }
        if (durationScale == 0f) {
            _animations.forEach { it.skipToEnd() }
        } else {
            onFrame((currentTimeNanos - startTimeNanos) / durationScale.toDouble())
        }
    }

    private fun onFrame(playTimeNanos: Double) {
        var allFinished = true
        // Pulse new playtime
        _animations.forEach {
            if (!it.isFinished) {
                it.onPlayTimeChanged(playTimeNanos)
            }
            // Check isFinished flag again after the animation pulse
            if (!it.isFinished) {
                allFinished = false
            }
        }
        isRunning = !allFinished
    }
}

/**
 * One frame await for every running [InfiniteTransition]. Each transition used to suspend in its
 * own [withInfiniteAnimationFrameNanos], which allocated a continuation per badge per frame.
 */
private object InfiniteTransitionClock {
    private val transitions = mutableVectorOf<InfiniteTransition>()
    private var job: Job? = null

    fun register(transition: InfiniteTransition, context: CoroutineContext) {
        if (transitions.indexOf(transition) < 0) {
            transitions.add(transition)
        }
        if (job?.isActive == true) return
        val scope = CoroutineScope(context.minusKey(Job) + SupervisorJob())
        val launched = scope.launch {
            try {
                drive()
            } finally {
                if (job === coroutineContext[Job]) {
                    job = null
                }
            }
        }
        job = launched
    }

    fun unregister(transition: InfiniteTransition) {
        transitions.remove(transition)
        if (transitions.size == 0) {
            val running = job
            job = null
            running?.cancel()
        }
    }

    private suspend fun drive() {
        var durationScale = 1f
        while (transitions.size > 0) {
            val frameTime = withInfiniteAnimationFrameNanos { it.toDouble() }
            durationScale = coroutineContext.durationScale
            val count = transitions.size
            for (i in 0 until count) {
                if (i >= transitions.size) break
                transitions[i].onSharedFrame(frameTime, durationScale)
            }
            while (durationScale == 0f && transitions.size > 0) {
                withInfiniteAnimationFrameNanos { }
                durationScale = coroutineContext.durationScale
            }
        }
    }
}

/**
 * Creates an animation of type [T] that runs infinitely as a part of the given
 * [InfiniteTransition]. Any data type can be animated so long as it can be converted from and to
 * an [AnimationVector]. This conversion needs to be provided as a [typeConverter]. Some examples
 * of such [TwoWayConverter] are: [Int.VectorConverter][Int.Companion.VectorConverter],
 * [Dp.VectorConverter][Dp.Companion.VectorConverter],
 * [Size.VectorConverter][Size.Companion.VectorConverter], etc
 *
 * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
 * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
 * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration (i.e
 * . [RepeatMode.Reverse]).
 *
 * If [initialValue] or [targetValue] is changed at any point during the animation, the animation
 * will be restarted with the new [initialValue] and [targetValue]. __Note__: this means
 * continuity will *not* be preserved.
 *
 * A [label] for differentiating this animation from others in android studio.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionAnimateValueSample
 *
 * @see [InfiniteTransition.animateFloat]
 * @see [androidx.compose.animation.animateColor]
 */
@Composable
fun <T, V : AnimationVector> InfiniteTransition.animateValue(
    initialValue: T,
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: InfiniteRepeatableSpec<T>,
    label: String = "ValueAnimation"
): State<T> {
    val transitionAnimation =
        remember {
            TransitionAnimationState(
                initialValue, targetValue, typeConverter, animationSpec, label
            )
        }

    SideEffect {
        if (initialValue != transitionAnimation.initialValue ||
            targetValue != transitionAnimation.targetValue
        ) {
            transitionAnimation.updateValues(
                initialValue = initialValue,
                targetValue = targetValue,
                animationSpec = animationSpec
            )
        }
    }

    DisposableEffect(transitionAnimation) {
        addAnimation(transitionAnimation)
        onDispose {
            removeAnimation(transitionAnimation)
        }
    }
    return transitionAnimation
}

/**
 * Creates an animation of Float type that runs infinitely as a part of the given
 * [InfiniteTransition].
 *
 * Once the animation is created, it will run from [initialValue] to [targetValue] and repeat.
 * Depending on the [RepeatMode] of the provided [animationSpec], the animation could either
 * restart after each iteration (i.e. [RepeatMode.Restart]), or reverse after each iteration (i.e
 * . [RepeatMode.Reverse]).
 *
 * If [initialValue] or [targetValue] is changed at any point during the animation, the animation
 * will be restarted with the new [initialValue] and [targetValue]. __Note__: this means
 * continuity will *not* be preserved.
 *
 * A [label] for differentiating this animation from others in android studio.
 *
 * @sample androidx.compose.animation.core.samples.InfiniteTransitionSample
 *
 * @see [InfiniteTransition.animateValue]
 * @see [androidx.compose.animation.animateColor]
 */
@Composable
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>,
    label: String = "FloatAnimation"
): State<Float> =
    animateValue(initialValue, targetValue, Float.VectorConverter, animationSpec, label)

@Deprecated(
    "rememberInfiniteTransition APIs now have a new label parameter added.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun rememberInfiniteTransition(): InfiniteTransition {
    return rememberInfiniteTransition("InfiniteTransition")
}

@Deprecated(
    "animateValue APIs now have a new label parameter added.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun <T, V : AnimationVector> InfiniteTransition.animateValue(
    initialValue: T,
    targetValue: T,
    typeConverter: TwoWayConverter<T, V>,
    animationSpec: InfiniteRepeatableSpec<T>,
): State<T> {
    return animateValue(
        initialValue = initialValue,
        targetValue = targetValue,
        typeConverter = typeConverter,
        animationSpec = animationSpec,
        label = "ValueAnimation"
    )
}

@Deprecated(
    "animateFloat APIs now have a new label parameter added.",
    level = DeprecationLevel.HIDDEN
)
@Composable
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: InfiniteRepeatableSpec<Float>
): State<Float> {
    return animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = animationSpec, label = "FloatAnimation"
    )
}

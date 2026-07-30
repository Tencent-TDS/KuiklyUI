/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.compose.animation.core.AnimationSpec
import com.tencent.kuikly.compose.animation.core.SpringSpec
import com.tencent.kuikly.compose.foundation.gestures.FlingBehavior
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.gestures.ScrollScope
import com.tencent.kuikly.compose.foundation.gestures.ScrollableState
import com.tencent.kuikly.compose.foundation.gestures.scrollBy
import com.tencent.kuikly.compose.foundation.interaction.InteractionSource
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.composed
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasurable
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasureScope
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.node.LayoutModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.platform.debugInspectorInfo
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.util.fastRoundToInt
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.core.views.SpringAnimation

/**
 * Create and [remember] the [ScrollState] based on the currently appropriate scroll
 * configuration to allow changing scroll position or observing scroll behavior.
 *
 * Learn how to control the state of [Modifier.verticalScroll] or [Modifier.horizontalScroll]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial initial scroller position to start with
 */
@Composable
fun rememberScrollState(initial: Int = 0): ScrollState {
    return rememberSaveable(saver = ScrollState.Saver) {
        ScrollState(initial = initial)
    }
}

/**
 * State of the scroll. Allows the developer to change the scroll position or get current state by
 * calling methods on this object. To be hosted and passed to [Modifier.verticalScroll] or
 * [Modifier.horizontalScroll]
 *
 * To create and automatically remember [ScrollState] with default parameters use
 * [rememberScrollState].
 *
 * Learn how to control the state of [Modifier.verticalScroll] or [Modifier.horizontalScroll]:
 * @sample androidx.compose.foundation.samples.ControlledScrollableRowSample
 *
 * @param initial value of the scroll
 */
@Stable
class ScrollState(initial: Int) : ScrollableState {

    override var contentPadding: PaddingValues = PaddingValues(0.dp)

    /**
     * current scroll position value in pixels
     */
    var value: Int by mutableIntStateOf(initial)
        private set

    /**
     * maximum bound for [value], or [Int.MAX_VALUE] if still unknown
     */
    var maxValue: Int
        get() = _maxValueState.intValue
        internal set(newMax) {
            _maxValueState.intValue = newMax
            Snapshot.withoutReadObservation {
                if (value > newMax) {
                    value = newMax
                }
            }
        }

    /**
     * Size of the viewport on the scrollable axis, or 0 if still unknown. Note that this value
     * is only populated after the first measure pass.
     */
    var viewportSize: Int by mutableIntStateOf(0)
        internal set

    /**
     * [InteractionSource] that will be used to dispatch drag events when this
     * list is being dragged. If you want to know whether the fling (or smooth scroll) is in
     * progress, use [isScrollInProgress].
     */
    val interactionSource: InteractionSource get() = internalInteractionSource

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    private var _maxValueState = mutableIntStateOf(Int.MAX_VALUE)

    /**
     * We receive scroll events in floats but represent the scroll position in ints so we have to
     * manually accumulate the fractional part of the scroll to not completely ignore it.
     */
    private var accumulator: Float = 0f

    internal val scrollableState = ScrollableState {
        val absolute = (value + it + accumulator)
        val newValue = absolute.coerceIn(0f, maxValue.toFloat())
        val changed = absolute != newValue
        val consumed = newValue - value
        val consumedInt = consumed.fastRoundToInt()
        value += consumedInt
        accumulator = consumed - consumedInt

        // Avoid floating-point rounding error
        if (changed) consumed else it
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ): Unit = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float): Float =
        scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override val canScrollForward: Boolean by derivedStateOf { value < maxValue }

    override val canScrollBackward: Boolean by derivedStateOf { value > 0 }

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    /**
     * Scroll to position in pixels with animation.
     *
     * @param value target value in pixels to smooth scroll to, value will be coerced to
     * 0..maxPosition
     * @param animationSpec animation curve for smooth scroll animation
     */
    suspend fun animateScrollTo(
        value: Int,
        animationSpec: AnimationSpec<Float> = SpringSpec()
    ) {
        val orientation = kuiklyInfo.orientation
        kuiklyInfo.scrollView?.run {
            val density = kuiklyInfo.getDensity()
            val curOffset = IntOffset(contentViewOffsetX.toInt(), contentViewOffsetY.toInt())
            val newOffset = if (orientation == Orientation.Vertical) {
                IntOffset(curOffset.x, (value / density).toInt())
            } else {
                IntOffset((value / density).toInt(), curOffset.y)
            }
            setContentOffset(
                newOffset.x.toFloat(),
                newOffset.y.toFloat(),
                true,
                SpringAnimation(400, 1f, 0f)
            )
        }

//        this.animateScrollBy((value - this.value).toFloat(), animationSpec)
    }

    /**
     * Instantly jump to the given position in pixels.
     *
     * Cancels the currently running scroll, if any, and suspends until the cancellation is
     * complete.
     *
     * @see animateScrollTo for an animated version
     *
     * @param value number of pixels to scroll by
     * @return the amount of scroll consumed
     */
    suspend fun scrollTo(value: Int): Float = this.scrollBy((value - this.value).toFloat())

    companion object {
        /**
         * The default [Saver] implementation for [ScrollState].
         */
        val Saver: Saver<ScrollState, *> = Saver(
            save = { it.value },
            restore = { ScrollState(it) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.scroll(
    state: ScrollState,
    reverseScrolling: Boolean,
    flingBehavior: FlingBehavior?,
    isScrollable: Boolean,
    isVertical: Boolean
) = composed(
    factory = {
        Modifier
            .then(ScrollingLayoutElement(state, reverseScrolling, isVertical))
    },
    inspectorInfo = debugInspectorInfo {
        name = "scroll"
        properties["state"] = state
        properties["reverseScrolling"] = reverseScrolling
        properties["flingBehavior"] = flingBehavior
        properties["isScrollable"] = isScrollable
        properties["isVertical"] = isVertical
    }
)

internal class ScrollingLayoutElement(
    val scrollState: ScrollState,
    val isReversed: Boolean,
    val isVertical: Boolean
) : ModifierNodeElement<ScrollingLayoutNode>() {
    override fun create(): ScrollingLayoutNode {
        return ScrollingLayoutNode(
            scrollerState = scrollState,
            isReversed = isReversed,
            isVertical = isVertical
        )
    }

    override fun update(node: ScrollingLayoutNode) {
        node.scrollerState = scrollState
        node.isReversed = isReversed
        node.isVertical = isVertical
    }

    override fun hashCode(): Int {
        var result = scrollState.hashCode()
        result = 31 * result + isReversed.hashCode()
        result = 31 * result + isVertical.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ScrollingLayoutElement) return false
        return scrollState == other.scrollState &&
            isReversed == other.isReversed &&
            isVertical == other.isVertical
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layoutInScroll"
        properties["state"] = scrollState
        properties["isReversed"] = isReversed
        properties["isVertical"] = isVertical
    }
}

internal class ScrollingLayoutNode(
    var scrollerState: ScrollState,
    var isReversed: Boolean,
    var isVertical: Boolean
) : LayoutModifierNode, Modifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        checkScrollableContainerConstraints(
            constraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )

        val childConstraints = constraints.copy(
            maxHeight = if (isVertical) Constraints.Infinity else constraints.maxHeight,
            maxWidth = if (isVertical) constraints.maxWidth else Constraints.Infinity
        )
        val placeable = measurable.measure(childConstraints)
        val width = placeable.width.coerceAtMost(constraints.maxWidth)
        val height = placeable.height.coerceAtMost(constraints.maxHeight)
        val scrollHeight = placeable.height - height
        val scrollWidth = placeable.width - width
        val side = if (isVertical) scrollHeight else scrollWidth
        // The max value must be updated before returning from the measure block so that any other
        // chained RemeasurementModifiers that try to perform scrolling based on the new
        // measurements inside onRemeasured are able to scroll to the new max based on the newly-
        // measured size.
        scrollerState.maxValue = side
        scrollerState.viewportSize = if (isVertical) height else width
        return layout(width, height) {
            val scroll = scrollerState.value.coerceIn(0, side)
            val absScroll = if (isReversed) scroll - side else -scroll
            val xOffset = if (isVertical) 0 else absScroll
            val yOffset = if (isVertical) absScroll else 0

            // Tagging as direct manipulation, such that consumers of this offset can decide whether
            // to exclude this offset on their coordinates calculation. Such as whether an
            // `approachLayout` will animate it or directly apply the offset without animation.
            withMotionFrameOfReferencePlacement {
                placeable.placeRelativeWithLayer(xOffset, yOffset)
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (isVertical) {
            measurable.minIntrinsicWidth(Constraints.Infinity)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (isVertical) {
            measurable.minIntrinsicHeight(width)
        } else {
            measurable.minIntrinsicHeight(Constraints.Infinity)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (isVertical) {
            measurable.maxIntrinsicWidth(Constraints.Infinity)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (isVertical) {
            measurable.maxIntrinsicHeight(width)
        } else {
            measurable.maxIntrinsicHeight(Constraints.Infinity)
        }
    }
}

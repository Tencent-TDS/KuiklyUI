/*
 * Copyright 2022 The Android Open Source Project
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

package com.tencent.kuikly.compose.foundation.lazy.staggeredgrid

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.lazy.layout.LazyLayout
import com.tencent.kuikly.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.scroller.kuiklyInfo
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalLayoutDirection
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyStaggeredGrid(
    /** State controlling the scroll position */
    state: LazyStaggeredGridState,
    /** The layout orientation of the grid */
    orientation: Orientation,
    /** Cross axis positions and sizes of slots per line, e.g. the columns for vertical grid. */
    slots: LazyGridStaggeredGridSlotsProvider,
    /** Modifier to be applied for the inner layout */
    modifier: Modifier = Modifier,
    /** The inner padding to be added for the whole content (not for each individual item) */
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean = false,
//    /** fling behavior to be used for flinging */
//    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean = true,
    /** The vertical spacing for items/lines. */
    mainAxisSpacing: Dp = 0.dp,
    /** The horizontal spacing for items/lines. */
    crossAxisSpacing: Dp = 0.dp,
    /** The content of the grid */
    content: LazyStaggeredGridScope.() -> Unit
) {
    val itemProviderLambda = rememberStaggeredGridItemProviderLambda(state, content)
    val coroutineScope = rememberCoroutineScope()
    state.kuiklyInfo.scope = coroutineScope
//    val graphicsContext = LocalGraphicsContext.current
    val measurePolicy = rememberStaggeredGridMeasurePolicy(
        state,
        itemProviderLambda,
        contentPadding,
        reverseLayout,
        orientation,
        mainAxisSpacing,
        crossAxisSpacing,
        coroutineScope,
        slots,
//        graphicsContext
    )
//    val semanticState = rememberLazyStaggeredGridSemanticState(state, reverseLayout)

    state.contentPadding = contentPadding
    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
//            .lazyLayoutSemantics(
//                itemProviderLambda = itemProviderLambda,
//                state = semanticState,
//                orientation = orientation,
//                userScrollEnabled = userScrollEnabled,
//                reverseScrolling = reverseLayout,
//            )
            .lazyLayoutBeyondBoundsModifier(
                state = rememberLazyStaggeredGridBeyondBoundsState(state = state),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                layoutDirection = LocalLayoutDirection.current,
                orientation = orientation,
                enabled = userScrollEnabled
            )
            .then(state.itemAnimator.modifier),
//            .scrollingContainer(
//                state = state,
//                orientation = orientation,
//                enabled = userScrollEnabled,
//                reverseScrolling = reverseLayout,
//                flingBehavior = flingBehavior,
//                interactionSource = state.mutableInteractionSource
//            ),
//        prefetchState = state.prefetchState,
        itemProvider = itemProviderLambda,
        orientation = orientation,
        scrollableState = state,
        userScrollEnabled = userScrollEnabled,
        measurePolicy = measurePolicy
    )
}

/** Slot configuration of staggered grid */
internal class LazyStaggeredGridSlots(
    val positions: IntArray,
    val sizes: IntArray
)

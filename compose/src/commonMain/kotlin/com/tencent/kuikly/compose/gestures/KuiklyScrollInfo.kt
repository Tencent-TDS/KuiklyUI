/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.gestures

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.ui.node.StickyHeaderCacheManager
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.views.ScrollerAttr
import com.tencent.kuikly.core.views.ScrollerEvent
import com.tencent.kuikly.core.views.ScrollerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * Scroll information management class, responsible for handling scroll-related state and calculations
 */
class KuiklyScrollInfo {
    companion object {
        private const val DEFAULT_CONTENT_SIZE = 3000
        private const val SCROLL_BOTTOM_THRESHOLD = 100
        private const val DEFAULT_DENSITY = 3f
    }

    /**
     * Scroll offset that needs to be ignored
     */
    var ignoreScrollOffset: IntOffset? = null

    /**
     * Scroll view instance
     */
    var scrollView: ScrollerView<ScrollerAttr, ScrollerEvent>? = null
        set(value) {
            field = value
            if (hasPullToRefresh && value != null) {
                value.setHasPullToRefresh(true)
            }
        }

    /**
     * Scroll orientation
     */
    var orientation: Orientation = Orientation.Vertical

    /**
     * Whether native scrolling should be mirrored for reverse lazy layouts.
     */
    var reverseLayout: Boolean = false

    /**
     * Offset on the Compose side, does not exceed boundaries
     */
    var composeOffset = 0f

    /**
     * Temporary native-coordinate correction used while a Pager snap animation is running.
     * When items are inserted before the snap target, this keeps the target item's native frame
     * anchored to the original snap target offset until the snap settles.
     */
    var snapAnchorOffsetCorrection = 0

    /**
     * Current contentView size, used to expand the bottom boundary
     */
    var currentContentSize by mutableStateOf((DEFAULT_CONTENT_SIZE * getDensity()).toInt())

    /**
     * Real contentSize after scrolling to the bottom
     */
    var realContentSize: Int? = null

    /**
     * Whether the offset has deviation
     */
    var offsetDirty = false

    /**
     * ScrollView's scroll offset
     */
    var contentOffset: Int by mutableStateOf(0)

    /**
     * ScrollView is dragging
     */
    var isDragging: Boolean by mutableStateOf(false)

    /**
     * List height cache
     */
    internal var itemMainSpaceCache = hashMapOf<Any, Int>()

    /**
     * Used to track delayed execution of applyScrollViewOffsetDelta tasks
     */
    internal var appleScrollViewOffsetJob: Job? = null

    /**
     * Coroutine scope
     */
    internal var scope: CoroutineScope? = null

    /**
     * PageData related data
     */
    var pageData: PageData? = null

    /**
     * The key of the current sticky item, used to identify which item is in sticky state
     * In LazyList, when an item is set as sticky, its key will be stored here
     * KNode can determine if it's a sticky node by comparing its own slotId with this key
     */
    var stickyItemKey: Any? = null

    /**
     * Flag indicating whether the current list uses PullToRefresh
     * When PullToRefresh is used, the isAtTop judgment logic needs to be adjusted
     */
    var hasPullToRefresh: Boolean = false
        set(value) {
            field = value
            if (value) {
                scrollView?.setHasPullToRefresh(true)
            } else {
                scrollView?.setHasPullToRefresh(false)
            }
        }

    /**
     * Extra top inset on the pull-to-refresh lazy item in pixels,
     * from [com.tencent.kuikly.compose.material3.pullToRefreshItem.topInset].
     */
    var pullToRefreshTopInsetPx: Int = 0

    /**
     * Cached total number of items, used to detect changes in item count
     */
    var cachedTotalItems: Int = 0

    /**
     * Sticky Header Position Cache Manager
     */
    val stickyHeaderCacheManager = StickyHeaderCacheManager()

    /**
     * Scroll to top event callback.
     * If set, the callback will be invoked instead of the default scroll to top behavior.
     * This aligns with iOS behavior where scrollToTop event can be intercepted.
     */
    var scrollToTopCallback: (() -> Unit)? = null

    /**
     * Update content size to render view
     */
    fun updateContentSizeToRender() {
        val frame = createContentFrame()
        scrollView?.contentView?.setFrameToRenderView(frame)
    }

    /**
     * Reset scroll-related state when binding to a new ScrollView (e.g., when LazyColumn's key changes and causes rebuild)
     * Note: This depends on scrollView to get density, so it should be called after setting scrollView
     */
    fun resetForNewScrollView() {
        // Cancel and clear any pending tasks
        appleScrollViewOffsetJob?.cancel()
        appleScrollViewOffsetJob = null

        // Reset basic offset and scroll state
        ignoreScrollOffset = null
        composeOffset = 0f
        contentOffset = 0
        isDragging = false
        offsetDirty = false
        reverseLayout = false

        // Reset content size related (reinitialize based on current density)
        currentContentSize = (DEFAULT_CONTENT_SIZE * getDensity()).toInt()
        realContentSize = null

        // Clear list items and pagination caches
        itemMainSpaceCache.clear()
        stickyItemKey = null
        cachedTotalItems = 0
        pullToRefreshTopInsetPx = 0
    }

    /**
     * Create content Frame
     */
    private fun createContentFrame(): Frame {
        return if (isVertical()) {
            Frame(
                x = 0f,
                y = 0f,
                width = scrollView?.renderView?.currentFrame?.width ?: 0f,
                height = currentContentSize / getDensity()
            )
        } else {
            Frame(
                x = 0f,
                y = 0f,
                width = currentContentSize / getDensity(),
                height = scrollView?.renderView?.currentFrame?.height ?: 0f
            )
        }
    }

    /**
     * Get viewport size
     */
    val viewportSize: Int
        get() {
            val size = if (isVertical()) {
                scrollView?.renderView?.currentFrame?.height ?: 0f
            } else {
                scrollView?.renderView?.currentFrame?.width ?: 0f
            }
            return (size * getDensity()).toInt()
        }

    /**
     * Maximum native offset on the scrolling axis.
     */
    val maxNativeOffset: Int
        get() = maxOf(0, currentContentSize - viewportSize)

    /**
     * Get density
     */
    fun getDensity(): Float {
        return scrollView?.getPager()?.pagerDensity() ?: DEFAULT_DENSITY
    }

    /**
     * Check if it's vertical scrolling
     */
    fun isVertical(): Boolean = orientation == Orientation.Vertical

    /**
     * Convert a native ScrollView offset to a Compose logical offset.
     */
    fun logicalOffsetFromNative(nativeOffset: Int): Float {
        return if (reverseLayout) {
            (maxNativeOffset - nativeOffset).toFloat()
        } else {
            nativeOffset.toFloat()
        }
    }

    /**
     * Convert a Compose logical offset to a native ScrollView offset.
     */
    fun nativeOffsetFromLogical(logicalOffset: Float): Int {
        val boundedLogicalOffset = logicalOffset.toInt().coerceIn(0, maxNativeOffset)
        return if (reverseLayout) {
            maxNativeOffset - boundedLogicalOffset
        } else {
            boundedLogicalOffset
        }
    }

    /**
     * Convert a Compose logical delta to a native ScrollView delta.
     */
    fun nativeDeltaFromLogicalDelta(logicalDelta: Int): Int {
        return if (reverseLayout) -logicalDelta else logicalDelta
    }

    /**
     * Main-axis component from an offset pair.
     */
    fun mainAxisOffset(offset: IntOffset): Int {
        return if (isVertical()) offset.y else offset.x
    }

    /**
     * Build an offset pair from a main-axis offset.
     */
    fun offsetFromMainAxis(mainAxisOffset: Int): IntOffset {
        return if (isVertical()) {
            IntOffset(0, mainAxisOffset)
        } else {
            IntOffset(mainAxisOffset, 0)
        }
    }

    /**
     * Native offset used to place child render views into the ScrollView content coordinates.
     */
    fun renderOffset(): Float {
        return if (reverseLayout) nativeOffsetFromLogical(composeOffset).toFloat() else composeOffset
    }

    /**
     * Keep native ScrollView position aligned to the current Compose logical offset.
     */
    fun syncContentOffsetToComposeOffset(force: Boolean = false) {
        val nativeOffset = nativeOffsetFromLogical(composeOffset)
        if (!force && nativeOffset == contentOffset) {
            return
        }
        contentOffset = nativeOffset
        val nativeOffsetPair = offsetFromMainAxis(nativeOffset)
        ignoreScrollOffset = nativeOffsetPair
        setContentOffsetToRender(nativeOffsetPair)
    }

    /**
     * Apply a native content offset to the render ScrollView.
     */
    fun setContentOffsetToRender(nativeOffset: IntOffset, animated: Boolean = false) {
        val scrollView = scrollView ?: return
        val density = getDensity()
        val offsetX = nativeOffset.x / density
        val offsetY = nativeOffset.y / density
        if (pageData?.isAndroid == true) {
            // Android can drop a setContentOffset that lands exactly at the max edge.
            scrollView.setContentOffset(
                maxOf(0f, offsetX - 0.01f),
                maxOf(0f, offsetY - 0.01f),
                animated
            )
        } else {
            scrollView.setContentOffset(offsetX, offsetY, animated)
        }
    }

    /**
     * Check if it's near the bottom of scrolling
     */
    fun nearScrollBottom(): Boolean {
        val threshold = SCROLL_BOTTOM_THRESHOLD * getDensity()
        return composeOffset + viewportSize + threshold > currentContentSize
    }
}

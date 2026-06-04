package com.tencent.kuikly.compose.scroller

import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.foundation.lazy.LazyListItemInfo
import com.tencent.kuikly.compose.foundation.lazy.LazyListLayoutInfo
import com.tencent.kuikly.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LazyListReverseLayoutSupportTest {

    @Test
    fun `non reverse content size uses trailing item end`() {
        val layoutInfo = fakeLayoutInfo(
            reverseLayout = false,
            totalItemsCount = 10,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(
                fakeItem(index = 7, offset = 0, size = 100),
                fakeItem(index = 8, offset = 100, size = 100),
                fakeItem(index = 9, offset = 200, size = 100),
            ),
        )

        assertEquals(1000, layoutInfo.calculateExactContentSizeFromVisualEnd(curOffset = 700f))
    }

    @Test
    fun `reverse content size uses visual bottom edge`() {
        val layoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            totalItemsCount = 10,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(
                fakeItem(index = 7, offset = 0, size = 100),
                fakeItem(index = 8, offset = 100, size = 100),
                fakeItem(index = 9, offset = 200, size = 100),
            ),
        )

        assertEquals(1000, layoutInfo.calculateExactContentSizeFromVisualEnd(curOffset = 700f))
    }

    @Test
    fun `reverse top offset estimate uses visual top item`() {
        val layoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            totalItemsCount = 10,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(
                fakeItem(index = 5, offset = 40, size = 100),
                fakeItem(index = 6, offset = 140, size = 100),
                fakeItem(index = 7, offset = 240, size = 100),
            ),
        )

        assertEquals(240, layoutInfo.estimateVisualTopScrollOffset(hasPullToRefresh = false))
    }

    @Test
    fun `reverse top and bottom helpers follow visual edges`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 3,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 0, offset = 0, size = 100)),
        )

        assertTrue(reverseLayoutInfo.isAtVisualTop(canScrollForward = false, canScrollBackward = true))
        assertTrue(reverseLayoutInfo.isAtVisualBottom(canScrollForward = true, canScrollBackward = false))
    }

    @Test
    fun `native scroll delta keeps same sign for reverse layout bridge`() {
        assertEquals(120f, nativeScrollDeltaForCompose(120f))
        assertEquals(-80f, nativeScrollDeltaForCompose(-80f))
    }

    @Test
    fun `reverse vertical expands native end while compose can still scroll to visual top`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 10, offset = 0, size = 100)),
        )

        assertTrue(
            reverseLayoutInfo.shouldExpandReverseNativeEnd(
                contentOffset = 700,
                viewportSize = 300,
                currentContentSize = 1000,
                canScrollForward = true,
                canScrollBackward = true,
            )
        )
    }

    @Test
    fun `reverse vertical pre-expands before native end to avoid bounce resistance`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 10, offset = 0, size = 100)),
        )

        assertTrue(
            reverseLayoutInfo.shouldExpandReverseNativeEnd(
                contentOffset = 400,
                viewportSize = 300,
                currentContentSize = 1000,
                expandThreshold = 300,
                canScrollForward = true,
                canScrollBackward = true,
            )
        )
    }

    @Test
    fun `reverse horizontal expands native end while compose can still scroll to visual start`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Horizontal,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 100),
            visibleItems = listOf(fakeItem(index = 10, offset = 0, size = 100)),
        )

        assertTrue(
            reverseLayoutInfo.shouldExpandReverseNativeEnd(
                contentOffset = 700,
                viewportSize = 300,
                currentContentSize = 1000,
                canScrollForward = true,
                canScrollBackward = true,
            )
        )
    }

    @Test
    fun `reverse native start keeps headroom when visual bottom is reached`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 0, offset = 0, size = 100)),
        )

        assertEquals(
            120,
            reverseLayoutInfo.reverseNativeStartHeadroomDelta(
                contentOffset = 180,
                startHeadroom = 300,
                canScrollForward = true,
                canScrollBackward = false,
            )
        )
    }

    @Test
    fun `reverse native start headroom compensates native overscroll`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 0, offset = 0, size = 100)),
        )

        assertEquals(
            324,
            reverseLayoutInfo.reverseNativeStartHeadroomDelta(
                contentOffset = -24,
                startHeadroom = 300,
                canScrollForward = true,
                canScrollBackward = false,
            )
        )
    }

    @Test
    fun `reverse native start headroom does not block scrolling away from visual bottom`() {
        val reverseLayoutInfo = fakeLayoutInfo(
            reverseLayout = true,
            orientation = Orientation.Vertical,
            totalItemsCount = 20,
            viewportSize = IntSize(300, 300),
            visibleItems = listOf(fakeItem(index = 0, offset = 0, size = 100)),
        )

        assertEquals(
            0,
            reverseLayoutInfo.reverseNativeStartHeadroomDelta(
                contentOffset = 360,
                startHeadroom = 300,
                canScrollForward = true,
                canScrollBackward = false,
            )
        )
    }
}

private fun fakeLayoutInfo(
    reverseLayout: Boolean,
    orientation: Orientation = Orientation.Vertical,
    totalItemsCount: Int,
    viewportSize: IntSize,
    visibleItems: List<FakeLazyListItemInfo>,
): LazyListLayoutInfo = object : LazyListLayoutInfo {
    override val visibleItemsInfo: List<LazyListItemInfo> = visibleItems
    override val viewportStartOffset: Int = 0
    override val viewportEndOffset: Int = if (viewportSize.height != 0) viewportSize.height else viewportSize.width
    override val totalItemsCount: Int = totalItemsCount
    override val viewportSize: IntSize = viewportSize
    override val orientation: Orientation = orientation
    override val reverseLayout: Boolean = reverseLayout
    override val mainAxisItemSpacing: Int = 0
}

private data class FakeLazyListItemInfo(
    override val index: Int,
    override val offset: Int,
    override val size: Int,
    override val key: Any = index,
) : LazyListItemInfo

private fun fakeItem(index: Int, offset: Int, size: Int) =
    FakeLazyListItemInfo(index = index, offset = offset, size = size)

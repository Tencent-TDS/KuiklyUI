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

package com.tencent.kuikly.compose.foundation.drawer

import com.tencent.kuikly.compose.foundation.pager.*

import com.tencent.kuikly.compose.ui.unit.Density

/**
 * Manages per-page size computation, caching, and prefix-sum-based offset lookups
 * for [DrawerInternalPagerState].
 *
 * @param pageSizeProvider A function that returns the main-axis pixel size for the given
 * page index. Called with the [Density] receiver, page index, and the available main-axis
 * space (in pixels).
 */
internal class DrawerSizeTracker(
    var pageSizeProvider: Density.(pageIndex: Int, availableSpace: Int) -> Int
) {
    private var cachedAvailableSpace: Int = -1
    private var cachedSpacing: Int = -1
    private var cachedPageCount: Int = 0
    private var cachedSizes: IntArray = IntArray(0)
    // prefixSums[i] = offset to the START of page i (sum of sizes[0..i-1] + spacings)
    private var prefixSums: LongArray = LongArray(0)

    // Last known density so we can eagerly recompute when the cache is dirty
    // and a lookup is requested before the next measure pass.
    private var lastDensity: Density? = null
    // Preserved across invalidate() so we can recompute without waiting for measure.
    private var lastAvailableSpace: Int = -1

    /**
     * Ensures the size cache is valid for the given parameters. Call before any lookup.
     */
    fun ensureComputed(
        density: Density,
        pageCount: Int,
        availableSpace: Int,
        spacing: Int
    ) {
        lastDensity = density
        lastAvailableSpace = availableSpace
        if (pageCount == cachedPageCount &&
            availableSpace == cachedAvailableSpace &&
            spacing == cachedSpacing
        ) return

        recompute(density, pageCount, availableSpace, spacing)
    }

    private fun recompute(
        density: Density,
        pageCount: Int,
        availableSpace: Int,
        spacing: Int
    ) {
        cachedPageCount = pageCount
        cachedAvailableSpace = availableSpace
        cachedSpacing = spacing

        cachedSizes = IntArray(pageCount) { index ->
            with(density) {
                pageSizeProvider(index, availableSpace).coerceAtLeast(0)
            }
        }

        // prefixSums has (pageCount + 1) entries:
        //   prefixSums[0] = 0
        //   prefixSums[i] = sum of (size[j] + spacing) for j in 0..<i
        prefixSums = LongArray(pageCount + 1)
        for (i in 0 until pageCount) {
            prefixSums[i + 1] = prefixSums[i] + cachedSizes[i] + spacing
        }
    }

    /**
     * If the cache was invalidated (e.g. by [invalidate] after a pageSizeProvider change),
     * eagerly recompute using the last known parameters so that lookups return fresh values
     * even before the next measure pass. This prevents stale offsets from being used in
     * progress calculations between composition and measurement.
     */
    private fun ensureFreshIfDirty() {
        if (cachedAvailableSpace != -1) return
        val density = lastDensity ?: return
        val space = lastAvailableSpace
        if (space <= 0) return
        recompute(density, cachedPageCount, space, cachedSpacing.coerceAtLeast(0))
    }

    fun getPageSize(pageIndex: Int): Int {
        ensureFreshIfDirty()
        if (pageIndex !in cachedSizes.indices) return 0
        return cachedSizes[pageIndex]
    }

    fun getPageSizeWithSpacing(pageIndex: Int): Int {
        return getPageSize(pageIndex) + cachedSpacing.coerceAtLeast(0)
    }

    /**
     * Returns the pixel offset to the start of [pageIndex].
     */
    fun getOffsetForPage(pageIndex: Int): Long {
        ensureFreshIfDirty()
        if (prefixSums.isEmpty()) return 0L
        val clamped = pageIndex.coerceIn(0, cachedPageCount)
        return prefixSums[clamped]
    }

    /**
     * Total content size = sum of all page sizes + (pageCount - 1) * spacing.
     */
    fun getTotalContentSize(): Long {
        ensureFreshIfDirty()
        if (cachedPageCount <= 0 || prefixSums.isEmpty()) return 0L
        // prefixSums[pageCount] includes an extra spacing at the end, so subtract it
        return prefixSums[cachedPageCount] - cachedSpacing.coerceAtLeast(0)
    }

    /**
     * Binary search: given an absolute [offset], find which page it falls in.
     * @return Pair(pageIndex, remainderOffset within that page)
     */
    fun getPageForOffset(offset: Long): Pair<Int, Int> {
        ensureFreshIfDirty()
        if (cachedPageCount <= 0 || prefixSums.isEmpty()) return Pair(0, 0)
        if (offset <= 0) return Pair(0, 0)

        // Binary search in prefixSums for the largest i where prefixSums[i] <= offset
        var lo = 0
        var hi = cachedPageCount
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (prefixSums[mid] <= offset) {
                lo = mid
            } else {
                hi = mid - 1
            }
        }
        val pageIndex = lo.coerceIn(0, cachedPageCount - 1)
        val remainder = (offset - prefixSums[pageIndex]).toInt()
        return Pair(pageIndex, remainder)
    }

    fun invalidate() {
        cachedAvailableSpace = -1
    }
}

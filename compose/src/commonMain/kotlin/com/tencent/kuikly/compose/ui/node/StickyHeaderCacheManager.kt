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

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.compose.ui.geometry.Offset

/**
 * Sticky Header Cache Manager
 * Specifically responsible for managing position cache for multiple sticky headers
 * Used in conjunction with KuiklyScrollInfo
 */
class StickyHeaderCacheManager {

    /**
     * Cache data structure
     */
    private data class StickyHeaderCache(
        val position: Offset
    )

    /**
     * Cache mapping table, using lazyItemKey as identifier
     */
    private val stickyHeaderCacheMap = mutableMapOf<Any, StickyHeaderCache>()

    fun clear() {
        stickyHeaderCacheMap.clear()
    }

    /**
     * Get cached position for sticky header.
     * When not pinned, [currentPos] is always written so layout drift above the header is absorbed.
     * When pinned, reuse the first cached document-flow anchor (#481).
     */
    fun getCachedStickyPosition(itemKey: Any, currentPos: Offset, isPinnedSticky: Boolean): Offset {
        if (!isPinnedSticky) {
            stickyHeaderCacheMap[itemKey] = StickyHeaderCache(currentPos)
            return currentPos
        }
        val cachedData = stickyHeaderCacheMap[itemKey]
        return if (cachedData == null) {
            stickyHeaderCacheMap[itemKey] = StickyHeaderCache(currentPos)
            currentPos
        } else {
            cachedData.position
        }
    }
}
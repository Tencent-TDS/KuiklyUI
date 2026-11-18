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

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class StickyHeaderCacheManagerTest {

    @Test
    fun flowRefreshesWithCurrentPosition() {
        val manager = StickyHeaderCacheManager()
        val key = "header"
        assertEquals(
            Offset(0f, 100f),
            manager.getCachedStickyPosition(key, Offset(0f, 100f), isPinnedSticky = false),
        )
        assertEquals(
            Offset(0f, 200f),
            manager.getCachedStickyPosition(key, Offset(0f, 200f), isPinnedSticky = false),
        )
    }

    @Test
    fun pinnedLocksFirstDocumentPosition() {
        val manager = StickyHeaderCacheManager()
        val key = "header"
        assertEquals(
            Offset(0f, 500f),
            manager.getCachedStickyPosition(key, Offset(0f, 500f), isPinnedSticky = true),
        )
        assertEquals(
            Offset(0f, 500f),
            manager.getCachedStickyPosition(key, Offset(0f, 120f), isPinnedSticky = true),
        )
    }

    @Test
    fun clearDropsAllEntries() {
        val manager = StickyHeaderCacheManager()
        val key = "header"
        manager.getCachedStickyPosition(key, Offset(0f, 10f), isPinnedSticky = true)
        manager.clear()
        assertEquals(
            Offset(0f, 20f),
            manager.getCachedStickyPosition(key, Offset(0f, 20f), isPinnedSticky = true),
        )
    }
}
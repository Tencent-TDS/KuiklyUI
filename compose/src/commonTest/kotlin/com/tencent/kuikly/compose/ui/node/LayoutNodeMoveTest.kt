/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.compose.ui.node

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for [LayoutNode.move].
 *
 * Reference semantics (aligned with androidx.compose LayoutNode): the segment
 * [from, from + count) is removed and re-inserted so that it occupies indices
 * [to, to + count) in the resulting list.
 *
 * The previous implementation computed `toIndex = to + count - 2` for the
 * `from < to` case, which is off by one: nodes landed one slot too early,
 * desynchronizing _foldedChildren from the compose slot table. Later
 * insert/remove/change directives then indexed out of bounds and crashed the
 * frame pump with IndexOutOfBoundsException.
 */
class LayoutNodeMoveTest {

    private fun parentWith(vararg childIds: Int): LayoutNode {
        val parent = LayoutNode()
        childIds.forEach { parent.insertAt(parent.foldedChildren.size, LayoutNode(semanticsId = it)) }
        return parent
    }

    private fun LayoutNode.childIds(): List<Int> = foldedChildren.map { it.semanticsId }

    @Test
    fun movesSingleChildRightByOne() {
        val parent = parentWith(0, 1)
        parent.move(from = 0, to = 1, count = 1)
        // Before fix: [0, 1] (no-op due to off-by-one). Expected: [1, 0].
        assertEquals(listOf(1, 0), parent.childIds())
    }

    @Test
    fun movesSingleChildRightToEnd() {
        val parent = parentWith(0, 1, 2)
        parent.move(from = 0, to = 2, count = 1)
        // Before fix: [1, 0, 2]. Expected: [1, 2, 0].
        assertEquals(listOf(1, 2, 0), parent.childIds())
    }

    @Test
    fun movesSingleChildLeft() {
        val parent = parentWith(0, 1, 2)
        parent.move(from = 2, to = 0, count = 1)
        assertEquals(listOf(2, 0, 1), parent.childIds())
    }

    @Test
    fun movesSegmentRightPreservingSegmentOrder() {
        val parent = parentWith(0, 1, 2, 3, 4)
        parent.move(from = 0, to = 3, count = 2)
        // Segment [0, 1] moves to index 3 -> [2, 3, 4, 0, 1].
        assertEquals(listOf(2, 3, 4, 0, 1), parent.childIds())
    }

    @Test
    fun movesSegmentLeftPreservingSegmentOrder() {
        val parent = parentWith(0, 1, 2, 3, 4)
        parent.move(from = 3, to = 0, count = 2)
        // Segment [3, 4] moves to index 0 -> [3, 4, 0, 1, 2].
        assertEquals(listOf(3, 4, 0, 1, 2), parent.childIds())
    }

    @Test
    fun moveFromEqualsToIsNoOp() {
        val parent = parentWith(0, 1, 2)
        parent.move(from = 1, to = 1, count = 1)
        assertEquals(listOf(0, 1, 2), parent.childIds())
    }
}

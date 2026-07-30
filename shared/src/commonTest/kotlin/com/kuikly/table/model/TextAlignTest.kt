package com.kuikly.table.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TextAlignTest {

    @Test
    fun leftValue() {
        assertEquals("left", TextAlign.LEFT.value)
    }

    @Test
    fun centerValue() {
        assertEquals("center", TextAlign.CENTER.value)
    }

    @Test
    fun rightValue() {
        assertEquals("right", TextAlign.RIGHT.value)
    }

    @Test
    fun enumEntries() {
        val entries = TextAlign.values()
        assertEquals(3, entries.size)
    }
}

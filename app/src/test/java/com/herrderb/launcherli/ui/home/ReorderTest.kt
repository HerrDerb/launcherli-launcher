package com.herrderb.launcherli.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ReorderTest {

    private val list = listOf("a", "b", "c", "d")

    @Test
    fun lessThanOneStepDoesNotMove() {
        val r = reorderStep(list, fromIndex = 1, offsetPx = 40f, stepPx = 50f)
        assertSame(list, r.items)
        assertEquals(1, r.index)
        assertEquals(40f, r.offsetPx)
    }

    @Test
    fun movesSeveralStepsInOneDragAndKeepsTheRemainder() {
        val r = reorderStep(list, fromIndex = 0, offsetPx = 125f, stepPx = 50f)
        assertEquals(listOf("b", "c", "a", "d"), r.items)
        assertEquals(2, r.index)
        assertEquals(25f, r.offsetPx)
    }

    @Test
    fun movesUp() {
        val r = reorderStep(list, fromIndex = 3, offsetPx = -60f, stepPx = 50f)
        assertEquals(listOf("a", "b", "d", "c"), r.items)
        assertEquals(2, r.index)
        assertEquals(-10f, r.offsetPx)
    }

    @Test
    fun clampsAtTheEnds() {
        val r = reorderStep(list, fromIndex = 2, offsetPx = 500f, stepPx = 50f)
        assertEquals(listOf("a", "b", "d", "c"), r.items)
        assertEquals(3, r.index)
    }
}

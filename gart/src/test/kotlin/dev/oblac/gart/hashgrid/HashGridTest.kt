package dev.oblac.gart.hashgrid

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HashGridTest {

    // radius 10 makes cells 7.07 wide: (1, 1) and (2, 1) share a cell, (8, 1) is in the next one

    @Test
    fun aPointNextDoorCountsWhenTheOwnCellHoldsOnlyIgnoredPoints() {
        val g = HashGrid(10f)
        g.insert(Point(1f, 1f), "me")
        g.insert(Point(8f, 1f), "you")
        assertFalse(g.isFree(Point(2f, 1f), setOf("me")))
    }

    @Test
    fun ignoredPointsDoNotCount() {
        val g = HashGrid(10f)
        g.insert(Point(1f, 1f), "me")
        g.insert(Point(8f, 1f), "me")
        assertTrue(g.isFree(Point(2f, 1f), setOf("me")))
    }

    @Test
    fun aPointInTheOwnCellCountsWhenItIsNotIgnored() {
        val g = HashGrid(10f)
        g.insert(Point(1f, 1f), "me")
        g.insert(Point(1.5f, 1.5f), "you")
        assertFalse(g.isFree(Point(2f, 1f), setOf("me")))
        assertFalse(g.isFree(Point(2f, 1f)))
    }

    @Test
    fun pointsWithNoOwnerAlwaysCount() {
        val g = HashGrid(10f)
        g.insert(Point(1f, 1f))
        assertFalse(g.isFree(Point(2f, 1f), setOf("me")))
    }

    @Test
    fun aPointBeyondTheRadiusDoesNotCount() {
        val g = HashGrid(10f)
        g.insert(Point(13f, 1f), "you")
        assertTrue(g.isFree(Point(2f, 1f)))
    }
}

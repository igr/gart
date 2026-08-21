package dev.oblac.gart.hashgrid

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BucketGridTest {

    private fun BucketGrid.idsIn(c: Int): List<Int> {
        val out = ArrayList<Int>()
        forEachIn(c) { out += it }
        return out
    }

    private fun BucketGrid.cellsHolding(id: Int): List<Int> = (0 until cols * rows).filter { id in idsIn(it) }

    @Test
    fun pointLandsInTheOneCellUnderIt() {
        val g = BucketGrid(0f, 0f, 10f, 4, 3)
        g.add(7, 25f, 15f)
        assertEquals(2, g.col(25f))
        assertEquals(1, g.row(15f))
        assertEquals(1 * 4 + 2, g.cell(25f, 15f))
        assertEquals(listOf(6), g.cellsHolding(7))
    }

    @Test
    fun coordinatesOffTheBoxClampToTheEdgeCells() {
        val g = BucketGrid(-5f, -5f, 10f, 4, 3)
        assertEquals(0, g.col(-100f))
        assertEquals(3, g.col(1000f))
        assertEquals(0, g.row(-100f))
        assertEquals(2, g.row(1000f))
        assertEquals(0, g.col(-5f)) // the origin is the left edge of cell 0
        assertEquals(1, g.col(5f))
    }

    @Test
    fun discRegistersInEveryCellItsBoxTouches() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        g.add(1, 25f, 25f, 7f) // box 18..32 on both axes: cols 1..3, rows 1..3
        assertEquals((1..3).flatMap { r -> (1..3).map { c -> r * 5 + c } }, g.cellsHolding(1))
    }

    @Test
    fun aCellGrowsPastItsFirstHandfulAndKeepsOrder() {
        val g = BucketGrid(0f, 0f, 10f, 2, 2)
        for (id in 0 until 20) g.add(id, 3f, 3f)
        assertEquals((0 until 20).toList(), g.idsIn(0))
    }

    @Test
    fun forEachCellTouchingWalksRowsThenColumns() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        val seen = ArrayList<Int>()
        g.forEachCellTouching(25f, 25f, 7f) { seen += it }
        assertEquals(listOf(6, 7, 8, 11, 12, 13, 16, 17, 18), seen)
    }

    @Test
    fun ringEmptyKnowsNothingUntilSealed() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        assertFalse(g.ringEmpty(2, 2, 1)) // nothing there, but unsealed it has to say walk
        g.seal()
        assertTrue(g.ringEmpty(2, 2, 1))
    }

    @Test
    fun ringEmptyLooksAtThatRingOnly() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        g.add(1, 25f, 25f) // cell (2, 2)
        g.seal()
        assertFalse(g.ringEmpty(2, 2, 0))
        assertTrue(g.ringEmpty(2, 2, 1))
        assertTrue(g.ringEmpty(1, 1, 0))
        assertFalse(g.ringEmpty(1, 1, 1)) // (2, 2) sits on ring 1 of (1, 1)
        assertTrue(g.ringEmpty(1, 1, 2))
        assertFalse(g.ringEmpty(0, 0, 2)) // ring 2 of the corner, half of it off the grid
        assertTrue(g.ringEmpty(4, 4, 9)) // a ring entirely off the grid
    }

    @Test
    fun addingAfterSealUnseals() {
        val g = BucketGrid(0f, 0f, 10f, 3, 3)
        g.seal()
        assertTrue(g.ringEmpty(1, 1, 1))
        g.add(5, 15f, 15f) // the centre cell, not on that ring - stale sums would still say empty
        assertFalse(g.ringEmpty(1, 1, 1))
        g.seal()
        assertTrue(g.ringEmpty(1, 1, 1))
    }

    @Test
    fun ringZeroIsTheCellItself() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        val seen = ArrayList<Int>()
        assertTrue(g.ring(2, 2, 0) { seen += it })
        assertEquals(listOf(12), seen)
    }

    @Test
    fun ringOneIsTheEightNeighboursTopRowSidesBottomRow() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        val seen = ArrayList<Int>()
        assertTrue(g.ring(2, 2, 1) { seen += it })
        assertEquals(listOf(6, 7, 8, 11, 13, 16, 17, 18), seen)
    }

    @Test
    fun ringClipsAtTheGridEdgeAndEndsOnceFullyOutside() {
        val g = BucketGrid(0f, 0f, 10f, 3, 3)
        val seen = ArrayList<Int>()
        assertTrue(g.ring(0, 0, 1) { seen += it })
        assertEquals(listOf(1, 3, 4), seen)
        seen.clear()
        assertTrue(g.ring(0, 0, 2) { seen += it })
        assertEquals(listOf(2, 5, 6, 7, 8), seen)
        seen.clear()
        assertFalse(g.ring(0, 0, 3) { seen += it })
        assertTrue(seen.isEmpty())
    }

    @Test
    fun sealedRingSkipsAnEmptyRingWithoutVisiting() {
        val g = BucketGrid(0f, 0f, 10f, 5, 5)
        g.add(1, 45f, 45f) // cell (4, 4)
        g.seal()
        val seen = ArrayList<Int>()
        assertTrue(g.ring(2, 2, 1) { seen += it })
        assertTrue(seen.isEmpty())
        assertTrue(g.ring(2, 2, 2) { seen += it })
        assertTrue(24 in seen)
    }
}

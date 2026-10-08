package dev.oblac.gart.pixels

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RegionsTest {

    // '#' is a wall, '.' is open. the rows go from the top down
    private fun grid(vararg rows: String) = rows.joinToString("").map { it == '#' }.toBooleanArray()

    @Test
    fun idsFollowTheFirstCellRowByRow() {
        val r = labelRegions(grid(
            ".#.",
            ".#.",
            "#..",
        ), 3, 3)
        assertEquals(2, r.count)
        assertContentEquals(intArrayOf(
            0, -1, 1,
            0, -1, 1,
            -1, 1, 1,
        ), r.label)
    }

    @Test
    fun cellsThatTouchOnlyAtACornerAreApart() {
        val r = labelRegions(grid(
            ".#",
            "#.",
        ), 2, 2)
        assertEquals(2, r.count)
        assertContentEquals(intArrayOf(0, -1, -1, 1), r.label)
    }

    @Test
    fun aRegionThatJoinsUnderAWallIsOneRegion() {
        // the two top cells are apart in the first row. the bottom row joins them
        val r = labelRegions(grid(
            ".#.",
            ".#.",
            "...",
        ), 3, 3)
        assertEquals(1, r.count)
        assertContentEquals(intArrayOf(
            0, -1, 0,
            0, -1, 0,
            0, 0, 0,
        ), r.label)
    }

    @Test
    fun aGridOfWallsHasNoRegions() {
        val r = labelRegions(grid("##", "##"), 2, 2)
        assertEquals(0, r.count)
        assertContentEquals(intArrayOf(-1, -1, -1, -1), r.label)
    }

    @Test
    fun theLabelsGoIntoTheArrayThatYouGive() {
        val out = IntArray(4) { 7 }
        val r = labelRegions(grid(".#", "#."), 2, 2, out)
        assertSame(out, r.label)
        assertContentEquals(intArrayOf(0, -1, -1, 1), out)
    }

    @Test
    fun aGridOfTheWrongSizeIsAnError() {
        assertFailsWith<IllegalArgumentException> { labelRegions(BooleanArray(3), 2, 2) }
        assertFailsWith<IllegalArgumentException> { labelRegions(BooleanArray(4), 2, 2, IntArray(5)) }
    }

    @Test
    fun onlyTheRegionInsideTheRingIsOffTheEdge() {
        val r = labelRegions(grid(
            ".....",
            ".###.",
            ".#.#.",
            ".###.",
            ".....",
        ), 5, 5)
        assertEquals(2, r.count)
        assertContentEquals(booleanArrayOf(true, false), r.touchesEdge())
    }

    @Test
    fun aCellInTheFarCornerTouchesTheEdge() {
        val r = labelRegions(grid(
            "###",
            "##.",
        ), 3, 2)
        assertContentEquals(booleanArrayOf(true), r.touchesEdge())
    }
}

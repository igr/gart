package dev.oblac.gart.util

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParallelBandsTest {

    @Test
    fun coversEveryRowExactlyOnce() {
        for (workers in 1..16) {
            val hits = IntArray(100)
            parallelBands(100, workers) { y0, y1 ->
                for (y in y0 until y1) hits[y]++
            }
            assertTrue(hits.all { it == 1 }, "workers=$workers gave ${hits.toList()}")
        }
    }

    @Test
    fun bandsAreContiguousAndInOrder() {
        val bands = mutableListOf<Pair<Int, Int>>()
        parallelBands(50, 4) { y0, y1 -> synchronized(bands) { bands += y0 to y1 } }
        val sorted = bands.sortedBy { it.first }
        assertEquals(0, sorted.first().first)
        assertEquals(50, sorted.last().second)
        sorted.zipWithNext { a, b -> assertEquals(a.second, b.first) }
    }

    /**
     * The reason the whole thing exists. Float addition is neither associative nor commutative,
     * so a sum accumulated in a different order comes out different in the last bit. Banding
     * gives every row one owner and every owner the same walk over the input, so the result
     * cannot depend on how many cores turned up.
     */
    @Test
    fun resultDoesNotDependOnWorkerCount() {
        // stand-in for a splat: lots of values landing in shared rows, in a deliberately
        // awkward order and magnitude range so any reordering shows up
        fun render(workers: Int): FloatArray {
            val out = FloatArray(64 * 8)
            parallelBands(64, workers) { y0, y1 ->
                for (i in 0 until 5000) {
                    val y = (i * 37) % 64
                    if (y < y0 || y >= y1) continue
                    val x = (i * 53) % 8
                    out[y * 8 + x] += if (i % 3 == 0) 1e7f else 1e-4f * (i % 11)
                }
            }
            return out
        }

        val one = render(1)
        for (workers in 2..16) {
            assertContentEquals(one, render(workers), "diverged at workers=$workers")
        }
    }

    @Test
    fun runsInlineForOneWorker() {
        val caller = Thread.currentThread()
        var ran: Thread? = null
        parallelBands(10, 1) { _, _ -> ran = Thread.currentThread() }
        assertEquals(caller, ran)
    }

    @Test
    fun neverCutsMoreBandsThanRows() {
        var bands = 0
        parallelBands(3, 12) { _, _ -> synchronized(this) { bands++ } }
        assertEquals(3, bands)
    }

    @Test
    fun emptyHeightIsANoop() {
        var ran = false
        parallelBands(0) { _, _ -> ran = true }
        parallelBands(-5) { _, _ -> ran = true }
        assertTrue(!ran)
    }

    @Test
    fun workerDefaultIsSane() {
        assertTrue(defaultWorkers in 1..12, "defaultWorkers was $defaultWorkers")
    }
}

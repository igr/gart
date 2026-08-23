package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RandomBetweenTest {

    @Test
    fun `int between is inclusive on both ends`() {
        val rnd = Random(7)
        val seen = HashSet<Int>()
        repeat(2000) { seen += rnd.between(3, 6) }
        assertEquals(setOf(3, 4, 5, 6), seen)
    }

    @Test
    fun `int between matches the plain nextInt form`() {
        val a = Random(11)
        val b = Random(11)
        repeat(500) {
            assertEquals(-2 + b.nextInt(5 + 2 + 1), a.between(-2, 5))
        }
    }

    @Test
    fun `degenerate int range returns from without drawing`() {
        val a = Random(3)
        val b = Random(3)
        assertEquals(4, a.between(4, 4))
        assertEquals(9, a.between(9, 1))
        // the sequence is untouched: the next real draw matches an unused generator
        assertEquals(b.nextInt(), a.nextInt())
    }

    @Test
    fun `float between stays inside the range`() {
        val rnd = Random(5)
        repeat(1000) {
            val v = rnd.between(-1.5f, 2.5f)
            assertTrue(v >= -1.5f && v < 2.5f, "out of range: $v")
        }
    }
}

package dev.oblac.gart.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FloatRangeTest {

    @Test
    fun testRangeOfFloats() {
        val floatRange = FloatRange.of(-1, 1, 10)
        assertEquals(10, floatRange.count())
    }

    @Test
    fun testRangeOfFloats_first_Last() {
        val floatRange = FloatRange.of(-1, 1, 10)
        val i = floatRange.iterator()
        val first = i.next()
        repeat(8) { i.next() }
        val last = i.next()
        assertEquals(-1f, first, 0.001f)
        assertEquals(1f, last, 0.001f)
    }
}

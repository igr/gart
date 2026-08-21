package dev.oblac.gart.io

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ParamsTest {

    private fun <T> withProperty(key: String, value: String?, body: () -> T): T {
        if (value == null) System.clearProperty(key) else System.setProperty(key, value)
        try {
            return body()
        } finally {
            System.clearProperty(key)
        }
    }

    @Test
    fun pf_range_returnsDefaultWhenNotOverridden() {
        val v = withProperty("ptest.a", null) { pf("ptest.a", 0.5f, 0f..1f) }
        assertEquals(0.5f, v)
    }

    @Test
    fun pf_range_acceptsOverrideInsideRange() {
        val v = withProperty("ptest.b", "0.75") { pf("ptest.b", 0.5f, 0f..1f) }
        assertEquals(0.75f, v)
    }

    @Test
    fun pf_range_rejectsOverrideOutsideRange() {
        val e = assertFailsWith<IllegalArgumentException> {
            withProperty("ptest.c", "1.5") { pf("ptest.c", 0.5f, 0f..1f) }
        }
        assertTrue(e.message!!.contains("ptest.c"), e.message)
        assertTrue(e.message!!.contains("between 0.0 and 1.0"), e.message)
    }

    @Test
    fun pf_range_rejectsDefaultOutsideRange() {
        assertFailsWith<IllegalArgumentException> {
            withProperty("ptest.d", null) { pf("ptest.d", 3f, 0f..1f) }
        }
    }

    @Test
    fun pf_range_boundsAreInclusive() {
        val lo = withProperty("ptest.e", "0") { pf("ptest.e", 0.5f, 0f..1f) }
        val hi = withProperty("ptest.e", "1") { pf("ptest.e", 0.5f, 0f..1f) }
        assertEquals(0f, lo)
        assertEquals(1f, hi)
    }

    @Test
    fun pi_range_acceptsValueInsideRange() {
        val v = withProperty("ptest.f", "4") { pi("ptest.f", 2, 1..4) }
        assertEquals(4, v)
    }

    @Test
    fun pi_range_rejectsValueOutsideRange() {
        val e = assertFailsWith<IllegalArgumentException> {
            withProperty("ptest.g", "9") { pi("ptest.g", 2, 1..4) }
        }
        assertTrue(e.message!!.contains("ptest.g"), e.message)
        assertTrue(e.message!!.contains("between 1 and 4"), e.message)
    }
}

package dev.oblac.gart.vector

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MutableVec2Test {

    @Test
    fun startsAtZero() {
        val v = MutableVec2()
        assertEquals(0f, v.x)
        assertEquals(0f, v.y)
    }

    @Test
    fun setReturnsTheSameInstance() {
        // the whole reason it exists - a setter that allocated would defeat the point
        val v = MutableVec2(1f, 2f)
        assertSame(v, v.set(3f, 4f))
        assertEquals(3f, v.x)
        assertEquals(4f, v.y)
    }

    @Test
    fun accumulates() {
        val v = MutableVec2()
        v.add(1f, 2f)
        v += Vec2(10f, 20f)
        v -= Vec2(1f, 1f)
        assertEquals(10f, v.x)
        assertEquals(21f, v.y)
    }

    @Test
    fun scales() {
        val v = MutableVec2(3f, -4f)
        v *= 2f
        assertEquals(6f, v.x)
        assertEquals(-8f, v.y)
        v /= 4f
        assertEquals(1.5f, v.x)
        assertEquals(-2f, v.y)
    }

    @Test
    fun lengthMatchesVec2() {
        val m = MutableVec2(3f, 4f)
        assertEquals(Vec2(3f, 4f).length(), m.length())
        assertEquals(5f, m.length())
    }

    @Test
    fun freezesToAnIndependentVec2() {
        val m = MutableVec2(1f, 2f)
        val frozen = m.toVec2()
        m.set(9f, 9f)
        assertEquals(1f, frozen.x)
        assertEquals(2f, frozen.y)
    }

    @Test
    fun destructures() {
        val (x, y) = MutableVec2(7f, 8f)
        assertEquals(7f, x)
        assertEquals(8f, y)
    }

    @Test
    fun setFromEitherKind() {
        val v = MutableVec2()
        v.set(Vec2(1f, 2f))
        assertEquals(Vec2(1f, 2f), v.toVec2())
        v.set(MutableVec2(3f, 4f))
        assertEquals(Vec2(3f, 4f), v.toVec2())
        v.zero()
        assertEquals(Vec2.ZERO, v.toVec2())
    }
}

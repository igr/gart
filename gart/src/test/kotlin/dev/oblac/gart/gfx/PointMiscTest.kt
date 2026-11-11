package dev.oblac.gart.gfx

import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PointMiscTest {

    private val a = Point(1f, 2f)
    private val b = Point(4f, 6f)

    @Test
    fun vecToGoesFromThisPointToTheOther() {
        assertEquals(Vec2(3f, 4f), a.vecTo(b))
        assertEquals(Vec2(-3f, -4f), b.vecTo(a))
    }

    @Test
    fun vecToTheSamePointIsZero() {
        assertEquals(Vec2.ZERO, a.vecTo(a))
    }

    @Test
    fun offsetByVecToLandsOnTheOtherPoint() {
        val p = a.offset(a.vecTo(b))
        assertEquals(b.x, p.x)
        assertEquals(b.y, p.y)
    }
}

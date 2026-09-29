@file:Suppress("DEPRECATION")

package dev.oblac.gart.flow

import dev.oblac.gart.Dimension
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AsVectorFieldTest {

    private fun assertVec(x: Float, y: Float, v: Vec2) {
        assertEquals(x, v.x, 1e-5f, "x of $v")
        assertEquals(y, v.y, 1e-5f, "y of $v")
    }

    @Test
    fun oldFieldsReadAsVectorFields() {
        val old = FlowField.from(Dimension(10, 10)) { x, y -> Vec2(x.toFloat(), 10f * y) }
        val f = old.asVectorField()
        assertVec(3f, 50f, f(Point(3.7f, 5.2f)))  // the old grid is nearest pixel, by truncation
        assertVec(0f, 0f, f(Point(-1f, 5f)))
        assertVec(0f, 0f, f(Point(5f, 10f)))
    }
}

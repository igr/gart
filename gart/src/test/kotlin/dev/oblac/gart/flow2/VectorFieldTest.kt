package dev.oblac.gart.flow2

import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VectorFieldTest {

    private fun assertVec(x: Float, y: Float, v: Vec2, eps: Float = 1e-5f) {
        assertEquals(x, v.x, eps, "x of $v")
        assertEquals(y, v.y, eps, "y of $v")
    }

    @Test
    fun anglesPointAlongTheAngle() {
        assertVec(1f, 0f, VectorField.angles { _, _ -> 0f }(Point(3f, 4f)))
        // y grows down, so a quarter turn points down the canvas
        assertVec(0f, 1f, VectorField.angles { _, _ -> HALF_PIf }(Point(3f, 4f)))
    }

    @Test
    fun anglesReadTheAngleAtThePoint() {
        val f = VectorField.angles { x, _ -> if (x < 10f) 0f else HALF_PIf }
        assertVec(1f, 0f, f(Point(5f, 0f)))
        assertVec(0f, 1f, f(Point(15f, 0f)))
    }

    @Test
    fun vortexSpinsClockwiseOnScreen() {
        val f = VectorField.vortex(0f, 0f)
        assertVec(0f, 1f, f(Point(10f, 0f)))
        assertVec(-1f, 0f, f(Point(0f, 10f)))
    }

    @Test
    fun negativeSpinTurnsTheOtherWay() {
        assertVec(0f, -1f, VectorField.vortex(0f, 0f, spin = -1f)(Point(10f, 0f)))
    }

    @Test
    fun vortexPullPointsToTheCentre() {
        val f = VectorField.vortex(5f, 5f, spin = 0f, pull = 1f)
        assertVec(-1f, 0f, f(Point(15f, 5f)))
        assertVec(0f, -2f, VectorField.vortex(5f, 5f, spin = 0f, pull = 2f)(Point(5f, 30f)))
    }

    @Test
    fun vortexReachFadesWithDistance() {
        val f = VectorField.vortex(0f, 0f, reach = 10f)
        assertVec(0f, 0.5f, f(Point(10f, 0f)))  // 1 / (1 + 10² / 10²)
        assertVec(0f, 0.1f, f(Point(30f, 0f)))  // 1 / (1 + 30² / 10²)
    }

    @Test
    fun vortexIsZeroAtItsCentre() {
        assertVec(0f, 0f, VectorField.vortex(7f, 7f, spin = 1f, pull = 1f)(Point(7f, 7f)))
    }

    @Test
    fun curlRunsAlongTheContours() {
        // a potential rising to the right has vertical contours; its curl runs up them
        assertVec(0f, -1f, VectorField.curl({ x, _ -> x })(Point(3f, 4f)))
        assertVec(1f, 0f, VectorField.curl({ _, y -> y })(Point(3f, 4f)))
        assertVec(0f, -2f, VectorField.curl({ x, _ -> 2f * x }, eps = 0.5f)(Point(3f, 4f)))
    }

    @Test
    fun plusAddsTheVectors() {
        val f = VectorField.angles { _, _ -> 0f } + VectorField.vortex(0f, 0f)
        assertVec(1f, 1f, f(Point(10f, 0f)))
        val out = MutableVec2(9f, 9f)
        f.at(10f, 0f, out)
        assertEquals(1f, out.x, 1e-5f)
        assertEquals(1f, out.y, 1e-5f)
    }

    @Test
    fun timesScalesTheVectors() {
        assertVec(-3f, 0f, (VectorField.angles { _, _ -> 0f } * -3f)(Point(1f, 1f)))
    }

}

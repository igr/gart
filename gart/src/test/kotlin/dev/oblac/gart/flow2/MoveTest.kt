package dev.oblac.gart.flow2

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MoveTest {

    private val right = VectorField { _, _, out -> out.set(1f, 0f) }

    @Test
    fun eachPointMovesOneStepInOrder() {
        val seen = mutableListOf<Pair<Point, Point>>()
        val moved = right.move(listOf(Point(0f, 0f), Point(5f, 5f)), step = 2f) { a, b -> seen += a to b }
        assertEquals(listOf(Point(2f, 0f), Point(7f, 5f)), moved)
        assertEquals(listOf(Point(0f, 0f) to Point(2f, 0f), Point(5f, 5f) to Point(7f, 5f)), seen)
    }

    @Test
    fun aPointWithNoDirectionIsDropped() {
        val wall = VectorField { x, _, out -> if (x < 5f) out.set(1f, 0f) else out.zero() }
        var calls = 0
        val moved = wall.move(listOf(Point(1f, 0f), Point(6f, 0f))) { _, _ -> calls++ }
        assertEquals(listOf(Point(2f, 0f)), moved)
        assertEquals(1, calls)
    }

    @Test
    fun aPointOutsideTheBoundsOrAboutToLeaveThemIsDropped() {
        val moved = right.move(listOf(Point(9.5f, 5f), Point(-1f, 5f), Point(3f, 5f)), bounds = Rect(0f, 0f, 10f, 10f))
        assertEquals(listOf(Point(4f, 5f)), moved)
    }

    @Test
    fun withVelocityASlowPointIsKept() {
        val fading = VectorField { x, _, out -> out.set(kotlin.math.exp(-20f * x), 0f) }
        val moved = fading.move(listOf(Point(0f, 0f)), velocity = true)
        assertEquals(1, moved.size)
        assertEquals(4.54e-5f, moved[0].x, 1e-7f)
    }

    @Test
    fun withVelocityAPointThatGoesNowhereIsDropped() {
        val sink = VectorField.vortex(50f, 50f, spin = 0f, pull = 1f)
        var calls = 0
        val moved = sink.move(listOf(Point(50.3f, 50f)), rk = Integrator.RK4, velocity = true) { _, _ -> calls++ }
        assertEquals(emptyList(), moved)
        assertEquals(0, calls)
    }

    @Test
    fun withVelocityTheVectorSetsTheSpeed() {
        val fast = VectorField { _, _, out -> out.set(3f, 0f) }
        assertEquals(listOf(Point(1.5f, 0f)), fast.move(listOf(Point(0f, 0f)), step = 0.5f, velocity = true))
    }
}

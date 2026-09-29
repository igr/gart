package dev.oblac.gart.flow2

import dev.oblac.gart.Dimension
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VectorGridTest {

    private val linear = VectorField { x, y, out -> out.set(x, y) }
    private val d = Dimension(96, 64)

    private fun assertVec(x: Float, y: Float, v: Vec2) {
        assertEquals(x, v.x, 1e-3f, "x of $v")
        assertEquals(y, v.y, 1e-3f, "y of $v")
    }

    @Test
    fun aLinearFieldBakesExactlyBetweenTheNodes() {
        val g = linear.bake(d, cell = 8f)
        assertVec(13.3f, 41.7f, g(Point(13.3f, 41.7f)))
        assertVec(95.5f, 0.25f, g(Point(95.5f, 0.25f)))
    }

    @Test
    fun bakingReadsTheSourceOnlyAtTheNodes() {
        // x² is 0 and 64 on the nodes at 0 and 8, so halfway the grid gives their mean, not 16
        val g = VectorField { x, _, out -> out.set(x * x, 0f) }.bake(d, cell = 8f)
        assertVec(32f, 0f, g(Point(4f, 4f)))
        assertVec(64f, 0f, g(Point(8f, 4f)))
    }

    @Test
    fun theMarginBakesPastTheCanvas() {
        val g = linear.bake(d, cell = 8f, margin = 16f)
        assertVec(-10f, -5f, g(Point(-10f, -5f)))
        assertVec(105f, 70f, g(Point(105f, 70f)))
    }

    @Test
    fun theGridReachesTheFarEdgeWhenTheCellDoesNotDivideIt() {
        val g = linear.bake(Dimension(100, 30), cell = 8f)
        assertVec(99.5f, 29.5f, g(Point(99.5f, 29.5f)))
    }

    @Test
    fun offTheGridTheEdgeValueHolds() {
        val g = linear.bake(d, cell = 8f)
        assertVec(96f, 0f, g(Point(150f, -20f)))
        assertVec(0f, 30f, g(Point(-5f, 30f)))
    }
}

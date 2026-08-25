package dev.oblac.gart.brush

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.gfx.Line
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HatchLinesTest {

    private fun square(l: Float, t: Float, r: Float, b: Float): Path =
        PathBuilder().addRect(Rect(l, t, r, b)).detach()

    private fun onSquareEdge(p: Point, l: Float, t: Float, r: Float, b: Float, tol: Float = 0.5f): Boolean {
        val inside = p.x >= l - tol && p.x <= r + tol && p.y >= t - tol && p.y <= b + tol
        val edge = min(min(abs(p.x - l), abs(p.x - r)), min(abs(p.y - t), abs(p.y - b)))
        return inside && edge <= tol
    }

    @Test
    fun horizontalLinesAcrossASquare() {
        val lines = hatchLines(square(0f, 0f, 100f, 100f), Hatch(dist = 10f, angle = Degrees(0f)))
        assertEquals(10, lines.size)
        lines.forEachIndexed { i, line ->
            assertEquals(line.a.y, line.b.y, 1e-3f)
            assertEquals(5f + 10f * i, line.a.y, 1e-3f)
            assertEquals(0f, min(line.a.x, line.b.x), 0.5f)
            assertEquals(100f, maxOf(line.a.x, line.b.x), 0.5f)
        }
    }

    @Test
    fun holeSplitsTheLine() {
        val path = PathBuilder()
            .addRect(Rect(0f, 0f, 100f, 100f))
            .addRect(Rect(30f, 30f, 70f, 70f))
            .detach()
        val lines = hatchLines(path, Hatch(dist = 10f, angle = Degrees(0f)))
        val byRow = lines.groupBy { (it.a.y + 0.5f).toInt() }
        assertEquals(10, byRow.size)
        for ((y, segs) in byRow) {
            if (y > 30 && y < 70) {
                assertEquals(2, segs.size, "row $y")
                val xs = segs.flatMap { listOf(it.a.x, it.b.x) }.sorted()
                assertEquals(listOf(0f, 30f, 70f, 100f), xs.map { (it + 0.5f).toInt().toFloat() })
            } else {
                assertEquals(1, segs.size, "row $y")
            }
        }
    }

    @Test
    fun diagonalEndsStayOnTheBoundary() {
        val lines = hatchLines(square(10f, 20f, 110f, 120f), Hatch(dist = 7f, angle = Degrees(45f)))
        assertTrue(lines.size > 15)
        for (line in lines) {
            assertTrue(onSquareEdge(line.a, 10f, 20f, 110f, 120f), "a=${line.a}")
            assertTrue(onSquareEdge(line.b, 10f, 20f, 110f, 120f), "b=${line.b}")
            // runs at 45 degrees
            val dx = line.b.x - line.a.x
            val dy = line.b.y - line.a.y
            assertEquals(abs(dx), abs(dy), 0.01f * abs(dx) + 0.5f)
        }
    }

    @Test
    fun overshootRunsPastTheEdge() {
        val lines = hatchLines(square(0f, 0f, 100f, 100f), Hatch(dist = 25f, angle = Degrees(0f), overshoot = 5f))
        for (line in lines) {
            assertEquals(-5f, min(line.a.x, line.b.x), 0.5f)
            assertEquals(105f, maxOf(line.a.x, line.b.x), 0.5f)
        }
    }

    @Test
    fun gradientOpensTheSpacing() {
        val lines = hatchLines(square(0f, 0f, 100f, 200f), Hatch(dist = 10f, angle = Degrees(0f), gradient = 1f))
        val ys = lines.map { it.a.y }
        val gaps = ys.zipWithNext { a, b -> b - a }
        assertTrue(gaps.size > 3)
        gaps.zipWithNext { a, b -> assertTrue(b > a, "gaps should grow: $gaps") }
    }

    @Test
    fun randIsSeededAndStaysInTheShape() {
        val hatch = Hatch(dist = 10f, angle = Degrees(30f), rand = 0.5f)
        val a = hatchLines(square(0f, 0f, 100f, 100f), hatch, Random(3))
        val b = hatchLines(square(0f, 0f, 100f, 100f), hatch, Random(3))
        val c = hatchLines(square(0f, 0f, 100f, 100f), hatch, Random(4))
        assertEquals(a, b)
        assertTrue(a != c)
        assertTrue(a.size in 6..16, "got ${a.size} lines")
        for (line in a) {
            assertTrue(line.a.x > -6f && line.a.x < 106f && line.a.y > -6f && line.a.y < 106f)
        }
    }

    @Test
    fun emptyPathGivesNothing() {
        assertEquals(emptyList<Line>(), hatchLines(Path(), Hatch(dist = 5f)))
    }
}

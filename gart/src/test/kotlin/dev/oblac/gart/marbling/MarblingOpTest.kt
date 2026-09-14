package dev.oblac.gart.marbling

import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarblingOpTest {

    private val rnd = Random(3)
    private val points = List(200) { MutableVec2(rnd.nextFloat() * 400f, rnd.nextFloat() * 400f) }

    private fun roundTrips(op: MarblingOp, tol: Float = 1e-2f) {
        for (q in points) {
            val p = MutableVec2(q.x, q.y)
            op.forward(p)
            op.inverse(p)
            assertTrue(abs(p.x - q.x) < tol && abs(p.y - q.y) < tol, "$op moved (${q.x}, ${q.y}) to (${p.x}, ${p.y})")
        }
    }

    @Test
    fun dropRoundTripsAndEmptiesItsDisc() {
        val drop = Drop(200f, 200f, 50f, 0xFF0000FF.toInt())
        roundTrips(drop)
        for (q in points) {
            val p = MutableVec2(q.x, q.y)
            drop.forward(p)
            assertFalse(drop.contains(p.x, p.y), "paint ended up inside the fresh drop")
        }
        // paint sqrt(3) r out sits at 2r after: sqrt(3 r^2 + r^2)
        val p = MutableVec2(200f + 50f * 1.7320508f, 200f)
        drop.forward(p)
        assertEquals(300f, p.x, 1e-2f)
        assertEquals(200f, p.y, 1e-2f)
    }

    @Test
    fun combRoundTripsStraightAndWavy() {
        roundTrips(Comb(200f, 200f, Vec2(1f, 0f), 120f, 25f, floatArrayOf(-60f, 0f, 60f)))
        roundTrips(Comb(200f, 200f, Vec2(0f, 1f), 150f, 20f, floatArrayOf(-40f, 40f), amplitude = 30f, wavelength = 140f, phase = 0.7f))
        roundTrips(Comb(100f, 50f, Vec2(3f, 4f), 90f, 15f, floatArrayOf(0f), amplitude = 20f, wavelength = 80f))
    }

    @Test
    fun combPullsAlongItsDirectionAndHalvesAtC() {
        val comb = Comb(0f, 100f, Vec2(1f, 0f), 80f, 20f)
        val on = MutableVec2(50f, 100f)
        comb.forward(on)
        assertEquals(130f, on.x, 1e-3f)
        assertEquals(100f, on.y, 1e-3f)
        val off = MutableVec2(50f, 120f)
        comb.forward(off)
        assertEquals(90f, off.x, 1e-3f)
        assertEquals(120f, off.y, 1e-3f)
    }

    @Test
    fun tinesAddUp() {
        val both = Comb(0f, 0f, Vec2(1f, 0f), 50f, 30f, floatArrayOf(-20f, 20f))
        val a = Comb(0f, 0f, Vec2(1f, 0f), 50f, 30f, floatArrayOf(-20f))
        val b = Comb(0f, 0f, Vec2(1f, 0f), 50f, 30f, floatArrayOf(20f))
        for (q in points) {
            val p1 = MutableVec2(q.x, q.y)
            both.forward(p1)
            val p2 = MutableVec2(q.x, q.y)
            a.forward(p2)
            b.forward(p2)
            assertEquals(p1.x, p2.x, 1e-3f)
            assertEquals(p1.y, p2.y, 1e-3f)
        }
    }

    @Test
    fun whirlAndVortexRoundTripAndKeepTheRadius() {
        for (op in listOf(Whirl(200f, 200f, 80f, 100f, 30f), Whirl(200f, 200f, 0f, 60f, 40f))) {
            roundTrips(op, tol = 5e-2f)
            for (q in points) {
                val p = MutableVec2(q.x, q.y)
                op.forward(p)
                assertEquals(hypot(q.x - 200f, q.y - 200f), hypot(p.x - 200f, p.y - 200f), 1e-2f)
            }
        }
    }

    @Test
    fun waveRoundTripsAcrossAndAlong() {
        roundTrips(Wave(Vec2(0f, 1f), Vec2(1f, 0f), 12f, 60f)) // side to side, a pure shear
        roundTrips(Wave(Vec2(0f, 1f), Vec2(0f, 1f), 6f, 60f, 0.3f)) // stretch and squeeze, needs the solve
        roundTrips(Wave(Vec2(0f, 1f), Vec2(0.70710677f, 0.70710677f), 8f, 90f))
    }

    @Test
    fun shiftAndCustomRoundTrip() {
        roundTrips(Shift(13f, -7f))
        roundTrips(Custom({ it.set(it.x * 2f, it.y + 1f) }, { it.set(it.x / 2f, it.y - 1f) }))
    }
}

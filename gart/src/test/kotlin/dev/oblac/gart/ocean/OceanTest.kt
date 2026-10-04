package dev.oblac.gart.ocean

import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OceanTest {

    // small grids keep the tests quick, the physics does not care
    private fun ocean(seed: Int = 3, swell: Float = 1f, chop: Float = 0.9f, wind: Float = 9f, ripples: Float = 1f) =
        Ocean(seed, wind = wind, swell = swell, chop = chop, ripples = ripples, size = 64)

    private fun points(n: Int, span: Float, seed: Int = 1): List<Pair<Float, Float>> {
        val rnd = Random(seed)
        return List(n) { rnd.nextFloat() * span to rnd.nextFloat() * span }
    }

    @Test
    fun theSameSeedGivesTheSameSea() {
        val a = ocean().probe()
        val b = ocean().probe()
        val c = ocean(seed = 4).probe()
        for ((x, z) in points(50, 300f)) assertEquals(a.height(x, z, 0f), b.height(x, z, 0f))
        assertNotEquals(a.height(10f, 20f, 0f), c.height(10f, 20f, 0f))
    }

    @Test
    fun fourStandardDeviationsAreTheSignificantHeight() {
        val o = ocean(chop = 0f)
        val p = o.probe()
        val h = points(20000, o.peakLength * 8f).map { (x, z) -> p.height(x, z, 0f).toDouble() }
        val m = h.average()
        val sd = sqrt(h.sumOf { (it - m) * (it - m) } / h.size)
        assertEquals(o.hs.toDouble(), 4 * sd, 0.1 * o.hs)
    }

    @Test
    fun swellLiftsOnlyTheLongWaves() {
        // on a 64 grid the middle tile is gone from 0.193 peak lengths, the long tile starts to fade
        // at 0.25. so this footprint keeps the long tile alone
        val o = ocean(chop = 0f)
        val fp = 0.22f * o.peakLength
        val one = o.probe()
        val two = ocean(chop = 0f, swell = 2f).probe()
        for ((x, z) in points(50, 300f)) {
            assertEquals(2f * one.height(x, z, fp), two.height(x, z, fp), 1e-5f)
            val shortOne = one.height(x, z, 0f) - one.height(x, z, fp)
            val shortTwo = two.height(x, z, 0f) - two.height(x, z, fp)
            assertEquals(shortOne, shortTwo, 1e-5f)
        }
    }

    @Test
    fun ripplesLiftOnlyTheShortWaves() {
        // the same footprint as in the swell test keeps the long tile alone
        val o = ocean(chop = 0f)
        val fp = 0.22f * o.peakLength
        val one = o.probe()
        val two = ocean(chop = 0f, ripples = 2f).probe()
        for ((x, z) in points(50, 300f)) {
            assertEquals(one.height(x, z, fp), two.height(x, z, fp), 1e-5f)
            val shortOne = one.height(x, z, 0f) - one.height(x, z, fp)
            val shortTwo = two.height(x, z, 0f) - two.height(x, z, fp)
            assertEquals(2f * shortOne, shortTwo, 1e-5f)
        }
    }

    @Test
    fun ripplesTakeTheirSlopeFromTheRoughness() {
        // the ripples are the wind's own waves, the more of them the tiles draw, the less is left
        val one = ocean().residualSlope
        val two = ocean(ripples = 2f).residualSlope
        assertTrue(two < one, "ripples 2 leave $two, ripples 1 leave $one")
    }

    @Test
    fun peakFactorOneIsPiersonMoskowitz() {
        // gamma 1 adds nothing anywhere, gamma 3.3 makes the peak itself 3.3 times stronger
        for (w in listOf(0.5f, 0.9f, 1f, 1.1f, 2f)) assertEquals(1f, peakFactor(w, 1f, 1f))
        assertEquals(3.3f, peakFactor(1f, 1f, 3.3f), 1e-6f)
        // half way down: one sigma off the peak gives gamma^(e^-1/2). sigma is 0.07 below, 0.09 above
        val half = Math.pow(3.3, Math.exp(-0.5)).toFloat()
        assertEquals(half, peakFactor(0.93f, 1f, 3.3f), 1e-4f)
        assertEquals(half, peakFactor(1.09f, 1f, 3.3f), 1e-4f)
    }

    @Test
    fun aSharperPeakPutsMoreOfTheSeaIntoTheLongWaves() {
        fun longShare(gamma: Float): Double {
            val o = Ocean(3, gamma = gamma, size = 64)
            val vars = o.tiles.map { t -> (0 until 64 * 64).sumOf { t.f[it * 8].toDouble() * t.f[it * 8] } }
            return vars[0] / vars.sum()
        }
        assertTrue(longShare(3.3f) > longShare(1f))
    }

    @Test
    fun peakFactorUnderOneIsRejected() {
        assertFailsWith<IllegalArgumentException> { Ocean(1, gamma = 0.5f, size = 8) }
    }

    @Test
    fun theSlopeBoundHoldsOnAWildSea() {
        // steep chop pinches the crests, so the surface the march sees is steeper than the unmoved
        // one. the bound must hold there too, in every direction
        val p = Ocean(6, swell = 4f, ripples = 4f, spread = 40f).probe()
        val rnd = Random(2)
        val e = 0.01f
        for (fp in listOf(0f, 0.5f)) {
            val bound = p.slope(fp)
            var worst = 0f
            repeat(20000) {
                // the first points sit round the spot the review found, slope 8.23 against 7.22
                val near = it < 2000
                val x = if (near) 68.492f + (rnd.nextFloat() - 0.5f) * 0.4f else rnd.nextFloat() * 300f
                val z = if (near) 14.96f + (rnd.nextFloat() - 0.5f) * 0.4f else rnd.nextFloat() * 300f
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val h = p.height(x, z, fp)
                worst = max(worst, abs(p.height(x + e * cos(a), z + e * sin(a), fp) - h) / e)
            }
            assertTrue(worst <= bound, "fp $fp: slope $worst over the bound $bound")
        }
    }

    @Test
    fun theNormalPointsUpAlsoWhereTheChopFoldsTheSurface() {
        val p = ocean().probe()
        // no fold: the cross of the tangents is (-0.5, 1, 0), as it is
        p.normalFrom(0.5f, 0f, 1f, 1f, 0f)
        assertEquals(-0.5f / sqrt(1.25f), p.nx, 1e-6f)
        assertEquals(1f / sqrt(1.25f), p.ny, 1e-6f)
        // a fold, squeeze -0.5: the cross (-0.5, -0.5, 0) points down, so all of it turns over
        p.normalFrom(0.5f, 0f, -0.5f, 1f, 0f)
        assertEquals(sqrt(0.5f), p.nx, 1e-6f)
        assertEquals(sqrt(0.5f), p.ny, 1e-6f)
        assertEquals(0f, p.nz, 1e-6f)
        // a thin fold, squeeze -0.02: turned over to (0.5, 0.02, 0), then y is lifted to 0.05
        p.normalFrom(0.5f, 0f, -0.02f, 1f, 0f)
        assertEquals(0.5f / sqrt(0.2525f), p.nx, 1e-6f)
        assertEquals(0.05f / sqrt(0.2525f), p.ny, 1e-6f)
    }

    @Test
    fun heightStaysUnderTop() {
        val o = ocean()
        val p = o.probe()
        for ((x, z) in points(5000, 500f)) assertTrue(abs(p.height(x, z, 0f)) <= o.top)
    }

    @Test
    fun slopesStayUnderTheBound() {
        val p = ocean().probe()
        val bound = p.slope(0f)
        val e = 0.01f
        for ((x, z) in points(5000, 500f)) {
            val h = p.height(x, z, 0f)
            val s = max(abs(p.height(x + e, z, 0f) - h), abs(p.height(x, z + e, 0f) - h)) / e
            assertTrue(s <= bound, "slope $s over $bound at $x, $z")
        }
    }

    @Test
    fun theFoamShareIsTheOneAskedForOnTheMovedSurface() {
        // default chop: a pinched patch covers less of the world, so the share is counted there
        val o = ocean()
        val share = 3.84e-6f * Math.pow(9.0, 3.41).toFloat() * 2f
        val limit = o.foamLimit(2f)
        val p = o.probe()
        val pts = points(20000, o.peakLength * 8f, seed = 9)
        val foam = pts.count { (x, z) ->
            p.at(x, z, 0f)
            p.squeeze < limit
        }
        assertEquals(share, foam.toFloat() / pts.size, 0.003f)
        assertEquals(-10f, o.foamLimit(0f))
    }

    @Test
    fun heightAndAtDescribeTheSameSurface() {
        val p = ocean(swell = 2.6f).probe()
        for (fp in listOf(0f, 0.5f, 3f)) for ((x, z) in points(3000, 300f)) {
            val h = p.height(x, z, fp)
            p.at(x, z, fp)
            assertEquals(h, p.y, 1e-4f, "at $x, $z, fp $fp")
        }
    }

    @Test
    fun residualSlopeDoesNotDependOnTheSwell() {
        val one = ocean().residualSlope
        for (swell in listOf(0f, 2.6f)) {
            val r = ocean(swell = swell).residualSlope
            assertTrue(r.isFinite(), "swell $swell gives $r")
            assertEquals(one, r, 1e-6f)
        }
    }

    @Test
    fun aGridUnderEightHasNoWavesAndIsRejected() {
        for (size in listOf(1, 2, 4)) assertFailsWith<IllegalArgumentException> { Ocean(1, size = size) }
        val p = Ocean(1, size = 8).probe()
        assertTrue(p.height(3f, 4f, 0f).isFinite())
    }

    @Test
    fun theSpreadAddsUpToOneOverAFullTurn() {
        // so the spread moves energy between directions, never between wave numbers
        for (sp in listOf(0.5f, 1f, 3f, 8f, 60f)) {
            val n = 20000
            var sum = 0.0
            for (i in 0 until n) sum += spreading((-PI + (i + 0.5) * 2 * PI / n).toFloat(), sp) * 2 * PI / n
            assertEquals(1.0, sum, 1e-3, "s = $sp")
        }
    }

    @Test
    fun aHalfFadedTileLeavesThreeQuartersOfItsSlopeAsRoughness() {
        // a slope drawn at weight w keeps w^2 of its variance. at fp = 0.625 mid the small tile is at
        // w = 0.5, the two big ones are full
        val o = ocean()
        val p = o.probe()
        p.at(5f, 5f, 0.625f * o.tiles[2].mid)
        assertEquals(0.75f * o.tiles[2].slopeVar, p.unresolved, 1e-4f * o.tiles[2].slopeVar)
    }

    @Test
    fun withNoChopNothingMovesSideways() {
        val p = ocean(chop = 0f).probe()
        p.at(12.5f, -7.25f, 0f)
        assertEquals(12.5f, p.x0)
        assertEquals(-7.25f, p.z0)
        assertEquals(1f, p.squeeze)
        assertEquals(p.height(12.5f, -7.25f, 0f), p.y)
    }

    @Test
    fun theNormalIsAUnitVectorPointingUp() {
        val p = ocean().probe()
        for ((x, z) in points(500, 300f)) {
            p.at(x, z, 0f)
            assertEquals(1f, sqrt(p.nx * p.nx + p.ny * p.ny + p.nz * p.nz), 1e-5f)
            assertTrue(p.ny > 0f)
        }
    }

    @Test
    fun aWideFootprintLeavesTheSmallWavesAsRoughness() {
        val p = ocean().probe()
        p.at(5f, 5f, 0f)
        assertEquals(0f, p.unresolved)
        p.at(5f, 5f, 6f)
        assertTrue(p.unresolved > 0f)
    }
}

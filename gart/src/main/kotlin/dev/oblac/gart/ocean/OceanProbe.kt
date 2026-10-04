package dev.oblac.gart.ocean

import dev.oblac.gart.march.HeightField
import dev.oblac.gart.math.smoothstep
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Samples an [Ocean]. It is the [HeightField] for [dev.oblac.gart.march.march], and [at] gives
 * the full sample at a hit. It keeps scratch memory, so each thread needs its own.
 *
 * The math, in short:
 * - A tile is full while the footprint `fp` is less than a quarter of its middle wave length. At
 *   one wave length the tile is gone, so a far sea does not shimmer.
 * - A tile drawn at weight w keeps `w^2` of its slope variance. The other `1 - w^2` goes to
 *   [unresolved], and a shading model can add it to the roughness of the surface.
 * - The chop moved the water sideways. To find the water at (x, z), the probe does
 *   `p = (x, z) - chop * move(p)` 2 times. Near a sharp crest this is not fully converged.
 * - [height] and [at] take the same 2 steps, so a hit of the march lies on the surface that [at]
 *   shades.
 * - The normal is the cross product of the two tangents of the moved surface, turned to point up.
 *   Where the chop folds the surface over, the cross points down, so it turns over as a whole.
 * - [slope] holds for the surface the march sees. The 2 chop steps stretch a slope by at most
 *   `1 + c + c^2`, with c the chop times the steepest sideways move.
 *
 * References:
 * - Tessendorf, J. (2001). Simulating Ocean Water. SIGGRAPH course notes, revised 2004. The chop.
 *   <https://jtessen.people.clemson.edu/reports/papers_files/coursenotes2004.pdf>
 * - Bruneton, E., Neyret, F. and Holzschuch, N. (2010). Real-time realistic ocean lighting using
 *   seamless transitions from geometry to BRDF. Computer Graphics Forum 29(2), 487-496. Waves too
 *   small to draw go into the roughness. <https://doi.org/10.1111/j.1467-8659.2009.01618.x>
 */
private const val CHOP_STEPS = 2

class OceanProbe internal constructor(ocean: Ocean) : HeightField {

    private val tiles = ocean.tiles
    private val chop = ocean.chop
    private val w = FloatArray(3)
    private val acc = FloatArray(NF)
    private var lastFp = Float.NaN

    override val top = ocean.top

    /** The height of the surface at the last [at]. */
    var y = 0f

    /** The unit normal at the last [at]. */
    var nx = 0f
    var ny = 1f
    var nz = 0f

    /** How much the chop squeezes the surface at the last [at]: 1 is flat, low is a sharp crest. See [Ocean.foamLimit]. */
    var squeeze = 1f

    /** The slope variance of the tiles that faded out at the last footprint. Add it to the roughness. */
    var unresolved = 0f

    /** Where the water at the last sample was before the chop moved it. Good for foam noise. */
    var x0 = 0f
    var z0 = 0f

    override fun height(x: Float, z: Float, fp: Float): Float {
        lod(fp)
        return displaced(x, z)
    }

    override fun slope(fp: Float): Float {
        lod(fp)
        var g = 0f
        var m = 0f
        for (i in 0 until 3) {
            g += w[i] * tiles[i].maxGrad
            m += w[i] * tiles[i].maxMove
        }
        // the march sees h(p) with p from 2 chop steps, and the steps stretch a slope by 1 + c + c^2 at most
        val c = chop * m
        return g * (1f + c + c * c) + 0.02f
    }

    /**
     * The full sample at (x, z): [y], the normal, [squeeze], [unresolved], [x0] and [z0].
     */
    fun at(x: Float, z: Float, fp: Float) {
        lod(fp)
        displaced(x, z)
        for (k in 0 until NF) acc[k] = 0f
        var u = 0f
        for (i in 0 until 3) {
            if (w[i] > 0f) tiles[i].sample(x0, z0, 0, NF, w[i], acc)
            u += (1f - w[i] * w[i]) * tiles[i].slopeVar // a slope drawn at weight w keeps w^2 of its variance
        }
        unresolved = u
        y = acc[0]
        val jxx = 1f + chop * acc[5]
        val jzz = 1f + chop * acc[6]
        val jxz = chop * acc[7]
        squeeze = jxx * jzz - jxz * jxz
        normalFrom(acc[3], acc[4], jxx, jzz, jxz)
    }

    // the normal of the moved surface, the cross of its two tangents. where the chop folds the surface
    // over (squeeze under 0) the cross points down, so all of it turns up before y gets its floor
    internal fun normalFrom(sx: Float, sz: Float, jxx: Float, jzz: Float, jxz: Float) {
        val j = jxx * jzz - jxz * jxz
        val up = if (j < 0f) -1f else 1f
        val ax = (sz * jxz - jzz * sx) * up
        val ay = max(j * up, 0.05f)
        val az = (jxz * sx - sz * jxx) * up
        val nl = 1f / sqrt(ax * ax + ay * ay + az * az)
        nx = ax * nl
        ny = ay * nl
        nz = az * nl
    }

    // the tiles fade out once their waves get smaller than the footprint. slope() asks for the
    // footprint height() just used, so the last one is kept
    private fun lod(fp: Float) {
        if (fp == lastFp) return
        lastFp = fp
        for (i in 0 until 3) w[i] = 1f - smoothstep(0.25f, 1f, fp / tiles[i].mid)
    }

    // fixed point: the water at (x, z) came from (x0, z0) = (x, z) - chop * move(x0, z0). the same 2
    // steps for height() and at(), so the march and the shading see one surface. converging to a
    // quarter of the footprint made salum 2.2x slower
    private fun displaced(x: Float, z: Float): Float {
        var px = x
        var pz = z
        repeat(CHOP_STEPS) {
            acc[1] = 0f
            acc[2] = 0f
            for (i in 0 until 3) if (w[i] > 0f) tiles[i].sample(px, pz, 1, 3, w[i], acc)
            px = x - chop * acc[1]
            pz = z - chop * acc[2]
        }
        x0 = px
        z0 = pz
        acc[0] = 0f
        for (i in 0 until 3) if (w[i] > 0f) tiles[i].sample(px, pz, 0, 1, w[i], acc)
        return acc[0]
    }
}

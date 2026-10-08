package salum

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.InkRamp
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.ToneCurve
import dev.oblac.gart.color.space.MutableColor4f
import dev.oblac.gart.color.tintOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.march.RayCamera
import dev.oblac.gart.march.footprint
import dev.oblac.gart.march.march
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.degToRad
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.ocean.Ocean
import dev.oblac.gart.ocean.OceanProbe
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.util.parallelForRows
import dev.oblac.gart.vector.Vec3
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * salum - the open sea, printed in colored light.
 *
 * a real wave spectrum makes the sea, in three fft tiles, from the long swell to the ripples. the
 * eye is 0.6 m over the water and looks toward a low sun. at this height the eye is under the
 * crests, so the sea ends at a line of crests, not at a flat horizon.
 *
 * the water has a dark body. fresnel adds the sky that the water reflects. the sun makes glints.
 * when the sun is behind the crests, they glow, and high crests glow more. foam shows where the
 * waves compress the surface the most. far away, haze changes the water to the horizon color.
 *
 * the light has color. one palette, cool257, gives a hue to each part of the light, from the top
 * of the sky to the foam. the light and the slope of each face then give the sea its colors. faces
 * tipped toward the eye show the top of the sky, and faces tipped away show the horizon. last, a
 * tone curve shapes each color channel. then a ramp from navy ink to bone paper prints each channel
 * separately.
 *
 * the sun in the sky is a giant disc. its center is over the top edge, so only the lower arc
 * shows. a row of points along the middle of that arc makes the glints. each point has a share of
 * the light, so the glitter path is as wide as the row. the sky in the water has no disc. it has
 * only the glow of the low sun.
 */
private const val W = 1200
private const val H = 1500

// navy ink on bone paper
private const val INK = 0xFF151B2C.toInt()
private const val PAPER = 0xFFEEE8DB.toInt()

private class Light(p: Palette) {
    val top = tintOf(p[0])
    val low = tintOf(p[1])
    val halo = tintOf(p[2])
    val glare = tintOf(p[3])
    val sun = tintOf(p[4])
    val deep = tintOf(p[5])
    val crest = tintOf(p[6])
    val foam = tintOf(p[7])
}

private val SEED = pi("seed", 10)
private val OUT = ps("out", "salum")
private val SS = pi("ss", 3, 1..4) // rays per pixel, per side
private val L = Light(Palettes.cool257)

private val WIND = pf("wind", 9f, 2f..25f) // m/s. the wind sets how long and how tall the waves are
private val SWELL = pf("swell", 2.6f, 0.1f..4f) // the long waves, times this. 1 is plain physics, one swell band and ripples
private val RIPPLES = pf("ripples", 1.6f, 0f..4f) // the short waves, times this. 1.6 gives the crisp gold with holes in it. 1 is plain physics
private val EYE = pf("eye", 0.6f, 0.2f..80f) // m over the mean water. here the eye is under the crests, so the sea ends at them. at 3 the horizon is flat
private val SUN = pf("sun", 4f, 0.5f..70f) // deg over the horizon
private val GIANT = pf("giant", 9f, 3f..40f) // radius of the giant sun, deg. 16 cuts the sides and 30 is a gold ceiling. at less than 3 the whole disc fits
private val PATH = pf("path", 0.55f, 0.05f..1f) // how much of the visible arc lights the water. 1 makes the sea all gold

private val NZ = noiseOffset(SEED.toLong())

// the lens is long, so the sea stacks in bands. a wide lens left the bottom 40% black
private const val FOV = 24f // vertical, deg
private const val PITCH = -4f

// the low sun, straight ahead
private val TO_SUN = toward(degToRad(SUN))

// the giant sun. i put its lowest point 1 deg under the low sun, so the glitter path starts right under it
private val GR = degToRad(GIANT)
private val GE = degToRad(SUN - 1f + GIANT) // the center, over the frame
private val TO_GIANT = toward(GE)

private fun toward(el: Float) = Vec3(0f, sin(el), cos(el))

// what the water sees: a row of points on the lower arc of the giant sun. each point gets a share
// of the light, so the glitter path is as wide as the row
private val SUNS: Array<Vec3> = run {
    // down and side span the disc. both are square to TO_GIANT
    val down = Vec3(0f, -cos(GE), sin(GE))
    val side = Vec3(1f, 0f, 0f)
    val top = degToRad(PITCH + FOV / 2f)
    // the arc leaves the frame where the rim gets to the top edge
    val open = acos(((cos(GR) * sin(GE) - sin(top)) / (sin(GR) * cos(GE))).coerceIn(-1f, 1f)) * PATH
    Array(ARC_N) { i ->
        val psi = -open + 2f * open * (i + 0.5f) / ARC_N
        TO_GIANT * cos(GR) + (down * cos(psi) + side * sin(psi)) * sin(GR) // on the rim, GR from the center
    }
}

// the sky
private const val ZENITH = 0.12f
private const val HORIZON = 0.6f
private const val DISC = 1.2f // the disc in the sky. a strong disc burns every color to white, 1.2 to 2 keeps the color
private const val RIM = 0.25f // the glow on the giant rim. i keep it thin, a wide one washed the whole sky peach
private const val RIM_W = 4000f
private const val ARC_N = 13 // suns on the arc. at 13 their paths merge, even at path 1

// the water
private const val BODY = 0.025f // the light from under the surface. i see it when i look down
private const val GLOW = 0.25f // how much the crests glow with the sun behind them
private const val FOAM_L = 0.8f
private const val HAZE = 7000f // m
private const val FOAM = 2f // whitecaps, times what the wind makes
private const val GLOSS = 0.3f // the roughness that the tiles cant draw. low gives sparkle, high gives a smear

// the sea

private const val SPREAD = 8f // how long the crests run. 40 is corduroy
private const val WDIR = 12f // the swell heading, deg away from straight at me
private const val CHOP = 0.9f // the sideways pinch: sharp crests, flat troughs
private const val PEAK = 3.3f // the jonswap peak factor. 3.3 is a young sea, 1 is a fully grown one

// the light on the water. the ocean gives the shape
private class Water(ocean: Ocean) {
    // the floor is for the rays
    val rough = max(ocean.residualSlope * GLOSS, 0.0025f)
    // the cap
    val froth = min(ocean.foamLimit(FOAM), 0.8f)
    val hs = ocean.hs

    fun shade(p: OceanProbe, t: Float, dx: Float, dy: Float, dz: Float, fp: Float, out: MutableColor4f) {
        p.at(dx * t, dz * t, fp)
        val nx = p.nx
        val ny = p.ny
        val nz = p.nz
        val r = rough + p.unresolved // the waves too small to draw become roughness

        val dn = dx * nx + dy * ny + dz * nz
        val ndv = max(-dn, 0.02f)
        val fres = 0.02f + 0.98f * (1f - ndv).pow(5)
        val rx = dx - 2f * dn * nx
        val ry = max(dy - 2f * dn * ny, 0.004f)
        val rz = dz - 2f * dn * nz
        val rl = 1f / sqrt(rx * rx + ry * ry + rz * rz)
        skyInWater(ry * rl, rz * rl, out) // the reflection

        // the sun
        val ndl = ny * TO_SUN.y + nz * TO_SUN.z
        var spec = 0f
        for (s in SUNS) {
            var hx = s.x - dx
            var hy = s.y - dy
            var hz = s.z - dz
            val hl = 1f / sqrt(hx * hx + hy * hy + hz * hz)
            hx *= hl
            hy *= hl
            hz *= hl
            if (nx * s.x + ny * s.y + nz * s.z <= 0f) continue
            val ndh = max(nx * hx + ny * hy + nz * hz, 1e-3f)
            val c2 = ndh * ndh
            val d = exp(-(1f - c2) / (c2 * r)) / (PIf * r * c2 * c2)
            val fh = 0.02f + 0.98f * (1f - max(-(dx * hx + dy * hy + dz * hz), 0f)).pow(5)
            spec += d * fh / (4f * ndv) / SUNS.size
        }

        val glow = GLOW * smoothstep(0f, 0.5f * hs, p.y) * max(dz * TO_SUN.z, 0f).pow(3)
        val froth = smoothstep(froth + 0.06f, froth - 0.06f, p.squeeze + 0.08f * SimplexNoise.noise(p.x0 * 0.9f + NZ, p.z0 * 0.9f))
        val fl = FOAM_L + 0.3f * max(ndl, 0f)
        val haze = 1f - exp(-t / HAZE)
        for (c in 0 until 3) {
            val l = fres * out[c] + (1f - fres) * (BODY * L.deep[c] + glow * L.crest[c]) + spec * L.sun[c]
            out[c] = lerp(lerp(l, fl * L.foam[c], froth), HORIZON * L.low[c], haze)
        }
    }
}

// the open sky over the sea: the giant sun and a thin glow on its rim
private fun sky(y: Float, z: Float, out: MutableColor4f) {
    val ang = acos((y * TO_GIANT.y + z * TO_GIANT.z).coerceIn(-1f, 1f))
    if (ang < GR) {
        for (c in 0 until 3) out[c] = DISC * L.sun[c]
        return
    }
    val off = ang - GR // rad out from the rim
    val rim = RIM * exp(-RIM_W * off * off)
    for (c in 0 until 3) out[c] = bare(y, c) + rim * L.glare[c]
}

// the sky in the water. it has no disc, only the glare and the halo of the low sun
private fun skyInWater(y: Float, z: Float, out: MutableColor4f) {
    val cs = y * TO_SUN.y + z * TO_SUN.z // cos to the sun
    val glare = 0.35f * exp((cs - 1f) * 800f)
    val halo = 0.25f * exp((cs - 1f) * 25f)
    for (c in 0 until 3) out[c] = bare(y, c) + (glare * L.glare[c] + halo * L.halo[c])
}

// the bare sky
private fun bare(y: Float, c: Int) = ZENITH * L.top[c] + (HORIZON * L.low[c] - ZENITH * L.top[c]) * exp(-max(y, 0f) * 15f)

// smooth tone needs a curve that crushes the darks, or its all gray. gamma 1.8 sinks the mids
private val TONE = ToneCurve(1.3f, 1.8f)

fun main(args: Array<String>) {
    val gart = Gart.of("salum", W, H)
    val sw = Stopwatch()
    val ocean = Ocean(SEED, wind = WIND, swell = SWELL, spread = SPREAD, heading = -PIf / 2f + degToRad(WDIR), chop = CHOP, ripples = RIPPLES, gamma = PEAK)
    val water = Water(ocean)
    val lap = sw.lap()

    val cam = RayCamera(Vec3(0f, EYE, 0f), PITCH, FOV, W, H, SS)
    val far = ((ocean.peakLength * 0.5f * sqrt(EYE) / cam.spread).pow(2f / 3f)).coerceIn(200f, 6000f)
    val tone = Array(3) { FloatArray(W * H) } // per channel

    parallelForRows(H) { y0, y1 ->
        val p = ocean.probe()
        val c = MutableColor4f() // the light of one ray
        val sum = MutableColor4f()
        for (r in y0 until y1) for (cx in 0 until W) {
            val cy = ((r * 7919L) % H).toInt()
            sum.zero()
            for (j in 0 until SS) for (i in 0 until SS) {
                val (dx, dy, dz) = cam.ray(cx + (i + 0.5f) / SS, cy + (j + 0.5f) / SS)
                var t = p.march(0f, EYE, 0f, dx, dy, dz, cam.spread, far)
                if (t < 0f && dy < 0f) t = EYE / -dy // past the tiles, the flat sea
                if (t >= 0f) water.shade(p, t, dx, dy, dz, footprint(t, dy, cam.spread), c) else sky(dy, dz, c)
                for (k in 0 until 3) sum[k] += TONE(c[k])
            }
            for (k in 0 until 3) tone[k][cy * W + cx] = sum[k] / (SS * SS)
        }
    }
    val traced = sw.lap()

    val ramp = InkRamp(INK, PAPER)
    val g = gart.gartvas()
    Gartmap(g).use { m ->
        for (i in 0 until W * H) m.pixels[i] = ramp.rgb(tone[0][i], tone[1][i], tone[2][i])
        m.drawToCanvas()
    }
    gart.saveImage(g, OUT.ensureExtension())
    println(
        "salum: seed=$SEED hs=${"%.2f".format(ocean.hs)}m peak=${"%.0f".format(ocean.peakLength)}m top=${"%.2f".format(ocean.top)}m " +
            "rough=${"%.4f".format(water.rough)} froth=${"%.2f".format(water.froth)} far=${"%.0f".format(far)}m, " +
            "sea ${lap}ms, rays ${traced}ms, ${sw.ms}ms"
    )
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

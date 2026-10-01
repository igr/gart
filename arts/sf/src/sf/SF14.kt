package sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.rgb
import dev.oblac.gart.gfx.drawRoundBorder
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.degToRad
import dev.oblac.gart.math.frac
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndf
import dev.oblac.gart.pixels.shade
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * sf14 - the lens.
 *
 * behind the frame there is a plain sf picture: cream bands on a dark ground and one red sun. a
 * mass in front of the picture bends the light. you see the mass only in the bands. outside its
 * einstein ring, the bands bend around it. inside the ring, the bands become circles that all
 * touch at the middle.
 *
 * the sun is a small distance off the axis of the mass. as a result, you see the sun two times: a
 * long red arc outside the ring and a short one inside. if the sun is directly behind the mass,
 * the full ring is red.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("sf14", 1024, 1024)
    println(gart)
    println("seed=$SEED")
    val d = gart.d

    cast(d)
    val g = gart.gartvas()
    Gartmap(g.d).use { m ->
        m.shade(SS, colorAt = ::look)
        m.drawToCanvas(g)
    }
    g.canvas.drawRoundBorder(d, 10f, 40f, colorInk)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private val SEED = pi("seed", 49)
private val OUT = ps("out", "sf14")
private val rng = Random(SEED)
private val SS = pi("ss", 3, 1..4)

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01

// the lens
private val MASS = pf("mass", 0.25f, 0.02f..0.5f)   // its einstein radius, share of the width
private val SUNR = pf("sun", 0.31f, 0.02f..1.5f)    // sun radius in einstein radii, so the arcs grow with the mass
private val ALIGN = pf("align", 0.5f, 0f..2f)       // sun off its axis, in einstein radii. 0 is a closed ring

// the circles at the edge of the hole are pitch * h^2 / (1 + h^2) apart. 0.38 at pitch 32 gives
// 4px. under ~2px they beat into petals, so dont make the hole much smaller
private const val HOLE = 0.38f     // the dark in the middle, share of the einstein radius

// the mass: where it hangs and its einstein radius, px. the pull goes with e2
private class Lens(val x: Float, val y: Float, val e: Float) {
    val e2 = e * e
    val hole = e * HOLE
}

private lateinit var lens: Lens
private var sunX = 0f
private var sunY = 0f
private var sunR = 0f
private var phase = 0f

private fun cast(d: Dimension) {
    lens = Lens(d.wf * rng.rndf(0.3f, 0.7f), d.hf * rng.rndf(0.3f, 0.7f), d.wf * MASS)
    val a = rng.rndf(TAUf)
    phase = rng.rndf()
    sunX = lens.x + cos(a) * ALIGN * lens.e
    sunY = lens.y + sin(a) * ALIGN * lens.e
    sunR = SUNR * lens.e
}

// the bands behind
private const val PITCH = 32f
private const val DUTY = 0.32f
private const val TILT = 30f
private val nx = -sin(degToRad(TILT))
private val ny = cos(degToRad(TILT))

/**
 * the color at one sample point. the mass bends the ray, so the point behind is the sample point
 * moved toward the mass by e^2 / r. the jacobian gives the stretch there, and the filter uses it
 * to keep the band and sun edges smooth. the hole is in front of everything.
 */
private fun look(x: Float, y: Float): Int {
    val dx = x - lens.x
    val dy = y - lens.y
    val r2 = dx * dx + dy * dy
    val dark = cover(sqrt(r2) - lens.hole)
    if (dark >= 1f) return colorBack
    val k = lens.e2 / r2
    val k2 = 2f * k / r2
    val bx = x - k * dx
    val by = y - k * dy
    val j11 = 1f - (k - k2 * dx * dx)
    val j22 = 1f - (k - k2 * dy * dy)
    val j12 = k2 * dx * dy
    val u = (bx * nx + by * ny) / PITCH + phase
    val du = hypot(j11 * nx + j12 * ny, j12 * nx + j22 * ny) / PITCH   // periods per px here
    val band = stripe(u, du / SS)
    val sx = bx - sunX
    val sy = by - sunY
    val sd = hypot(sx, sy)
    val gs = hypot(j11 * sx + j12 * sy, j12 * sx + j22 * sy) / sd
    val sun = if (sd < 1e-4f || gs < 1e-6f) (if (sd < sunR) 1f else 0f) else cover((sd - sunR) / gs)
    return mix(band, sun, dark)
}

private fun cover(dist: Float) = (0.5f - dist * SS).coerceIn(0f, 1f)

private fun stripe(u: Float, fw: Float): Float {
    if (fw < 1e-4f) return if (frac(u) < DUTY) 1f else 0f
    return ((ramp(u + fw / 2) - ramp(u - fw / 2)) / fw).coerceIn(0f, 1f)
}

private fun ramp(u: Float) = floor(u) * DUTY + min(frac(u), DUTY)

private fun mix(band: Float, sun: Float, dark: Float): Int =
    rgb(channel(::red, band, sun, dark), channel(::green, band, sun, dark), channel(::blue, band, sun, dark))

// ground, the bands on it, the sun over them, and the hole over everything - its in front
private inline fun channel(of: (Int) -> Int, band: Float, sun: Float, dark: Float): Int {
    val k = of(colorBack).toFloat()
    val i = of(colorInk).toFloat()
    val b = of(colorBold).toFloat()
    return (lerp(lerp(lerp(k, i, band), b, sun), k, dark) + 0.5f).toInt()
}

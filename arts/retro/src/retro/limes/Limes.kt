package retro.limes

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.toClosedPath
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.Complex
import dev.oblac.gart.math.sqrt
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atanh
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

private val whirl = pf("whirl", 0.5f, -1.2f..1.2f) // how hard the blades turn round the hub. 0 == straight
private val curl = pf("curl", 0.35f, -0.8f..0.8f)  // how far the rim between two wheels bends. 0 == plain octagon
private val eyex = pf("eyex", 0.62f, 0.1f..0.9f)   // the hub of the first wheel
private val eyey = pf("eyey", 0.58f, 0.1f..0.9f)
private val turn = pf("turn", 0f, -180f..180f)     // the plane turned round that hub
private val out = ps("out", "limes")

private const val FRAME = 40f / 1024f
private const val GRAIN = 1.2f

private const val SS = 4

//private val inks = Palette(
//    0xFF1D4B49,
//    0xFFC8962B) + BwColors.parchment
private val inks = Palette.of(
    RetroColors.red01,
    RetroColors.black01,
    RetroColors.white01
)
private const val PAPER = 2

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("limes", 1024, 1024)
    println(gart)

    val big = gart.supersampled(SS)
    draw(big.canvas, big.d)
    val g = big.downsample(SS)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(inks[PAPER])
    val fills = List(inks.size) { fillOf(inks[it]).apply { isAntiAlias = false } }

    val half = (0.5f - FRAME) * d.wf
    fun onPage(z: Complex) = toSquare(z).let { Point(d.cx + (it.real * half).toFloat(), d.cy + (it.imag * half).toFloat()) }

    val eye = fromSquare(Complex((eyex - 0.5f) / (0.5f - FRAME), (eyey - 0.5f) / (0.5f - FRAME)))
    val spin = Complex.fromPolar(1.0, Math.toRadians(turn.toDouble()))
    val grain = GRAIN * d.wf / 1024f / half
    val all = wheels({ slide(it * spin, -eye) }) { a, b -> (toSquare(a) - toSquare(b)).abs() < grain }

    // a blade has its tail on the hub and its nose on a corner of the wheel. a wheel leaves one
    // ink out and deals the other two round
    for (w in all) {
        val mids = List(BLADES) { along(w.corners[it], w.corners[(it + 1) % BLADES], 0.5) }
        for (i in 0 until BLADES) {
            val ink = (w.skips + 1 + i % 2) % 3
            if (ink == PAPER) continue
            val nose = w.corners[i]
            val left = mids[(i + BLADES - 1) % BLADES]
            val right = mids[i]
            val blade = buildList {
                addAll(side(w.hub, left, -whirl).dropLast(1))
                addAll(side(nose, left, curl).reversed().dropLast(1))
                addAll(side(nose, right, curl).dropLast(1))
                addAll(side(w.hub, right, -whirl).reversed().dropLast(1))
            }
            c.drawPath(blade.map { onPage(it) }.toClosedPath(), fills[ink])
        }
    }
}

private fun side(a: Complex, b: Complex, bend: Float): List<Complex> {
    val m = along(a, b, 0.5)
    val h = slide(b, m)
    val n = (6 + 140 * (a - b).abs()).toInt()
    return (0..n).map {
        val s = it.toDouble() / n
        slide(h * Complex(2 * s - 1, bend * sin(PI * s)), -m)
    }
}

// the plane

private const val BLADES = 8 // a wheel is an octagon, and three of them meet at every corner. dont change one without the other
private val ONE = Complex.ONE

// how far from the hub the corners of such an octagon are
private val CORNER = sqrt(cos(PI / BLADES + PI / 3) / cos(PI / BLADES - PI / 3))

// moves the whole plane so that a lands in the middle. slide(z, -a) undoes it
private fun slide(z: Complex, a: Complex) = (z - a) / (ONE - a.conjugate() * z)

// the point a part t of the way from a to b, measured their way
private fun along(a: Complex, b: Complex, t: Double): Complex {
    val far = slide(b, a)
    val r = far.abs()
    return slide(far * (tanh(t * atanh(r)) / r), -a)
}

// z flipped over the line through a and b
private fun mirror(z: Complex, a: Complex, b: Complex): Complex {
    val u = slide(b, a).let { it / it.abs() }
    return slide(u * u * slide(z, a).conjugate(), -a)
}

private class Wheel(val corners: List<Complex>, val hub: Complex, val flipped: Boolean, val skips: Int)

// all the wheels, from the first one out. each is the mirror of its neighbour in the side they share
private fun wheels(see: (Complex) -> Complex, tooSmall: (Complex, Complex) -> Boolean): List<Wheel> {
    val first = Wheel(List(BLADES) { see(Complex.fromPolar(CORNER, 2 * PI / BLADES * it)) }, see(Complex.ZERO), false, 0)
    val found = mutableListOf<Wheel>()
    val seen = hashSetOf(key(first.hub))
    val queue = ArrayDeque(listOf(first))
    while (queue.isNotEmpty()) {
        val w = queue.removeFirst()
        found += w
        for (i in 0 until BLADES) {
            val a = w.corners[i]
            val b = w.corners[(i + 1) % BLADES]
            if (tooSmall(a, b)) continue
            val hub = mirror(w.hub, a, b)
            if (!seen.add(key(hub))) continue
            val step = if ((i % 2 == 0) != w.flipped) 2 else 1
            queue += Wheel(w.corners.map { mirror(it, a, b) }, hub, !w.flipped, (w.skips + step) % 3)
        }
    }
    return found
}

private fun key(z: Complex) = (z.real * 1e6).roundToLong() * 4_000_003L + (z.imag * 1e6).roundToLong()

// onto the page

private const val REACH = 0.9270373386 // how far the sum below gets from 0 to 1: half a side of the square

private fun toSquare(z: Complex): Complex {
    val z2 = z * z
    val z4 = z2 * z2
    var tr = z.real
    var ti = z.imag
    var sr = tr
    var si = ti
    for (k in 1 until 40000) {
        val f = -(2.0 * k - 1) / (2.0 * k)
        val nr = (tr * z4.real - ti * z4.imag) * f
        ti = (tr * z4.imag + ti * z4.real) * f
        tr = nr
        sr += tr / (4 * k + 1)
        si += ti / (4 * k + 1)
        if (abs(tr) + abs(ti) < 1e-8 * k) break
    }
    return Complex(sr / REACH, si / REACH)
}
private fun fromSquare(w: Complex): Complex {
    var z = w * REACH
    repeat(40) {
        val z2 = z * z
        z -= (toSquare(z) - w) * sqrt(ONE + z2 * z2) * REACH
    }
    return z
}

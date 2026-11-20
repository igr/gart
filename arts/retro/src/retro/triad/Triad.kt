package retro.triad

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.angle.cosf
import dev.oblac.gart.angle.sinf
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.rgb
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.lerp
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * triad - kandinskys three as a print. a yellow triangle, a red square and a blue circle, each one
 * screened in dots of its own shape, three screens at three angles printed over each other. where
 * the forms cross the inks mix, and the screens beat into rosettes.
 */
private val lpi = pi("lpi", 24, 4..200)       // dots across the page. 34 reads as print, 24 as a poster
private val lo = pf("lo", 0.2f, 0f..1f)       // tone at the top left, the all-circles diagonal
private val hi = pf("hi", 0.85f, 0f..1f)      // ...and at the bottom right
private val ay = Degrees(pf("ay", 0f))        // screen angles, degrees. each form turns with its screen
private val ar = Degrees(pf("ar", 30f))
private val ab = Degrees(pf("ab", 60f))
private val scale = pf("scale", 1.2f, 0.1f..3f) // the three together, round the page middle. 1.2 lets the frame cut them
private val out = ps("out", "triad")

private val paper = RetroColors.white01

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("triad", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private enum class Dot { TRIANGLE, SQUARE, CIRCLE }

private class Screen(val ink: Int, val dot: Dot, val angle: Angle, val form: Path)

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)
    val w = d.wf

    fun at(x: Float, y: Float) = Point(d.cx + (x - 0.5f) * w * scale, d.cy + (y - 0.5f) * w * scale)
    val triangle = polygon(at(0.43f, 0.55f), 0.66f * w * scale / sqrt(3f), 3, Degrees(-90f) + ay)
    val square = polygon(at(0.35f, 0.33f), 0.22f * w * scale * sqrt(2f), 4, Degrees(45f) + ar)
    val circle = at(0.64f, 0.63f).let { PathBuilder().addCircle(it.x, it.y, 0.25f * w * scale).detach() }

    val screens = listOf(
        Screen(RetroColors.yellow01, Dot.TRIANGLE, ay, triangle),
        Screen(RetroColors.red01, Dot.SQUARE, ar, square),
        Screen(RetroColors.blue01, Dot.CIRCLE, ab, circle),
    )
    val cell = w / lpi
    screens.forEach { s ->
        c.saveLayer(null, paint().apply { blendMode = BlendMode.MULTIPLY })
        c.clipPath(s.form, true)
        val fill = fillOf(filter(s.ink))
        lattice(d, cell, s.angle).filter { s.form.contains(it.x, it.y) }.forEach { at ->
            val tone = lerp(lo, hi, (at.x + at.y) / (d.wf + d.hf)).coerceIn(0f, 1f)
            c.drawPath(dot(s.dot, at, cell, tone, s.angle), fill)
        }
        c.restore()
    }

    c.drawBorder(d, 40f, paper)
}

// an ink as what it lets through, so one ink alone on paper is exactly its colour and only the
// overlaps go darker
private fun filter(ink: Int): Int {
    fun t(i: Int, p: Int) = (255 * i / p.coerceAtLeast(1)).coerceIn(0, 255)
    return rgb(t(red(ink), red(paper)), t(green(ink), green(paper)), t(blue(ink), blue(paper)))
}


private fun lattice(d: Dimension, cell: Float, angle: Angle): List<Point> {
    val m = ceil(d.wf * 0.75f / cell).toInt()
    return (-m..m).flatMap { j ->
        (-m..m).map { i -> Vec2(i * cell, j * cell).rotate(angle.radians) }
    }.map { Point(d.cx + it.x, d.cy + it.y) }
        .filter { it.x > -cell && it.y > -cell && it.x < d.wf + cell && it.y < d.hf + cell }
}

private fun dot(kind: Dot, at: Point, cell: Float, tone: Float, angle: Angle): Path = when (kind) {
    Dot.CIRCLE -> PathBuilder().addCircle(at.x, at.y, cell * sqrt(tone / PIf)).detach()
    Dot.SQUARE -> polygon(at, cell * sqrt(tone / 2f), 4, Degrees(45f) + angle)
    Dot.TRIANGLE -> polygon(at, cell * sqrt(tone * 4f / (3f * sqrt(3f))), 3, Degrees(-90f) + angle)
}

private fun polygon(at: Point, r: Float, n: Int, start: Angle): Path {
    val pb = PathBuilder()
    for (k in 0 until n) {
        val a = start + Degrees.D360 * k / n
        val x = at.x + r * cosf(a)
        val y = at.y + r * sinf(a)
        if (k == 0) pb.moveTo(x, y) else pb.lineTo(x, y)
    }
    return pb.closePath().detach()
}

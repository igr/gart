package sf

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.gfx.*
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.i
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.SimplexNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader
import kotlin.random.Random

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("sf11", 1024, 1024)
    println(gart)
    println("seed=$SEED")
    val d = gart.d

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)

    gart.saveImage(g, "$OUT.png")

    if (!headless) gart.window().showImage(g)
}

private val SEED = pi("seed", 11)
private val OUT = ps("out", "sf11")
private val rng = Random(SEED)

private val colorBack = RetroColors.black01
private val colorInk = RetroColors.white01
private val colorBold = RetroColors.red01
private const val DARKEN = 0.5f          // ray gradient fades toward this fraction of black
private val palette = Palettes.cool1.expand(256)   // teal -> gold -> ember -> rust
private val NOISE_S = pf("noise", 0.0025f)         // spatial scale of the color-noise field
private val NZOFF = (SEED and 0xffff) * 0.01f      // SimplexNoise is seedless -> shift its window by the seed

private fun draw(c: Canvas, d: Dimension) {
    c.clear(colorBack)
    val center = d.center.offset(0f, -200f)

    repeat(18) {
        drawCircleRays(c, center, 960f - it * 50)
    }

    //c.drawCircle(innerCircle, fillOf(colorBack))
    c.drawRoundBorder(d, 10f, 40f, colorBack)
}

private fun drawCircleRays(c: Canvas, center: Point, radius: Float) {
    val circle2 = Circle(center, radius)
    val points = circle2.points((radius).i())
    points.forEach { drawRay(c, center, it) }
}

private fun drawRay(c: Canvas, center: Point, rayPoint: Point) {
    // color driven by a simplex-noise field sampled at the ray's location (seed shifts the field)
    val n = 0.5f + 0.5f * SimplexNoise.noise(rayPoint.x * NOISE_S + NZOFF, rayPoint.y * NOISE_S)
    val color = palette.relative(n.coerceIn(0f, 0.9999f))

    val target = rayPoint// A/B variants(!) .offset(rng.rndf(-40f, 40f), rng.rndf(-40f, 40f))
    val line = Line(center, target)
    val tangent = Circle(center, line.length()).tangentAtPoint(target)
    val t1 = tangent.pointFromStart(rng.rndf(5f, 10f))
    val t2 = tangent.pointFromEnd(rng.rndf(5f, 10f))
    val tip = line.extendByLen(rng.rndf(5, 10f)).b
    val path = closedPathOf(center, t1, tip, t2)

//    // t1 -> t2 runs along the tangent, i.e. perpendicular to the center -> target ray;
//    // gradient fades across the ray's width from `color` to a darker variant
//    val fill = fillOf(color).apply {
//        shader = Shader.makeLinearGradient(
//            t1.x, t1.y, t2.x, t2.y,
//            gradientOf(intArrayOf(color, darken(color, DARKEN)))
//        )
//    }
    c.drawPath(path, fillOf(color).alpha(100))
    c.drawPath(path, strokeOf(RetroColors.black01, 1f).roundStroke())
}

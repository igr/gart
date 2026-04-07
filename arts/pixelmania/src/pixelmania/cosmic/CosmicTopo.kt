package pixelmania.cosmic

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.PaletteGenerator
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.gfx.*
import dev.oblac.gart.jfa.Jfa
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.stipple.stippleVoronoi
import org.jetbrains.skia.*
import kotlin.math.min
import kotlin.random.Random

private const val W = 1280
private const val H = 1280

fun main() {
    val gart = Gart.of("cosmic", W, H)
    val g = gart.gartvas()
    val c = g.canvas
    val d = g.d
    val rng = Random(42)

    val plt = PaletteGenerator.sequential(
        colors = listOf(NipponColors.col250_RO, NipponColors.col233_SHIRONERI),
        15
    ) + PaletteGenerator.sequential(
        colors = listOf(NipponColors.col233_SHIRONERI, NipponColors.col233_SHIRONERI),
        4
    )

    val red = NipponColors.col025_SHINSYU
    c.clear(plt[0])

    val xline = Line.of(0f, d.hf + 300, d.wf, d.cy / 2)
    val xcircle = Circle(0f, 0f, 800f)
    val xcirclePoints = pointsOn(xcircle.toPath(), 200)

    val monolith = Rect.ofCenter(d.center, 300f, 300f / 4 * 9)
    val monolithP = monolith.path()
    val jfaResult = Jfa(d).computeDistanceField(monolithP)
    (0 until 20)
        .map { jfaResult.tracePath(it * 15f) }
        .reversed()
        .forEachIndexed { ndx, it ->
            c.drawPath(it, strokeOf(40f, plt.safe(ndx)))
        }
    c.drawRect(monolith, fillOf(plt.last()).apply {
        this.mode = PaintMode.STROKE_AND_FILL
        this.strokeWidth = 40f
    })


    val b = Gartmap(g)
    b.updatePixelsFromCanvas()
    val points = stippleVoronoi(
        b,
        pointCount = 10_000,
        iterations = 10,
        maxRadius = 8f,
        gamma = 0.9f
    )

    c.clear(plt[0])
    drawStarField(c, rng)
    points.forEach { pt ->
        val dot = Circle(pt.x, pt.y, 10 - pt.radius)

        val distLine = Line.fromPointToLine(dot.center, xline)
        val lineDistance = distLine.length()
        val redDistance = xcirclePoints.minOf { it.distanceTo(dot.center) }

        val distance = min(lineDistance, redDistance)

        val bwcolor = plt.safe(2 + dot.radius + rndf(-0.5, 0.5))

        val t = (distance / 100f).coerceIn(0f, 1f)
        val color = lerpColor(red, bwcolor, t)

        c.drawCircle(dot, fillOf(color))
    }

    c.save()
    c.clipRect(monolith.shrink(-100f), ClipMode.DIFFERENCE, true)
    val stroke = strokeOf(red, 2f).apply {
        this.pathEffect = PathEffect.makeDiscrete(10f, 5f, 0)
        this.alpha = 200
    }
    c.drawLine(xline, stroke)
    c.drawCircle(xcircle, stroke)
    c.restore()

    gart.saveImage(c)

    gart.window().show(g)
}

// STAR FIELD

private fun drawStarField(c: Canvas, rng: Random) {
    val fill = fillOf(NipponColors.col233_SHIRONERI)
    val stroke = strokeOf(NipponColors.col233_SHIRONERI, 1f)

    repeat(5000) {
        val x = rng.nextFloat() * W
        val y = rng.nextFloat() * H
        val n = SimplexNoise.noise(x * 0.01, y * 0.01)
        if (n > -0.2) {
            val r = when {
                rng.nextFloat() < 0.75f -> 0.5f
                else -> 1f
            }
            if (rng.nextFloat() < 0.8f) c.drawCircle(x, y, r, fill)
            else c.drawCircle(x, y, r + 0.5f, stroke)
        }
    }
}

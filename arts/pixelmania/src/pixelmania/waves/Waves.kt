package pixelmania.waves

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.knot.Knot
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.smooth.chaikinSmooth
import dev.oblac.gart.stipple.stippleVoronoi
import org.jetbrains.skia.*

fun main() {
    val bw = false
    val gart = Gart.of("waves" + (if (bw) "-bw" else ""), 1024 * GOLDEN_RATIO, 1024)
    println(gart)

    val g = gart.gartvas()
    val d = g.d
    val c = g.canvas

    val paths = draw(g.canvas, g.d)

    val b = Gartmap(g)
    b.updatePixelsFromCanvas()
    //stippleNoisyDotDensity(b, step = 2, density = 0.2f)
    val points = stippleVoronoi(b, pointCount = 10_000, iterations = 10, maxRadius = 8f, gamma = 0.4f)

    val pColor = fillOf(Color.BLACK)
    val pl = Palettes.cool24.expand(10)
//    val pl = Palettes.cool19.expand(10)
    if (bw) {
        c.clear(Color.WHITE)
    } else {
        c.clear(pl[0])
    }
    points.forEach { pt ->
        val p = Point(pt.x, pt.y)
        for (i in paths.indices.reversed()) {
            if (p.isBelowPath(paths[i])) {
                val dot = Circle(pt.x, pt.y, pt.radius)
//                c.drawCircle(dot, fillOf(pl[i]))
                if (bw) {
                    c.drawCircle(dot, pColor)
                } else {
                    c.drawCircle(dot, fillOf(pl.safe(dot.radius + rndf(-0.5, 0.5))))
                }
                break
            }
        }
    }
    paths.forEachIndexed { index, path ->
        if (bw) {
            c.drawPath(path, strokeOfBlack(20f))
        } else {
            c.drawPath(path, strokeOf(20f, pl[pl.size - 2]))
        }
//        c.drawPath(path, strokeOf(20f, pl[index]))
    }

//
//    b.drawToCanvas()
    gart.saveImage(g)

    gart.window().show(g)
}

/**
 * Asymmetric arc baseline: rises to a peak in the left half,
 * then descends to finish below where it started.
 */
private fun baselineArc(x: Float, arcHeight: Float, dropOff: Float): Float {
    // Quadratic that passes through (0, 0), peaks around x=0.35, ends at (1, -dropOff)
    // y = ax^2 + bx where y(1) = -dropOff and y'(peakX) = 0
    val peakX = 0.35f
    val a = -dropOff / (1f - 2f * peakX)
    val b = -2f * a * peakX
    val rawY = a * x * x + b * x
    // Scale so peak matches arcHeight
    val rawPeak = a * peakX * peakX + b * peakX
    return if (rawPeak != 0f) rawY * (arcHeight / rawPeak) else 0f
}

private fun generateWaveKnots(arcHeight: Float, arcDrop: Float): List<Knot> {
    val slots = 22
    val hillCount = rndi(3, 5)

    // Pick hill positions from quantized slots, ensuring minimum gap of 2 slots
    val available = (1 until slots - 1).toMutableList()
    val hillSlots = mutableListOf<Int>()
    repeat(hillCount) {
        if (available.isEmpty()) return@repeat
        val pick = available.removeAt(rndi(available.size))
        // remove neighbors to enforce minimum spacing
        available.removeAll { (it - pick).let { d -> d >= -2 && d <= 2 } }
        hillSlots.add(pick)
    }
    hillSlots.sort()

    // Build knots: start → (valley, hill)* → end
    val knots = mutableListOf(Knot(0f, baselineArc(0f, arcHeight, arcDrop)))

    for (i in hillSlots.indices) {
        val hillX = hillSlots[i].toFloat() / slots
        val arc = baselineArc(hillX, arcHeight, arcDrop)
        val hillDy = arc + rndf(60f, 160f)

        // valley between previous point and this hill
        val prevX = if (i == 0) 0f else hillSlots[i - 1].toFloat() / slots
        val midX = (prevX + hillX) / 2f
        val valleyJitter = (hillX - prevX) * rndf(-0.15f, 0.15f)
        val valleyX = (midX + valleyJitter).coerceIn(prevX + 0.02f, hillX - 0.02f)
        val valleyArc = baselineArc(valleyX, arcHeight, arcDrop)
        val valleyDy = valleyArc - rndf(40f, 190f)

        knots.add(Knot(valleyX, valleyDy))
        knots.add(Knot(hillX, hillDy))
    }

    // final valley before end
    val lastHillX = hillSlots.last().toFloat() / slots
    val finalMidX = (lastHillX + 1f) / 2f
    val finalJitter = (1f - lastHillX) * rndf(-0.15f, 0.15f)
    val finalValleyX = (finalMidX + finalJitter).coerceIn(lastHillX + 0.02f, 0.98f)
    val finalArc = baselineArc(finalValleyX, arcHeight, arcDrop)
    knots.add(Knot(finalValleyX, finalArc - rndf(40f, 120f)))

    knots.add(Knot(1f, baselineArc(1f, arcHeight, arcDrop)))
    return knots
}

private fun draw(c: Canvas, d: Dimension): MutableList<Path> {
    c.clear(Color.WHITE)

    val pal = Palette.of(Color.BLACK, Color.WHITE).expand(60) + Palette.of(Color.WHITE, Color.WHITE).expand(80)
    val lineCount = 4
    val spacing = d.hf / (lineCount)

    val arcHeight = rndf(100f, 200f)
    val arcDrop = rndf(0f, 50f)

    val paths = mutableListOf<Path>()

    for (i in -1 until lineCount) {
        val baseY = spacing * (i) * 1.1f
        val knots = generateWaveKnots(arcHeight, arcDrop)

        repeat(100) { offset ->
            val pts = knots.map { k -> Point(k.x * d.wf, baseY - k.dy + offset * 6) }
            val p = chaikinSmooth(pts, 4).toPath()
            if (offset == 0) {
                paths.add(p)
            }

            val paint = Paint().apply {
                color = pal.safe(i + offset)
                mode = PaintMode.STROKE
                strokeWidth = 7f
            }
            c.drawPath(p, paint)
        }
    }
    return paths
}

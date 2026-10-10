package calamus

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.flow2.VectorField
import dev.oblac.gart.flow2.streamlines
import dev.oblac.gart.fx.PaperSurface
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.printOnPaper
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.between
import dev.oblac.gart.math.length
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * calamus - flow, ispisan sirokim perom.
 */
private const val W = 1414
private const val H = 2000
private val SEED = pi("seed", 5)
private val OUT = ps("out", "calamus")
private val SS = pi("ss", 3, 1..4)

// tok
private val HILLS = pi("hills", 5, 0..12)
private val DSEP = pf("dsep", 24f, 8f..80f) // od linije do linije, px
private const val DRIFT = 0.5f

// pero
private const val NIB = 0.7f
private const val ANGLE = 40f // stepeni
private const val RUBRIC = 2 // linije cinoberom
private const val HAIR = 1.2f
private const val BODY = 0.86f // mastilo ne pokriva skroz
private const val TOOTH = 0.18f

private val PAPER = NipponColors.col233_SHIRONERI
private val INK = NipponColors.col248_SUMI
private val VERMILION = NipponColors.col029_GINSYU

private const val LEFT = 0.1f * W
private const val TOP = 0.07f * H
private val BLOCK = Dimension((W - 2 * LEFT).toInt(), (H - TOP - 0.1f * H).toInt())

// flow

private class Hill(val x: Float, val y: Float, val r: Float, val lift: Float)

private fun field(rnd: Random): VectorField {
    val heading = rnd.between(-0.35f, 0.35f)
    val hills = List(HILLS) {
        val x = LEFT + BLOCK.wf * rnd.between(-0.1f, 1.1f)
        val y = TOP + BLOCK.hf * rnd.between(-0.1f, 1.1f)
        val r = W * rnd.between(0.12f, 0.3f)
        Hill(x, y, r, r * if (rnd.nextBoolean()) 1f else -1f)
    }
    return VectorField.curl({ x, y ->
        var p = DRIFT * (y * cos(heading) - x * sin(heading))
        for (h in hills) {
            val dx = x - h.x
            val dy = y - h.y
            p += h.lift * exp(-(dx * dx + dy * dy) / (h.r * h.r))
        }
        p
    })
}

private class Line(val points: List<Point>, val taper: FloatArray) {
    val length = (1 until points.size).sumOf { length(points[it].x - points[it - 1].x, points[it].y - points[it - 1].y).toDouble() }
}

private fun lines(field: VectorField, rnd: Random): List<Line> {
    val local = VectorField { x, y, out -> field.at(x + LEFT, y + TOP, out) }
    return local.streamlines(BLOCK, rnd, DSEP, minLength = 2f * DSEP).map { s ->
        Line(s.points.map { Point(it.x + LEFT, it.y + TOP) }, FloatArray(s.points.size) { s.taper(it) })
    }
}

private fun rubric(lines: List<Line>, rnd: Random): Set<Line> =
    lines.sortedByDescending { it.length }.take(max(RUBRIC, lines.size / 3)).shuffled(rnd).take(RUBRIC).toSet()

fun main(args: Array<String>) {
    val gart = Gart.of("calamus", W, H)
    val lines = lines(field(Random(SEED)), Random(SEED * 7919 + 1))
    val red = rubric(lines, Random(SEED * 7919 + 2))
    val hand = Random(SEED * 7919 + 3)
    val pen = Brushes.nib(NIB * DSEP, HAIR, ANGLE)
    val ink = lerpColor(PAPER, INK, BODY)
    val rub = lerpColor(PAPER, VERMILION, BODY)

    val big = gart.supersampled(SS)
    val c = big.canvas
    c.clear(PAPER)
    c.scale(SS.toFloat(), SS.toFloat())
    for (l in lines.filter { it !in red } + red) {
        // uz susednu liniju pero se stanji. donja granica ostavlja bar nit mastila
        val taper = { t: Float -> max(l.taper[(t * (l.taper.size - 1)).roundToInt()], 0.1f) }
        c.drawBrush(l.points, pen, if (l in red) rub else ink, rnd = hand, pressure = taper)
    }
    val g = big.downsample(SS)
    printOnPaper(g, PaperSurface(SEED), resist = PAPER, pool = PAPER, resistAmount = TOOTH, poolAmount = 0f)

    gart.saveImage(g, OUT.ensureExtension())
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

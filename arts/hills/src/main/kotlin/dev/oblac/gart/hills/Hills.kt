package dev.oblac.gart.hills

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.poissonDiskSamplingNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

val blue = NipponColors.col195_KON
val star = NipponColors.col108_KUCHINASHI
val hill = NipponColors.col228_BOTAN
val border = NipponColors.col234_GOFUN
val ship = NipponColors.col155_TOKUSA

fun main() {
    val gart = Gart.of("hills", 1024, 1024 * GOLDEN_RATIO)
    println(gart)

    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val c = g.canvas

    // DRAW
    c.clear(blue)

    drawStars(c, d)
    drawSun(c, d)
    val hill = Hill(d, 900f).path()
    drawHill(c, hill)
    drawTriangle(c)
    c.drawBorder(d, 40f, border)

    gart.saveImage(g)

    w.showImage(g)
}

fun drawTriangle(c: Canvas) {
    val t = pointOf(200f, 300f)
    val a = 100f
    val alpha = 0.15f
    val d = 100f

    // Vertex B1 coordinates
    val b1X = -(a * cos(alpha)) / 2.0 + t.x
    val b1Y = -(a * sin(alpha)) / 2.0 + t.y
    val b1 = Point(b1X, b1Y)

    // Vertex B2 coordinates
    val b2X = (a * cos(alpha)) / 2.0 + t.x
    val b2Y = (a * sin(alpha)) / 2.0 + t.y
    val b2 = Point(b2X, b2Y)

    // Vertex V coordinates
    val vX = d * sin(alpha) + t.x
    val vY = -d * cos(alpha) + t.y
    val v = Point(vX, vY)

    val p = Path()
        .moveTo(b1)
        .lineTo(b2)
        .lineTo(v)
        .lineTo(b1)
        .closePath()

    c.drawPath(p, fillOf(ship))
    p.offset(16f, 16f)
    c.drawPath(p, strokeOf(ship, 2f))
}

fun drawStars(c: Canvas, d: Dimension) {
    val top = Dimension(d.w, d.h)
    poissonDiskSamplingNoise(top, 30.0)
        .filter { rndb(8, 10) }
        .forEach {
            c.drawCircle(it, 2f + rndf(0, 2f), fillOf(star))
        }
}

fun drawHill(c: Canvas, p: Path) {
    p.offset(-20f, 0f)  // the generation of the hill is shifted to the right by 40 pixels
    for (off in 0..14) {
        p.offset(rndf(-40, 40), off.toFloat() * 10)
        c.drawPath(p, fillOf(blue))
        c.drawPath(p, strokeOf(hill, 4f))
    }
}

fun drawSun(c: Canvas, d: Dimension) {
    val circle = Circle(d.w3x2, 700f, 200f)
//    val circleForContains = Circle(d.w3x2, 680f, 250f)
    val circleForContains = Circle(d.w3x2, 700f, 190f)
    c.drawCircle(circle, fillOf(NipponColors.col016_KURENAI))
    poissonDiskSamplingNoise(d, 20.0)
        .filter { it.isInside(circleForContains) }
        .forEach {
            c.drawCircle(it, 8f, fillOf(NipponColors.col024_AKABENI))
        }
}


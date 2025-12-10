package dev.oblac.gart.nm

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Colors
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndGaussian
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.math.rndsgn
import dev.oblac.gart.shader.createNoiseGrain2Filter
import dev.oblac.gart.smooth.catmullRomSpline
import dev.oblac.gart.text.drawStringToRight
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeLinearGradient
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("nm1", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    val draw = MyDrawNM1(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDrawNM1(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    val n = 1000
    val mean = d.h * 0.8f
    val sigma = d.h * 0.2f  // 2 sigma = 40% of height

    repeat(n) {
        val x = rndf(0f, d.wf)
        val y = rndGaussian(mean, sigma)
        if (y < 300f) return@repeat
        val radius = rndf(50f, 100f)

        val circle = Circle(x, y, radius)
        drawCircleCloud(c, d, circle)

        if (rndf(0f, 1f) < 0.3f) {
            drawRandomVerticalTowerRect(c, d)
        }
    }
    drawTextLinesOnLeftSide(c, d)
}

private val textlines = """
    The sky|above|the port|was|the color|of television,|tuned to|a dead|channel
""".trimIndent().split("|")

private val font = font(FontFamily.SpaceMono, 40f)

private fun drawTextLinesOnLeftSide(c: Canvas, d: Dimension) {
    val fontSize = 40f
    val paint = fillOfWhite()

    var y = d.hf - textlines.size * fontSize * 1.1f - 50f
    for (line in textlines) {
        val howManySplits = line.length / 3
        val line2 = if (howManySplits > 0) {
            var l = line
            l = spaceIt(howManySplits, l)
            l
        } else {
            line
        }
        c.drawStringToRight(line2, d.wf - 80f, y, font, paint)
        y += fontSize * 1.1f
    }
}

private fun spaceIt(howManySplits: Int, l: String): String {
    var l1 = l
    var splits = 0
    var attempts = 0
    val maxAttempts = howManySplits * 10
    while (splits < howManySplits && attempts < maxAttempts) {
        attempts++
        if (l1.length < 3) break
        val splitNdx = rndi(1, l1.length - 1)
        if (l1[splitNdx] == ' ') continue
        l1 = l1.substring(0, splitNdx) + " ".repeat(rndi(1, 3)) + l1.substring(splitNdx)
        splits++
    }
    return l1
}

private fun drawRandomVerticalTowerRect(c: Canvas, d: Dimension) {
    val w = rndf(20f, 80f)
    val h = rndf(100f, 400f)
    val y = rndf(320f, d.hf)
    val x = rndf(50f, d.wf - 50f - h)

    val r = Rect.makeXYWH(x, y, w, h).grow(2f)

    c.save()
    c.clipRect(r)
    c.saveLayer(createNoiseGrain2Filter(0.4f, d))
    c.drawRect(r, paint().apply {
        this.alpha = 200
        this.isDither = true
        this.mode = PaintMode.FILL
        this.shader = rectTow(r, rndf(30f, 120f), 0.4f, 0.8f)
    })
    c.restore()
    c.restore()
}

private fun drawCircleCloud(c: Canvas, d: Dimension, circle: Circle) {
    val yRatio = d.normH(circle.center.y)
    val fill = fillOf(Colors.black).apply {
        this.alpha = 200
        this.shader = Shader.makeLinearGradient(
            circle.center.offset(0f, -circle.radius),
            circle.center.offset(0f, +circle.radius - 100f * yRatio),
            colors = intArrayOf(Colors.transparent, Colors.black),
        )
    }

    val peb = createPebble(circle.center, circle.radius)
    //val p = circle.toPath().toPoints(1000)
    val p = peb.toPoints(1000)
    val dp = deformPath(p, 15f).toClosedPath()

    c.save()
    c.clipPath(dp)
    c.saveLayer(createNoiseGrain2Filter(0.4f, d))
    c.drawPath(dp, fill)
    c.restore()
    c.restore()
}

private fun createPebble(p: Point, radius1: Float): Path {
    val c1 = Circle(p, radius1).toPath()
    val c2 = Circle(
        p.offset(
            rndf(radius1 / 2, radius1) * rndsgn(), rndf(radius1 / 2, radius1) * rndsgn()
        ), rndf(radius1 / 2, radius1 * 2 / 3)
    ).toPath()
    val c3 = Circle(
        p.offset(
            -rndf(radius1 / 2, radius1) * rndsgn(), rndf(radius1 / 2, radius1) * rndsgn()
        ), rndf(radius1 / 2, radius1 * 2 / 3)
    ).toPath()


    val combined = combinePathsWithOp(PathOp.UNION, c1, c2, c3)
    val ps = combined.toPoints(10)
    return catmullRomSpline(ps, 40)
}


private fun rectTow(rect: Rect, angleDegrees: Float, f1: Float, f2: Float): Shader {
    // Convert angle to radians
    val angleRad = Math.toRadians(angleDegrees.toDouble())

    val rc = rect.center()
    val centerX = rc.x
    val centerY = rc.y

    // Calculate the maximum distance from center (diagonal)
    val maxDistance = kotlin.math.sqrt((rect.width * rect.width + rect.height * rect.height).toDouble()).toFloat() / 2

    // Calculate start and end points along the angle
    val startX = centerX - (cos(angleRad) * maxDistance).toFloat()
    val startY = centerY - (sin(angleRad) * maxDistance).toFloat()
    val endX = centerX + (cos(angleRad) * maxDistance).toFloat()
    val endY = centerY + (sin(angleRad) * maxDistance).toFloat()

    return makeLinearGradient(
        Point(startX, startY),
        Point(endX, endY),
        intArrayOf(Colors.white, Colors.black),
        floatArrayOf(f1, f2),
    )
}

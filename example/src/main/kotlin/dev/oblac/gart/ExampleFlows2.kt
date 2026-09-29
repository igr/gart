package dev.oblac.gart

import dev.oblac.gart.flow2.Integrator
import dev.oblac.gart.flow2.VectorField
import dev.oblac.gart.flow2.bake
import dev.oblac.gart.flow2.drawField
import dev.oblac.gart.flow2.move
import dev.oblac.gart.flow2.plus
import dev.oblac.gart.flow2.streamlines
import dev.oblac.gart.flow2.times
import dev.oblac.gart.flow2.trace
import dev.oblac.gart.gfx.drawBlackText
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.fbm
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.random.Random

/**
 * ExampleFlows2: the flow2 package, one tile per tool.
 * - angles: a noise angle field as evenly spaced streamlines
 * - curl: curl noise, each line thinner where it closes in on another (its clearance)
 * - vortices: three whirls with a reach plus a drift, summed, as scaled arrows
 * - euler vs rk2: the same circles traced both ways at 3 px steps; euler drifts out
 * - move: a crowd of points moved with velocity = true, faster near the whirls
 * - bake: a slow 5-octave curl baked on an 8 px grid, the lines run off the tile (margin)
 * Run with `-Dheadless=1` to just write `output/exampleFlows2.png`.
 */
fun main(args: Array<String>) {
    val t = 420f
    val gap = 30f
    val gart = Gart.of("exampleFlows2", (3 * t + 4 * gap).toInt(), (2 * (t + gap + 20f) + gap).toInt())
    val g = gart.gartvas()
    val c = g.canvas
    c.clear(0xFFFFFFFF.toInt())

    val d = Dimension(t.toInt(), t.toInt())
    val paper = fillOf(0xFFF4F1EA.toInt())
    val ink = 0xFF1B1B1B.toInt()
    val line = strokeOf(ink, 1.2f).apply { strokeCap = PaintStrokeCap.ROUND }

    // three whirls, each kept to its own area by its reach, and a slow drift to the right
    val whirls = VectorField.vortex(t * 0.3f, t * 0.35f, spin = 1f, pull = 0.25f, reach = t * 0.2f) +
        VectorField.vortex(t * 0.7f, t * 0.4f, spin = -1f, reach = t * 0.18f) +
        VectorField.vortex(t * 0.5f, t * 0.75f, spin = 1.2f, pull = -0.1f, reach = t * 0.22f) +
        VectorField.angles { _, _ -> 0f } * 0.15f

    val tiles = listOf<Pair<String, (Canvas) -> Unit>>(
        "angles: streamlines" to { cv ->
            val field = VectorField.angles { x, y -> SimplexNoise.noise(x * 0.004f, y * 0.004f) * TAUf }
            for (l in field.streamlines(d, Random(1), dSep = 8f)) cv.drawPath(l.toPath(), line)
        },
        "curl: width from clearance" to { cv ->
            val field = VectorField.curl({ x, y -> SimplexNoise.noise(x * 0.006f + 31f, y * 0.006f) })
            val pen = strokeOf(ink, 1f).apply { strokeCap = PaintStrokeCap.ROUND }
            for (l in field.streamlines(d, Random(2), dSep = 9f)) {
                for (i in 1 until l.points.size) {
                    pen.strokeWidth = 0.3f + 2.4f * l.taper(i)
                    cv.drawLine(l.points[i - 1].x, l.points[i - 1].y, l.points[i].x, l.points[i].y, pen)
                }
            }
        },
        "vortices: drawField, scaled" to { cv ->
            cv.drawField(whirls, d, gap = 26f, scaled = true)
        },
        "euler (red) vs rk2, 3 px steps" to { cv ->
            val circle = VectorField.vortex(t / 2, t / 2)
            val red = strokeOf(0xFFC0392B.toInt(), 1.2f)
            for (r in listOf(40f, 90f, 140f, 190f)) {
                val start = Point(t / 2 + r, t / 2)
                val steps = (2 * TAUf * r / 3f).toInt()  // two turns
                val euler = circle.trace(start, steps, step = 3f, rk = Integrator.EULER)
                val rk2 = circle.trace(start, steps, step = 3f, rk = Integrator.RK2)
                for (i in 1 until euler.size) cv.drawLine(euler[i - 1].x, euler[i - 1].y, euler[i].x, euler[i].y, red)
                for (i in 1 until rk2.size) cv.drawLine(rk2[i - 1].x, rk2[i - 1].y, rk2[i].x, rk2[i].y, line)
            }
        },
        "move: velocity, 160 frames" to { cv ->
            val rnd = Random(5)
            val trail = strokeOf(0x661B1B1B, 1f)
            val bounds = Rect(0f, 0f, t, t)
            var points = List(700) { Point(rnd.nextFloat() * t, rnd.nextFloat() * t) }
            repeat(160) {
                points = whirls.move(points, step = 2f, velocity = true, bounds = bounds) { a, b ->
                    cv.drawLine(a.x, a.y, b.x, b.y, trail)
                }
            }
        },
        "bake: 5-octave curl, 8 px grid" to { cv ->
            // the finest octave is ~20 px across, so 8 px cells still hold it
            val slow = VectorField.curl({ x, y -> fbm(x * 0.003f, y * 0.003f, octaves = 5, lacunarity = 2f, gain = 0.5f) })
            val baked = slow.bake(d, cell = 8f, margin = 40f)
            for (l in baked.streamlines(d, Random(6), dSep = 7f, margin = 40f)) cv.drawPath(l.toPath(), line)
        },
    )

    for ((i, tile) in tiles.withIndex()) {
        val (name, draw) = tile
        val x = gap + (i % 3) * (t + gap)
        val y = gap + 20f + (i / 3) * (t + gap + 20f)
        c.save()
        c.translate(x, y)
        c.clipRect(Rect(0f, 0f, t, t))
        c.drawRect(Rect(0f, 0f, t, t), paper)
        draw(c)
        c.restore()
        c.drawBlackText(name, x, y - 6f)
    }

    gart.saveImage(g, "output/exampleFlows2.png")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

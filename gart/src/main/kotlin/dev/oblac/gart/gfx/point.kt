package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.math.rnd
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin

fun randomPoint(d: Dimension) = Point(rnd(0f, d.wf), rnd(0f, d.hf))

/**
 * Random point in radius.
 */
fun randomPoint(cx: Float, cy: Float, rmax: Float, rmin: Float = rmax): Point {
    val angle = rnd(0f, 360f)
    val r = rnd(rmin, rmax)
    val x = cx + r * cos(angle)
    val y = cy + r * sin(angle)
    return Point(x, y)
}

fun Point.Companion.random(d: Dimension): Point = randomPoint(d)

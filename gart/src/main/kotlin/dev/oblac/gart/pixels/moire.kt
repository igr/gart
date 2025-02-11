package dev.oblac.gart.pixels

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.RGBA
import dev.oblac.gart.math.fastSqrt
import org.jetbrains.skia.Point

fun drawBlackWhiteMoire(b: Pixels, p1: Point, p2: Point, width: Int = 32) {
    val cx1 = p1.x
    val cy1 = p1.y
    val cx2 = p2.x
    val cy2 = p2.y

    for (y in 0 until b.d.h) {
        val dy = (y - cy1) * (y - cy1)
        val dy2 = (y - cy2) * (y - cy2)

        for (x in 0 until b.d.w) {
            val dx = (x - cx1) * (x - cx1)
            val dx2 = (x - cx2) * (x - cx2)
            val shade = (((fastSqrt(dx + dy).toInt() xor fastSqrt(dx2 + dy2).toInt()) / width) and 1) * 255

            b[x, y] = RGBA(shade, shade, shade, 255).value
        }
    }
}

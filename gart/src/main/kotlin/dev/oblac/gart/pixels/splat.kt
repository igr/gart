package dev.oblac.gart.pixels

import org.jetbrains.skia.Point
import kotlin.math.floor

/**
 * Adds [v] to the float field [buf] (rows of [w]) at the point ([x], [y]), shared bilinearly over the
 * four nearest cells. This is the reverse of a bilinear sample: a point on a whole pixel goes all to
 * that pixel. A share that falls off the field is dropped. A NaN point adds nothing.
 */
fun splat(buf: FloatArray, w: Int, x: Float, y: Float, v: Float) {
    val h = buf.size / w
    if (!(x > -1f && y > -1f && x < w && y < h)) return
    val ix = floor(x).toInt()
    val iy = floor(y).toInt()
    val fx = x - ix
    val fy = y - iy
    addAt(buf, w, h, ix, iy, v * (1f - fx) * (1f - fy))
    addAt(buf, w, h, ix + 1, iy, v * fx * (1f - fy))
    addAt(buf, w, h, ix, iy + 1, v * (1f - fx) * fy)
    addAt(buf, w, h, ix + 1, iy + 1, v * fx * fy)
}

/** [splat] for a [Point]. */
fun splat(buf: FloatArray, w: Int, p: Point, v: Float) = splat(buf, w, p.x, p.y, v)

private fun addAt(buf: FloatArray, w: Int, h: Int, x: Int, y: Int, v: Float) {
    if (x in 0 until w && y in 0 until h) buf[y * w + x] += v
}

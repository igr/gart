package orbitr

import dev.oblac.gart.Gart
import dev.oblac.gart.color.ColorRamp
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.reactiondiffusion.GrayScott
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SIZE = 1024

private val obstaclePixels: List<Pair<Int, Int>> = run {
    pixs(
        cx = SIZE / 3,
        cy = SIZE / 3 + 50,
        radius = SIZE / 8,
        angleDeg = 30f,
    )
}

private fun pixs(cx: Int, cy: Int, radius: Int, angleDeg: Float = 0f): ArrayList<Pair<Int, Int>> {
    // Equilateral triangle, circumradius `radius`, rotated `angleDeg` clockwise around (cx, cy).
    // At angleDeg = 0 the triangle points up.
    val angleRad = (angleDeg * PI / 180.0).toFloat()
    val a0 = -HALF_PIf + angleRad
    val third = TWO_PIf / 3f
    val x0 = cx + (radius * cos(a0)).toInt()
    val y0 = cy + (radius * sin(a0)).toInt()
    val x1 = cx + (radius * cos(a0 + third)).toInt()
    val y1 = cy + (radius * sin(a0 + third)).toInt()
    val x2 = cx + (radius * cos(a0 + 2f * third)).toInt()
    val y2 = cy + (radius * sin(a0 + 2f * third)).toInt()

    val xMin = minOf(x0, x1, x2)
    val xMax = maxOf(x0, x1, x2)
    val yMin = minOf(y0, y1, y2)
    val yMax = maxOf(y0, y1, y2)

    val list = ArrayList<Pair<Int, Int>>()
    for (y in yMin..yMax) {
        for (x in xMin..xMax) {
            // Half-plane test for each edge via 2D cross product.
            val w0 = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
            val w1 = (x - x2) * (y0 - y2) - (y - y2) * (x0 - x2)
            val w2 = (x - x0) * (y1 - y0) - (y - y0) * (x1 - x0)
            if ((w0 >= 0 && w1 >= 0 && w2 >= 0) || (w0 <= 0 && w1 <= 0 && w2 <= 0)) {
                list += x to y
            }
        }
    }
    return list
}

fun main() {
    val size = SIZE

    val gart = Gart.of("triangle-in-space", size, size, 60)
    val g = gart.gartvas()
    val map = gart.gartmap(g)

    val cr = ColorRamp.of(Palettes.cool54)

    fun newGrayScott() = GrayScott(
        size, size,
        feed = 0.030f,
        kill = 0.055f,
        Du = 0.18f,
        Dv = 0.06f,
    ).also { seedGrayScott(it) }

    val rd = newGrayScott()

    val w = gart.window()
    w.show { canvas, _, frames ->
        if (frames.new) {
            repeat(30) {
                rd.step()
                mask(rd)
            }
            // render
            for (y in 0 until rd.height) {
                for (x in 0 until rd.width) {
                    val cf = (rd.displayValue(x, y) + sin(x * 0.005f)/2 + cos(y * 0.005f))
                    map[x, y] = cr.colorAt(cf.coerceIn(0f, 1f))
                }
            }
            map.drawToCanvas()
        }
        g.snapshotTo(canvas)
        if (frames.frame == 500L) {
            gart.saveImage(canvas)
        }
    }
}

private fun mask(rd: GrayScott) {
    for ((x, y) in obstaclePixels) {
        rd.setU(x, y, 1f); rd.setV(x, y, 0f)
    }
}

private fun seedGrayScott(rd: GrayScott) {
    repeat(1) {
        val radius = rd.width / (it+1)
        rd.stampU(radius, radius, radius, 1f)
        rd.stampV(radius, radius, radius, 0.25f)
    }
}

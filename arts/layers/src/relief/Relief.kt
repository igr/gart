package work.relief

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Dimension
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.parseColor
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.smoothstep
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode

/**
 * RELIEF
 */
private const val W = 1200
private const val H = 672
private const val SS = 2

private data class Params(
    val out: String,
    val layers: Int,
    val warp: Float,
    val bevel: Float,
    val faultLayer: Int,
    val faultWidth: Float,
    val fault: Float,
    val red: Int,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = Params(
        out = ps("out", "work/relief"),
        layers = pi("layers", 30),
        warp = pf("warp", 1.0f),
        bevel = pf("bevel", 1f),
        faultLayer = pi("faultlayer", 16),
        faultWidth = pf("faultwidth", 0.3f),
        fault = pf("fault", 1f),
        red = ps("red", "#dc143c").parseColor(),
    )
    require(p.layers in 8..80)
    require(p.faultLayer in 1 until p.layers)
    require(p.faultWidth > 0f)

    val gart = Gart.of("relief", W, H)
    val g = gart.gartvas()
    val rw = W * SS
    val rh = H * SS
    val map = Gartmap(Dimension(rw, rh))

    val field = FloatArray(rw * rh)
    var lo = Float.POSITIVE_INFINITY
    var hi = Float.NEGATIVE_INFINITY
    for (y in 0 until rh) for (x in 0 until rw) {
        val v = height(x / rw.toFloat(), y / rh.toFloat(), p.warp)
        field[y * rw + x] = v
        lo = min(lo, v)
        hi = max(hi, v)
    }

    for (y in 0 until rh) for (x in 0 until rw) {
        val i = y * rw + x
        val z = ((field[i] - lo) / (hi - lo)).coerceIn(0f, 1f)
        val stepped = z * p.layers
        val layer = floor(stepped).toInt().coerceAtMost(p.layers - 1)
        val f = stepped - floor(stepped)

        // Broad tonal divide
        val tone = (layer + 0.5f) / p.layers
        val base = palette(smoothstep(0.03f, 0.96f, tone))

        val shadow = 1f - smoothstep(0.00f, 0.62f, f)

        // Directional surface light
        val xm = max(0, x - 4); val xp = min(rw - 1, x + 4)
        val ym = max(0, y - 4); val yp = min(rh - 1, y + 4)
        val dx = field[y * rw + xp] - field[y * rw + xm]
        val dy = field[yp * rw + x] - field[ym * rw + x]
        val slopeLight = ((-dx * 1.4f - dy) * 0.15f).coerceIn(-0.12f, 0.12f)
        val layerJitter = sin(layer * 2.17f) * 0.025f
        val light = slopeLight + layerJitter - shadow * 0.42f * p.bevel
        var color = shade(base, light)

        // asdasd
        val distanceToFault = abs(stepped - p.faultLayer)
        val seam = 1f - smoothstep(p.faultWidth * 0.35f, p.faultWidth, distanceToFault)
        val nx = x / rw.toFloat()
        val ny = y / rh.toFloat()
        val breakSignal =
            0.62f * sin(TAUf * (1.7f * nx + 0.8f * ny) + 0.4f) +
                0.38f * sin(TAUf * (4.1f * nx - 2.3f * ny) + 1.7f)
        val fragments = smoothstep(-0.12f, 0.18f, breakSignal)
        val faultMask = (seam * fragments * p.fault).coerceIn(0f, 1f)
        color = lerpColor(color, p.red, faultMask)
        map[x, y] = color
    }

    val image = map.image()
    g.canvas.drawImageRect(
        image,
        Rect.makeWH(rw.toFloat(), rh.toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    map.close()
    val output = if (p.out.endsWith(".png", true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun height(x: Float, y: Float, warp: Float): Float {
    val qx = x + warp * (
        0.105f * sin(TAUf * (1.15f * y + 0.10f * sin(TAUf * x))) +
            0.032f * sin(TAUf * (3.1f * y - 0.7f * x))
        )
    val qy = y + warp * (
        0.095f * sin(TAUf * (0.92f * x + 0.13f * cos(TAUf * y))) +
            0.026f * cos(TAUf * (2.7f * x + 0.8f * y))
        )

    var v = 0.70f * qx + 0.57f * qy
    v += 0.17f * sin(TAUf * (1.45f * qx - 0.72f * qy))
    v += 0.105f * sin(TAUf * (2.55f * qx + 1.34f * qy + 0.18f))
    v += 0.055f * cos(TAUf * (4.8f * qx - 2.1f * qy))
    v += 0.018f * sin(TAUf * (7.1f * qx + 3.2f * qy))

    // Local hills and hollows curl contours into the reference's nested caves.
    v += blob(qx, qy, 0.25f, 0.56f, 0.19f, 0.28f)
    v -= blob(qx, qy, 0.78f, 0.58f, 0.21f, 0.34f)
    v += blob(qx, qy, 0.57f, 0.16f, 0.23f, 0.20f)
    v -= blob(qx, qy, 0.92f, 0.18f, 0.15f, 0.17f)
    return v
}

private fun blob(x: Float, y: Float, cx: Float, cy: Float, r: Float, amount: Float): Float {
    val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / r
    return amount * (1f - smoothstep(0f, 1f, d))
}

private fun palette(t: Float): Int {
    val stops = arrayOf(
        floatArrayOf(245f, 245f, 239f),
        floatArrayOf(210f, 221f, 218f),
        floatArrayOf(117f, 157f, 170f),
        floatArrayOf(36f, 91f, 113f),
        floatArrayOf(7f, 42f, 60f),
        floatArrayOf(1f, 17f, 29f),
    )
    val u = t * (stops.size - 1)
    val k = floor(u).toInt().coerceIn(0, stops.size - 2)
    val f = u - k
    return argb(
        255,
        (stops[k][0] + (stops[k + 1][0] - stops[k][0]) * f).toInt(),
        (stops[k][1] + (stops[k + 1][1] - stops[k][1]) * f).toInt(),
        (stops[k][2] + (stops[k + 1][2] - stops[k][2]) * f).toInt(),
    )
}

private fun shade(color: Int, amount: Float): Int {
    return if (amount >= 0f) lighten(color, amount) else colorScale(color, 1f + amount)
}

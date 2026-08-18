package work.relief

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.parseColor
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
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
 * RELIEF 2
 */
private const val W = 1200
private const val H = 672
private const val SS = 2
private const val ARC_COVER_PERIOD = 12
private const val ARC_COVER_LAYERS = 3
private const val ARC_START_SCALE = 1.12f
private const val ARC_END_SCALE = 0.6f

private data class Relief2Params(
    val out: String,
    val layers: Int,
    val warp: Float,
    val arcLayer: Int,
    val arcWidth: Float,
    val arcStartX: Float,
    val arcEndX: Float,
    val arcStartY: Float,
    val arcEndY: Float,
    val arcHeight: Float,
    val arcOffsetY: Float,
    val temperature: Float,
    val red: Int,
)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val layers = pi("layers", 90)
    val p = Relief2Params(
        out = ps("out", "work/relief2"),
        layers = layers,
        warp = pf("warp", 1.1f),
        arcLayer = pi("arclayer", 16),
        arcWidth = pf("arcwidth", 0.42f),
        arcStartX = pf("arcstartx", 0.04f),
        arcEndX = pf("arcendx", 0.96f),
        arcStartY = pf("arcstarty", 0.43f),
        arcEndY = pf("arcendy", 0.66f),
        arcHeight = pf("archeight", 0.5f),
        arcOffsetY = pf("arcoffsety", 200f),
        temperature = pf("temperature", 0.8f),
        red = ps("red", "#dc143c").parseColor(),
    )
    require(p.layers in 8..160)
    require(p.arcLayer in 0 until p.layers)
    require(p.arcWidth in 0.01f..1f)
    require(p.arcStartX in 0f..1f && p.arcEndX in 0f..1f)
    require(p.arcStartX < p.arcEndX)
    require(p.arcStartY in 0f..1f && p.arcEndY in 0f..1f)
    require(p.arcHeight in 0f..1f)
    require(p.arcOffsetY in -H.toFloat()..H.toFloat())
    require(p.temperature in 0f..2f)

    val gart = Gart.of("relief2", W, H)
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

    // Warm the flat bands nearest the arc's depth while the rest of the palette stays cool.
    val arcTone = (p.arcLayer + 0.5f) / p.layers
    val layerColors = IntArray(p.layers) { layer ->
        val tone = (layer + 0.5f) / p.layers
        val warmth =
            (1f - smoothstep(0f, 0.14f, abs(tone - arcTone))) * p.temperature
        palette(smoothstep(0.03f, 0.96f, tone), warmth)
    }
    val arcHalfWidth = p.arcWidth * rh * 0.5f

    for (y in 0 until rh) for (x in 0 until rw) {
        val i = y * rw + x
        val z = ((field[i] - lo) / (hi - lo)).coerceIn(0f, 1f)
        val stepped = z * p.layers
        val layer = floor(stepped).toInt().coerceAtMost(p.layers - 1)
        var color = layerColors[layer]

        if (layer >= p.arcLayer) {
            // Later groups of contour layers pass back over the broad stroke.
            val foregroundLayer =
                (layer - p.arcLayer) % ARC_COVER_PERIOD < ARC_COVER_LAYERS
            if (!foregroundLayer) {
                val arcT = closestArcT(p, x.toFloat(), y.toFloat(), rw, rh)
                val arcPoint = arcPoint(p, arcT, rw, rh)
                val dx = x - arcPoint.first
                val dy = y - arcPoint.second
                if (dx * dx + dy * dy <= arcHalfWidth * arcHalfWidth) {
                    val arcColor = colorScale(
                        p.red,
                        ARC_START_SCALE + (ARC_END_SCALE - ARC_START_SCALE) * arcT,
                    )
                    color = arcColor
                }
            }
        }
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
    val output = p.out.ensureExtension("png")
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun arcPoint(
    p: Relief2Params,
    t: Float,
    rw: Int,
    rh: Int,
): Pair<Float, Float> {
    val baselineY = p.arcStartY + (p.arcEndY - p.arcStartY) * t
    return Pair(
        (p.arcStartX + (p.arcEndX - p.arcStartX) * t) * rw,
        (baselineY - p.arcHeight * sin(TAUf * 0.5f * t)) * rh + p.arcOffsetY * SS,
    )
}

private fun closestArcT(
    p: Relief2Params,
    x: Float,
    y: Float,
    rw: Int,
    rh: Int,
): Float {
    var t = ((x / rw - p.arcStartX) / (p.arcEndX - p.arcStartX)).coerceIn(0f, 1f)
    val dt = 1f / 256f
    repeat(4) {
        val before = arcPoint(p, (t - dt).coerceAtLeast(0f), rw, rh)
        val after = arcPoint(p, (t + dt).coerceAtMost(1f), rw, rh)
        val tangentX = after.first - before.first
        val tangentY = after.second - before.second
        val point = arcPoint(p, t, rw, rh)
        val tangentLengthSquared = tangentX * tangentX + tangentY * tangentY
        if (tangentLengthSquared > 0f) {
            t = (t + (tangentX * (x - point.first) + tangentY * (y - point.second)) /
                tangentLengthSquared * (2f * dt)).coerceIn(0f, 1f)
        }
    }
    return t
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

    // Local hills and hollows curl contours into nested caves.
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

private fun palette(t: Float, warmth: Float): Int {
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
    val red = stops[k][0] + (stops[k + 1][0] - stops[k][0]) * f
    val green = stops[k][1] + (stops[k + 1][1] - stops[k][1]) * f
    val blue = stops[k][2] + (stops[k + 1][2] - stops[k][2]) * f
    return argb(
        255,
        (red + 30f * warmth).toInt().coerceIn(0, 255),
        (green + 5f * warmth).toInt().coerceIn(0, 255),
        (blue - 22f * warmth).toInt().coerceIn(0, 255),
    )
}

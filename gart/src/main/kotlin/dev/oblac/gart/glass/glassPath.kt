package dev.oblac.gart.glass

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.MemPixels
import dev.oblac.gart.color.*
import dev.oblac.gart.gfx.toRegion
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.*

/**
 * Glass refraction effect on an arbitrary closed path.
 */
fun drawGlassPath(
    g: Gartvas,
    path: Path,
    eta: Double = 1.0 / 1.5,
    thickness: Double = 1.2,
    baseColor: Int = Color.BLACK,
    whiteSpot: Boolean = false
) {
    val c = g.canvas
    val bounds = path.bounds

    val baseR = red(baseColor)
    val baseG = green(baseColor)
    val baseB = blue(baseColor)
    fun base(a: Int) = argb(a, baseR, baseG, baseB)
    fun light(a: Int, mix: Float) = argb(
        a,
        (baseR + (255 - baseR) * mix).toInt(),
        (baseG + (255 - baseG) * mix).toInt(),
        (baseB + (255 - baseB) * mix).toInt()
    )

    val cx = (bounds.left + bounds.right) / 2f
    val cy = (bounds.top + bounds.bottom) / 2f
    val halfW = (bounds.right - bounds.left) / 2f
    val halfH = (bounds.bottom - bounds.top) / 2f
    val avgRadius = (halfW + halfH) / 2f

    // Precompute effective radius for each angular direction
    val region = path.toRegion()
    val maxDist = sqrt(halfW * halfW + halfH * halfH) * 1.5f
    val numAngles = 720
    val effectiveRadius = FloatArray(numAngles)

    for (a in 0 until numAngles) {
        val angle = a * 2.0 * PI / numAngles
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        var lo = 0f
        var hi = maxDist
        repeat(20) {
            val mid = (lo + hi) / 2f
            if (region.contains((cx + dx * mid).toInt(), (cy + dy * mid).toInt())) lo = mid else hi = mid
        }
        effectiveRadius[a] = lo
    }

    val gartmap = Gartmap(g)
    val srcPixels = MemPixels(g.d)
    srcPixels.copyPixelsFrom(gartmap)

    val width = g.d.w
    val height = g.d.h
    val bLeft = bounds.left.toInt().coerceAtLeast(0)
    val bRight = bounds.right.toInt().coerceAtMost(width - 1)
    val bTop = bounds.top.toInt().coerceAtLeast(0)
    val bBottom = bounds.bottom.toInt().coerceAtMost(height - 1)

    for (py in bTop..bBottom) {
        for (px in bLeft..bRight) {
            if (!region.contains(px, py)) continue

            val dx = (px - cx).toDouble()
            val dy = (py - cy).toDouble()
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.001) continue

            // Look up effective radius for this direction
            val angle = atan2(dy, dx)
            val angleNorm = if (angle < 0) angle + 2 * PI else angle
            val angleIndex = ((angleNorm / (2 * PI)) * numAngles).toInt().coerceIn(0, numAngles - 1)
            val effR = effectiveRadius[angleIndex].toDouble()

            if (effR < 1.0) continue

            val nd = (dist / effR).coerceAtMost(1.0)

            val z = sqrt(1.0 - nd * nd)
            val k = 1.0 - eta * eta * nd * nd
            if (k <= 0) continue

            val refractOffset = eta * z - sqrt(k)
            val factor = 1.0 + refractOffset * thickness

            val srcXd = cx + dx * factor
            val srcYd = cy + dy * factor

            val x0 = floor(srcXd).toInt()
            val y0 = floor(srcYd).toInt()
            val xf = x0 + 1
            val yf = y0 + 1
            val deltaX = srcXd - x0
            val deltaY = srcYd - y0

            if (x0 < 0 || xf >= width || y0 < 0 || yf >= height) continue

            val p00 = srcPixels[x0, y0]
            val p10 = srcPixels[xf, y0]
            val p01 = srcPixels[x0, yf]
            val p11 = srcPixels[xf, yf]

            val a = bilerp(alpha(p00), alpha(p10), alpha(p01), alpha(p11), deltaX, deltaY)
            val rv = bilerp(red(p00), red(p10), red(p01), red(p11), deltaX, deltaY)
            val gv = bilerp(green(p00), green(p10), green(p01), green(p11), deltaX, deltaY)
            val b = bilerp(blue(p00), blue(p10), blue(p01), blue(p11), deltaX, deltaY)

            gartmap[px, py] = argb(a, rv, gv, b)
        }
    }

    gartmap.drawToCanvas()

    // Visual overlays clipped to path

    // 1. Rim darkening
    c.save()
    c.clipPath(path)
    c.drawCircle(cx, cy, avgRadius * 1.5f, Paint().apply {
        isAntiAlias = true
        shader = makeRadialGradient(
            cx, cy, avgRadius,
            intArrayOf(base(0x00), base(0x00), base(0x70), base(0xBB)),
            floatArrayOf(0f, 0.5f, 0.85f, 1f),
            GradientStyle.DEFAULT
        )
    })

    // 2. Diffuse specular highlight (top-left)
    val hlX = cx - avgRadius * 0.3f
    val hlY = cy - avgRadius * 0.35f
    val hlR = avgRadius * 0.5f
    c.drawCircle(hlX, hlY, hlR, Paint().apply {
        isAntiAlias = true
        shader = makeRadialGradient(
            hlX, hlY, hlR,
            intArrayOf(light(0x50, 0.85f), light(0x18, 0.85f), light(0x00, 0.85f)),
            floatArrayOf(0f, 0.4f, 1f),
            GradientStyle.DEFAULT
        )
    })

    // 3. Small bright spot
    if (whiteSpot) {
        val spotX = cx - avgRadius * 0.22f
        val spotY = cy - avgRadius * 0.38f
        val spotR = avgRadius * 0.1f
        c.drawCircle(spotX, spotY, spotR, Paint().apply {
            isAntiAlias = true
            shader = makeRadialGradient(
                spotX, spotY, spotR,
                intArrayOf(light(0xC0, 0.95f), light(0x00, 0.95f)),
                floatArrayOf(0f, 1f),
                GradientStyle.DEFAULT
            )
        })
    }
    c.restore()

    // 4. Rim outline (follows the path shape)
    c.drawPath(path, Paint().apply {
        isAntiAlias = true
        mode = PaintMode.STROKE
        strokeWidth = 1.5f
        color = light(0x30, 0.7f)
    })
}

private fun bilerp(c00: Int, c10: Int, c01: Int, c11: Int, dx: Double, dy: Double): Int {
    val top = c00 * (1.0 - dx) + c10 * dx
    val bot = c01 * (1.0 - dx) + c11 * dx
    return (top * (1.0 - dy) + bot * dy).toInt().coerceIn(0, 255)
}

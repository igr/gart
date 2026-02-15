package dev.oblac.gart.glass

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.MemPixels
import dev.oblac.gart.color.*
import org.jetbrains.skia.Color
import org.jetbrains.skia.GradientStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import kotlin.math.floor
import kotlin.math.sqrt

// Spherical refraction distortion using pixel manipulation
fun drawGlassBall(
    g: Gartvas,
    cx: Float,
    cy: Float,
    radius: Float,
    eta: Double = 1.0 / 1.5,   // air-to-glass refraction ratio (Snell's law)
    thickness: Double = 1.2,   // refraction displacement strength
    baseColor: Int = Color.BLACK,
    whiteSpot: Boolean = false
) {
    val c = g.canvas

    val baseR = red(baseColor)
    val baseG = green(baseColor)
    val baseB = blue(baseColor)

    // base color with given alpha (for rim/shadow)
    fun base(a: Int) = argb(a, baseR, baseG, baseB)

    // base color mixed toward white with given alpha (for highlights)
    fun light(a: Int, mix: Float) = argb(
        a,
        (baseR + (255 - baseR) * mix).toInt(),
        (baseG + (255 - baseG) * mix).toInt(),
        (baseB + (255 - baseB) * mix).toInt()
    )

    val gartmap = Gartmap(g)
    val srcPixels = MemPixels(g.d)
    srcPixels.copyPixelsFrom(gartmap)

    val centerX = cx.toInt()
    val centerY = cy.toInt()
    val r = radius.toInt()
    val rSq = r * r
    val width = g.d.w
    val height = g.d.h

    for (y in -r until r) {
        for (x in -r until r) {
            val distSq = x * x + y * y
            if (distSq >= rSq) continue

            val destX = x + centerX
            val destY = y + centerY
            if (destX !in 0 until width || destY !in 0 until height) continue

            val dist = sqrt(distSq.toDouble())
            val nd = dist / r  // normalized distance 0..1

            if (nd < 0.001) continue  // center pixel stays as-is

            // Sphere surface height
            val z = sqrt(1.0 - nd * nd)

            // Snell's law refraction: compute refraction displacement
            val k = 1.0 - eta * eta * nd * nd
            if (k <= 0) continue  // total internal reflection at extreme edge

            val refractOffset = eta * z - sqrt(k)
            val factor = 1.0 + refractOffset * thickness

            // Source coordinates after refraction
            val srcXd = cx + x * factor
            val srcYd = cy + y * factor

            // Bilinear interpolation
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

            gartmap[destX, destY] = argb(a, rv, gv, b)
        }
    }

    // Write distorted pixels back to canvas
    gartmap.drawToCanvas()

    // Glass ball visual overlays

    // 1. Rim darkening (Fresnel effect - edges reflect more, transmit less)
    c.drawCircle(cx, cy, radius, Paint().apply {
        isAntiAlias = true
        shader = makeRadialGradient(
            cx, cy, radius,
            intArrayOf(base(0x00), base(0x00), base(0x70), base(0xBB)),
            floatArrayOf(0f, 0.5f, 0.85f, 1f),
            GradientStyle.DEFAULT
        )
    })

    // 2. Diffuse specular highlight (top-left)
    val hlX = cx - radius * 0.3f
    val hlY = cy - radius * 0.35f
    val hlR = radius * 0.5f
    c.drawCircle(hlX, hlY, hlR, Paint().apply {
        isAntiAlias = true
        shader = makeRadialGradient(
            hlX, hlY, hlR,
            intArrayOf(light(0x50, 0.85f), light(0x18, 0.85f), light(0x00, 0.85f)),
            floatArrayOf(0f, 0.4f, 1f),
            GradientStyle.DEFAULT
        )
    })

    // 3. Small bright spot (concentrated light reflection)
    if (whiteSpot) {
        val spotX = cx - radius * 0.22f
        val spotY = cy - radius * 0.38f
        val spotR = radius * 0.1f
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

    // 4. Subtle rim outline
    c.drawCircle(cx, cy, radius, Paint().apply {
        isAntiAlias = true
        mode = PaintMode.STROKE
        strokeWidth = 1.5f
        shader = makeRadialGradient(
            cx, cy, radius,
            intArrayOf(light(0x00, 0.7f), light(0x30, 0.7f)),
            floatArrayOf(0.85f, 1f),
            GradientStyle.DEFAULT
        )
    })
}

private fun bilerp(c00: Int, c10: Int, c01: Int, c11: Int, dx: Double, dy: Double): Int {
    val top = c00 * (1.0 - dx) + c10 * dx
    val bot = c01 * (1.0 - dx) + c11 * dx
    return (top * (1.0 - dy) + bot * dy).toInt().coerceIn(0, 255)
}

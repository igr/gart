package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.color.gradientOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Shader.Companion.makeRadialGradient

/**
 * Draws a soft radial vignette over the whole canvas: fully transparent in the center,
 * darkening to [color] toward the edges.
 *
 * @param d         canvas dimensions
 * @param strength  vignette opacity (`0f`..`1.4f`); `1f` is the default darkness, saturating near `1.15f`
 * @param color     edge colour as RGB (its alpha is derived from [strength])
 * @param radius    gradient radius as a fraction of the width
 * @param innerStop fraction of the radius that stays fully transparent before the fade begins
 */
fun Canvas.drawVignette(
    d: Dimension,
    strength: Float = 1f,
    color: Int = 0x050409,
    radius: Float = 0.74f,
    innerStop: Float = 0.6f,
) {
    val a = (0xDD * strength.coerceIn(0f, 1.4f)).toInt().coerceIn(0, 255)
    drawPaint(Paint().apply {
        shader = makeRadialGradient(
            d.cx, d.cy, d.wf * radius,
            gradientOf(
                intArrayOf(0x00000000, 0x00000000, (a shl 24) or (color and 0xFFFFFF)),
                floatArrayOf(0f, innerStop, 1f)
            )
        )
    })
}

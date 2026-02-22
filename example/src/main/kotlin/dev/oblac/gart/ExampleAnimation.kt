package dev.oblac.gart

import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Shader
import kotlin.math.cos
import kotlin.math.sin

fun main() {
    val gart = Gart.of("Example Animation", 1280, 1280, 60)
    println(gart)

    val w = gart.window()

    w.show{ c, d, f ->
        f.print()

        // Background gradient
        val bgPaint = Paint().apply {
            shader = Shader.makeLinearGradient(
                x0 = 0f, y0 = 0f,
                x1 = d.width, y1 = d.height,
                colors = intArrayOf(0xFF1a1a2e.toInt(), 0xFF16213e.toInt(), 0xFF0f3460.toInt())
            )
        }
        c.drawRect(Rect.makeWH(d.width, d.height), bgPaint)

        // Draw rotating petals
        val t = f.timeSeconds
        val numPetals = 100
        for (i in 0 until numPetals) {
            val angle = t + i * (PIf * 2 / numPetals)
            val r = 450f
            val px = d.cx + cos(angle) * r
            val py = d.cy + sin(angle) * r

            val alpha = (0.4f + 0.3f * sin(t + i)).coerceIn(0f, 1f)
            val hue = (i * 60f + t * 30f) % 360f
            val color = hsvToArgb(hue, 0.8f, 1.0f, alpha)

            val petalPaint = Paint().apply {
                this.color = color
                isAntiAlias = true
            }
            c.drawCircle(px, py, 350f, petalPaint)
        }

        // Center circle with glow effect
        repeat(10) { ring ->
            val radius = 40f - ring * 6f
            val alpha = 0.15f + ring * 0.1f
            val glowPaint = Paint().apply {
                color = (((alpha * 255).toInt() shl 24) or 0x00e94560).toInt()
                isAntiAlias = true
            }
            c.drawCircle(d.cx, d.cy, radius + 20f, glowPaint)
        }

        val centerPaint = Paint().apply {
            shader = Shader.makeRadialGradient(
                x = d.cx, y = d.cy, r = 140f,
                colors = intArrayOf(0xFFFFFFFF.toInt(), 0xFFe94560.toInt())
            )
            isAntiAlias = true
        }
        c.drawCircle(d.cx, d.cy, 140f, centerPaint)
    }
}

private fun hsvToArgb(h: Float, s: Float, v: Float, a: Float): Int {
    val hi = ((h / 60f) % 6).toInt()
    val f = h / 60f - hi
    val p = v * (1 - s)
    val q = v * (1 - f * s)
    val t = v * (1 - (1 - f) * s)
    val (r, g, b) = when (hi) {
        0 -> Triple(v, t, p)
        1 -> Triple(q, v, p)
        2 -> Triple(p, v, t)
        3 -> Triple(p, q, v)
        4 -> Triple(t, p, v)
        else -> Triple(v, p, q)
    }
    val ai = (a * 255).toInt() and 0xFF
    val ri = (r * 255).toInt() and 0xFF
    val gi = (g * 255).toInt() and 0xFF
    val bi = (b * 255).toInt() and 0xFF
    return (ai shl 24) or (ri shl 16) or (gi shl 8) or bi
}



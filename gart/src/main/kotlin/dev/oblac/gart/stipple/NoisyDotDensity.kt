package dev.oblac.gart.stipple

import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.SampleMode.CLAMP
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.space.luminance
import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color
import org.jetbrains.skia.Color4f
import kotlin.math.floor
import kotlin.math.pow
import kotlin.random.Random

/**
 * Noisy stipple dithering algorithm that places dots with varying density and size
 * based on the darkness of the source image. Works fine with small radius.
 * I dont know if this algorithm exists, but I came somehow to this and it worked.
 */
fun stippleNoisyDotDensity(
    b: Pixels,
    step: Int = 6,
    minRadius: Float = 0.8f,
    maxRadius: Float = 1.8f,
    density: Float = 0.72f,
    seed: Int = 42,
    backgroundColor: Int = Color.WHITE,
) {
    val width = b.d.w
    val height = b.d.h
    val source = MemPixels(b.d)
    source.copyPixelsFrom(b)
    b.fill(backgroundColor)

    val random = Random(seed)
    val field = FloatArray(width * height)

    fun sourceDarkness(x: Int, y: Int): Float {
        return 1f - Color4f.of(source.sampleNearest(x, y, CLAMP, backgroundColor)).luminance
    }

    fun smoothDarkness(x: Int, y: Int): Float {
        var sum = 0f
        var weightSum = 0f
        for (dy in -4..4) {
            for (dx in -4..4) {
                val dist2 = (dx * dx + dy * dy).toFloat()
                val weight = 1f / (1f + dist2)
                sum += sourceDarkness(x + dx, y + dy) * weight
                weightSum += weight
            }
        }
        return (sum / weightSum).coerceIn(0f, 1f)
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val d = smoothDarkness(x, y)
            val shaped = d * d * (3f - 2f * d)
            field[y * width + x] = shaped
        }
    }

    fun fieldAt(x: Float, y: Float): Float {
        val x0 = floor(x).toInt().coerceIn(0, width - 1)
        val y0 = floor(y).toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceAtMost(width - 1)
        val y1 = (y0 + 1).coerceAtMost(height - 1)
        val tx = (x - x0).coerceIn(0f, 1f)
        val ty = (y - y0).coerceIn(0f, 1f)

        val v00 = field[y0 * width + x0]
        val v10 = field[y0 * width + x1]
        val v01 = field[y1 * width + x0]
        val v11 = field[y1 * width + x1]

        val top = v00 + (v10 - v00) * tx
        val bottom = v01 + (v11 - v01) * tx
        return (top + (bottom - top) * ty).coerceIn(0f, 1f)
    }

    fun drawDot(cx: Float, cy: Float, radius: Float) {
        val left = floor(cx - radius - 1f).toInt().coerceAtLeast(0)
        val top = floor(cy - radius - 1f).toInt().coerceAtLeast(0)
        val right = floor(cx + radius + 1f).toInt().coerceAtMost(width - 1)
        val bottom = floor(cy + radius + 1f).toInt().coerceAtMost(height - 1)
        val radiusSq = radius * radius

        for (py in top..bottom) {
            for (px in left..right) {
                val dx = px + 0.5f - cx
                val dy = py + 0.5f - cy
                if (dx * dx + dy * dy <= radiusSq) {
                    b[px, py] = CssColors.black
                }
            }
        }
    }

    val offsetCount = 3
    val jitter = step * 0.9f

    for (oy in 0 until offsetCount) {
        for (ox in 0 until offsetCount) {
            val baseX = (ox.toFloat() / offsetCount) * step
            val baseY = (oy.toFloat() / offsetCount) * step

            var gy = baseY
            while (gy < height) {
                var gx = baseX
                while (gx < width) {
                    val px = gx + (random.nextFloat() - 0.5f) * jitter
                    val py = gy + (random.nextFloat() - 0.5f) * jitter
                    val sx = px.coerceIn(0f, width - 1f)
                    val sy = py.coerceIn(0f, height - 1f)

                    val darkness = fieldAt(sx, sy)
                    val probability = (darkness.pow(1.6f) * density).coerceIn(0f, 1f)
                    if (random.nextFloat() < probability) {
                        val radiusT = darkness.pow(0.9f)
                        val radius = minRadius + (maxRadius - minRadius) * radiusT
                        drawDot(sx, sy, radius)
                    }

                    gx += step.toFloat()
                }
                gy += step.toFloat()
            }
        }
    }
}

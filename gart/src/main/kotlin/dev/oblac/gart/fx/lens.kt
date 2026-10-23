package dev.oblac.gart.fx

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.rgb
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.smoothstep
import org.jetbrains.skia.Point
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Makes [target] look as if it was seen through a cheap lens, in place: red and blue fringes, dark
 * edges and grain. The result is opaque.
 *
 * Distances from [center] are in units of half the short side. Call such a distance `r`.
 *
 * Each pixel takes its red from farther out and its blue from farther in, on the line from [center].
 * The step is `split * r` of the distance to [center], so it grows with the square of the distance.
 * On a 1024 px square, `0.006f` gives 3 px at the middle of each side and 6 px in the corners.
 * Green does not move.
 *
 * The light falls smoothly from `r = fade.start` to `r = fade.endInclusive`. After that, `1 - vignette`
 * of the light stays.
 *
 * The grain adds one random value to the three channels of a pixel. The value is from `-grain / 2`
 * to `grain / 2`, in levels of `0..255`. The same [seed] gives the same grain.
 *
 * @param center the middle of the lens, in px
 * @param split how fast red and blue move apart. `0f` keeps them in place
 * @param maxSplit the largest step of red or blue, in px. Small type looks double above 2-3 px
 * @param vignette how much the edges darken, `0f`..`1f`. `0f` does not darken
 * @param fade where the darkening starts and ends, in units of half the short side
 * @param grain the size of the grain, in levels. `0f` adds no grain
 * @param seed the seed of the grain
 */
fun addLens(
    target: Pixels,
    center: Point = target.d.center,
    split: Float = 0.006f,
    maxSplit: Float = Float.POSITIVE_INFINITY,
    vignette: Float = 0.35f,
    fade: ClosedFloatingPointRange<Float> = 0.7f..1.45f,
    grain: Float = 46f,
    seed: Int = 0,
) {
    require(fade.start < fade.endInclusive) { "fade must start before it ends: $fade" }
    val d = target.d
    val px = target.pixels
    val src = px.copyOf()
    val cx = center.x
    val cy = center.y
    val unit = min(d.wf, d.hf) / 2
    val f0 = fade.start
    val f1 = fade.endInclusive
    fun at(x: Float, y: Float) = src[y.toInt().coerceIn(0, d.h - 1) * d.w + x.toInt().coerceIn(0, d.w - 1)]
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val dx = x - cx
        val dy = y - cy
        val dist = hypot(dx, dy)
        val r = dist / unit
        val k = min(split * r, maxSplit / max(dist, 1f))
        val vig = 1f - vignette * smoothstep(f0, f1, r)
        val n = (hash01(x, y, 9, seed) - 0.5f) * grain
        px[y * d.w + x] = rgb(
            ((red(at(x + dx * k, y + dy * k)) * vig + n) / 255f).coerceIn(0f, 1f),
            ((green(src[y * d.w + x]) * vig + n) / 255f).coerceIn(0f, 1f),
            ((blue(at(x - dx * k, y - dy * k)) * vig + n) / 255f).coerceIn(0f, 1f),
        )
    }
}

/**
 * Makes [gartvas] look as if it was seen through a cheap lens, in place.
 * It reads the pixels, does the same as the [Pixels] version, and writes the pixels back.
 */
fun addLens(
    gartvas: Gartvas,
    center: Point = gartvas.d.center,
    split: Float = 0.006f,
    maxSplit: Float = Float.POSITIVE_INFINITY,
    vignette: Float = 0.35f,
    fade: ClosedFloatingPointRange<Float> = 0.7f..1.45f,
    grain: Float = 46f,
    seed: Int = 0,
) {
    Gartmap(gartvas).use {
        addLens(it, center, split, maxSplit, vignette, fade, grain, seed)
        it.drawToCanvas()
    }
}

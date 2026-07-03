package dev.oblac.gart.fx

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.math.hash01

/**
 * Adds fine monochrome film grain over the whole canvas, in place.
 *
 * Deterministic for a given [seed]: the default noise field is a pure hash of pixel coordinates,
 * so a seeded generative piece renders identically across runs. Pass [noise] to substitute a
 * different per-pixel field; it receives the pixel `(x, y)` and must return a value in `[0, 1)`.
 *
 * @param gartvas canvas to grain in place
 * @param amount  grain strength (`0f`..`1f`); mapped to a ±`amount * 40` luma jitter per channel
 * @param seed    seed for the default coordinate hash
 * @param noise   per-pixel noise source returning `[0, 1)`; defaults to a seeded coordinate hash
 */
fun addGrain(
    gartvas: Gartvas,
    amount: Float,
    seed: Int,
    noise: (x: Int, y: Int) -> Float = { x, y -> hash01(x, y + 1, seed) },
) {
    val m = Gartmap(gartvas)
    val px = m.pixels
    val amp = amount.coerceIn(0f, 1f) * 40f
    val w = gartvas.d.w
    for (i in px.indices) {
        val n = (noise(i % w, i / w) - 0.5f) * 2f * amp
        val col = px[i]
        val r = (((col ushr 16) and 0xFF) + n).toInt().coerceIn(0, 255)
        val gg = (((col ushr 8) and 0xFF) + n).toInt().coerceIn(0, 255)
        val b = ((col and 0xFF) + n).toInt().coerceIn(0, 255)
        px[i] = (0xFF shl 24) or (r shl 16) or (gg shl 8) or b
    }
    m.drawToCanvas(gartvas)
}

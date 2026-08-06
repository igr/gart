package dev.oblac.gart.util

import kotlin.math.min

/**
 * Worker count to fan a render out over: one per core, capped.
 *
 * The cap is not politeness. Past a dozen threads a banded pixel loop is usually held up by
 * memory bandwidth rather than by arithmetic, so the extra workers buy nothing and the bands
 * get short enough that the split itself starts to show.
 */
val defaultWorkers: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 12)

/**
 * Splits `0 until height` into contiguous bands, one per worker, and runs [body] on each,
 * returning once they have all finished.
 *
 * The point of banding rather than handing out rows round-robin is that **every row belongs to
 * exactly one worker**. For the usual job — many primitives splatted into one shared output
 * buffer — that makes the result independent of both the worker count and the order the
 * threads happen to run in: a given pixel is only ever touched by one thread, and that thread
 * walks the primitives in the same order it always would. Float addition is neither
 * associative nor commutative, so this is the difference between a seeded piece that renders
 * identically on any machine and one that drifts in the last bit. Verify the usual way: render
 * twice, `shasum`.
 *
 * [body] gets a half-open row range and must confine its writes to it. Reads may go anywhere,
 * as long as nothing else is writing there at the same time — which rules out reading a
 * neighbouring row of the same buffer another band is writing to. For a stencil pass over a
 * buffer that is being updated, double-buffer and read the other one.
 *
 * Runs inline on the calling thread when there is only one worker or one row, so a debug run
 * with `workers = 1` has no threading in it at all.
 *
 * @param height number of rows to divide up
 * @param workers how many bands to cut, defaults to [defaultWorkers]
 * @param body    called once per band with `[y0, y1)`
 */
fun parallelBands(height: Int, workers: Int = defaultWorkers, body: (y0: Int, y1: Int) -> Unit) {
    if (height <= 0) return

    val n = workers.coerceIn(1, height)
    if (n == 1) {
        body(0, height)
        return
    }

    val band = (height + n - 1) / n
    (0 until n)
        .map { t -> Thread { body(t * band, min(height, (t + 1) * band)) } }
        .onEach { it.start() }
        .forEach { it.join() }
}

package dev.oblac.gart.math

/**
 * A 1D function tabled once and read back by lerping between two samples.
 *
 * For a function that is expensive to evaluate and gets hammered at arbitrary `x` - a profile
 * sampled by every particle, a crest asked for its height thousands of times per row. Sampling
 * it [size] times and interpolating is usually indistinguishable from the real thing and can
 * be several times faster; pick a [step] small against the finest feature the function has.
 *
 * Reads outside the tabled range clamp to the ends, so it is safe to ask for anything, but a
 * table that does not cover the range in use quietly goes flat there - give it room.
 *
 * @param x0   where the table starts
 * @param step distance between samples
 * @param size number of samples
 * @param f    the function, called [size] times right here in the constructor
 */
class Lut1D(private val x0: Float, private val step: Float, val size: Int, f: (Float) -> Float) {
    init {
        require(size >= 2) { "size must be >= 2" }
        require(step > 0f) { "step must be > 0" }
    }

    private val v = FloatArray(size) { f(x0 + it * step) }

    operator fun get(x: Float): Float {
        val t = ((x - x0) / step).coerceIn(0f, size - 1.001f)
        val i = t.toInt()
        return lerp(v[i], v[i + 1], t - i)
    }
}

package dev.oblac.gart.math

import kotlin.math.cos
import kotlin.math.sin

/**
 * Fast fourier transform, radix 2, in place. The result replaces the input in [re] and [im].
 * [fft] gives the result as a new array instead.
 *
 * The values are complex: the real parts are in [re], the imaginary parts in [im]. The length
 * must be a power of two. Forward is `X[k] = sum x[j] e^(-i 2pi jk/n)`, and [inverse] uses
 * `e^(+i ..)`. Neither direction scales, so a forward and then an inverse gives the input
 * times n.
 *
 * The twiddle factors come from a running product, not from a table. This keeps the code
 * short, but the error grows with the length.
 */
fun fftInPlace(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
    val n = re.size
    require(im.size == n) { "re and im must have the same length, got $n and ${im.size}" }
    require(n.isPowerOfTwo()) { "length must be a power of two, got $n" }
    require(re !== im) { "re and im must be two arrays, not the same one" }
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while (j and bit != 0) {
            j = j xor bit
            bit = bit shr 1
        }
        j = j xor bit
        if (i < j) {
            val tr = re[i]
            re[i] = re[j]
            re[j] = tr
            val ti = im[i]
            im[i] = im[j]
            im[j] = ti
        }
    }
    var len = 2
    while (len <= n) {
        val half = len / 2
        val a = 2.0 * Math.PI / len
        val wr = cos(a)
        val wi = if (inverse) sin(a) else -sin(a)
        for (s in 0 until n step len) {
            var cr = 1.0
            var ci = 0.0
            for (k in 0 until half) {
                val p = s + k
                val q = p + half
                val br = re[q] * cr - im[q] * ci
                val bi = re[q] * ci + im[q] * cr
                re[q] = re[p] - br
                im[q] = im[p] - bi
                re[p] += br
                im[p] += bi
                val nr = cr * wr - ci * wi
                ci = cr * wi + ci * wr
                cr = nr
            }
        }
        if (len == n) break // one more doubling wraps negative at n = 2^30
        len = len shl 1
    }
}

/**
 * [fftInPlace] of real values, as a new array of [n] complex numbers. [x] does not change. It is cut
 * to [n], or padded with zeros up to [n]. [n] must be a power of two.
 */
fun fft(x: DoubleArray, n: Int = x.size, inverse: Boolean = false): Array<Complex> {
    require(n.isPowerOfTwo()) { "n must be a power of two, got $n" }
    val re = x.copyOf(n)
    val im = DoubleArray(n)
    fftInPlace(re, im, inverse)
    return complexOf(re, im)
}

/**
 * [fftInPlace] of complex values, as a new array of [n] complex numbers. [x] does not change. It is
 * cut to [n], or padded with zeros up to [n]. [n] must be a power of two.
 */
fun fft(x: Array<Complex>, n: Int = x.size, inverse: Boolean = false): Array<Complex> {
    require(n.isPowerOfTwo()) { "n must be a power of two, got $n" }
    val re = DoubleArray(n) { if (it < x.size) x[it].real else 0.0 }
    val im = DoubleArray(n) { if (it < x.size) x[it].imag else 0.0 }
    fftInPlace(re, im, inverse)
    return complexOf(re, im)
}

/**
 * [fftInPlace] on a [w] x [h] grid, stored row by row (`index = y * w + x`). It transforms the rows
 * first, then the columns. Both sides must be powers of two.
 */
fun fft2InPlace(re: DoubleArray, im: DoubleArray, w: Int, h: Int = w, inverse: Boolean = false) {
    require(w.isPowerOfTwo() && h.isPowerOfTwo()) { "both sides must be powers of two, got $w x $h" }
    val cells = w.toLong() * h // as an Int, 65536 x 65536 wraps to 0
    require(re.size.toLong() == cells && im.size.toLong() == cells) { "re and im must hold $w x $h values, got ${re.size} and ${im.size}" }
    require(re !== im) { "re and im must be two arrays, not the same one" }
    val rr = DoubleArray(w)
    val ri = DoubleArray(w)
    for (y in 0 until h) {
        for (x in 0 until w) {
            rr[x] = re[y * w + x]
            ri[x] = im[y * w + x]
        }
        fftInPlace(rr, ri, inverse)
        for (x in 0 until w) {
            re[y * w + x] = rr[x]
            im[y * w + x] = ri[x]
        }
    }
    val cr = DoubleArray(h)
    val ci = DoubleArray(h)
    for (x in 0 until w) {
        for (y in 0 until h) {
            cr[y] = re[y * w + x]
            ci[y] = im[y * w + x]
        }
        fftInPlace(cr, ci, inverse)
        for (y in 0 until h) {
            re[y * w + x] = cr[y]
            im[y * w + x] = ci[y]
        }
    }
}

/**
 * [fft2InPlace] of real values, as a new array of w x h complex numbers. [x] does not change. It
 * must hold w x h values, row by row.
 */
fun fft2(x: DoubleArray, w: Int, h: Int = w, inverse: Boolean = false): Array<Complex> {
    val re = x.copyOf()
    val im = DoubleArray(x.size)
    fft2InPlace(re, im, w, h, inverse)
    return complexOf(re, im)
}

/**
 * [fft2InPlace] of complex values, as a new array of w x h complex numbers. [x] does not change. It
 * must hold w x h values, row by row.
 */
fun fft2(x: Array<Complex>, w: Int, h: Int = w, inverse: Boolean = false): Array<Complex> {
    val re = DoubleArray(x.size) { x[it].real }
    val im = DoubleArray(x.size) { x[it].imag }
    fft2InPlace(re, im, w, h, inverse)
    return complexOf(re, im)
}

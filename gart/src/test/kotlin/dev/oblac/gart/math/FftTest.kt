package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FftTest {

    private val eps = 1e-9

    @Test
    fun anImpulseTransformsToAFlatSpectrum() {
        val re = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        val im = DoubleArray(8)
        fftInPlace(re, im)
        for (k in 0 until 8) {
            assertEquals(1.0, re[k], eps)
            assertEquals(0.0, im[k], eps)
        }
    }

    @Test
    fun forwardPutsAPositiveWaveInItsOwnBin() {
        // e^(+i 2pi 3n/16): forward is e^(-i..), so all of it lands in bin 3, not in bin 13
        val n = 16
        val re = DoubleArray(n) { cos(2 * PI * 3 * it / n) }
        val im = DoubleArray(n) { sin(2 * PI * 3 * it / n) }
        fftInPlace(re, im)
        for (k in 0 until n) {
            assertEquals(if (k == 3) 16.0 else 0.0, re[k], eps, "re[$k]")
            assertEquals(0.0, im[k], eps, "im[$k]")
        }
    }

    @Test
    fun inverseBuildsThePositiveWaveAndDoesNotScale() {
        val n = 16
        val re = DoubleArray(n)
        val im = DoubleArray(n)
        re[3] = 1.0
        fftInPlace(re, im, inverse = true)
        for (x in 0 until n) {
            assertEquals(cos(2 * PI * 3 * x / n), re[x], eps, "re[$x]")
            assertEquals(sin(2 * PI * 3 * x / n), im[x], eps, "im[$x]")
        }
    }

    @Test
    fun forwardThenInverseGivesTheInputTimesN() {
        val rnd = Random(7)
        val n = 32
        val re0 = DoubleArray(n) { rnd.nextDouble(-1.0, 1.0) }
        val im0 = DoubleArray(n) { rnd.nextDouble(-1.0, 1.0) }
        val re = re0.copyOf()
        val im = im0.copyOf()
        fftInPlace(re, im)
        fftInPlace(re, im, inverse = true)
        for (x in 0 until n) {
            assertEquals(re0[x] * n, re[x], eps)
            assertEquals(im0[x] * n, im[x], eps)
        }
    }

    @Test
    fun forwardMatchesTheDftSum() {
        val rnd = Random(3)
        val n = 64
        val re = DoubleArray(n) { rnd.nextDouble(-1.0, 1.0) }
        val im = DoubleArray(n) { rnd.nextDouble(-1.0, 1.0) }
        // the sum straight from the definition, X[k] = sum x[j] e^(-i 2pi jk/n)
        val wantRe = DoubleArray(n)
        val wantIm = DoubleArray(n)
        for (k in 0 until n) for (j in 0 until n) {
            val a = -2 * PI * j * k / n
            wantRe[k] += re[j] * cos(a) - im[j] * sin(a)
            wantIm[k] += re[j] * sin(a) + im[j] * cos(a)
        }
        fftInPlace(re, im)
        for (k in 0 until n) {
            assertEquals(wantRe[k], re[k], eps, "re[$k]")
            assertEquals(wantIm[k], im[k], eps, "im[$k]")
        }
    }

    @Test
    fun fft2FindsAWaveOnANonSquareGrid() {
        // 8 wide, 4 tall, row major. 2 turns along x and 1 along y land at (2, 1), index 1 * 8 + 2
        val w = 8
        val h = 4
        val re = DoubleArray(w * h)
        val im = DoubleArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val a = 2 * PI * (2.0 * x / w + 1.0 * y / h)
            re[y * w + x] = cos(a)
            im[y * w + x] = sin(a)
        }
        fft2InPlace(re, im, w, h)
        for (i in 0 until w * h) {
            assertEquals(if (i == 10) 32.0 else 0.0, re[i], eps, "re[$i]")
            assertEquals(0.0, im[i], eps, "im[$i]")
        }
    }

    @Test
    fun fft2InverseBuildsTheWaveBack() {
        val w = 4
        val h = 8
        val re = DoubleArray(w * h)
        val im = DoubleArray(w * h)
        re[3 * w + 1] = 1.0 // 1 turn along x, 3 along y
        fft2InPlace(re, im, w, h, inverse = true)
        for (y in 0 until h) for (x in 0 until w) {
            val a = 2 * PI * (1.0 * x / w + 3.0 * y / h)
            assertEquals(cos(a), re[y * w + x], eps)
            assertEquals(sin(a), im[y * w + x], eps)
        }
    }

    @Test
    fun rejectsALengthThatIsNotAPowerOfTwo() {
        assertFailsWith<IllegalArgumentException> { fftInPlace(DoubleArray(12), DoubleArray(12)) }
    }

    @Test
    fun rejectsPartsOfDifferentLength() {
        assertFailsWith<IllegalArgumentException> { fftInPlace(DoubleArray(8), DoubleArray(4)) }
    }

    @Test
    fun fft2RejectsAGridThatDoesNotMatchTheArrays() {
        assertFailsWith<IllegalArgumentException> { fft2InPlace(DoubleArray(32), DoubleArray(32), 8, 8) }
    }

    // [1, 2, 3, 4] by hand: 10, -2 + 2i, -2, -2 - 2i
    private val spectrum1234 = listOf(10.0 to 0.0, -2.0 to 2.0, -2.0 to 0.0, -2.0 to -2.0)

    private fun assertComplex(want: List<Pair<Double, Double>>, got: Array<Complex>) {
        assertEquals(want.size, got.size, "length")
        for (k in want.indices) {
            assertEquals(want[k].first, got[k].real, eps, "real[$k]")
            assertEquals(want[k].second, got[k].imag, eps, "imag[$k]")
        }
    }

    @Test
    fun aRealArrayGivesANewArrayOfComplexNumbers() {
        val x = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        assertComplex(spectrum1234, fft(x))
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), x)
    }

    @Test
    fun nPadsAShortInputWithZeros() {
        // [1, 1, 0, 0]: 2, 1 - i, 0, 1 + i
        assertComplex(listOf(2.0 to 0.0, 1.0 to -1.0, 0.0 to 0.0, 1.0 to 1.0), fft(doubleArrayOf(1.0, 1.0), 4))
    }

    @Test
    fun nCutsALongInput() {
        assertComplex(spectrum1234, fft(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0), 4))
    }

    @Test
    fun aRealArrayCanGoTheInverseWay() {
        // e^(+i ..) flips the sign of the imaginary parts for a real input
        assertComplex(listOf(10.0 to 0.0, -2.0 to -2.0, -2.0 to 0.0, -2.0 to 2.0), fft(doubleArrayOf(1.0, 2.0, 3.0, 4.0), inverse = true))
    }

    @Test
    fun aComplexArrayGoesBackToTheSignalTimesN() {
        val spectrum = spectrum1234.map { Complex(it.first, it.second) }.toTypedArray()
        assertComplex(listOf(4.0 to 0.0, 8.0 to 0.0, 12.0 to 0.0, 16.0 to 0.0), fft(spectrum, inverse = true))
    }

    @Test
    fun aComplexArrayIsPaddedToNToo() {
        // 1 + i alone is an impulse, flat at 1 + i everywhere
        assertComplex(List(4) { 1.0 to 1.0 }, fft(arrayOf(Complex(1.0, 1.0)), 4))
    }

    // 4 x 2, the top row 1 2 3 4, the bottom row 0. by hand: the rows give 1234's spectrum on top
    // and 0 below, then each 2-high column [a, 0] gives [a, a]
    private val grid = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0)
    private val gridSpectrum = spectrum1234 + spectrum1234

    @Test
    fun fft2OfARealGridGivesANewArrayOfComplexNumbers() {
        val x = grid.copyOf()
        assertComplex(gridSpectrum, fft2(x, 4, 2))
        assertContentEquals(grid, x)
    }

    @Test
    fun fft2OfAComplexGridGoesBackToTheGridTimesWH() {
        val spectrum = gridSpectrum.map { Complex(it.first, it.second) }.toTypedArray()
        assertComplex(grid.map { it * 8 to 0.0 }, fft2(spectrum, 4, 2, inverse = true))
    }

    @Test
    fun fft2OfComplexRejectsAGridThatDoesNotMatch() {
        assertFailsWith<IllegalArgumentException> { fft2(arrayOf(Complex(1.0, 0.0)), 2, 2) }
        assertFailsWith<IllegalArgumentException> { fft2(DoubleArray(6), 2, 3) }
    }

    @Test
    fun nMustBeAPowerOfTwo() {
        assertFailsWith<IllegalArgumentException> { fft(DoubleArray(4), 6) }
        assertFailsWith<IllegalArgumentException> { fft(DoubleArray(4), -4) }
        assertFailsWith<IllegalArgumentException> { fft(arrayOf(Complex(1.0, 0.0)), 3) }
    }

    @Test
    fun rejectsTheSameArrayAsBothParts() {
        // the butterflies read back what they just wrote: [1, 2] comes out as [5, -1]
        val a = doubleArrayOf(1.0, 2.0)
        assertFailsWith<IllegalArgumentException> { fftInPlace(a, a) }
    }

    @Test
    fun fft2RejectsTheSameArrayAsBothParts() {
        val a = DoubleArray(8)
        assertFailsWith<IllegalArgumentException> { fft2InPlace(a, a, 4, 2) }
    }

    @Test
    fun fft2RejectsAGridTooBigForAnInt() {
        // 65536 x 65536 wraps to 0 as an Int, so two empty arrays look like the right size
        assertFailsWith<IllegalArgumentException> { fft2InPlace(DoubleArray(0), DoubleArray(0), 65536, 65536) }
    }
}

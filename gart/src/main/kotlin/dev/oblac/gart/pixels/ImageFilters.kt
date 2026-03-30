package dev.oblac.gart.pixels

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Boundary handling modes for image filters.
 */
enum class PadMode {
    /** Mirror at boundary: [d c b a | a b c d | d c b a] */
    REFLECT,
    /** Wrap around (circular): [a b c d | a b c d | a b c d] */
    WRAP,
}

/**
 * Access pixel with boundary handling.
 */
private fun DoubleArray.getPixel(x: Int, y: Int, w: Int, h: Int, mode: PadMode): Double {
    val cx = clampIndex(x, w, mode)
    val cy = clampIndex(y, h, mode)
    return this[cy * w + cx]
}

private fun clampIndex(i: Int, size: Int, mode: PadMode): Int {
    if (i in 0 until size) return i
    return when (mode) {
        PadMode.REFLECT -> {
            var idx = i
            if (idx < 0) idx = -idx - 1
            if (idx >= size) idx = 2 * size - idx - 1
            idx.coerceIn(0, size - 1)
        }
        PadMode.WRAP -> {
            ((i % size) + size) % size
        }
    }
}

/**
 * 2D Gaussian blur using separable 1D passes.
 */
fun gaussianBlur(data: DoubleArray, w: Int, h: Int, sigma: Double, mode: PadMode = PadMode.REFLECT): DoubleArray {
    require(data.size == w * h)
    if (sigma <= 0) return data.copyOf()

    val radius = (3.0 * sigma).roundToInt().coerceAtLeast(1)
    val kernel = gaussianKernel1D(radius, sigma)

    // Horizontal pass
    val temp = DoubleArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0.0
            for (k in -radius..radius) {
                sum += data.getPixel(x + k, y, w, h, mode) * kernel[k + radius]
            }
            temp[y * w + x] = sum
        }
    }

    // Vertical pass
    val result = DoubleArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0.0
            for (k in -radius..radius) {
                sum += temp.getPixel(x, y + k, w, h, mode) * kernel[k + radius]
            }
            result[y * w + x] = sum
        }
    }
    return result
}

private fun gaussianKernel1D(radius: Int, sigma: Double): DoubleArray {
    val size = 2 * radius + 1
    val kernel = DoubleArray(size)
    val s2 = 2.0 * sigma * sigma
    var sum = 0.0
    for (i in 0 until size) {
        val x = (i - radius).toDouble()
        kernel[i] = exp(-(x * x) / s2)
        sum += kernel[i]
    }
    // Normalize
    for (i in 0 until size) kernel[i] /= sum
    return kernel
}

/**
 * Uniform (box/mean) filter with given window size.
 * @param origin shifts the filter window (like scipy's origin parameter)
 */
fun uniformFilter(data: DoubleArray, w: Int, h: Int, size: Int, mode: PadMode = PadMode.REFLECT, origin: Int = 0): DoubleArray {
    require(data.size == w * h)
    val result = DoubleArray(w * h)
    val radius = size / 2

    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0.0
            for (dy in -radius until -radius + size) {
                for (dx in -radius until -radius + size) {
                    sum += data.getPixel(x + dx + origin, y + dy + origin, w, h, mode)
                }
            }
            result[y * w + x] = sum / (size * size)
        }
    }
    return result
}

/**
 * Discrete Laplacian using the standard 3x3 kernel:
 * ```
 *  0  1  0
 *  1 -4  1
 *  0  1  0
 * ```
 */
fun laplacianFilter(data: DoubleArray, w: Int, h: Int): DoubleArray {
    require(data.size == w * h)
    val result = DoubleArray(w * h)

    for (y in 0 until h) {
        for (x in 0 until w) {
            val center = data.getPixel(x, y, w, h, PadMode.REFLECT)
            val top = data.getPixel(x, y - 1, w, h, PadMode.REFLECT)
            val bottom = data.getPixel(x, y + 1, w, h, PadMode.REFLECT)
            val left = data.getPixel(x - 1, y, w, h, PadMode.REFLECT)
            val right = data.getPixel(x + 1, y, w, h, PadMode.REFLECT)
            result[y * w + x] = top + bottom + left + right - 4.0 * center
        }
    }
    return result
}

/**
 * Sliding window maximum filter.
 */
fun maximumFilter(data: DoubleArray, w: Int, h: Int, size: Int): DoubleArray {
    require(data.size == w * h)
    val result = DoubleArray(w * h)
    val radius = size / 2

    for (y in 0 until h) {
        for (x in 0 until w) {
            var maxVal = Double.NEGATIVE_INFINITY
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val nx = (x + dx).coerceIn(0, w - 1)
                    maxVal = max(maxVal, data[ny * w + nx])
                }
            }
            result[y * w + x] = maxVal
        }
    }
    return result
}

/**
 * Sliding window minimum filter.
 */
fun minimumFilter(data: DoubleArray, w: Int, h: Int, size: Int): DoubleArray {
    require(data.size == w * h)
    val result = DoubleArray(w * h)
    val radius = size / 2

    for (y in 0 until h) {
        for (x in 0 until w) {
            var minVal = Double.POSITIVE_INFINITY
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val nx = (x + dx).coerceIn(0, w - 1)
                    minVal = min(minVal, data[ny * w + nx])
                }
            }
            result[y * w + x] = minVal
        }
    }
    return result
}

/**
 * Adaptive median filter (RAMF - Recursive Adaptive Median Filter).
 * Starts with [windowSize] and grows up to [maxWindowSize] if needed.
 */
fun adaptiveMedianFilter(data: DoubleArray, w: Int, h: Int, windowSize: Int, maxWindowSize: Int): DoubleArray {
    val result = data.copyOf()
    val padSize = windowSize / 2

    for (y in padSize until h - padSize) {
        for (x in padSize until w - padSize) {
            var currentSize = windowSize

            while (true) {
                val r = currentSize / 2
                val y0 = max(0, y - r)
                val y1 = min(h, y + r + 1)
                val x0 = max(0, x - r)
                val x1 = min(w, x + r + 1)

                val window = mutableListOf<Double>()
                for (wy in y0 until y1) {
                    for (wx in x0 until x1) {
                        window.add(data[wy * w + wx])
                    }
                }
                window.sort()

                val medianVal = window[window.size / 2]
                val maxVal = window.last()
                val minVal = window.first()

                if (minVal < medianVal && medianVal < maxVal) {
                    if (!(minVal < data[y * w + x] && data[y * w + x] < maxVal)) {
                        result[y * w + x] = medianVal
                    }
                    break
                } else {
                    currentSize += 2
                    if (currentSize > maxWindowSize) {
                        result[y * w + x] = medianVal
                        break
                    }
                }
            }
        }
    }
    return result
}

package dev.oblac.gart.math

import kotlin.math.roundToInt

// frame ratios, long side over short side
const val WIDE_SCREEN_RATIO = 16.0f / 9
const val ULTRA_WIDE_SCREEN_RATIO = 21.0f / 9
const val CINEMA_RATIO = 2.4f
const val GOLDEN_RATIO = 1.61803398875
const val GOLDEN_RATIOf = 1.618034f

// the other side of a frame from one of these. a ratio works either way round,
// 16/9 and 9/16 give the same frame

/**
 * The long side of a frame with this [short] side and [ratio], to the nearest pixel.
 * A golden portrait 1024 wide is `longSide(1024, GOLDEN_RATIO)` = 1657 tall.
 */
fun longSide(short: Int, ratio: Number): Int = (short * longOverShort(ratio)).roundToInt()

/**
 * The short side of a frame with this [long] side and [ratio], to the nearest pixel.
 * A 9:16 portrait 2000 tall is `shortSide(2000, WIDE_SCREEN_RATIO)` = 1125 wide.
 */
fun shortSide(long: Int, ratio: Number): Int = (long / longOverShort(ratio)).roundToInt()

private fun longOverShort(ratio: Number): Double {
    val r = ratio.toDouble()
    return if (r < 1.0) 1.0 / r else r
}

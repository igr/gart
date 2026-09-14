package dev.oblac.gart.math

const val WIDE_SCREEN_RATIO = 16.0f / 9
const val ULTRA_WIDE_SCREEN_RATIO = 21.0f / 9
const val CINEMA_RATIO = 2.4f
const val GOLDEN_RATIO = 1.61803398875
const val GOLDEN_RATIOf = 1.618034f
const val GOLDEN_TURN = 2 - GOLDEN_RATIO             // 1 - 1/phi = 1/phi², the golden angle as a share of a turn. the hardest rotation number to lock
const val GOLDEN_TURNf = 0.38196601f
const val GOLDEN_ANGLE = 2 * Math.PI * GOLDEN_TURN   // 137.5°, the small part of a turn cut in golden ratio. sunflower seeds sit this far apart
const val GOLDEN_ANGLEf = 2.3999631f
const val PIf = Math.PI.toFloat()
const val TAUf = 2 * PIf
const val TAU = 2 * Math.PI
const val DOUBLE_PIf = 2 * Math.PI.toFloat()
const val TWO_PIf = 2 * Math.PI.toFloat()
const val HALF_PIf = PIf / 2
const val QUARTER_PIf = PIf / 4
const val LN2 = 0.6931471805599453                  // ln 2: a falloff that halves every c px is exp(-LN2 * d / c)
const val LN2f = 0.6931472f

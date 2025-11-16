package dev.oblac.gart.noise

import kotlin.math.floor

fun cellnoise(x: Float): Float {
    var n = floor(x).toInt()
    n = (n shl 13) xor n
    n = n and 0xffffffff.toInt()
    val m = n
    n *= 15731
    n = n and 0xffffffff.toInt()
    n *= m
    n = n and 0xffffffff.toInt()
    n += 789221
    n = n and 0xffffffff.toInt()
    n *= m
    n = n and 0xffffffff.toInt()
    n += 1376312589
    n = n and 0xffffffff.toInt()
    n = (n shr 14) and 65535
    return n / 65535.0f
}

fun noise(x: Float): Float {
    val i = floor(x).toInt()
    val f = x - i
    val w = f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f)
    val a = (2.0f * cellnoise((i + 0).toFloat()) - 1.0f) * (f + 0.0f)
    val b = (2.0f * cellnoise((i + 1).toFloat()) - 1.0f) * (f - 1.0f)
    return 2.0f * (a + (b - a) * w)
}

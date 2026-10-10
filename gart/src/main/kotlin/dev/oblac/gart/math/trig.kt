package dev.oblac.gart.math

fun Float.toRadians(): Float = (this / 180 * Math.PI).toFloat()
fun Float.toDegrees(): Float = (this * 180 / Math.PI).toFloat()

/**
 * Safe degrees' subtraction.
 */
fun Float.subDeg(delta: Number) = normalizeDeg(this - delta.toFloat())

/**
 * Safe degrees' addition.
 */
fun Float.addDeg(delta: Number) = normalizeDeg(this + delta.toFloat())

fun sinDeg(degrees: Number) = kotlin.math.sin(degrees.toFloat().toRadians())
fun cosDeg(degrees: Number) = kotlin.math.cos(degrees.toFloat().toRadians())

/**
 * Wraps the angle [rad] into `0..2PI`, both ends kept. [wrapPi] wraps into `-PI..PI`.
 */
fun normalizeRad(rad: Float): Float = normalizeTurn(rad, DOUBLE_PIf)

/**
 * Wraps the angle [rad] into `-PI..PI`. Both ends stay: `PIf` gives `PIf`, `-PIf` gives `-PIf`.
 * [normalizeRad] wraps into `0..2PI` instead.
 *
 * Up to two turns, it adds or subtracts one turn at a time. This is the loop that the pieces write
 * by hand, so the bits are the same. Past two turns, it takes one remainder first. Then a big angle
 * does not loop for a long time, and an infinite angle gives NaN, not a hang.
 */
fun wrapPi(rad: Float): Float {
    var a = rad
    if (kotlin.math.abs(a) > 2 * TAUf) a %= TAUf
    while (a > PIf) a -= TAUf
    while (a < -PIf) a += TAUf
    return a
}

internal fun normalizeDeg(deg: Float): Float = normalizeTurn(deg, 360f)

/**
 * Wraps [v] into `0..turn`, both ends kept. Up to two turns, it adds or subtracts one turn at a time.
 * This is the old loop, so the old pieces keep their bits. Past two turns, it takes one remainder first.
 * Then a big value does not loop for a long time, and an infinite value gives NaN, not a hang.
 */
private fun normalizeTurn(v: Float, turn: Float): Float {
    var a = v
    if (kotlin.math.abs(a) > 2 * turn) a %= turn
    while (a < 0f) a += turn
    while (a > turn) a -= turn
    return a
}

/**
 * Degrees to radians in single precision, `deg * PIf / 180f` - the form the pieces write by hand.
 * [toRadians] goes through Double and rounds differently, so in a seeded piece the two are not
 * interchangeable; this one is bit-exact with the hand-written expression.
 */
fun degToRad(deg: Float): Float = deg * PIf / 180f

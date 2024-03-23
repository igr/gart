package dev.oblac.gart.math

import java.util.*
import kotlin.math.*


/**
 * Imaginary unit.
 */
val i = Complex(0.0, 1.0)

/**
 * Complex exponential.
 */
fun exp(c: Complex): Complex {
    val e = exp(c.real)
    return Complex(e * cos(c.img), e * sin(c.img))
}

/**
 * Hyperbolic sine.
 */
fun sinh(c: Complex) = (exp(c) - exp(-c)) / 2

/**
 * Hyperbolic cosine.
 */
fun cosh(c: Complex) = (exp(c) + exp(-c)) / 2

/**
 * Hyperbolic tangent.
 */
fun tanh(c: Complex) = sinh(c) / cosh(c)

/**
 * Hyperbolic cotangent.
 */
fun coth(c: Complex) = cosh(c) / sinh(c)

/**
 * Complex cosine.
 */
fun cos(c: Complex) = (exp(i * c) + exp(-i * c)) / 2.0

/**
 * Complex sine.
 */
fun sin(c: Complex) = i * (exp(-i * c) - exp(i * c)) / 2.0

/**
 * Complex tangent.
 */
fun tan(c: Complex) = sin(c) / cos(c)

/**
 * Complex cotangent.
 */
fun cot(c: Complex) = cos(c) / sin(c)

/**
 * Complex secant.
 */
fun sec(c: Complex) = Complex.ONE / cos(c)

/**
 * The natural logarithm on the principal branch.
 */
fun ln(c: Complex) = Complex(ln(c.abs()), c.phase())

/**
 * Roots of unity.
 */
fun roots(n: Int) =
    (1..n).map { exp(i * 2 * PI * it / n) }

operator fun Number.plus(c: Complex) = Complex(this.toDouble() + c.real, c.img)

operator fun Number.minus(c: Complex) = Complex(this.toDouble() - c.real, -c.img)

operator fun Number.times(c: Complex) = Complex(this.toDouble() * c.real, this.toDouble() * c.img)

operator fun Number.div(c: Complex) = Complex.ONE / c

/**
 * Defines complex numbers and their algebraic operations.
 * @param real the real component
 * @param img the imaginary component
 */
class Complex(val real: Double, val img: Double) {

    constructor(real: Number, img: Number) : this(real.toDouble(), img.toDouble())

    override fun equals(other: Any?): Boolean {
        return (other is Complex && real == other.real && img == other.img)
    }

    override fun hashCode(): Int {
        return Objects.hash(real, img)
    }

    operator fun unaryMinus() = Complex(-real, -img)

    operator fun plus(c: Complex) = Complex(real + c.real, img + c.img)

    operator fun plus(n: Number) = Complex(real + n.toDouble(), img)

    operator fun minus(c: Complex) = Complex(real - c.real, img - c.img)

    operator fun minus(n: Number) = Complex(real - n.toDouble(), img)

    operator fun times(c: Complex) = Complex(real * c.real - img * c.img, real * c.img + img * c.real)

    operator fun times(n: Number) = Complex(n.toDouble() * real, n.toDouble() * img)

    operator fun div(n: Number) = Complex(real / n.toDouble(), img / n.toDouble())

    operator fun div(c: Complex): Complex {
        val den = c.normSquared()
        if (isPracticallyZero(den)) {
            return this / 0 // todo make this consistent with division by zero number
        }
        val num = this * c.conjugate()
        return num / den
    }

    operator fun component1() = real
    operator fun component2() = img

    /**
     * Complex conjugate = x-y*i.
     */
    fun conjugate() = Complex(real, -img)

    fun normSquared() = real * real + img * img

    fun abs(): Double = sqrt(this.normSquared())

    fun phase(): Double = atan(img / real)

    fun pow(a: Double) = exp(ln(this) * a)

    fun pow(a: Number) = exp(ln(this) * a)

    fun pow(a: Complex) = exp(ln(this) * a)

    override fun toString(): String {
        return when {
            isPracticallyZero(img) -> "$real"
            isPracticallyZero(real) -> "${img}i"
            img < 0 -> "$real-${-img}i"
            else -> "${real}+${img}i"
        }
    }

    private fun isPracticallyZero(d: Double) = abs(d) < DEFAULT_TOLERANCE

    companion object {
        /**
         * Complex 0 = 0 + 0i
         */
        val ZERO = Complex(0.0, 0.0)

        /**
         * Complex 1 = 1 + 0i
         */
        val ONE = Complex(1.0, 0.0)

        const val DEFAULT_TOLERANCE = 1.0E-15

        fun fromNumber(n: Number) = Complex(n.toDouble(), 0.0)

        fun fromPolar(radius: Double, theta: Double): Complex = radius * exp(i * theta)

    }

    /**
     * Tests if the norm of the complex number is smaller than the given tolerance
     */
    fun isZero(tolerance: Double) = this.abs() < tolerance

    infix fun to(exponent: Int): Complex {
        if (exponent == 0) {
            return ONE
        }
        if (exponent == 1) {
            return this
        }
        val half = to(exponent / 2)
        return if (exponent.isEven()) {
            half * half
        } else {
            half * half * this
        }
    }

    infix fun to(exponent: Complex) = this.pow(exponent)

    infix fun to(exponent: Number) = this.pow(exponent)
}

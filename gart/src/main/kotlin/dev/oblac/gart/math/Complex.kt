package dev.oblac.gart.math

import dev.oblac.gart.math.Complex.Companion.ONE
import dev.oblac.gart.math.Complex.Companion.ZERO
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
    return Complex(e * cos(c.imag), e * sin(c.imag))
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
 * The natural logarithm.
 */
fun ln(c: Complex) = Complex(ln(c.abs()), c.phase())

/**
 * Square Root.
 */
fun sqrt(z: Complex): Complex {
    return when (z) {
        ZERO -> ZERO
        else -> {
            val t: Double = sqrt((abs(z.real) + z.mod()) / 2)
            if (z.real >= 0) {
                Complex(t, z.imag / (2 * t))
            } else {
                Complex(abs(z.imag) / (2 * t), 1.0.withSign(z.imag) * t)
            }
        }
    }
}

/**
 * Inverse hyperbolic sine is defined as:
 * ln(z + sqrt(z^2 + 1))
 */
fun arcsinh(z: Complex): Complex {
    val inner = z + sqrt((z * z + ONE))
    return ln(inner)
}


/**
 * Roots of unity.
 */
fun roots(n: Int) =
    (1..n).map { exp(i * 2 * PI * it / n) }

operator fun Number.plus(c: Complex) = Complex(this.toDouble() + c.real, c.imag)

operator fun Number.minus(c: Complex) = Complex(this.toDouble() - c.real, -c.imag)

operator fun Number.times(c: Complex) = Complex(this.toDouble() * c.real, this.toDouble() * c.imag)

operator fun Number.div(c: Complex) = Complex.ONE / c

/**
 * Defines complex numbers and their algebraic operations.
 *
 * @param real the real component
 * @param imag the imaginary component
 */
class Complex(val real: Double, val imag: Double) {

    constructor(real: Number, img: Number) : this(real.toDouble(), img.toDouble())

    override fun equals(other: Any?): Boolean {
        return (other is Complex && real == other.real && imag == other.imag)
    }

    override fun hashCode(): Int {
        return Objects.hash(real, imag)
    }

    operator fun unaryMinus() = Complex(-real, -imag)

    operator fun plus(c: Complex) = Complex(real + c.real, imag + c.imag)

    operator fun plus(n: Number) = Complex(real + n.toDouble(), imag)

    operator fun minus(c: Complex) = Complex(real - c.real, imag - c.imag)

    operator fun minus(n: Number) = Complex(real - n.toDouble(), imag)

    operator fun times(c: Complex) = Complex(real * c.real - imag * c.imag, real * c.imag + imag * c.real)

    operator fun times(n: Number) = Complex(n.toDouble() * real, n.toDouble() * imag)

    operator fun div(n: Number) = Complex(real / n.toDouble(), imag / n.toDouble())

    operator fun div(c: Complex): Complex {
        val den = c.normSquared()
        if (isPracticallyZero(den)) {
            return this / 0 // todo make this consistent with division by zero number
        }
        val num = this * c.conjugate()
        return num / den
    }

    operator fun component1() = real
    operator fun component2() = imag

    /**
     * Complex conjugate = x-y*i.
     */
    fun conjugate() = Complex(real, -imag)

    fun normSquared() = real * real + imag * imag

    fun norm() = sqrt(real * real + imag * imag)
    fun mod() = norm()

    fun abs(): Double = sqrt(this.normSquared())

    fun phase(): Double = atan2(imag, real)

    fun pow(a: Double) = exp(ln(this) * a)

    fun pow(a: Number) = exp(ln(this) * a)

    fun pow(a: Complex) = exp(ln(this) * a)


    override fun toString(): String {
        return when {
            isPracticallyZero(imag) -> "$real"
            isPracticallyZero(real) -> "${imag}i"
            imag < 0 -> "$real-${-imag}i"
            else -> "${real}+${imag}i"
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

        /**
         * Complex 1i = 0 + 1i
         */
        val i = Complex(0.0, 1.0)

        const val DEFAULT_TOLERANCE = 1.0E-15

        fun fromNumber(n: Number) = Complex(n.toDouble(), 0.0)

        fun fromPolar(radius: Double, theta: Double): Complex = radius * exp(i * theta)

        fun real(n: Number) = Complex(n.toDouble(), 0.0)
        fun imag(n: Number) = Complex(0.0, n.toDouble())

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

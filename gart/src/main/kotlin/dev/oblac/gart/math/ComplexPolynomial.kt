package dev.oblac.gart.math

val Z = ComplexPolynomial.of(0.0, 1.0)

operator fun Number.times(cp: ComplexPolynomial) = cp * this.toDouble()

operator fun Complex.times(cp: ComplexPolynomial) = cp * this

operator fun Number.plus(cp: ComplexPolynomial) = cp + this.toDouble()

operator fun Complex.plus(cp: ComplexPolynomial) = cp + this

/**
 * a complex polynomial of the form c[0] + c[1]z + c[2]z^2 + ...
 * where the c's are complex numbers.
 */
class ComplexPolynomial(vararg coefficients: Complex) {

    private val tolerance: Double = 0.00001

    private val coefficients = create(arrayOf(*coefficients))

    // discard leading zero coefficients from the right
    private fun create(coeff: Array<Complex>): Array<Complex> {
        var n = coeff.size
        while ((n > 1) && coeff[n - 1].isZero(tolerance)) --n
        return coeff.sliceArray(IntRange(0, n - 1))
    }

    constructor(cp: ComplexPolynomial) : this(*cp.coefficients)

    companion object {
        val ZERO = ComplexPolynomial(Complex.ZERO)

        /**
         * Create the constant polynomial.
         */
        fun constant(c: Complex) = ComplexPolynomial(c)

        /**
         * Create the constant polynomial.
         */
        fun constant(n: Number) = ComplexPolynomial(Complex.fromNumber(n))

        /**
         * Create the monomial: coefficient * x^degree
         * @param degree the degree of the monomial
         * @param coefficient the multiplier of the monomial
         */
        fun monomial(degree: Int, coefficient: Complex): ComplexPolynomial {
            val a = Array(degree + 1) { Complex.ZERO }
            a[degree] = coefficient
            return ComplexPolynomial(*a)
        }

        fun monomial(degree: Int, number: Number) = monomial(degree, Complex(number, 0))

        /**
         * Create coefficients complex polynomial with real coefficients.
         * @param coefficients the polynomial coefficients
         */

        fun of(vararg coefficients: Double) = ComplexPolynomial(*(coefficients.map { Complex.fromNumber(it) }.toTypedArray()))
    }

    override fun equals(other: Any?): Boolean {
        return (other is ComplexPolynomial && coefficients contentEquals other.coefficients)
    }

    override fun hashCode(): Int {
        return coefficients.contentHashCode()
    }

    /**
     * A polynomial can be applied as a function
     */
    operator fun invoke(z: Complex): Complex {
        var powx = z
        var v = coefficients[0]
        for (n in 1 until coefficients.size) {
            v += coefficients[n] * powx
            powx *= z
        }
        return v
    }

    operator fun invoke(n: Number) = invoke(Complex.fromNumber(n))

    val degree get() = coefficients.size - 1

    /**
     * Access the ith coefficient
     */
    operator fun get(i: Int) = coefficients[i]

    override fun toString(): String {

        if (this.isZero()) return "0"

        fun coefficientToString(i: Int): String {
            if (coefficients[i].isZero(0.00001)) {
                return ""
            }
            val s = "(${coefficients[i]})"
            return when (i) {
                0 -> s
                1 -> s + "z"
                else -> s + "z^" + i.toString()
            }
        }

        return (coefficients.indices).map { coefficientToString(it) }.filter { !it.isEmpty() }.joinToString(separator = "+")
    }

    operator fun unaryMinus() = ComplexPolynomial(*coefficients.map { c -> -c }.toTypedArray())

    operator fun times(n: Number) = ComplexPolynomial(*coefficients.map { c -> c * n }.toTypedArray())

    operator fun times(z: Complex) = ComplexPolynomial(*coefficients.map { c -> c * z }.toTypedArray())

    operator fun div(z: Complex) = ComplexPolynomial(*coefficients.map { c -> c / z }.toTypedArray())

    operator fun div(n: Number) = ComplexPolynomial(*coefficients.map { c -> c / n }.toTypedArray())

    operator fun plus(n: Number): ComplexPolynomial {
        val coeff = this.coefficients.copyOf()
        coeff[0] = coeff[0] + n
        return ComplexPolynomial(*coeff)
    }

    operator fun plus(c: Complex): ComplexPolynomial {
        val coeff = this.coefficients.copyOf()
        coeff[0] = coeff[0] + c
        return ComplexPolynomial(*coeff)
    }

    operator fun minus(n: Number): ComplexPolynomial {
        val coeff = this.coefficients.copyOf()
        coeff[0] = coeff[0] - n
        return ComplexPolynomial(*coeff)
    }

    operator fun minus(c: Complex) = this.plus(-c)

    /**
     * Add two polynomials
     */
    operator fun plus(other: ComplexPolynomial): ComplexPolynomial {

        fun addCoefficient(i: Int): Complex {
            return when {
                i > this.degree -> other.coefficients[i]
                i > other.degree -> coefficients[i]
                else -> coefficients[i] + other.coefficients[i]
            }
        }

        val maxOrder = maxOf(degree, other.degree)
        val coeff = Array(maxOrder + 1) { addCoefficient(it) }
        return ComplexPolynomial(*coeff)
    }

    /**
     * Subtract two polynomials
     */
    operator fun minus(other: ComplexPolynomial): ComplexPolynomial {

        fun subtractCoefficient(i: Int): Complex {
            return when {
                i > degree -> -other.coefficients[i]
                i > other.degree -> coefficients[i]
                else -> coefficients[i] - other.coefficients[i]
            }
        }

        val maxOrder = maxOf(degree, other.degree)
        val coeff = Array(maxOrder + 1, { subtractCoefficient(it) })
        return ComplexPolynomial(*coeff)
    }

    /**
     * Multiply two polynomials
     */
    operator fun times(other: ComplexPolynomial): ComplexPolynomial {
        val resultOrder = degree + other.degree

        val coeff = Array(resultOrder + 1) { Complex.fromNumber(0) }
        for (k in coefficients.indices) {
            for (j in 0 until other.coefficients.size) {
                coeff[k + j] += coefficients[k] * other.coefficients[j]
            }
        }
        return ComplexPolynomial(*coeff)
    }

    operator fun div(other: ComplexPolynomial): Pair<ComplexPolynomial, ComplexPolynomial> {
        return divide(this, other)
    }

    fun derivative(): ComplexPolynomial {
        val d = (1..degree).map { coefficients[it] * it }.toTypedArray()
        return ComplexPolynomial(*d)
    }

    private fun isZero() = degree == 0 && coefficients[0].isZero(tolerance)

    infix fun to(exponent: Int): ComplexPolynomial {
        if (exponent == 0) {
            return ZERO
        }
        if (exponent == 1) {
            return this
        }
        if (this.isMonomial()) {
            val deg = this.degree * exponent
            return monomial(deg, coefficients[degree])
        }
        val half = to(exponent / 2)
        return if (exponent.isEven()) {
            half * half
        } else {
            half * half * this
        }
    }

    fun isMonomial() = (0 until degree).map { coefficients[it] }.all { it.isZero(tolerance) }

}


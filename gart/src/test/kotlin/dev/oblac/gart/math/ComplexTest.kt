package dev.oblac.gart.math

import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ComplexTest {

    @Test
    fun complexOfPairsTheTwoArrays() {
        val z = complexOf(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, -4.0))
        assertContentEquals(arrayOf(Complex(1.0, 3.0), Complex(2.0, -4.0)), z)
    }

    @Test
    fun complexOfRejectsArraysOfDifferentLength() {
        assertFailsWith<IllegalArgumentException> { complexOf(DoubleArray(2), DoubleArray(3)) }
    }
}

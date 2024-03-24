package dev.oblac.gart.math

import dev.oblac.gart.Dimension

class ComplexField(
    val xFrom: Float,
    val xTo: Float,
    val yFrom: Float,
    val yTo: Float,
    val stepsX: Int,
    val stepsY: Int,
    val supplier: (x: Float, y: Float) -> Complex
) {

    private val xStep = (xTo - xFrom) / stepsX
    private val yStep = (yTo - yFrom) / stepsY

    private val data = Array(stepsX) { x ->
        Array(stepsY) { y ->
            supplier(xFrom + x * xStep, yFrom + y * yStep)
        }
    }

    operator fun get(x: Int, y: Int): Complex = data[x][y]

    companion object {
        fun of(d: Dimension, supplier: (x: Float, y: Float) -> Complex): ComplexField {
            return ComplexField(-1f, 1f, -1f, 1f, d.w, d.h, supplier)
        }
    }
}

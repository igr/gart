package dev.oblac.gart.math

class ComplexField(
    val xFrom: Float,
    val xTo: Float,
    val yFrom: Float,
    val yTo: Float,
    val width: Int,
    val height: Int
) {
    private val xStep = (xTo - xFrom) / width
    private val yStep = (yTo - yFrom) / height

    private val data = Array(width) { x ->
        Array(height) { y ->
            Complex(xFrom + x * xStep, yFrom + y * yStep)
        }
    }
}

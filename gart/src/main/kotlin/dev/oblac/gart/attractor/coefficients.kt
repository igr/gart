package dev.oblac.gart.attractor

fun buildCoefficients(input: String): Array<Float> {
    return Array(input.length) {
        (input[it].code - 77) / 10.0f
    }
}

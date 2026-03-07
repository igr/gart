package dev.oblac.gart.color

import dev.oblac.gart.math.i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaletteGeneratorTest {

    @Test
    fun testSequentialPalette() {
        val inputColors = listOf(0xFF00429D.i(), 0xFF96FFEA.i(), 0xFFFFFFE0.i())
        val palette = PaletteGenerator.sequential(inputColors, numColors = 9, bezier = false, correctLightness = false)
        assertEquals(9, palette.colors.size)

        // reference: linear RGB interpolation
        val reference = listOf(
            "#00429d", "#2671b0", "#4ba1c4", "#71d0d7", "#96ffea", "#b0ffe8", "#cbffe5", "#e5ffe3", "#ffffe0"
        ).map { it.parseColor() }

        for (i in reference.indices) {
            assertEquals(reference[i], palette[i], "Color at index $i does not match reference")
        }
    }

    @Test
    fun testSequentialPalette_bezier() {
        val inputColors = listOf(0xFF00429D.i(), 0xFF96FFEA.i(), 0xFFFFFFE0.i())
        val palette = PaletteGenerator.sequential(inputColors, numColors = 9, bezier = true, correctLightness = false)
        assertEquals(9, palette.colors.size)

        // reference: linear RGB interpolation
        val reference = listOf(
            "#00429d", "#416ab0", "#6290bf", "#80b0cc", "#9bccd5", "#b6e2dc", "#cff2e0", "#e8fce1", "#ffffe0"
        ).map { it.parseColor() }

        for (i in reference.indices) {
            assertEquals(reference[i], palette[i], "Color at index $i does not match reference")
        }
    }

    @Test
    fun testSequentialPalette_bezier_lightness() {
        val inputColors = listOf(0xFF00429D.i(), 0xFF96FFEA.i(), 0xFFFFFFE0.i())
        val palette = PaletteGenerator.sequential(inputColors, numColors = 9, bezier = true, correctLightness = true)
        assertEquals(9, palette.colors.size)

        // reference: linear RGB interpolation
        val reference = listOf(
            "#00429d", "#2e59a8", "#4771b2", "#5d8abd", "#73a2c6", "#8abccf", "#a5d5d8", "#c5eddf", "#ffffe0"
        ).map { it.parseColor() }

        for (i in reference.indices) {
            assertEquals(reference[i], palette[i], "Color at index $i does not match reference")
        }
    }

    @Test
    fun testSequentialPalette_lightness() {
        val inputColors = listOf(0xFF00429D.i(), 0xFF96FFEA.i(), 0xFFFFFFE0.i())
        val palette = PaletteGenerator.sequential(inputColors, numColors = 9, bezier = false, correctLightness = true)
        assertEquals(9, palette.colors.size)

        // reference: linear RGB interpolation
        val reference = listOf(
            "#00429d", "#145ca8", "#2975b2", "#3d8ebc", "#51a8c6", "#65c2d1", "#7adcdc", "#8ff6e6", "#ffffe0"
        ).map { it.parseColor() }

        for (i in reference.indices) {
            assertEquals(reference[i], palette[i], "Color at index $i does not match reference")
        }
    }
}

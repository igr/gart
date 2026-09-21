package dev.oblac.gart.gfx

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PolygonTest {

    private val rect = listOf(Point(0f, 0f), Point(4f, 0f), Point(4f, 3f), Point(0f, 3f))

    @Test
    fun clockwiseOnScreenIsPositive() {
        assertEquals(12f, signedArea(rect))
    }

    @Test
    fun reversedWindingFlipsTheSign() {
        assertEquals(-12f, signedArea(rect.reversed()))
    }

    @Test
    fun fewerThanThreePointsHaveNoArea() {
        assertEquals(0f, signedArea(emptyList()))
        assertEquals(0f, signedArea(listOf(Point(1f, 2f))))
        assertEquals(0f, signedArea(listOf(Point(1f, 2f), Point(5f, 7f))))
    }
}

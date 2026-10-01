package dev.oblac.gart.pixels

import dev.oblac.gart.Dimension
import dev.oblac.gart.MemPixels
import org.jetbrains.skia.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ShadeTest {

    @Test
    fun oneSampleIsThePixelCenter() {
        var seen = Pair(0f, 0f)
        val c = shadeBlock(3, 7, 1) { x, y ->
            seen = Pair(x, y)
            Color.RED
        }
        assertEquals(Color.RED, c)
        assertEquals(Pair(3.5f, 7.5f), seen)
    }

    @Test
    fun anEdgeThroughTheMiddleAveragesToHalf() {
        // white left of x = 0.5, black right of it: at aa 2 two samples land on each side
        val c = shadeBlock(0, 0, 2) { x, _ -> if (x < 0.5f) Color.WHITE else Color.BLACK }
        assertEquals(Color.makeARGB(255, 127, 127, 127), c)
    }

    @Test
    fun alphaIsAveragedToo() {
        val c = shadeBlock(0, 0, 2) { _, y -> if (y < 0.5f) 0x00000000 else Color.WHITE }
        assertEquals(Color.makeARGB(127, 127, 127, 127), c)
    }

    @Test
    fun theResultDoesNotDependOnTheWorkers() {
        val d = Dimension(37, 23)
        val one = MemPixels(d)
        val many = MemPixels(d)
        val ring = { x: Float, y: Float -> if ((x - 18f) * (x - 18f) + (y - 11f) * (y - 11f) < 64f) Color.WHITE else Color.BLACK }
        one.shade(3, workers = 1, colorAt = ring)
        many.shade(3, workers = 5, colorAt = ring)
        assertContentEquals(one.pixels, many.pixels)
        assertEquals(Color.WHITE, one[18, 11])
        assertEquals(Color.BLACK, one[0, 0])
    }
}

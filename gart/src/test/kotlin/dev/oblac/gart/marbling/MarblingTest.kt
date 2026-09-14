package dev.oblac.gart.marbling

import dev.oblac.gart.Dimension
import dev.oblac.gart.MemPixels
import dev.oblac.gart.angle.Degrees
import org.jetbrains.skia.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MarblingTest {

    private val red = 0xFFCC2222.toInt()
    private val blue = 0xFF2244CC.toInt()

    @Test
    fun aDropIsItsColourInsideAndBathOutside() {
        val m = Marbling(Color.WHITE).drop(100f, 100f, 30f, red)
        assertEquals(red, m.colorAt(100f, 100f))
        assertEquals(red, m.colorAt(125f, 100f))
        assertEquals(Color.WHITE, m.colorAt(135f, 100f))
        assertEquals(Color.WHITE, m.colorAt(5f, 5f))
    }

    @Test
    fun concentricDropsGiveTheTextbookRadii() {
        // r 50 then r 30 at the same centre: the second sits in the middle, the first is pushed
        // out to a ring. 40 px out is still first paint (it came from 26), 60 px out is bath (52)
        val m = Marbling(Color.WHITE).drop(100f, 100f, 50f, red).drop(100f, 100f, 30f, blue)
        assertEquals(blue, m.colorAt(100f, 100f))
        assertEquals(blue, m.colorAt(129f, 100f))
        assertEquals(red, m.colorAt(140f, 100f))
        assertEquals(red, m.colorAt(100f, 157f))
        assertEquals(Color.WHITE, m.colorAt(160f, 100f))
    }

    @Test
    fun aTineDragsTheDropAlong() {
        val m = Marbling(Color.WHITE).drop(100f, 100f, 20f, red).tine(0f, 100f, Degrees(0f), 200f, 10f)
        assertEquals(red, m.colorAt(300f, 100f)) // the centre line went 200 px right
        assertEquals(Color.WHITE, m.colorAt(100f, 100f))
        val far = m.map(100f, 100f)
        assertEquals(300f, far.x, 1e-3f)
        val back = m.unmap(far.x, far.y)
        assertEquals(100f, back.x, 1e-2f)
        assertEquals(100f, back.y, 1e-2f)
    }

    @Test
    fun strokeDragsTheWholeWay() {
        val m = Marbling(Color.WHITE).stroke(org.jetbrains.skia.Point(10f, 10f), org.jetbrains.skia.Point(70f, 90f), 5f)
        val p = m.map(10f, 10f)
        assertEquals(70f, p.x, 1e-3f)
        assertEquals(90f, p.y, 1e-3f)
    }

    @Test
    fun renderIsIndependentOfWorkersAndAaBlendsTheEdge() {
        val m = Marbling(Color.WHITE)
        m.drop(40f, 40f, 20f, red).drop(60f, 45f, 12f, blue).comb(0f, 40f, Degrees(0f), 30f, 6f, 5, 10f)
        val one = MemPixels(Dimension(80, 80))
        val four = MemPixels(Dimension(80, 80))
        m.render(one, workers = 1)
        m.render(four, workers = 4)
        assertContentEquals(one.pixels, four.pixels)
        assertEquals(m.colorAt(40.5f, 40.5f), one[40, 40])
        assertEquals(m.colorAt(70.5f, 10.5f), one[70, 10])
        assertTrue(one.pixels.count { it == red } > 300, "the red drop is gone")

        val aa = MemPixels(Dimension(80, 80))
        m.render(aa, aa = 3)
        val blends = aa.pixels.count { it != red && it != blue && it != Color.WHITE }
        assertTrue(blends > 20, "expected blended edge pixels, got $blends")
    }

    @Test
    fun imageInkReproducesThePictureWhenNothingHappens() {
        val src = MemPixels(Dimension(16, 16))
        for (i in src.pixels.indices) src.pixels[i] = (0xFF shl 24) or (i * 7919 and 0xFFFFFF)
        val out = MemPixels(Dimension(16, 16))
        Marbling(Ink.image(src)).render(out)
        assertContentEquals(src.pixels, out.pixels)
    }

    @Test
    fun imageInkCanBeRenderedBackIntoItsOwnBuffer() {
        // the picture is copied when the ink is made, so writing the result over the source is
        // fine: a one px shift right must give A A B C, not a smear of A
        val p = MemPixels(Dimension(4, 1))
        val a = 0xFF101010.toInt()
        val b = 0xFF202020.toInt()
        val c = 0xFF303030.toInt()
        val d = 0xFF404040.toInt()
        p.pixels[0] = a
        p.pixels[1] = b
        p.pixels[2] = c
        p.pixels[3] = d
        Marbling(Ink.image(p)).shift(1f, 0f).render(p, workers = 1)
        assertContentEquals(intArrayOf(a, a, b, c), p.pixels)
    }

    @Test
    fun imageInkIsWarpedByTheOps() {
        val src = MemPixels(Dimension(40, 40))
        src.fill(Color.WHITE)
        for (y in 0 until 40) src[20, y] = Color.BLACK // one vertical line
        val out = MemPixels(Dimension(40, 40))
        // the tine runs through the pixel centres of row 20, so that row is pulled exactly 10 px
        Marbling(Ink.image(src)).tine(0f, 20.5f, Degrees(0f), 10f, 2f).render(out)
        assertNotEquals(Color.BLACK, out[20, 20])
        assertEquals(Color.BLACK, out[30, 20]) // the middle of the line went 10 px right
        assertTrue((out[20, 0] ushr 16 and 0xFF) < 8, "far from the tine the line should have stayed put")
    }
}

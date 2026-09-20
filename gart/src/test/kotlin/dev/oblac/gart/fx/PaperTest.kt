package dev.oblac.gart.fx

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import org.jetbrains.skia.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PaperTest {

    @Test
    fun oneSeedIsOneSheet() {
        val a = PaperSurface(7)
        val b = PaperSurface(7)
        for (y in 0 until 16) for (x in 0 until 16) assertEquals(a.heightAt(x, y), b.heightAt(x, y), 0f)
    }

    @Test
    fun anotherSeedIsAnotherSheet() {
        val a = PaperSurface(7)
        val b = PaperSurface(8)
        val same = (0 until 64).count { a.heightAt(it, it) == b.heightAt(it, it) }
        assertTrue(same < 8, "two seeds gave the same surface at $same of 64 pixels")
    }

    @Test
    fun toothAloneIsTheToothField() {
        val paper = Paper(felt = null, fibre = 0f, tooth = 1f)
        val s = PaperSurface(3, paper)
        for (y in 0 until 16) for (x in 0 until 16) assertEquals(s.toothAt(x, y), s.heightAt(x, y), 0f)
    }

    @Test
    fun theToothIsCentredOnTheSheet() {
        val s = PaperSurface(11)
        var sum = 0f
        for (y in 0 until 64) for (x in 0 until 64) {
            val t = s.toothAt(x, y)
            assertTrue(t >= -0.5f && t < 0.5f, "tooth out of range: $t")
            sum += t
        }
        assertEquals(0f, sum / (64 * 64), 0.02f)
    }

    @Test
    fun feltMovesTheSurface() {
        val withFelt = PaperSurface(5)
        val without = PaperSurface(5, Paper(felt = null))
        val moved = (0 until 64).count { withFelt.heightAt(it, 2 * it) != without.heightAt(it, 2 * it) }
        assertTrue(moved > 56, "felt only moved $moved of 64 pixels")
    }

    @Test
    fun aPassedToothIsTheOneUsed() {
        val s = PaperSurface(5)
        val plain = s.heightAt(4, 9)
        assertEquals(plain, s.heightAt(4, 9, s.toothAt(4, 9)), 0f)
        assertNotEquals(plain, s.heightAt(4, 9, 0.5f))
    }

    @Test
    fun printingMovesEveryPixelAndLeavesThemOpaque() {
        val grey = Color.makeRGB(128, 128, 128)
        val g = Gartvas(Dimension(32, 32))
        g.canvas.clear(grey)
        printOnPaper(g, PaperSurface(7), Color.WHITE, Color.BLACK, 1f)
        Gartmap(g).use { m ->
            var moved = 0
            for (i in m.pixels.indices) {
                assertEquals(255, m.pixels[i] ushr 24 and 0xFF, "pixel $i lost its alpha")
                if (m.pixels[i] != grey) moved++
            }
            assertTrue(moved > 1000, "only $moved of 1024 pixels took any ink")
        }
    }
}

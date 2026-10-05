package dev.oblac.gart.color.space

import org.jetbrains.skia.Color4f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MutableColor4fTest {

    @Test
    fun itStartsAsOpaqueBlack() {
        val c = MutableColor4f()
        assertEquals(listOf(0f, 0f, 0f, 1f), listOf(c.r, c.g, c.b, c.a))
    }

    @Test
    fun theIndexIsTheChannel() {
        val c = MutableColor4f(0.1f, 0.2f, 0.3f, 0.4f)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f), (0..3).map { c[it] })
        c[0] = 0.5f
        c[3] = 0.6f
        assertEquals(0.5f, c.r)
        assertEquals(0.6f, c.a)
    }

    @Test
    fun anIndexPastAlphaFails() {
        val c = MutableColor4f()
        assertFailsWith<IndexOutOfBoundsException> { c[4] }
        assertFailsWith<IndexOutOfBoundsException> { c[-1] = 1f }
    }

    @Test
    fun setReturnsTheSameInstance() {
        val c = MutableColor4f()
        assertSame(c, c.set(0.1f, 0.2f, 0.3f))
        assertSame(c, c.set(Color4f(0.4f, 0.5f, 0.6f, 0.7f)))
        assertEquals(listOf(0.4f, 0.5f, 0.6f, 0.7f), listOf(c.r, c.g, c.b, c.a))
    }

    @Test
    fun setWithThreeChannelsKeepsTheAlpha() {
        val c = MutableColor4f(a = 0.25f)
        c.set(0.1f, 0.2f, 0.3f)
        assertEquals(0.25f, c.a)
    }

    @Test
    fun zeroClearsEveryChannelAlphaToo() {
        val c = MutableColor4f(0.1f, 0.2f, 0.3f, 0.4f).zero()
        assertEquals(listOf(0f, 0f, 0f, 0f), listOf(c.r, c.g, c.b, c.a))
    }

    @Test
    fun theChannelsAreNotClamped() {
        val c = MutableColor4f(7f, -2f, 0f)
        assertEquals(7f, c[0])
        assertEquals(-2f, c[1])
    }

    @Test
    fun toColor4fFreezesTheCurrentValue() {
        val c = MutableColor4f(0.1f, 0.2f, 0.3f, 0.4f)
        val frozen = c.toColor4f()
        c.zero()
        assertEquals(Color4f(0.1f, 0.2f, 0.3f, 0.4f), frozen)
    }
}

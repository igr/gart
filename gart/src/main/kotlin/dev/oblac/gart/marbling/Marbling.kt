package dev.oblac.gart.marbling

import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Pixels
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.util.defaultWorkers
import dev.oblac.gart.util.parallelBands
import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Color
import org.jetbrains.skia.Point

/**
 * A marbling bath as the list of everything done to it, in order: drops of paint, tines and
 * combs dragged through, the sheet wiggled as it is laid on. Nothing is simulated - every op is a
 * closed-form bijection of the plane, so the colour at any point is found by walking the history
 * backwards until the point lands inside a drop ([colorAt]), and every pixel is independent
 * ([render] is exact at any size and splits across cores). [contours] walks the drops' rims
 * forwards instead and hands back vector outlines.
 *
 * Angles: `Degrees(0)` drags to the right, `Degrees(90)` down (y grows downward). Everything is
 * px. Builder calls return `this`, so a recipe chains. No randomness in here - place the drops
 * with your own seeded Random.
 *
 * Colours are ARGB ints copied through untouched, drop colours and ink samples alike, and the
 * block mean averages them channel by channel. The canvas overload of [render] hands them to a
 * Gartmap, which holds premultiplied pixels - so a translucent drop or flat ink meant for a
 * canvas should be premultiplied itself (opaque inks, the usual case, are the same either way),
 * and an `Ink.image` of a Gartmap already is.
 */
class Marbling(val background: Ink = Ink.flat(Color.WHITE)) {

    constructor(background: Int) : this(Ink.flat(background))

    private val list = ArrayList<MarblingOp>()

    /** Everything that happened, in order. */
    val ops: List<MarblingOp> get() = list

    fun add(op: MarblingOp): Marbling {
        list += op
        return this
    }

    fun drop(x: Float, y: Float, r: Float, color: Int) = add(Drop(x, y, r, color))
    fun drop(p: Point, r: Float, color: Int) = drop(p.x, p.y, r, color)

    /** One tine through ([x], [y]) dragged along [angle]: paint on its line goes [z] px, [c] px off the line half that. */
    fun tine(x: Float, y: Float, angle: Angle, z: Float, c: Float, amplitude: Float = 0f, wavelength: Float = 0f, phase: Float = 0f) =
        add(Comb(x, y, Vec2.of(angle), z, c, floatArrayOf(0f), amplitude, wavelength, phase))

    /** A stylus dragged from [from] to [to]: the paint on its line travels the whole way. */
    fun stroke(from: Point, to: Point, c: Float): Marbling {
        val d = Vec2(to.x - from.x, to.y - from.y)
        val len = d.length()
        require(len > 0f) { "a stroke needs two different points" }
        return add(Comb(from.x, from.y, d / len, len, c))
    }

    /** [tines] tines [spacing] px apart, centred on ([x], [y]), dragged along [angle]. */
    fun comb(x: Float, y: Float, angle: Angle, z: Float, c: Float, tines: Int, spacing: Float, amplitude: Float = 0f, wavelength: Float = 0f, phase: Float = 0f) =
        comb(x, y, angle, z, c, FloatArray(tines) { (it - (tines - 1) / 2f) * spacing }, amplitude, wavelength, phase)

    /** Tines at [offsets] px to the side of ([x], [y]), positive being [angle] turned +90 degrees. */
    fun comb(x: Float, y: Float, angle: Angle, z: Float, c: Float, offsets: FloatArray, amplitude: Float = 0f, wavelength: Float = 0f, phase: Float = 0f) =
        add(Comb(x, y, Vec2.of(angle), z, c, offsets, amplitude, wavelength, phase))

    /** A tine round the circle of radius [r] about ([cx], [cy]); paint on the circle travels [z] px along it. */
    fun whirl(cx: Float, cy: Float, r: Float, z: Float, c: Float) = add(Whirl(cx, cy, r, z, c))

    /** A whirl of radius 0: the turn grows without end toward the centre. */
    fun vortex(cx: Float, cy: Float, z: Float, c: Float) = whirl(cx, cy, 0f, z, c)

    /**
     * The sheet wiggling as it is laid on: [amplitude] px along [push], one wave per [wavelength]
     * px along [dir]. The default push is across dir - the side-to-side wiggle.
     */
    fun wave(amplitude: Float, wavelength: Float, dir: Angle = Degrees(90f), push: Angle = dir + Degrees(90f), phase: Float = 0f) =
        add(Wave(Vec2.of(dir), Vec2.of(push), amplitude, wavelength, phase))

    fun shift(dx: Float, dy: Float) = add(Shift(dx, dy))

    /** Where the paint that was at ([x], [y]) at the start is now. */
    fun map(x: Float, y: Float): Point {
        val p = MutableVec2(x, y)
        for (op in list) op.forward(p)
        return Point(p.x, p.y)
    }

    /** Where the paint now at ([x], [y]) was at the start (a drop's inside maps to its centre). */
    fun unmap(x: Float, y: Float): Point {
        val p = MutableVec2(x, y)
        var i = list.size - 1
        while (i >= 0) {
            list[i].inverse(p)
            i--
        }
        return Point(p.x, p.y)
    }

    /** The colour at one point of the finished bath. */
    fun colorAt(x: Float, y: Float): Int = colorAt(x, y, MutableVec2())

    private fun colorAt(x: Float, y: Float, p: MutableVec2): Int {
        p.set(x, y)
        var i = list.size - 1
        while (i >= 0) {
            val op = list[i]
            if (op is Drop && op.contains(p.x, p.y)) return op.color
            op.inverse(p)
            i--
        }
        return background.at(p.x, p.y)
    }

    /**
     * Fills [target] with the bath, one sample per pixel centre, or [aa] x [aa] samples averaged
     * (the integer block mean, same as a box downsample). Rows are banded across [workers] and
     * every pixel is its own walk, so the output does not depend on the worker count.
     */
    fun render(target: Pixels, aa: Int = 1, workers: Int = defaultWorkers) {
        require(aa >= 1) { "aa must be at least 1" }
        val w = target.d.w
        val px = target.pixels
        parallelBands(target.d.h, workers) { y0, y1 ->
            val p = MutableVec2()
            for (y in y0 until y1) {
                var i = y * w
                for (x in 0 until w) {
                    px[i] = if (aa == 1) colorAt(x + 0.5f, y + 0.5f, p) else block(x, y, aa, p)
                    i++
                }
            }
        }
    }

    private fun block(x: Int, y: Int, aa: Int, p: MutableVec2): Int {
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (j in 0 until aa) {
            for (i in 0 until aa) {
                val c = colorAt(x + (i + 0.5f) / aa, y + (j + 0.5f) / aa, p)
                a += (c ushr 24) and 0xFF
                r += (c ushr 16) and 0xFF
                g += (c ushr 8) and 0xFF
                b += c and 0xFF
            }
        }
        val n = aa * aa
        return ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
    }

    /**
     * [render] into a canvas: a pixel buffer of its size, filled and pushed onto it. The buffer is
     * premultiplied ARGB, so translucent colours must arrive premultiplied - see the class note.
     */
    fun render(g: Gartvas, aa: Int = 1, workers: Int = defaultWorkers) {
        Gartmap(g.d).use { m ->
            render(m, aa, workers)
            m.drawToCanvas(g)
        }
    }
}

package dev.oblac.gart.marbling

import dev.oblac.gart.math.LN2f
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One thing that happened to the bath. [forward] carries a point of paint to where this op
 * leaves it, [inverse] takes a point back to where its paint was before. Every op is a bijection
 * of the plane with both directions in closed form - thats what lets a pixel be coloured by
 * walking the history backwards, and a drop's rim be walked forwards.
 *
 * Points are moved in place (a cursor), so the per-pixel walk allocates nothing.
 */
sealed interface MarblingOp {
    fun forward(p: MutableVec2)
    fun inverse(p: MutableVec2)
}

// a tine further off than this many halvings pulls under 1e-4 of z - skipped, and skipped the
// same way in both directions so the pair stays exact
private const val REACH = 13.3f

/**
 * Paint of radius [r] dropped at ([cx], [cy]). Everything already there is shoved straight out
 * from the centre, just far enough to make room for the disc: the area of any patch away from
 * the centre is kept, so the bands of earlier drops thin but never vanish. Nothing ends up
 * inside the fresh disc - the map sends the whole plane onto the outside of it.
 */
class Drop(val cx: Float, val cy: Float, val r: Float, val color: Int) : MarblingOp {
    private val r2 = r * r

    fun contains(x: Float, y: Float): Boolean {
        val dx = x - cx
        val dy = y - cy
        return dx * dx + dy * dy < r2
    }

    override fun forward(p: MutableVec2) {
        val dx = p.x - cx
        val dy = p.y - cy
        val h2 = dx * dx + dy * dy
        if (h2 == 0f) return // the centre point has nowhere in particular to go
        val s = sqrt(1f + r2 / h2)
        p.set(cx + dx * s, cy + dy * s)
    }

    /** Only means something outside the disc, check [contains] first. Inside maps to the centre. */
    override fun inverse(p: MutableVec2) {
        val dx = p.x - cx
        val dy = p.y - cy
        val h2 = dx * dx + dy * dy
        if (h2 <= r2) {
            p.set(cx, cy)
            return
        }
        val s = sqrt(1f - r2 / h2)
        p.set(cx + dx * s, cy + dy * s)
    }

    override fun toString() = "Drop($cx, $cy, r=$r)"
}

/**
 * Tines dragged in a straight line, all together. [dir] is the drag direction, tine i sits
 * [offsets][i] px to the side of ([x], [y]) along dir turned +90 degrees. Paint on a tine moves
 * [z] px along dir, [c] px to either side the pull has halved, and it keeps halving. A tine only
 * moves paint along dir and only cares about the sideways distance, so tines neither notice nor
 * disturb each other: the pulls just add, and the inverse is the same pull the other way.
 *
 * With [amplitude] > 0 the comb wiggles from side to side as it goes, one full wiggle per
 * [wavelength] px of travel. Done as a sideways shear that straightens the wiggly tine paths,
 * the plain pull, and the shear undone at the new position - so the paint rides the wiggle and
 * the inverse is still the same three steps with z negated.
 */
class Comb(
    val x: Float, val y: Float, dir: Vec2, val z: Float, val c: Float,
    val offsets: FloatArray = floatArrayOf(0f),
    val amplitude: Float = 0f, val wavelength: Float = 0f, val phase: Float = 0f,
) : MarblingOp {
    val dir: Vec2 = dir.normalize()

    init {
        require(c > 0f) { "c must be positive, got $c" }
        require(amplitude == 0f || wavelength > 0f) { "a wavy comb needs a wavelength" }
    }

    private val mx = this.dir.x
    private val my = this.dir.y
    private val nx = -my
    private val ny = mx
    private val k = LN2f / c
    private val reach = REACH * c
    private val omega = if (wavelength > 0f) TAUf / wavelength else 0f

    private fun pull(n: Float): Float {
        var sum = 0f
        for (o in offsets) {
            val d = abs(n - o)
            if (d < reach) sum += exp(-k * d)
        }
        return sum * z
    }

    private fun wiggle(m: Float) = if (amplitude == 0f) 0f else amplitude * sin(omega * m + phase)

    override fun forward(p: MutableVec2) = move(p, 1f)
    override fun inverse(p: MutableVec2) = move(p, -1f)

    private fun move(p: MutableVec2, sign: Float) {
        val px = p.x - x
        val py = p.y - y
        val m = px * mx + py * my
        val n = px * nx + py * ny
        val n1 = n - wiggle(m)
        val m1 = m + sign * pull(n1)
        val n2 = n1 + wiggle(m1)
        p.set(x + m1 * mx + n2 * nx, y + m1 * my + n2 * ny)
    }

    override fun toString() = "Comb($x, $y, z=$z, c=$c, tines=${offsets.size})"
}

/**
 * A tine dragged round a circle of radius [r] about ([cx], [cy]): paint on the circle travels
 * [z] px along it, paint [c] px off the circle half that, and so on. The travel is an arc, so
 * nearer the centre the same arc is a bigger turn. [r] = 0 is a vortex - the turn keeps growing
 * toward the centre and the paint there winds tighter than any pixel, which is the look.
 */
class Whirl(val cx: Float, val cy: Float, val r: Float, val z: Float, val c: Float) : MarblingOp {
    init {
        require(c > 0f) { "c must be positive, got $c" }
    }

    private val k = LN2f / c

    override fun forward(p: MutableVec2) = turn(p, z)
    override fun inverse(p: MutableVec2) = turn(p, -z)

    private fun turn(p: MutableVec2, z: Float) {
        val dx = p.x - cx
        val dy = p.y - cy
        val h = sqrt(dx * dx + dy * dy)
        if (h < 1e-6f) return
        val a = z * exp(-k * abs(h - r)) / h
        val ca = cos(a)
        val sa = sin(a)
        p.set(cx + dx * ca - dy * sa, cy + dx * sa + dy * ca)
    }

    override fun toString() = "Whirl($cx, $cy, r=$r, z=$z, c=$c)"
}

/**
 * The sheet wiggling as it is laid on the bath: every point slides [amplitude] px along [push],
 * times the sine of where it is along [dir], one wave per [wavelength] px. push across dir is the
 * side-to-side wiggle, push along dir stretches and squeezes, anything between jags. The sheet
 * folds over itself once amplitude * (push . dir) * 2pi / wavelength passes 1 - allowed, the
 * raster then shows one of the layers and a contour crosses itself.
 */
class Wave(dir: Vec2, push: Vec2, val amplitude: Float, val wavelength: Float, val phase: Float = 0f) : MarblingOp {
    val dir: Vec2 = dir.normalize()
    val push: Vec2 = push.normalize()

    init {
        require(wavelength > 0f) { "wavelength must be positive, got $wavelength" }
    }

    private val omega = TAUf / wavelength
    private val e = this.push.x * this.dir.x + this.push.y * this.dir.y // the part of the push that lands along dir

    override fun forward(p: MutableVec2) {
        val s = p.x * dir.x + p.y * dir.y
        val w = amplitude * sin(omega * s + phase)
        p.add(w * push.x, w * push.y)
    }

    override fun inverse(p: MutableVec2) {
        val s1 = p.x * dir.x + p.y * dir.y
        val s = if (e == 0f) s1 else solve(s1)
        val w = amplitude * sin(omega * s + phase)
        p.add(-w * push.x, -w * push.y)
    }

    // s + A e sin(w s + phi) = s1. the root sits within A e of s1, newton from s1 with the bracket
    // as a safety net - bisect whenever a step would leave it, so a folded sheet still lands on
    // one of its layers instead of flying off
    private fun solve(s1: Float): Float {
        val ae = amplitude * e
        var lo = s1 - abs(ae)
        var hi = s1 + abs(ae)
        var s = s1
        for (round in 1..32) {
            val f = s + ae * sin(omega * s + phase) - s1
            if (f > 0f) hi = s else lo = s
            val df = 1f + ae * omega * cos(omega * s + phase)
            var next = if (df > 1e-4f) s - f / df else (lo + hi) / 2f
            if (next <= lo || next >= hi) next = (lo + hi) / 2f
            val done = abs(next - s) < 1e-4f
            s = next
            if (done) break
        }
        return s
    }

    override fun toString() = "Wave(amp=$amplitude, len=$wavelength)"
}

/** The whole bath slid by ([dx], [dy]). */
class Shift(val dx: Float, val dy: Float) : MarblingOp {
    override fun forward(p: MutableVec2) {
        p.add(dx, dy)
    }

    override fun inverse(p: MutableVec2) {
        p.add(-dx, -dy)
    }
}

/**
 * Your own pair of maps. Both must be given and they must undo each other, or the raster and
 * the contours will disagree about where the paint is.
 */
class Custom(private val fwd: (MutableVec2) -> Unit, private val inv: (MutableVec2) -> Unit) : MarblingOp {
    override fun forward(p: MutableVec2) = fwd(p)
    override fun inverse(p: MutableVec2) = inv(p)
}

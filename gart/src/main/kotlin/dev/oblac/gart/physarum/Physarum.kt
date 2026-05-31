package dev.oblac.gart.physarum

import dev.oblac.gart.math.*
import java.util.stream.IntStream
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Slime-mould transport-network simulation.
 *
 * Implementation of Jeff Jones' agent model
 * ("Characteristics of pattern formation and evolution in approximations of
 * Physarum transport networks")
 *
 * The model has two coupled parts:
 *
 *  - Agents. Each one samples a pheromone field at three sensors — left, centre
 *    and right of its heading, [sensorAngle] apart, [sensorDistance] ahead —
 *    turns toward the strongest reading by [rotationAngle], steps [stepSize]
 *    forward (wrapping toroidally) and stamps its cell.
 *
 *  - The field. Mirrors the original's two texture channels. `red` is the decayed
 *    deposit map (where agents are now); `green` is what the agents actually sense
 *    and what gets drawn (exposed as [trail]). Each frame, following
 *    diffuse_decay_fs.glsl:
 *        green := decay · ( boxblur3x3(red) + 0.5 · boxblur3x3(green) )
 *        red   := decay · deposits
 *    both clamped to [0.01, 1]. The 3x3 blur is the "diffuse", the ×decay the
 *    "evaporate". From these trivial local rules, global vein-like networks emerge.
 *
 * Parameters use the original's effective defaults: sa=2, ra=4, so=12, ss=1.1,
 * decay=0.9 (the shader maps its sa/ra sliders to radians via ×1/π, so sa=2 is
 * really ~0.64 rad — reproduced here directly).
 *
 * Agents are stored as parallel primitive arrays (no per-agent objects) so
 * hundreds of thousands of them stay cache-friendly and GC-free; the heavy
 * per-cell / per-agent loops run over a parallel [IntStream].
 *
 * Typical use, per frame: [step], then read [trail] (length [area], row-major
 * `y * w + x`) and map it to colours. [scatter], [seedDisc], [draw] and [clear]
 * reset or seed the agents; the tunables below can be changed live.
 */
class Physarum(val w: Int, val h: Int, agentCount: Int) {

    val area = w * h
    private val wf = w.toFloat()
    private val hf = h.toFloat()

    // field channels (see class doc). green is ping-ponged each step.
    private var green = FloatArray(area)        // sensed + displayed (the .g channel)
    private var greenNext = FloatArray(area)
    private val red = FloatArray(area)          // decayed deposit echo (the .r channel)
    private val dep = FloatArray(area)          // this frame's raw agent presence (points)

    /** The field agents sense and that gets drawn; `area` floats in [0.01, 1], row-major. */
    val trail get() = green

    private val n = agentCount
    private val ax = FloatArray(n)   // x position (pixels)
    private val ay = FloatArray(n)   // y position (pixels)
    private val ah = FloatArray(n)   // heading (radians)

    // tunables (the original's SA / RA / SO / SS / decay, as effective values)
    var sensorAngle = 2f / PIf       // sa = 2
    var rotationAngle = 4f / PIf     // ra = 4
    var sensorDistance = 12f         // so = 12 px
    var stepSize = 1.1f              // ss = 1.1 px
    var decay = 0.9f

    init {
        scatter()
        renderDeposits()
    }

    fun scatter() {
        for (i in 0 until n) {
            ax[i] = rndf() * wf
            ay[i] = rndf() * hf
            ah[i] = rndf() * TWO_PIf
        }
    }

    fun seedDisc(cx: Float, cy: Float, radius: Float) {
        for (i in 0 until n) place(i, cx, cy, radius)
    }

    /** Relocate [count] random agents into a disc — used for mouse "drawing". */
    fun draw(cx: Float, cy: Float, radius: Float, count: Int) {
        repeat(count) { place(rndi(n), cx, cy, radius) }
    }

    private fun place(i: Int, cx: Float, cy: Float, radius: Float) {
        val a = rndf() * TWO_PIf
        val r = (radius * sqrt(rndf()))  // sqrt -> uniform over the disc
        ax[i] = wrap(cx + cos(a) * r, wf)
        ay[i] = wrap(cy + sin(a) * r, hf)
        ah[i] = rndf() * TWO_PIf
    }

    fun clear() {
        green.fill(0f); greenNext.fill(0f); red.fill(0f); dep.fill(0f)
    }

    private fun senseGreen(x: Float, y: Float, angle: Float): Float {
        val sx = x + cos(angle) * sensorDistance
        val sy = y + sin(angle) * sensorDistance
        return green[wrap(sy.toInt(), h) * w + wrap(sx.toInt(), w)]
    }

    private fun moveAgent(i: Int) {
        val a = ah[i]
        val x = ax[i]
        val y = ay[i]

        val fl = senseGreen(x, y, a - sensorAngle)
        val fc = senseGreen(x, y, a)
        val fr = senseGreen(x, y, a + sensorAngle)

        val na = when {
            fc > fl && fc > fr -> a                                                   // straight ahead is best
            fc < fl && fc < fr -> a + if (rndf() < 0.5f) rotationAngle else -rotationAngle  // dead end -> random turn
            fl < fr -> a + rotationAngle                                              // right is stronger
            fr < fl -> a - rotationAngle                                              // left is stronger
            else -> a
        }

        ax[i] = wrap(x + cos(na) * stepSize, wf)
        ay[i] = wrap(y + sin(na) * stepSize, hf)
        ah[i] = na
    }

    // green := decay * ( boxblur(red) + 0.5 * boxblur(green) ), clamped — the diffuse+decay pass
    private fun diffuseRow(y: Int) {
        val ym = wrap(y - 1, h) * w
        val y0 = y * w
        val yp = wrap(y + 1, h) * w
        val inv9 = 1f / 9f
        for (x in 0 until w) {
            val xm = wrap(x - 1, w)
            val xp = wrap(x + 1, w)
            val rs =
                red[ym + xm] + red[ym + x] + red[ym + xp] +
                    red[y0 + xm] + red[y0 + x] + red[y0 + xp] +
                    red[yp + xm] + red[yp + x] + red[yp + xp]
            val gs =
                green[ym + xm] + green[ym + x] + green[ym + xp] +
                    green[y0 + xm] + green[y0 + x] + green[y0 + xp] +
                    green[yp + xm] + green[yp + x] + green[yp + xp]
            var v = decay * (rs * inv9 + 0.5f * gs * inv9)
            if (v < 0.01f) v = 0.01f else if (v > 1f) v = 1f
            greenNext[y0 + x] = v
        }
    }

    // red := decay * deposits, clamped
    private fun redRow(y: Int) {
        val base = y * w
        for (x in 0 until w) {
            var v = decay * dep[base + x]
            if (v < 0.01f) v = 0.01f else if (v > 1f) v = 1f
            red[base + x] = v
        }
    }

    private fun renderDeposits() {
        dep.fill(0f)
        for (i in 0 until n) {
            dep[ay[i].toInt() * w + ax[i].toInt()] = 1f
        }
    }

    fun step() {
        // 1. diffuse + decay  (each row independent -> parallel)
        IntStream.range(0, h).parallel().forEach { diffuseRow(it) }
        IntStream.range(0, h).parallel().forEach { redRow(it) }   // after diffuse has read the old red
        val t = green; green = greenNext; greenNext = t
        // 2. sense + steer + move  (each agent independent -> parallel)
        IntStream.range(0, n).parallel().forEach { moveAgent(it) }
        // 3. stamp deposits at the new positions, ready for the next diffuse
        renderDeposits()
    }
}

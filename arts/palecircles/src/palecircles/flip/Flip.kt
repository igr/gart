package palecircles.flip

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.drawBorder
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.random.Random

/**
 * flip - all-circles on a flip-disc board, stopped while it changes.
 */
private val seed = pi("seed", 1)
private val dropx = pf("dropx", 0.605f, -1f..2f)
private val dropy = pf("dropy", 0.605f, -1f..2f)
private val reach = pf("reach", 0.38f, 0f..3f)
private val out = ps("out", "flip")

private val paper = RetroColors.white01
private val board = RetroColors.black01
private val backFill = fillOf(paper)
private val faceFill = fillOf(RetroColors.red01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("flip", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private const val N = 22        // discs across
private const val SIZE = 0.98f  // red side siz eover cell
private const val PALE = 0.71f  // pale side over cell
private const val SLOP = 0.35f

private fun draw(c: Canvas, d: Dimension) {
    c.clear(board)

    val rng = Random(seed)
    val cell = d.wf / N
    val r = SIZE * cell / 2f
    val rp = PALE * cell / 2f
    val px = dropx * d.wf
    val py = dropy * d.hf

    for (j in 0 until N) for (i in 0 until N) {
        val cx = (i + 0.5f) * cell
        val cy = (j + 0.5f) * cell
        val age = (reach * d.wf - hypot(cx - px, cy - py)) / cell + rng.rndf(-SLOP, SLOP)
        drawDisc(c, cx, cy, rp, r, turn(age))
    }

    c.drawBorder(d, 40f, paper)
}

private const val RISE = 3.2f
private const val KEEP = 0.62f

private fun turn(age: Float): Float {
    if (age <= 0f) return 0f
    if (age < RISE) return PIf * (age / RISE) * (age / RISE)
    var t = age - RISE
    var len = 2f * KEEP * RISE
    var lift = KEEP * KEEP * PIf
    repeat(16) {
        if (t < len) return PIf - lift * 4f * (t / len) * (1f - t / len)
        t -= len
        len *= KEEP
        lift *= KEEP * KEEP
    }
    return PIf
}

private fun drawDisc(c: Canvas, cx: Float, cy: Float, rp: Float, rr: Float, th: Float) {
    val ct = cos(th)
    val r = if (ct > 0f) rp else rr
    val b = r * abs(ct)
    if (b < 0.25f) return // on its edge, nothing to see
    c.drawOval(Rect(cx - r, cy - b, cx + r, cy + b), if (ct > 0f) backFill else faceFill)
}

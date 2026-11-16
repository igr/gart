package palecircles.weft

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
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * weft - the dot board woven. ink warp runs down the page and floats over seven red weft threads at
 * a time, so all that shows of the weft is a lattice of red dots. the threads were dyed before
 * weaving, like ikat: where they pass a hidden sun the warp is red and the weft ink. the weave
 * itself never changes, only the dye.
 */
private val sunx = pf("sunx", 0.58f, -1f..2f)  // the sun, page widths. only in the dye
private val suny = pf("suny", 0.55f, -1f..2f)
private val sunr = pf("sunr", 0.3f, 0f..2f)
private val n = pi("n", 28, 2..200)            // threads across, both ways
private val float = pi("float", 7, 1..30)      // crossings a thread floats over before it ties down. 1 is plain weave, all dots
private val shift = pf("shift", 0.5f, 0f..1f)  // how far the tie moves on from one thread to the next, over the repeat. 0.5 lays bricks, 1/8 a twill
private val gap = pf("gap", 0.16f, 0f..0.9f)   // paper between threads, over cell
private val out = ps("out", "weft")

private val paper = RetroColors.white01
private val inkFill = fillOf(RetroColors.black01)
private val redFill = fillOf(RetroColors.red01)

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("weft", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    draw(g.canvas, g.d)

    gart.saveImage(g, out.ensureExtension("png"))
    if (!headless) gart.window().showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(paper)

    val cell = d.wf / n
    val sun = Point(sunx * d.wf, suny * d.hf)
    val repeat = float + 1
    val k = (shift * repeat).roundToInt()

    // the draft: warp i over weft j, or under. the weft only comes up once a repeat
    val weftOnTop = Array(n) { i -> BooleanArray(n) { j -> Math.floorMod(j + k * i + 1, repeat) == 0 } }

    weave(c, cell, weftOnTop, inkFill, redFill)
    c.save()
    c.clipPath(PathBuilder().addCircle(sun.x, sun.y, sunr * d.wf).detach(), true)
    weave(c, cell, weftOnTop, redFill, inkFill)
    c.restore()

    c.drawBorder(d, 40f, paper)
}

private fun weave(c: Canvas, cell: Float, weftOnTop: Array<BooleanArray>, warp: Paint, weft: Paint) {
    val g = gap * cell / 2f
    for (i in 0 until n) runs { !weftOnTop[i][it] }.forEach {
        pill(c, Rect(i * cell + g, it.first * cell + g, (i + 1) * cell - g, (it.last + 1) * cell - g), warp)
    }
    for (j in 0 until n) runs { weftOnTop[it][j] }.forEach {
        pill(c, Rect(it.first * cell + g, j * cell + g, (it.last + 1) * cell - g, (j + 1) * cell - g), weft)
    }
}

private fun runs(on: (Int) -> Boolean): List<IntRange> {
    val found = mutableListOf<IntRange>()
    var a = 0
    while (a < n) {
        if (!on(a)) { a++; continue }
        val start = a
        while (a < n && on(a)) a++
        found += start until a
    }
    return found
}

private fun pill(c: Canvas, r: Rect, p: Paint) {
    c.drawRRect(RRect.makeLTRB(r.left, r.top, r.right, r.bottom, min(r.width, r.height) / 2f), p)
}

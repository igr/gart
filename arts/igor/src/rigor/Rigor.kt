package rigor

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.pixels.boxDownsample
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("rigor", W, H)
    println(gart)

    val t0 = System.currentTimeMillis()
    val deal = deal()

    val gv = Gartvas(Dimension(GW, GH))
    val c = gv.canvas
    c.drawRect(gv.d.rect, fillOf(PAPER))
    drawGrid(c, deal)
    println("seed=$SEED grid ${COLS}x$ROWS, letters at ${deal.letters.joinToString { "${it.letter}(${it.col},${it.row})" }}, ${System.currentTimeMillis() - t0}ms")

    val g = gart.gartvas()
    val map = Gartmap(g.d)
    boxDownsample(Gartmap(gv).pixels, SS, map)
    map.drawToCanvas(g)
    if (VIG > 0f) g.canvas.drawVignette(g.d, VIG)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private const val W = 1024
private const val H = 1280

private val SEED = pi("seed", 14)                 // 14 deals the eclipse rows and seats all four letters where they can breathe
private val OUT = ps("out", "rigor")
private val SS = pi("ss", 3, 1..4)

private val COLS = pi("cols", 8, 4..12)
private val ROWS = pi("rows", 10, 4..14)
private val PERIOD = pi("period", 3, 1..5)        // rows in the repeating block (its always 2 tiles wide). 1 is drumbeat, 4 starts to hide the repeat
private val STRIKE = pf("rust", 0.12f, 0f..0.5f)  // chance a block tile is struck in rust. it rides the repeat, so colour keeps the rhythm - only the letters break it
private val ACCENT = pi("accent", 0, 0..1)        // 1 strikes the letters in rust instead of ink, if quiet isnt landing

private val LTILT = pf("ltilt", 6f, 0f..15f)      // the letters lean, degrees. the first igor piece leaned 6
private val REACH = pf("reach", 2.2f, 0f..4f)     // how many cells out a letters influence carries
private val ITILT = pf("itilt", 6.5f, 0f..15f)    // strongest twist a neighbour tile picks up, degrees
private val PUSH = pf("push", 0.11f, 0f..0.3f)    // how far neighbours get shoved off grid, in cells. the dent that makes the twist legible

private val VIG = pf("vig", 0.10f, 0f..1.4f)
private val GRAIN = pf("grain", 0.03f, 0f..1f)

private val GW = W * SS
private val GH = H * SS
private val rng = Random(SEED)

private const val PAPER = 0xFFDBD1BF.toInt()
private const val INK = 0xFF242526.toInt()
private const val RUST = 0xFFA8402C.toInt()

private const val QUARTER = 0
private const val HALF = 1
private const val DISK = 2
private const val LEAF = 3
private const val TRI = 4

private class Tile(val type: Int, val orient: Int, val rust: Boolean)
private class Letter(val letter: String, val col: Int, val row: Int, val lean: Float)
private class Deal(val block: Array<Array<Tile>>, val letters: List<Letter>)

private fun deal(): Deal {
    val block = Array(2) { Array(PERIOD) { rollTile() } }

    val cells = mutableListOf<Pair<Int, Int>>()
    for (b in bands()) {
        var tries = 0
        while (true) {
            val col = rng.nextInt(COLS)
            val row = b.first + rng.nextInt(b.second - b.first + 1)
            val clear = cells.none { (pc, _) -> abs(pc - col) < 2 } || tries > 40
            if (clear) { cells += col to row; break }
            tries++
        }
    }
    val letters = cells.mapIndexed { i, (col, row) ->
        Letter("IGOR"[i].toString(), col, row, (if (rng.nextBoolean()) 1f else -1f) * LTILT)
    }
    return Deal(block, letters)
}

private fun rollTile(): Tile {
    val r = rng.nextFloat()
    val type = when {
        r < 0.30f -> QUARTER
        r < 0.55f -> HALF
        r < 0.72f -> LEAF
        r < 0.87f -> TRI
        else -> DISK
    }
    return Tile(type, rng.nextInt(4), rng.nextFloat() < STRIKE)
}

private fun bands(): List<Pair<Int, Int>> {
    val edges = IntArray(5) { it * ROWS / 4 }
    return (0..3).map { edges[it] to (edges[it + 1] - 1).coerceAtLeast(edges[it]) }
}

private fun drawGrid(c: Canvas, deal: Deal) {
    val cell = GW / (COLS + 1f)
    val x0 = (GW - COLS * cell) / 2f
    val y0 = (GH - ROWS * cell) / 2f

    for (col in 0 until COLS) for (row in 0 until ROWS) {
        val letter = deal.letters.find { it.col == col && it.row == row }
        val (twist, ox, oy) = unscrew(col, row, deal.letters)
        val cx = x0 + (col + 0.5f) * cell + ox * cell
        val cy = y0 + (row + 0.5f) * cell + oy * cell

        c.save()
        if (letter != null) {
            c.rotate(letter.lean + twist, cx, cy)
            glyph(c, letter.letter, cx, cy, cell * 0.98f, if (ACCENT == 1) RUST else INK)
        } else {
            if (twist != 0f) c.rotate(twist, cx, cy)
            tile(c, deal.block[col % 2][row % PERIOD], cx, cy, cell * 0.84f)
        }
        c.restore()
    }
}

private fun unscrew(col: Int, row: Int, letters: List<Letter>): Triple<Float, Float, Float> {
    var t = 0f
    var ox = 0f
    var oy = 0f
    for (l in letters) {
        if (l.col == col && l.row == row) continue
        val d = hypot((l.col - col).toFloat(), (l.row - row).toFloat())
        if (d >= REACH || d == 0f) continue
        val f = 1f - d / REACH
        t += (if (l.lean > 0) 1f else -1f) * ITILT * f
        ox += (col - l.col) / d * PUSH * f
        oy += (row - l.row) / d * PUSH * f
    }
    return Triple(t, ox, oy)
}

private fun tile(c: Canvas, t: Tile, cx: Float, cy: Float, size: Float) {
    val ink = fillOf(if (t.rust) RUST else INK)
    val h = size / 2f
    when (t.type) {
        QUARTER -> {
            val (px, py) = corner(t.orient, cx, cy, h)
            c.drawArc(px - size, py - size, px + size, py + size, quarterStart(t.orient), 90f, true, ink)
        }
        HALF -> {
            val (px, py) = edge(t.orient, cx, cy, h)
            c.drawArc(px - h, py - h, px + h, py + h, quarterStart(t.orient), 180f, true, ink)
        }
        DISK -> c.drawCircle(cx, cy, h * 0.82f, ink)
        LEAF -> {
            val a = t.orient % 2
            val r = size * 0.55f
            val (x1, y1) = corner(a, cx, cy, h)
            val (x2, y2) = corner(a + 2, cx, cy, h)
            c.drawArc(x1 - r, y1 - r, x1 + r, y1 + r, quarterStart(a), 90f, true, ink)
            c.drawArc(x2 - r, y2 - r, x2 + r, y2 + r, quarterStart(a + 2), 90f, true, ink)
        }
        TRI -> {
            val pb = PathBuilder()
            val (x1, y1) = corner(t.orient, cx, cy, h)
            val (x2, y2) = corner((t.orient + 1) % 4, cx, cy, h)
            val (x3, y3) = corner((t.orient + 2) % 4, cx, cy, h)
            pb.moveTo(x1, y1)
            pb.lineTo(x2, y2)
            pb.lineTo(x3, y3)
            pb.closePath()
            val p = pb.detach()
            c.drawPath(p, ink)
            p.close()
        }
    }
}

private fun corner(o: Int, cx: Float, cy: Float, h: Float): Pair<Float, Float> = when (o % 4) {
    0 -> cx - h to cy - h
    1 -> cx + h to cy - h
    2 -> cx + h to cy + h
    else -> cx - h to cy + h
}

private fun edge(o: Int, cx: Float, cy: Float, h: Float): Pair<Float, Float> = when (o % 4) {
    0 -> cx to cy - h
    1 -> cx + h to cy
    2 -> cx to cy + h
    else -> cx - h to cy
}

private fun quarterStart(o: Int): Float = when (o % 4) {
    0 -> 0f
    1 -> 90f
    2 -> 180f
    else -> 270f
}

private fun glyph(c: Canvas, letter: String, cx: Float, cy: Float, size: Float, colour: Int) {
    val ink = fillOf(colour)
    val stroke = size * 0.26f
    when (letter) {
        "I" -> c.drawRect(Rect(cx - size * 0.15f, cy - size * 0.48f, cx + size * 0.15f, cy + size * 0.48f), ink)
        "O" -> c.drawCircle(cx, cy, size * 0.36f, strokeOf(colour, stroke))
        "G" -> {
            val r = size * 0.36f
            c.drawArc(cx - r, cy - r, cx + r, cy + r, 0f, 318f, false, strokeOf(colour, stroke))
            c.drawRect(Rect(cx - size * 0.04f, cy - stroke * 0.45f, cx + r + stroke * 0.5f, cy + stroke * 0.45f), ink)
        }
        "R" -> {
            val bx = cx - size * 0.30f
            c.drawRect(Rect(bx - stroke * 0.5f, cy - size * 0.48f, bx + stroke * 0.5f, cy + size * 0.48f), ink)
            val br = size * 0.345f
            val bcx = bx + stroke * 0.33f
            val bcy = cy - size * 0.48f + br
            c.drawArc(bcx - br, bcy - br, bcx + br, bcy + br, -90f, 180f, true, ink)
            val legW = stroke * 1.13f
            val foot = cx + size * 0.48f
            val pb = PathBuilder()
            pb.moveTo(bx, bcy)
            pb.lineTo(bx + legW, bcy)
            pb.lineTo(foot, cy + size * 0.48f)
            pb.lineTo(foot - legW, cy + size * 0.48f)
            pb.closePath()
            val p = pb.detach()
            c.drawPath(p, ink)
            p.close()
        }
    }
}

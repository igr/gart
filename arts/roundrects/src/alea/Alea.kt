package alea

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * ALEA
 *
 * Every random domino tiling of an Aztec diamond looks the same at a large scale. The four
 * corners freeze into perfect brickwork. All the disorder stays inside one circle. This is the
 * arctic circle theorem. Nobody designs the symmetry. The boundary forces chance into it.
 *
 * The piece inverts the brief. An accident does not break the pattern here. The accident is
 * the middle, and the pattern grows around the accident. A new seed rolls every domino again.
 * The circle stays where it is.
 *
 * The piece rotates the diamond 45 degrees, so the diamond is the canvas. The frozen corners
 * of the diamond are the corners of the image. The arctic circle is the inscribed circle of
 * the image. It touches the middle of each edge.
 *
 * The tiling grows by domino shuffling. Each of the n generations has three steps:
 * 1. The shuffle removes each pair of dominoes that face each other.
 * 2. Each remaining domino moves one cell outward, in the direction that it faces.
 * 3. One coin flip fills each empty 2x2 hole.
 * The shuffle never rejects a result. Each possible tiling has the same probability.
 */

private const val W = 1080
private const val H = 1080

// knobs -----

private val OUT = ps("out", "alea")
private val SEED = pi("seed", 5)      // roundest circle in the first casting of 8
private val SS = pi("ss", 3, 1..4)
private val N = pi("n", 56, 4..120)              // diamond order. dominoes = n(n+1), the circle sharpens as it grows
private val PAL = pi("pal", 0, 0..181)           // 0 = the house quartet, else 4 colours pulled off coolPalette(pal)
private val GAP = pf("gap", 0.08f, 0f..0.35f)    // grout between dominoes, in cells
private val ROUND = pf("round", 0.14f, 0f..0.5f) // corner radius, in cells
private val JITTER = pf("jitter", 0.10f, 0f..0.4f) // per-domino tone wobble, or the frozen fields go dead flat
private val BLEED = pf("bleed", 2.2f, 0f..8f)    // extra cells past the edge, hides the staircase rim

private val DARK = pi("dark", 1, 0..1)           // 1 = ink grout instead of cream, mosaic at night

private val PAPER = 0xFFF2EDE2.toInt()
private val INKG = 0xFF2A2420.toInt()

// one colour per marching direction. the top corner is all N so it comes out solid,
// same for the other three - the corners name themselves. keep the four at about the same
// value: a bright corner reads bigger than a dark one and the circle looks off center
private fun quartet() = if (PAL == 0)
    Palette(0xFFC4694A, 0xFFC08F35, 0xFF44807C, 0xFF96587B)  // N S E W: terracotta, ochre, teal, plum
else {
    val p = Palettes.coolPalette(PAL)
    Palette(*(0..3).map { p.safe(it * (p.size - 1) / 3).toLong() }.toLongArray())
}

// the shuffle ----------

// dominoes live on a 2N x 2N grid of cells, the diamond of order k is |2x+1-2N|+|2y+1-2N| <= 2k.
// a dominos direction is not stored - it falls out of the checkerboard, and the board flips
// colour every generation, thats what keeps a north domino north while it walks
private const val HORIZ = 0
private const val VERT = 1

private class Domino(var x: Int, var y: Int, val o: Int)

// 0=N 1=S 2=E 3=W at generation k
private fun dir(d: Domino, k: Int): Int {
    // got this backwards first: a fresh 2x2 pair has to move apart, not cross
    val white = (d.x + d.y + k) and 1 == 0
    return if (d.o == HORIZ) (if (white) 1 else 0) else (if (white) 2 else 3)
}

// one cell per direction
private val DX = intArrayOf(0, 0, 1, -1)
private val DY = intArrayOf(-1, 1, 0, 0)

private fun inDiamond(x: Int, y: Int, k: Int) = abs(2 * x + 1 - 2 * N) + abs(2 * y + 1 - 2 * N) <= 2 * k

private fun shuffle(rnd: Random): List<Domino> {
    var dominoes = mutableListOf<Domino>()
    fill22(dominoes, N - 1, N - 1, rnd) // AD_1 is one 2x2 block, one coin
    for (k in 1 until N) {
        dominoes = killPairs(dominoes, k)
        slide(dominoes, k)
        fillHoles(dominoes, k + 1, rnd)
    }
    return dominoes
}

// whos where: index of the domino on each cell, -1 for nobody
private fun occupancy(dominoes: List<Domino>): Array<IntArray> {
    val occ = Array(2 * N) { IntArray(2 * N) { -1 } }
    dominoes.forEachIndexed { i, d ->
        occ[d.x][d.y] = i
        if (d.o == HORIZ) occ[d.x + 1][d.y] = i else occ[d.x][d.y + 1] = i
    }
    return occ
}

// a south sitting right above a north, an east right left of a west: they would pass through
// each other on the slide, so they never get to. only south and east look ahead, and the same
// shape sitting exactly one cell on is enough - the checkerboard says it faces back
private fun killPairs(dominoes: List<Domino>, k: Int): MutableList<Domino> {
    val occ = occupancy(dominoes)
    val dead = BooleanArray(dominoes.size)
    for ((i, d) in dominoes.withIndex()) {
        val dd = dir(d, k)
        if (dd != 1 && dd != 2) continue
        val j = occ[d.x + DX[dd]][d.y + DY[dd]]
        if (j < 0) continue
        val p = dominoes[j]
        if (p.o == d.o && p.x == d.x + DX[dd] && p.y == d.y + DY[dd]) {
            dead[i] = true
            dead[j] = true
        }
    }
    return dominoes.filterIndexedTo(ArrayList()) { i, _ -> !dead[i] }
}

// everyone one cell the way they face
private fun slide(dominoes: List<Domino>, k: Int) {
    for (d in dominoes) {
        val dd = dir(d, k)
        d.x += DX[dd]
        d.y += DY[dd]
    }
}

// whats empty in AD_k decomposes into clean 2x2 holes. each hole is one coin
private fun fillHoles(dominoes: MutableList<Domino>, k: Int, rnd: Random) {
    val occ = occupancy(dominoes)
    for (y in 0 until 2 * N) for (x in 0 until 2 * N) {
        if (occ[x][y] >= 0 || !inDiamond(x, y, k)) continue
        fill22(dominoes, x, y, rnd)
        occ[x].fill(0, y, y + 2) // taken now, or the other three cells of the hole coin it again
        occ[x + 1].fill(0, y, y + 2)
    }
}

private fun fill22(list: MutableList<Domino>, x: Int, y: Int, rnd: Random) {
    if (rnd.nextBoolean()) {
        list.add(Domino(x, y, HORIZ))
        list.add(Domino(x, y + 1, HORIZ))
    } else {
        list.add(Domino(x, y, VERT))
        list.add(Domino(x + 1, y, VERT))
    }
}

// the page -------

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("alea", W, H)
    println(gart)

    val rnd = Random(SEED)
    val dominoes = shuffle(rnd)
    println("n=$N dominoes=${dominoes.size}")

    val pal = quartet()
    // the diamond wears the canvas rotated 45: tips land in the corners, the arctic circle
    // becomes the inscribed circle of the image, kissing the edge midpoints. an upright diamond
    // cropped square can never do this - the crop provably sits inside the circle
    val cell = W * 0.5f * sqrt(2f) / (N - BLEED)

    val big = Gartvas(Dimension(W * SS, H * SS))
    val c = big.canvas
    c.scale(SS.toFloat(), SS.toFloat())
    c.drawRect(Rect.makeWH(W.toFloat(), H.toFloat()), fillOf(if (DARK == 1) INKG else PAPER))
    c.translate(W / 2f, H / 2f)
    c.rotate(45f)

    val g = GAP * cell
    val r = ROUND * cell
    for (d in dominoes) {
        val w = if (d.o == HORIZ) 2 * cell else cell
        val h = if (d.o == HORIZ) cell else 2 * cell
        var col = pal[dir(d, N)]
        // wobble each stone a touch or the frozen corners read as printer fill
        val j = hash01(d.x, d.y, 11, SEED) - 0.5f
        col = if (j > 0) lerpColor(col, 0xFFFFFFFF.toInt(), j * 2 * JITTER)
            else lerpColor(col, 0xFF201C18.toInt(), -j * 2 * JITTER)
        c.drawRRect(
            RRect.makeXYWH((d.x - N) * cell + g, (d.y - N) * cell + g, w - 2 * g, h - 2 * g, r),
            fillOf(col)
        )
    }

    val out = big.downsample(SS)
    gart.saveImage(out, "$OUT.png")
    if (!headless) gart.window().showImage(out)
}

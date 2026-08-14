package rectflow

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Angle
import dev.oblac.gart.color.MidCenturyColors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.gfx.Poly4
import dev.oblac.gart.gfx.drawPoly4
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Point
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * rectflow - rectapart, but the carrier bends.
 *
 * here rects ride noise streamlines instead, each one turned to the local field angle, so you get rivers of
 * tiles. three fields and none of them line up, which is the entire point: one steers the trails,
 * one stretches the rects so they lap over each other in bands that cut across the flow, and one
 * is the depth of the pile.
 *
 * that last one arrived late and changed what the piece is. none of this is on paper - theyre
 * chips, cut out and dropped in a heap, and youre looking at a photograph of the heap with the
 * lens wide open. near chips print big and sharp and crop whatever they land on, far ones drift
 * out of register until the plates come apart and sink back toward the ground. before it existed
 * the picture was an all-over field, every square inch equally busy. handsome wallpaper.
 */
private const val W = 1024
private const val H = 1024

private data class Params(
    val seed: Long,
    val out: String,
    // field
    val scale: Float,
    val octaves: Int,
    val turns: Float,
    val swirl: Float,
    val ax: Float,
    val ay: Float,
    // tracing
    val sep: Float,
    val step: Float,
    val steps: Int,
    val seedEvery: Int,
    val margin: Float,
    // rects
    val pitch: Float,
    val len: Float,
    val lenVar: Float, val lenScale: Float,
    val thick: Float,
    val taper: Float,
    val stroke: Float,
    val knock: Float,
    val minRects: Int,
    val fill: Int,
    val accent: Int,
    // the press
    val plates: Int,
    val plateOff: Float,
    val pal: Int,
    val plateA: Int, val plateB: Int,
    // the pile, and the lens pointed at it
    val dof: Float,
    val focus: Float, val depthScale: Float,
    val blur: Float,
    val haze: Float,
    val spread: Float,
    val persp: Float,
    val depthSort: Int,
    // the automaton
    val caGens: Int,
    val caRadius: Float,
    val caThresh: Int,
    val caRefrac: Int,
    val caBand: Int,
)

private val p = resolveParams()
private val rnd = Random(p.seed)

// simplex carries no seed, so shift the sample window instead. one window per field, kept well
// apart - if two of them rhyme their structures line up and you quietly lose one of the three
private val nzoff = (p.seed and 0xffff) * 0.01f
private val lenoff = 311f + (p.seed and 0xffff) * 0.017f
private val depthoff = 907f + (p.seed and 0xffff) * 0.023f

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("rectflow", W, H)

    println(gart)
    println(
        "seed=${p.seed} scale=${p.scale} turns=${p.turns} swirl=${p.swirl} sep=${p.sep} " +
            "pitch=${p.pitch} len=${p.len} thick=${p.thick} taper=${p.taper} " +
            "lenvar=${p.lenVar} lenscale=${p.lenScale} knock=${p.knock} " +
            "pal=${p.pal}(${pal.name}) plates=${p.plates} plateoff=${p.plateOff} " +
            "cagens=${p.caGens} caradius=${p.caRadius} cathresh=${p.caThresh} carefrac=${p.caRefrac} " +
            "dof=${p.dof} focus=${p.focus} blur=${p.blur} haze=${p.haze} " +
            "spread=${p.spread} persp=${p.persp} depthsort=${p.depthSort}",
    )

    val g = gart.gartvas()
    val draw = RectFlowDraw(g)

    val output = if (p.out.endsWith(".png", ignoreCase = true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)

    if (!headless) gart.window().show(draw)
}

// hot reload needs a real class, not a lambda
private class RectFlowDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

/**
 * one laid-out rect. [firedAt] is the generation the wave first reached it, -1 for never.
 *
 * history, not current state - the medium burns out by about gen 6 and a snapshot taken then is
 * an empty canvas. took me a wasted render to work that out.
 */
private class Tile(
    val at: Point,
    val len: Float,
    val ang: Angle,
    val accent: Boolean,
    val depth: Float,
) {
    var firedAt = -1
}

// the automaton is an excitable medium, the BZ kind rather than the Life kind. fires when enough
// neighbours are firing, then goes deaf for carefrac gens - without the deaf period the wave
// washes back over itself and the whole board saturates
private const val RESTING = 0
private const val EXCITED = 1
// 2 and up = refractory, counting back down to RESTING

// ---- the inks ----

/**
 * ground, ink, accent and the two plates. five slots, thats the whole piece.
 *
 * the ground is not just whats behind the picture - everything fades toward it as it goes out of
 * focus (see [ramp]), so it decides what colour the far half of the canvas turns into.
 *
 * plates want one warm and one cool. a pair from the same temperature goes flat and reads as a
 * chosen outline colour rather than as registration going wrong. they also have to differ from
 * the INK, not just from the ground, or the fringe is technically there and visually absent.
 */
private class Pal(
    val name: String,
    val ground: Int, val ink: Int, val accent: Int,
    val warm: Int, val cool: Int,
)

private val palettes = listOf(
    // 0 - the original
    Pal(
        "retro", RetroColors.black01, RetroColors.white01, RetroColors.red01,
        RetroColors.amber01, RetroColors.purple01,
    ),
    // 1 - the repo's own set, near enough a riso house palette already
    Pal(
        "midcentury", MidCenturyColors.black, MidCenturyColors.white1, MidCenturyColors.red,
        MidCenturyColors.yellow, MidCenturyColors.blue,
    ),
    // 2 - night. indigo ground, so the far corner goes blue-grey fog instead of muddy brown
    Pal(
        "nocturne", NipponColors.col196_KACHI, NipponColors.col105_TORINOKO,
        NipponColors.col037_SYOJYOHI, NipponColors.col108_KUCHINASHI, NipponColors.col190_HANADA,
    ),
    // 3 - wine and sand, accent thrown across to green
    Pal(
        "oxblood", NipponColors.col222_KUROBENI, NipponColors.col089_TONOKO,
        NipponColors.col159_AOTAKE, NipponColors.col047_TERIGAKI, NipponColors.col214_EDOMURASAKI,
    ),
    // 4 - damp, and the ground reads as unlit phosphor rather than paper. the CRT one
    Pal(
        "moss", NipponColors.col166_TETSU, NipponColors.col124_MUSHIKURI,
        NipponColors.col037_SYOJYOHI, NipponColors.col084_KUCHIBA, NipponColors.col167_MIZUASAGI,
    ),
    // 5 - almost monochrome, which throws the whole thing onto the shapes
    Pal(
        "ash", NipponColors.col248_SUMI, NipponColors.col233_SHIRONERI,
        NipponColors.col037_SYOJYOHI, NipponColors.col079_KOHAKU, NipponColors.col184_AINEZUMI,
    ),
    // 6 - the loud one
    Pal(
        "plum", NipponColors.col215_SHIKON, NipponColors.col026_HAIZAKURA,
        NipponColors.col109_TOHOH, NipponColors.col228_BOTAN, NipponColors.col202_KONJYO,
    ),
)

// checked here and not in resolveParams, which runs before this list exists. one confusing null
private val pal = palettes.getOrNull(p.pal)
    ?: error("pal must be 0..${palettes.lastIndex}, got ${p.pal}")

private const val LEVELS = 16   // defocus steps in the paint ramps

// everything the tile painter needs, bundled
private class Ink(
    val stroke: Array<Paint>,
    val fill: Array<Paint>,
    val accent: Array<Paint>,
    val knock: Paint?,
    val plates: List<Plate>,
)

/** one printing plate: the direction its sheet slipped, and its ink in both treatments. */
private class Plate(val dx: Float, val dy: Float, val stroke: Array<Paint>, val fill: Array<Paint>)

/**
 * one colour, cut into [LEVELS] steps of going out of focus. out-of-focus ink spreads, so the
 * stroke gets fatter, and it loses contrast against the ground.
 *
 * strokes haze harder than fills, and not by taste - the same ink is smeared over `widen` times
 * the area, so its got that much less of itself per unit. skip that and the far half goes the
 * wrong way entirely: the fills fade out on schedule while the plate outlines, which have just
 * gone from 2px to 5px, hold their colour and take over the corner.
 */
private fun ramp(col: Int, stroked: Boolean, haze: Float = p.haze): Array<Paint> = Array(LEVELS) { i ->
    val t = i / (LEVELS - 1f)
    val widen = 1f + p.spread * t
    val h = if (stroked) 1f - (1f - haze * t) / widen else haze * t
    val c = lerpColor(col, pal.ground, h)
    if (stroked) strokeOf(c, p.stroke * widen) else fillOf(c)
}

// slip directions, opposed and off-axis. constant across the canvas - a real misregistration is
// the whole sheet shifting, per-tile directions would just read as fuzz. only magnitude varies
private val plateAngles = floatArrayOf(3.58f, 0.44f) // ~205 and ~25 degrees

// the page ======

private fun draw(c: Canvas, d: Dimension) {
    c.clear(pal.ground)

    // ground-coloured fill under each rect so it occludes what it laps over. null rather than a
    // transparent paint, so knock=0 skips the draw call outright and stays byte-exact
    val knockPaint = if (p.knock > 0f) {
        fillOf(pal.ground).apply { alpha = (p.knock * 255).toInt().coerceIn(0, 255) }
    } else {
        null
    }

    // -1 = whatever the palette says. the override indexes RetroColors whichever palette youre
    // on, so its handy for a quick swap and incoherent for anything else
    val plateCols = intArrayOf(
        if (p.plateA >= 0) RetroColors.allColors[p.plateA] else pal.warm,
        if (p.plateB >= 0) RetroColors.allColors[p.plateB] else pal.cool,
    )
    val ink = Ink(
        stroke = ramp(pal.ink, stroked = true),
        fill = ramp(pal.ink, stroked = false),
        // half haze on the accent - theres only ever a handful, and at full haze the ones that
        // landed in the far hump just disappeared
        accent = ramp(pal.accent, stroked = false, haze = p.haze * 0.5f),
        knock = knockPaint,
        plates = (0 until p.plates).map { i ->
            val col = plateCols[i]
            Plate(
                cos(plateAngles[i]), sin(plateAngles[i]),
                ramp(col, stroked = true), ramp(col, stroked = false),
            )
        },
    )

    val trails = trace(d)

    // lay every tile out first, the CA needs them all to exist before it can run
    val tiles = trails.flatMap { tilesOf(it) }
    println("trails=${trails.size} tiles=${tiles.size}")

    if (p.caGens > 0) {
        runCa(tiles, TileGrid(tiles, p.caRadius, d))
        val reached = tiles.count { it.firedAt > 0 }
        val lit = tiles.count { it.firedAt >= 0 && it.firedAt % p.caBand == 0 }
        println("ca: gens=${p.caGens} reached=$reached/${tiles.size} lit=$lit")
    }

    // paint order IS the stacking, because of the knockout. furthest first hands that job to the
    // depth field instead of to whatever order trace() happened to spit trails out in.
    // stable sort, so equal depths keep trace order. nothing here touches rnd
    val order = if (p.depthSort == 1) tiles.sortedByDescending { it.depth } else tiles
    order.forEach { drawTile(c, it, ink) }
}

// the fields /////////

/**
 * second field - how long a rect is, not which way it points. comes back as a multiplier around
 * 1, so lenvar=0 leaves every rect at [Params.len] and the whole thing switches off.
 */
private fun lenAt(pt: Point): Float {
    val n = fbm(
        pt.x * p.lenScale + lenoff,
        pt.y * p.lenScale + lenoff,
        octaves = 2, // broad soft zones, the fine detail belongs to the flow
        lacunarity = 2f,
        gain = 0.5f,
    )
    // floor it, else a deep enough trough hands back a negative rect and Poly4 winds it inside out
    return (1f + p.lenVar * n).coerceAtLeast(0.12f)
}

/**
 * third field - how deep in the pile a chip is lying, 0 nearest 1 furthest. low frequency on
 * purpose: a heap has a hump or two across a frame this size, it doesnt jitter chip to chip.
 */
private fun depthAt(pt: Point): Float {
    val n = fbm(
        pt.x * p.depthScale + depthoff,
        pt.y * p.depthScale + depthoff,
        octaves = 2,
        lacunarity = 2f,
        gain = 0.5f,
    )
    return (n / 1.5f + 0.5f).coerceIn(0f, 1f) // fbm at 2 octaves spans about +-0.75
}

/**
 * how far out of focus a chip at [depth] is, 0 dead sharp and 1 as bad as it gets. normalised by
 * whichever side of the focal plane has more room, so moving [Params.focus] slides the sharp band
 * around without also changing how soft the worst of it goes.
 *
 * dof folds in here rather than at each use site, so one multiply turns the entire lens off.
 */
private fun defocusOf(depth: Float): Float {
    if (p.dof <= 0f) return 0f
    val room = max(p.focus, 1f - p.focus).coerceAtLeast(1e-3f)
    return (abs(depth - p.focus) / room).coerceIn(0f, 1f) * p.dof
}

/**
 * the flow direction at a point: fbm angle plus a circulation term around an off-canvas
 * attractor, so the trails all sweep the same way instead of milling about.
 */
private fun fieldAt(pt: Point, d: Dimension): Vec2 {
    val n = fbm(
        pt.x * p.scale + nzoff,
        pt.y * p.scale + nzoff,
        octaves = p.octaves,
        lacunarity = 2f,
        gain = 0.5f,
    )
    val a = n * TAUf * p.turns

    val dx = pt.x - d.wf * p.ax
    val dy = pt.y - d.hf * p.ay
    val r = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-3f)

    // perpendicular to the radius = circulation
    return Vec2(
        cos(a) + p.swirl * (-dy / r),
        sin(a) + p.swirl * (dx / r),
    ).normalize()
}

// ---- laying the tiles out ----

/**
 * evenly spaced streamlines, Jobard & Lefer style. start from one seed, and while a trail is
 * being integrated drop fresh seed candidates one separation to its left and right - so the next
 * trail grows alongside the last one instead of somewhere random. a trail is cut the moment it
 * wanders within [Params.sep] of a foreign one.
 */
private fun trace(d: Dimension): List<List<Point>> {
    val grid = SepGrid(p.sep, d)
    val trails = mutableListOf<List<Point>>()
    val queue = ArrayList<Point>()
    queue += Point(d.wf * 0.5f, d.hf * 0.5f)

    var owner = 0
    while (queue.isNotEmpty()) {
        val seed = queue.removeAt(rnd.nextInt(queue.size))
        owner++
        if (!grid.isFree(seed, owner)) continue

        val fwd = integrate(seed, 1f, d, grid, owner, queue)
        val bwd = integrate(seed, -1f, d, grid, owner, queue)
        val pts = bwd.asReversed() + fwd.drop(1)
        if (pts.size >= 2) trails += pts
    }

    return trails
}

private fun integrate(
    seed: Point,
    dir: Float,
    d: Dimension,
    grid: SepGrid,
    owner: Int,
    queue: MutableList<Point>,
): List<Point> {
    var cur = seed
    grid.insert(cur, owner)
    val pts = mutableListOf(cur)

    for (i in 0 until p.steps) {
        val v = fieldAt(cur, d)
        val next = Point(cur.x + dir * v.x * p.step, cur.y + dir * v.y * p.step)

        if (!inBounds(next, d)) return pts
        if (!grid.isFree(next, owner)) return pts

        pts += next
        grid.insert(next, owner)
        cur = next

        if (i % p.seedEvery == 0) {
            val off = p.sep * 1.05f
            val left = Point(next.x - v.y * off, next.y + v.x * off)
            val right = Point(next.x + v.y * off, next.y - v.x * off)
            if (inBounds(left, d)) queue += left
            if (inBounds(right, d)) queue += right
        }
    }
    return pts
}

private fun inBounds(pt: Point, d: Dimension) =
    pt.x >= -p.margin && pt.y >= -p.margin && pt.x <= d.wf + p.margin && pt.y <= d.hf + p.margin

/**
 * walks the trail at a fixed arc pitch and lays out a rect turned to the local tangent. pitch
 * stays metronomic on purpose - only the lengths move, so the beat survives and the tiles breathe
 * against it. overlap is just len > pitch happening locally.
 *
 * lays out only, draws nothing.
 */
private fun tilesOf(trail: List<Point>): List<Tile> {
    val cum = FloatArray(trail.size)
    for (i in 1 until trail.size) {
        val dx = trail[i].x - trail[i - 1].x
        val dy = trail[i].y - trail[i - 1].y
        cum[i] = cum[i - 1] + sqrt(dx * dx + dy * dy)
    }

    val n = (cum.last() / p.pitch).toInt()
    if (n < p.minRects) return emptyList() // bail before rolling

    val out = ArrayList<Tile>(n)
    var seg = 1
    for (k in 0 until n) {
        val s = (k + 0.5f) * p.pitch
        while (seg < trail.size - 1 && cum[seg] < s) seg++

        val a = trail[seg - 1]
        val b = trail[seg]
        val span = (cum[seg] - cum[seg - 1]).coerceAtLeast(1e-4f)
        val t = ((s - cum[seg - 1]) / span).coerceIn(0f, 1f)
        val at = Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

        // taper first, then let the coarse field stretch whats left, so the fade survives either way
        val u = if (n > 1) k / (n - 1f) else 0.5f
        val len = p.len * (1f - p.taper * (1f - sin(PIf * u))) * lenAt(at)

        // ⚠️ accent rolls every time, fill only when accent missed. reorder these, or slip another
        // roll in anywhere above, and every seeded render in the sweep moves
        val isAccent = rnd.nextInt(p.accent) == 0
        val solid = isAccent || rnd.nextInt(p.fill) == 0

        val tile = Tile(at, len, Vec2(b.x - a.x, b.y - a.y).angle, isAccent, depthAt(at))
        tile.firedAt = if (solid) 0 else -1 // the scatter IS the CA seed, gen 0
        out += tile
    }
    return out
}

private fun drawTile(c: Canvas, t: Tile, ink: Ink) {
    val df = defocusOf(t.depth)
    val lv = (df * (LEVELS - 1)).toInt().coerceIn(0, LEVELS - 1)

    // near chips print bigger. keep it modest, pitch does NOT scale with them, so past about 0.35
    // the near ones start swallowing their own neighbours along the trail and the beat goes
    val s = 1f + p.persp * (p.focus - t.depth) * p.dof
    val poly = Poly4.rectAroundPoint(t.at, t.len * s, p.thick * s, t.ang)
    // solid on every caband'th ring out from whichever seed reached it first. the rings break
    // where two fronts met and killed each other, which is the bit you couldnt draw by hand
    val lit = t.firedAt >= 0 && t.firedAt % p.caBand == 0
    // accent is held out of the CA's hands. it seeds and spreads like anything else but always
    // prints accent-coloured - let the banding decide it and most of them fall in a dark ring
    val solid = t.accent || lit

    // punch the ground out first, so an overlap reads as one tile lapping another
    if (ink.knock != null) c.drawPoly4(poly, ink.knock)

    // then the off-register plates, then the real ink on top, so the colour only ever peeks out
    // as a fringe. plates take the same treatment as the tile or a hollow rect shows solid colour
    // through its middle and the whole thing goes stained-glass
    if (ink.plates.isNotEmpty()) {
        // in focus the plates sit at plateoff and give one crisp doubled image, out of focus they
        // walk apart into separate ghosts. thats the actual blur - theres no filter anywhere in
        // here, its two copies of the same rect drifting
        val slip = p.plateOff + (p.blur - p.plateOff) * df
        ink.plates.forEach { pl ->
            val off = Point(t.at.x + pl.dx * slip, t.at.y + pl.dy * slip)
            c.drawPoly4(
                Poly4.rectAroundPoint(off, t.len * s, p.thick * s, t.ang),
                if (solid) pl.fill[lv] else pl.stroke[lv],
            )
        }
    }

    val paint = when {
        t.accent -> ink.accent[lv]
        lit -> ink.fill[lv]
        else -> ink.stroke[lv]
    }
    c.drawPoly4(poly, paint)
}

// the medium ///////////

/**
 * runs the automaton over every tile and writes the settled states back.
 *
 * neighbours come from distance, not from any index - a tile doesnt know which trail it sits on.
 * index-neighbours would run the waves along the rivers and nowhere else, and the interesting
 * part is a front crossing the flow and breaking where a trail ran out.
 */
private fun runCa(tiles: List<Tile>, grid: TileGrid) {
    var cur = IntArray(tiles.size) { if (tiles[it].firedAt == 0) EXCITED else RESTING }
    var next = IntArray(tiles.size)

    for (gen in 1..p.caGens) {
        for (i in tiles.indices) {
            val s = cur[i]
            next[i] = when {
                s == EXCITED -> 2 // fired, now go deaf
                s >= 2 -> if (s >= 1 + p.caRefrac) RESTING else s + 1
                grid.countExcited(i, cur) >= p.caThresh -> EXCITED
                else -> RESTING
            }
            // first catch only, the second firing is just the same front sloshing back
            if (next[i] == EXCITED && tiles[i].firedAt < 0) tiles[i].firedAt = gen
        }
        val swap = cur; cur = next; next = swap
    }
}

// plumbing

/**
 * bucket grid over tile centres, so the CA can find neighbours without scanning the lot
 * every generation.
 */
private class TileGrid(private val tiles: List<Tile>, private val radius: Float, d: Dimension) {
    private val cols = (d.wf / radius).toInt() + 4
    private val rows = (d.hf / radius).toInt() + 4
    private val cells = Array(cols * rows) { IntArrayList() }
    private val r2 = radius * radius

    init {
        tiles.forEachIndexed { i, t -> cells[row(t.at.y) * cols + col(t.at.x)].add(i) }
    }

    private fun col(x: Float) = ((x / radius).toInt() + 2).coerceIn(0, cols - 1)
    private fun row(y: Float) = ((y / radius).toInt() + 2).coerceIn(0, rows - 1)

    fun countExcited(i: Int, states: IntArray): Int {
        val t = tiles[i]
        val cx = col(t.at.x)
        val cy = row(t.at.y)
        var n = 0
        for (j in -1..1) {
            val ny = cy + j
            if (ny < 0 || ny >= rows) continue
            for (k in -1..1) {
                val nx = cx + k
                if (nx < 0 || nx >= cols) continue
                val bucket = cells[ny * cols + nx]
                for (b in 0 until bucket.size) {
                    val o = bucket[b]
                    if (o == i || states[o] != EXCITED) continue
                    val dx = tiles[o].at.x - t.at.x
                    val dy = tiles[o].at.y - t.at.y
                    if (dx * dx + dy * dy <= r2) n++
                }
            }
        }
        return n
    }
}

/** uniform grid of already-visited points, so the separation test stays local. */
private class SepGrid(private val sep: Float, d: Dimension) {
    private val cols = (d.wf / sep).toInt() + 4
    private val rows = (d.hf / sep).toInt() + 4
    private val xs = Array(cols * rows) { FloatArrayList() }
    private val ys = Array(cols * rows) { FloatArrayList() }
    private val owners = Array(cols * rows) { IntArrayList() }
    private val sep2 = sep * sep

    private fun col(x: Float) = ((x / sep).toInt() + 2).coerceIn(0, cols - 1)
    private fun row(y: Float) = ((y / sep).toInt() + 2).coerceIn(0, rows - 1)

    fun isFree(pt: Point, owner: Int): Boolean {
        val cx = col(pt.x)
        val cy = row(pt.y)
        for (j in -1..1) {
            val ny = cy + j
            if (ny < 0 || ny >= rows) continue
            for (i in -1..1) {
                val nx = cx + i
                if (nx < 0 || nx >= cols) continue
                val cell = ny * cols + nx
                val cxs = xs[cell]
                val cys = ys[cell]
                val cow = owners[cell]
                for (k in 0 until cxs.size) {
                    if (cow[k] == owner) continue
                    val dx = cxs[k] - pt.x
                    val dy = cys[k] - pt.y
                    if (dx * dx + dy * dy < sep2) return false
                }
            }
        }
        return true
    }

    fun insert(pt: Point, owner: Int) {
        val cell = row(pt.y) * cols + col(pt.x)
        xs[cell].add(pt.x)
        ys[cell].add(pt.y)
        owners[cell].add(owner)
    }
}

// tiny primitive lists - the grid holds ~20k entries and boxing all of them is silly
private class FloatArrayList {
    var size = 0
        private set
    private var data = FloatArray(8)

    fun add(v: Float) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    operator fun get(i: Int) = data[i]
}

private class IntArrayList {
    var size = 0
        private set
    private var data = IntArray(8)

    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }

    operator fun get(i: Int) = data[i]
}

// knobs -------------------------------------

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 7L),
        out = ps("out", "rectflow"),
        scale = pf("scale", 0.0011f),
        octaves = pi("octaves", 3),
        turns = pf("turns", 0.6f),
        swirl = pf("swirl", 1.8f),
        ax = pf("ax", -0.15f),
        ay = pf("ay", 1.05f),

        sep = pf("sep", 20f), // under thick, so the trails overlap and crop each other. masonry
        step = pf("step", 2f),
        steps = pi("steps", 600),
        seedEvery = pi("seedevery", 5),
        margin = pf("margin", 40f),

        pitch = pf("pitch", 48f),
        len = pf("len", 70f), // over pitch, so the whole board shingles
        lenVar = pf("lenvar", 0.55f),
        // narrow band, this one. below and its a single gradient across the frame, above and
        // neighbouring trails stop agreeing with each other, which is the whole point of a
        // *spatial* field in the first place
        lenScale = pf("lenscale", 0.0018f),
        thick = pf("thick", 30f),
        taper = pf("taper", 0.2f),
        stroke = pf("stroke", 2f),
        knock = pf("knock", 1f),
        minRects = pi("minrects", 2),
        fill = pi("fill", 12), // wave sources, 1-in-N. caband sets the final density
        accent = pi("accent", 140), // genuinely 1-in-N, the CA never touches it

        plates = pi("plates", 2),
        plateOff = pf("plateoff", 4.5f), // slip where its sharp. the lens ramps it up to blur
        pal = pi("pal", 4), // moss, the CRT one
        // -1 = use the palette. otherwise an index into RetroColors.allColors:
        //   0 black  1 white  2 red   3 green  4 blue   5 yellow  6 orange
        //   7 purple 8 brown  9 teal 10 pink  11 gray  12 maroon 13 amber
        plateA = pi("platea", -1),
        plateB = pi("plateb", -1),

        caGens = pi("cagens", 12), // saturates around 8, the rest is just headroom
        // a bit over pitch, so a tile reaches along its own trail and not only across to the next
        // one. falls off a cliff below ~45 rather than fading out, trails arent evenly spaced
        caRadius = pf("caradius", 55f),
        caThresh = pi("cathresh", 1),
        caRefrac = pi("carefrac", 3),
        // every caband'th ring prints solid. 0 % n is always 0, which is why cagens=0 still gives
        // the plain scatter back untouched
        caBand = pi("caband", 3),

        // master for the lens. dont expect it to hand back the pre-lens picture on its own, the
        // sort and the palette moved too:
        //   -Dpal=0 -Ddof=0 -Ddepthsort=0
        //     -> 0708d67c96e184656d967e55ddaa3ac5db17fbe1ddbe501d1f93b9d8cfacae38
        dof = pf("dof", 1f),
        focus = pf("focus", 0.38f), // sharp band cuts diagonally through the middle
        depthScale = pf("depthscale", 0.0008f),
        blur = pf("blur", 22f), // slip at the worst of it, wants to be several times plateoff
        // the atmospheric half of the falloff. haze=0 does NOT flatten it, since spread is still
        // diluting the strokes whatever this says - what you get is far fills at full strength
        // sitting inside outlines that have all but vanished. odd, quite good, badly named
        haze = pf("haze", 0.5f),
        // by 5 a 2px line has swollen to 12px on a 30px chip and the outline has closed up into a
        // blob. thats what makes the far half read as out of focus instead of just dim - leave it
        // low and that half comes out with MORE edges than the sharp half, which is nonsense
        spread = pf("spread", 5f),
        persp = pf("persp", 0.3f),
        depthSort = pi("depthsort", 1), // furthest chip printed first
    )

    require(p.scale in 0.0001f..0.02f) { "scale must be between 0.0001 and 0.02" }
    require(p.octaves in 1..8) { "octaves must be between 1 and 8" }
    require(p.turns in 0f..6f) { "turns must be between 0 and 6" }
    require(p.swirl in 0f..4f) { "swirl must be between 0 and 4" }
    require(p.sep in 4f..200f) { "sep must be between 4 and 200" }
    require(p.step in 0.25f..8f) { "step must be between 0.25 and 8" }
    require(p.steps in 10..4000) { "steps must be between 10 and 4000" }
    require(p.seedEvery in 1..200) { "seedevery must be between 1 and 200" }
    require(p.pitch in 2f..400f) { "pitch must be between 2 and 400" }
    require(p.len in 2f..400f) { "len must be between 2 and 400" }
    require(p.lenVar in 0f..1.5f) { "lenvar must be between 0 and 1.5" }
    require(p.lenScale in 0.00005f..0.01f) { "lenscale must be between 0.00005 and 0.01" }
    require(p.thick in 1f..400f) { "thick must be between 1 and 400" }
    require(p.taper in 0f..1f) { "taper must be between 0 and 1" }
    require(p.stroke in 0.25f..20f) { "stroke must be between 0.25 and 20" }
    require(p.knock in 0f..1f) { "knock must be between 0 and 1" }
    require(p.fill >= 1) { "fill must be at least 1" }
    require(p.accent >= 1) { "accent must be at least 1" }
    // 2 max because thats how many slip directions plateAngles defines
    require(p.plates in 0..2) { "plates must be between 0 and 2" }
    require(p.plateOff in 0f..40f) { "plateoff must be between 0 and 40" }
    // pal is checked at its own declaration, not here - see the note there
    require(p.plateA in -1..RetroColors.allColors.lastIndex) { "platea must be -1 or a RetroColors index" }
    require(p.plateB in -1..RetroColors.allColors.lastIndex) { "plateb must be -1 or a RetroColors index" }
    require(p.caGens in 0..200) { "cagens must be between 0 and 200" }
    require(p.caRadius in 4f..400f) { "caradius must be between 4 and 400" }
    require(p.caThresh >= 1) { "cathresh must be at least 1" }
    require(p.caRefrac in 1..40) { "carefrac must be between 1 and 40" }
    require(p.caBand >= 1) { "caband must be at least 1" }
    require(p.dof in 0f..1f) { "dof must be between 0 and 1" }
    require(p.focus in 0f..1f) { "focus must be between 0 and 1" }
    require(p.depthScale in 0.00005f..0.01f) { "depthscale must be between 0.00005 and 0.01" }
    require(p.blur in 0f..60f) { "blur must be between 0 and 60" }
    require(p.haze in 0f..1f) { "haze must be between 0 and 1" }
    require(p.spread in 0f..8f) { "spread must be between 0 and 8" }
    require(p.persp in 0f..0.6f) { "persp must be between 0 and 0.6" }
    require(p.depthSort in 0..1) { "depthsort must be 0 or 1" }

    return p
}

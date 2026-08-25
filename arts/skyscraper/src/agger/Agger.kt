package agger

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.chromaOf
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.pixels.boxDownsample
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathEffect
import org.jetbrains.skia.RRect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * agger - an interchange from straight above. the romans rammed earth into a causeway and
 * called it agger; ours are poured concrete and stacked five decks high. looked at from a
 * plane the whole thing flattens back into a drawing - pale ribbons over dark ground - and
 * the only thing that still gives the stacking away is the shadows: every deck throws a
 * stripe down sun-side, and the higher the deck the further its stripe drifts. so thats the
 * picture. flat inks, no outlines, elevation carried entirely by how far a shadow sits from
 * its road.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("agger", W, H)
    println(gart)

    val t0 = System.currentTimeMillis()
    val way = colourway(PAL)
    val gv = Gartvas(Dimension(GW, GH))
    val c = gv.canvas

    ground(c, way)
    val net = network(way)
    var cars = 0
    net.forEachIndexed { i, rd ->
        shadow(c, rd, i, way)
        if (!rd.ghost) {
            pave(c, rd, way)
            cars += traffic(c, rd, way)
        }
    }
    println("seed=$SEED pal=$PAL ${net.size} roads (${net.count { it.ghost }} ghost), $cars vehicles, ${System.currentTimeMillis() - t0}ms")

    val g = gart.gartvas()
    val map = Gartmap(g.d)
    boxDownsample(Gartmap(gv).pixels, SS, map)
    map.drawToCanvas(g)
    if (VIG > 0f) g.canvas.drawVignette(g.d, VIG)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// knobss

private const val W = 1536
private const val H = 1024

private val SEED = pi("seed", 13)                 // 13 threads the ribbons past the loop and leaves the middle open
private val OUT = ps("out", "agger")
private val SS = pi("ss", 3, 1..4)
private val PAL = pi("pal", 35, 1..181)           // a cool palette. darks go ground, lights go tarmac, the loudest colour goes to the cars. 35 is cobalt and peach, 31 the quiet petrol one, 177 the green (seed 46 at traffic 1 is agger2)

private val ROADS = pi("roads", 8, 2..16)
private val ARCS = pf("arcs", 0.6f, 0f..1f)       // share of roads that curve, the rest run straight
private val LANEW = pf("lanew", 20f, 9f..30f)     // lane width px, the gauge - road, paint and cars all scale off it. 15 goes wiry, 23 starts to crowd
private val CURL = pf("curl", 1f, 0.3f..3f)       // arc radius factor, under 1 winds tighter
private val LOOP = pf("loop", 0.65f, 0f..1f)      // chance one arc closes into a little loop ramp inside the frame
private val GHOSTS = pi("ghosts", 2, 0..6)        // shadow-only decks, cast by structure outside the picture
private val GROUND = pf("ground", 0f, -1f..1f)    // push the ground darker (-) or lighter (+) off the palettes darkest

private val TRAFFIC = pf("traffic", 0.55f, 0f..3f)   // how full the roads are, 0 empties them

private val SHAZ = pf("shaz", 62f, 0f..360f)      // where shadows fall, degrees. 62 is down-right
private val SHSTEP = pf("shstep", 26f, 0f..80f)   // px of shadow drift per deck of elevation. this is the whole depth cue
private val SHADE = pf("shade", 0.38f, 0f..1f)    // how dark a shadow multiplies

private val VIG = pf("vig", 0.16f, 0f..1.4f)
private val GRAIN = pf("grain", 0.035f, 0f..1f)

// plumbing

private val GW = W * SS
private val GH = H * SS
private val S = SS.toFloat()                      // px knobs are quoted at 1x, drawn at SSx
private val EXT = hypot(GW.toFloat(), GH.toFloat())
private val rng = Random(SEED)
private val DEG = (PI / 180).toFloat()
private val SHX = cos(SHAZ * DEG)                 // the sun direction, every shadow slides this way
private val SHY = sin(SHAZ * DEG)

// the map of the world

private class Way(
    val ground: Int, val shades: IntArray, val tarmac: IntArray, val accent: Int,
    val mark: Int, val grass: Int, val gunmetal: Int, val pool: IntArray,
    val umbra: Int
)

private fun colourway(n: Int): Way {
    val p = Palettes.coolPalette(n)
    val cols = p.toIntArray().sortedBy { lumOf(it) }
    val nd = max(2, cols.size / 2)
    val darks = cols.take(nd)
    val lights = cols.drop(nd).ifEmpty { listOf(0xFFE8E2D4.toInt()) }
    val dark2 = darks[min(1, nd - 1)]
    var ground = darks[0]
    ground = if (GROUND < 0f) lerpColor(ground, 0xFF06080C.toInt(), -GROUND) else lerpColor(ground, 0xFFF2EEE4.toInt(), GROUND * 0.7f)
    // near-ground tones for the big field arcs. keep them a whisper off the ground or they
    // start reading as roads that lost their paint
    val shades = intArrayOf(
        colorScale(ground, 1.07f), colorScale(ground, 0.93f),
        lerpColor(dark2, ground, 0.72f),
        lerpColor(ground, lights[0], 0.07f),
    )
    // tarmac leans to paper, the palette keeps its say through the ground and the cars.
    // full-chroma decks looked like a toy racetrack. lights are already sorted dim to bright
    val tarmac = IntArray(lights.size) { i ->
        val rank = if (lights.size < 2) 1f else i.toFloat() / (lights.size - 1)
        lerpColor(lights[i], 0xFFF6F2E8.toInt(), 0.35f + 0.4f * rank)
    }
    return Way(
        ground, shades, tarmac,
        accent = cols.maxBy { chromaOf(it) },
        mark = lighten(lights.last(), 0.55f),
        grass = lerpColor(dark2, ground, 0.3f),
        gunmetal = lerpColor(darks[0], 0xFF1A1E26.toInt(), 0.5f),
        pool = cols.toIntArray(),
        umbra = lerpColor(Color.WHITE, lerpColor(darks[0], 0xFF141C2C.toInt(), 0.55f), SHADE),
    )
}

// a car the same tone as its deck disappears, so anything too close gets pushed off it
private fun offGround(col: Int, under: Int): Int {
    if (abs(lumOf(col) - lumOf(under)) > 42f) return col
    return if (lumOf(under) > 128f) lerpColor(col, 0xFF23272E.toInt(), 0.45f) else lerpColor(col, 0xFFF2EFE6.toInt(), 0.5f)
}

// the network of roads

// a straight runs off the frame both ways, an arc is a big circle hung off a point, a loop a
// small one that closes inside the picture
private enum class Kind { STRAIGHT, ARC, LOOP }

private class Road(
    val kind: Kind, val cx: Float, val cy: Float, val r: Float,
    val ux: Float, val uy: Float,
    val lanes: Int, val oneWay: Boolean, val flip: Boolean, val median: Boolean,
    val edges: Boolean, val dashes: Boolean,
    val ghost: Boolean, val wf: Float, val dens: Float,
) {
    var rungs = false
    var surface = 0
    val circle = kind != Kind.STRAIGHT
    val loop = kind == Kind.LOOP
    val lw = LANEW * S
    val med = if (median) lw * 0.9f else 0f
    val perSide = lanes / 2                       // two-way only, one-way roads never ask
    val halfw = (if (oneWay) lanes * lw / 2f else perSide * lw + med / 2f) * wf + lw * 0.45f
}

private fun network(way: Way): List<Road> {
    val nArc = (ROADS * ARCS).roundToInt().let { if (ROADS >= 3) it.coerceIn(1, ROADS - 1) else it.coerceIn(0, ROADS) }
    val nStr = ROADS - nArc
    val kinds = MutableList(ROADS) { if (it < nArc) Kind.ARC else Kind.STRAIGHT }
    if (nArc > 0 && rng.nextFloat() < LOOP) kinds[rng.nextInt(nArc)] = Kind.LOOP
    kinds.shuffle(rng)
    // straights fan over the half circle so two dont end up running parallel by accident
    val angles = MutableList(max(nStr, 1)) { k ->
        (k + 0.5f) / max(nStr, 1) * 180f + (rng.nextFloat() - 0.5f) * 110f / max(nStr, 1)
    }
    angles.shuffle(rng)
    var si = 0
    val roads = ArrayList<Road>()
    for (k in kinds) roads += road(k, if (k == Kind.STRAIGHT) angles[si++] else 0f, false)
    repeat(GHOSTS) {
        // ghosts ride the upper half of the stack, a low unexplained shadow looks like a stain
        val at = roads.size / 2 + rng.nextInt(roads.size - roads.size / 2 + 1)
        roads.add(at, road(if (rng.nextFloat() < 0.6f) Kind.ARC else Kind.STRAIGHT, rng.nextFloat() * 180f, true))
    }
    if (rng.nextFloat() < 0.3f) roads.lastOrNull { !it.ghost && it.lanes >= 4 }?.rungs = true
    surfaces(roads, way)
    return roads
}

// pale decks up top, ground level keeps the deeper tarmac
private fun surfaces(roads: List<Road>, way: Way) {
    val normals = roads.filter { !it.ghost }
    val nl = way.tarmac.size
    normals.forEachIndexed { i, rd ->
        val rank = if (normals.size < 2) 1f else i.toFloat() / (normals.size - 1)
        val idx = (rank * (nl - 1) + (rng.nextFloat() - 0.5f) * 1.4f).roundToInt().coerceIn(0, nl - 1)
        var col = way.tarmac[idx]
        if (i == normals.size - 1) col = lighten(col, 0.22f)
        rd.surface = offGround(col, way.ground)
    }
}

// kao jedan put
private fun road(kind: Kind, ang: Float, ghost: Boolean): Road {
    val tx = GW * (0.12f + 0.76f * rng.nextFloat())
    val ty = GH * (0.12f + 0.76f * rng.nextFloat())
    val loop = kind == Kind.LOOP
    val oneWay = loop || rng.nextFloat() < 0.25f
    val lanes = when {
        ghost -> 4 + 2 * rng.nextInt(2)
        loop -> if (rng.nextFloat() < 0.7f) 1 else 2
        oneWay -> 2 + rng.nextInt(2)
        else -> {
            val u = rng.nextFloat()
            if (u < 0.55f) 2 else if (u < 0.88f) 4 else 6
        }
    }
    val median = !oneWay && !ghost && lanes >= 4 && rng.nextFloat() < 0.4f
    // there was a loud painted centre line here once. its gone - it shouted over everything -
    // but its dice still roll, drop them and the deal shifts under every pinned seed
    if (!ghost) {
        if (!oneWay && !median) rng.nextFloat()
        else if (oneWay && !loop && rng.nextFloat() < 0.35f) rng.nextInt(lanes + 1)
    }
    val wf = if (ghost) 1f + rng.nextFloat() * 0.5f else 1f
    // plenty of roads carry no paint at all, a bare ribbon with one coloured line is the look
    val edges = rng.nextFloat() < 0.55f
    val dashes = rng.nextFloat() < 0.65f
    val dens = 0.35f + 1.05f * rng.nextFloat()
    // the spine: a direction through the target point, or a circle hung off it
    val cx: Float
    val cy: Float
    val r: Float
    val ux: Float
    val uy: Float
    when (kind) {
        Kind.STRAIGHT -> {
            val a = ang * DEG
            cx = tx
            cy = ty
            r = 0f
            ux = cos(a); uy = sin(a)
        }
        Kind.LOOP -> {
            r = (85f + 130f * rng.nextFloat()) * min(CURL, 1.5f) * S
            cx = GW * (0.28f + 0.44f * rng.nextFloat())
            cy = GH * (0.28f + 0.44f * rng.nextFloat())
            ux = 0f; uy = 0f
        }

        Kind.ARC -> {
            r = exp(ln(420f) + (ln(2400f) - ln(420f)) * rng.nextFloat()) * CURL * S
            val d = rng.nextFloat() * TAUf
            cx = tx + cos(d) * r
            cy = ty + sin(d) * r
            ux = 0f; uy = 0f
        }
    }
    return Road(
        kind,
        cx, cy, r, ux, uy,
        lanes, oneWay,
        rng.nextBoolean(),
        median, edges, dashes,
        ghost, wf, dens
    )
}

// laying the roads ------

// everything on a road is the same spine stroked at some sideways offset. for a circle the
// offset is just a radius change, so lane lines come out concentric for free
private fun spine(c: Canvas, rd: Road, o: Float, p: Paint) {
    if (rd.circle) {
        if (rd.r + o > 1f) c.drawCircle(rd.cx, rd.cy, rd.r + o, p)
    } else {
        val nx = -rd.uy
        val ny = rd.ux
        c.drawLine(rd.cx - rd.ux * EXT + nx * o, rd.cy - rd.uy * EXT + ny * o, rd.cx + rd.ux * EXT + nx * o, rd.cy + rd.uy * EXT + ny * o, p)
    }
}

// simple shadow
private fun shadow(c: Canvas, rd: Road, elev: Int, way: Way) {
    val d = (3f + elev * SHSTEP) * S
    c.save()
    c.translate(SHX * d, SHY * d)
    val p = strokeOf(way.umbra, rd.halfw * 2f + 3f * S).apply { blendMode = BlendMode.MULTIPLY }
    spine(c, rd, 0f, p)
    c.restore()
}

private fun pave(c: Canvas, rd: Road, way: Way) {
    spine(c, rd, 0f, strokeOf(rd.surface, rd.halfw * 2f))
    // paint scales with the gauge so a fat road doesnt end up pinstriped
    val lw = rd.lw
    if (rd.edges) {
        val p = strokeOf(way.mark.alpha(0xE8), lw * 0.115f)
        spine(c, rd, rd.halfw - lw * 0.30f, p)
        spine(c, rd, -(rd.halfw - lw * 0.30f), p)
    }
    if (rd.dashes) {
        val p = dashed(way.mark.alpha(0xD8), lw * 0.115f, lw * 0.6f, lw * 0.87f, 22f)
        if (rd.oneWay) for (k in 1 until rd.lanes) spine(c, rd, (k - rd.lanes / 2f) * lw, p)
        else for (k in 1 until rd.perSide) {
            spine(c, rd, rd.med / 2f + k * lw, p)
            spine(c, rd, -(rd.med / 2f + k * lw), p)
        }
    }
    if (rd.median) spine(c, rd, 0f, strokeOf(way.grass, rd.med))
    // a dash as wide as the deck is a stripe across it - rungs, and the road reads as a bridge
    if (rd.rungs) spine(c, rd, 0f, dashed(way.mark.alpha(0x55), rd.halfw * 2f * 0.86f, lw * 0.16f, lw * 2.1f, 10f))
}

// dashed stroke, phase rolled so no two roads dash in step. phase is a 1x px knob like the rest
private fun dashed(col: Int, w: Float, on: Float, off: Float, phase: Float) = strokeOf(col, w).apply {
    pathEffect = PathEffect.makeDash(floatArrayOf(on, off), rng.nextFloat() * phase * S)
}

// the ground ------

private fun ground(c: Canvas, way: Way) {
    c.clear(way.ground)
    // big lazy arcs in near-ground tones, the interchange echoed in the fields under it
    repeat(3 + rng.nextInt(4)) {
        val tx = GW * rng.nextFloat()
        val ty = GH * rng.nextFloat()
        val r = (500f + 2200f * rng.nextFloat()) * S
        val d = rng.nextFloat() * TAUf
        val p = strokeOf(way.shades[rng.nextInt(way.shades.size)], (170f + 480f * rng.nextFloat()) * S)
        c.drawCircle(tx + cos(d) * r, ty + sin(d) * r, r, p)
    }
}

// -- the traffic: cars and trucks --

private enum class Vehicle { CAR, TRUCK, BUS }

private fun traffic(c: Canvas, rd: Road, way: Way): Int {
    if (TRAFFIC <= 0f) return 0
    val umbra = fillOf(way.umbra).apply { blendMode = BlendMode.MULTIPLY }
    val offs = ArrayList<Float>()
    if (rd.oneWay) for (k in 0 until rd.lanes) offs += (k - (rd.lanes - 1) / 2f) * rd.lw
    else for (k in 0 until rd.perSide) {
        offs += rd.med / 2f + (k + 0.5f) * rd.lw
        offs += -(rd.med / 2f + (k + 0.5f) * rd.lw)
    }
    var n = 0
    for (o in offs) {
        val sgn = (if (rd.oneWay) 1f else if (o > 0f) 1f else -1f) * (if (rd.flip) -1f else 1f)
        if (rng.nextFloat() < 0.18f) continue   // the empty lane is what makes the jammed one tell
        val mean = (6f / (TRAFFIC * rd.dens * (0.4f + 0.9f * rng.nextFloat()))).coerceIn(0.6f, 40f)
        n += lane(c, rd, o, sgn, mean, way, umbra)
    }
    return n
}

private fun lane(c: Canvas, rd: Road, o: Float, sgn: Float, mean: Float, way: Way, umbra: Paint): Int {
    val rl = if (rd.circle) rd.r + o else 0f
    if (rd.circle && rl < 30f * S) return 0
    val total = if (rd.circle) TAUf * rl else 2f * EXT
    val a0 = rng.nextFloat() * TAUf
    val pad = 60f * S
    var cur = rng.nextFloat() * 60f * S
    var platoon = 0
    var n = 0
    while (true) {
        val u = rng.nextFloat()
        val kind = if (rd.loop || u > 0.17f) Vehicle.CAR else if (u < 0.10f) Vehicle.TRUCK else Vehicle.BUS
        // vehicles wear the lane, so they fatten with the gauge too
        val wid = (when (kind) {
            Vehicle.CAR -> 0.53f + 0.08f * rng.nextFloat()
            Vehicle.TRUCK -> 0.64f
            Vehicle.BUS -> 0.60f
        }) * rd.lw
        val len = when (kind) {
            Vehicle.CAR -> wid * (2.0f + 0.4f * rng.nextFloat())
            Vehicle.TRUCK -> rd.lw * (2.25f + 0.5f * rng.nextFloat())
            Vehicle.BUS -> rd.lw * (1.75f + 0.25f * rng.nextFloat())
        }
        // gaps breathe with an exponential, and every so often a platoon bunches up nose to tail
        val gap = if (platoon > 0) {
            platoon--
            len * (0.22f + 0.3f * rng.nextFloat())
        } else {
            if (rng.nextFloat() < 0.10f) platoon = 2 + rng.nextInt(5)
            len * (0.3f + mean * -ln(1f - rng.nextFloat()))
        }
        cur += gap + len / 2f
        if (cur > total) break
        val oj = o + (rng.nextFloat() - 0.5f) * 0.12f * rd.lw
        val x: Float
        val y: Float
        var deg: Float
        if (rd.circle) {
            val a = a0 + cur / rl
            x = rd.cx + cos(a) * (rd.r + oj)
            y = rd.cy + sin(a) * (rd.r + oj)
            deg = a / DEG + 90f * sgn
        } else {
            val t = cur - EXT
            val nx = -rd.uy
            val ny = rd.ux
            x = rd.cx + rd.ux * t + nx * oj
            y = rd.cy + rd.uy * t + ny * oj
            deg = atan2(rd.uy * sgn, rd.ux * sgn) / DEG
        }
        deg += (rng.nextFloat() - 0.5f) * 4f
        if (x > -pad && x < GW + pad && y > -pad && y < GH + pad) {
            vehicle(c, x, y, deg, kind, len, wid, way, rd.surface, umbra)
            n++
        }
        cur += len / 2f
    }
    return n
}

private fun carColour(way: Way, under: Int): Int {
    val u = rng.nextFloat()
    val col = when {
        u < 0.28f -> lighten(way.mark, 0.5f)
        u < 0.42f -> way.gunmetal
        u < 0.54f -> lighten(way.accent, rng.nextFloat() * 0.25f)
        else -> lighten(way.pool[rng.nextInt(way.pool.size)], 0.15f)
    }
    return offGround(col, under)
}

private val bodyP = paint() // reuse trick

// one rounded slab of a vehicle, in body space: x runs along the road
private fun box(c: Canvas, x: Float, y: Float, w: Float, h: Float, rr: Float, col: Int) {
    bodyP.color = col
    c.drawRRect(RRect.makeXYWH(x, y, w, h, rr), bodyP)
}

private fun vehicle(c: Canvas, x: Float, y: Float, deg: Float, kind: Vehicle, len: Float, wid: Float, way: Way, under: Int, umbra: Paint) {
    c.save()
    c.translate(x + SHX * wid * 0.31f, y + SHY * wid * 0.31f)
    c.rotate(deg)
    c.drawRRect(RRect.makeXYWH(-len / 2f, -wid / 2f, len, wid, wid * 0.28f), umbra)
    c.restore()
    c.save()
    c.translate(x, y)
    c.rotate(deg)
    val col = carColour(way, under)
    val glass = lerpColor(col, 0xFF10161C.toInt(), 0.72f)
    when (kind) {
        Vehicle.CAR -> { // body, windshield, rear glass
            box(c, -len / 2f, -wid / 2f, len, wid, wid * 0.30f, col)
            box(c, len * 0.06f, -wid * 0.36f, len * 0.20f, wid * 0.72f, wid * 0.14f, glass)
            box(c, -len * 0.31f, -wid * 0.33f, len * 0.13f, wid * 0.66f, wid * 0.12f, glass)
        }

        Vehicle.TRUCK -> { // pale trailer, cab, its windshield
            box(c, -len / 2f, -wid / 2f, len * 0.70f, wid, 1.4f * S, offGround(lerpColor(Color.WHITE, way.mark, 0.45f), under))
            box(c, len * 0.26f, -wid * 0.46f, len * 0.22f, wid * 0.92f, wid * 0.20f, col)
            box(c, len * 0.40f, -wid * 0.36f, len * 0.05f, wid * 0.72f, 1f * S, glass)
        }

        Vehicle.BUS -> { // body, lighter roof strip, windshield
            box(c, -len / 2f, -wid / 2f, len, wid, wid * 0.26f, col)
            box(c, -len * 0.34f, -wid * 0.17f, len * 0.62f, wid * 0.34f, wid * 0.12f, lighten(col, 0.22f))
            box(c, len * 0.38f, -wid * 0.35f, len * 0.07f, wid * 0.70f, 1f * S, glass)
        }
    }
    c.restore()
}


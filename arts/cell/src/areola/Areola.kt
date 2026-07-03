package areola

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.lighten
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01 as seededHash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * AREOLA. Where NERVURE drew the
 * veins, AREOLA draws the islands the veins enclose.
 */

private const val W = 1200
private const val H = 1200
private val HALF_PI = (PI / 2).toFloat()
private val PIf = PI.toFloat()

// io / determinism
private val SEED = pi("seed", 7).toLong()
private val OUT = ps("out", "areola")
private val SS = pi("ss", 2)                        // supersample; engine in logical px, raster in GW*GH
private val GW = W * SS
private val GH = H * SS
private val NZOFF = (SEED and 0xffff) * 0.01f       // SimplexNoise is seedless -> shift its sample window by the seed

// crack engine (logical px)
private val FIELD_S = pf("fields", 0.0016f)         // primary stress-orientation frequency
private const val FIELD_S2 = 0.0051f                // secondary orientation frequency
private val CURL = pf("curl", 0.12f)                // curvature-noise amplitude (rad)
private const val CURL_S = 0.004f                   // curvature-noise frequency
private const val WOBBLE = 0.04f                    // residual per-step angular jitter (rad)
private val STEP = pf("step", 3f)                   // crack step length
private const val R_TURN = 42f                      // engine spatial-hash cell size
private val R_FEEL = pf("feel", 18f)                // free-surface radius: a head only bends to meet a wall this close
private val TURN_GAIN = pf("turn", 1.1f)            // how hard a head bends to meet an old crack square-on
private const val SNAP_EPS = 3.2f                   // T-junction snap distance (>= STEP, so heads cannot tunnel)
private const val SLIVER_MIN = 7f                   // beside-not-ahead near neighbour -> early snap (kills slivers)
private const val NODE_MERGE = 4f                   // weld a snap onto an existing junction within this
private const val SEED_JIT = 0.25f                  // nucleus orientation jitter (rad)
private val N_CAND = pi("cand", 150)               // darts per nucleation (largest-empty-disc)
private const val HOMING_STEPS = 90                 // extra steps a stalled head may march straight to close
private const val MARGIN = 8f                       // keep nuclei this far off frame
private val LEN_GAIN = pf("lengain", 2.4f)          // crack max length as a multiple of nucleus clearance
private val CRACK_BUDGET = pi("budget", 5000)       // safety cap; the graded stop usually halts well before this

// the graded fineness that crowds small cells against the main lines
private val FINE = pf("fine", 6f)                   // min plate clearance hugging a primary line (small -> tiny cells)
private val COARSE = pf("coarse", 28f)              // min plate clearance far from any line (large -> coarse field)
private val BAND = pf("band", 88f)                  // distance over which fineness relaxes from FINE to COARSE
private const val PRIMARY_GEN = 1                    // cracks of this generation or lower are "main lines"

// extraction
private val MIN_PLATE_AREA = pi("speck", 48)        // dissolve plates smaller than this (SS-px^2) back into fissure
private const val GAPE_MAX = 1.0f                   // gen-0 (primary) crack gape -> widest AO valley
private const val GAPE_MIN = 0.34f                  // finest crack gape
private val CRACK_W = pf("crackw", 1.0f)            // global crack-width multiplier (>=1 SS-px floor keeps it watertight)

// render
private val GRAD_STEPS = pi("steps", 512)
private val DOME_H = pf("dome", 0.72f)              // plate doming strength (slope of the lit tile)
private val AO_W = pf("aow", 3.4f)                  // ambient-occlusion valley width (logical px, scaled by gape)
private val AO_MIN = pf("aomin", 0.48f)             // darkest shade at the fissure floor
private val RIMK = pf("rim", 0.95f)                 // bevel contrast (Lambert rim light/shade)
private val LIGHT_JIT = pf("ljit", 0.07f)           // per-plate lightness jitter so equal-tone neighbours separate
private val NOISE_F = pf("noisef", 0.003f)          // plate base-colour noise frequency (lower = broader colour regions)
private val VIG = pf("vig", 0.0f)                   // vignette strength multiplier
private val GRAIN = pf("grain", 0.02f)              // fine surface grain

// light unit vector, screen coords (y down): +deg => up, 135 = upper-left
private val LIGHT = pf("light", 135f) * PIf / 180f
private val LX = cos(LIGHT)
private val LY = -sin(LIGHT)
private val LZ = pf("lz", 0.72f)

// palettes
private val COOL = pi("cool", 85)
private val RAMP: Palette = Palettes.coolPalette(COOL).expand(GRAD_STEPS)

// fissure floor: the palette's OWN darkest tone, sunk a little further, so the cracks live in its world
private val VOID = run {
    var dark = RAMP.safe(0)
    var dl = lumOf(dark)
    for (i in 0 until GRAD_STEPS) {
        val c = RAMP.safe(i)
        val l = lumOf(c); if (l < dl) {
            dl = l; dark = c
        }
    }
    darken(dark, 0.45f)
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("areola", W, H)
    println(gart)
    println("seed=$SEED ss=$SS cool=$COOL fine=$FINE coarse=$COARSE band=$BAND budget=$CRACK_BUDGET")

    val g = gart.gartvas()
    val c = g.canvas

    var t0 = System.currentTimeMillis()
    val net = Net()
    net.build(SEED)
    println("cracks: ${net.crackCount}  segments: ${net.segs.size}  in ${System.currentTimeMillis() - t0}ms")

    t0 = System.currentTimeMillis()
    val field = Extraction(net)
    println("plates: ${field.alivePlates}/${field.plateCount}  in ${System.currentTimeMillis() - t0}ms")

    t0 = System.currentTimeMillis()
    val mapMain = Gartmap(g.d)
    shadePlates(field, mapMain)
    mapMain.drawToCanvas(g)
    c.drawVignette(g.d, VIG)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED.toInt())
    println("render in ${System.currentTimeMillis() - t0}ms")

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// ENGINE
// sequential crack insertion in logical px. one Random(SEED), single thread.

private class Seg(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val crack: Int)

/** result of a proximity query: the nearest point on a foreign crack and how far it is. */
private class Hit(val fx: Float, val fy: Float, val dist: Float)

private class Net {
    val segs = ArrayList<Seg>(1 shl 16)
    val crackGape = ArrayList<Float>()
    val crackWidth = ArrayList<Float>()
    var crackCount = 0; private set

    private val cell = R_TURN
    private val cols = ceil(W / cell).toInt()
    private val rows = ceil(H / cell).toInt()
    private val grid = Array(cols * rows) { ArrayList<Int>(4) }   // all crack segments
    private val pgrid = Array(cols * rows) { ArrayList<Int>(2) }  // primary (main-line) segments only
    private var clrFirst = -1f

    private fun cix(x: Float) = (x / cell).toInt().coerceIn(0, cols - 1)
    private fun ciy(y: Float) = (y / cell).toInt().coerceIn(0, rows - 1)

    private fun addSeg(s: Seg, primary: Boolean) {
        val idx = segs.size
        segs.add(s)
        val cx0 = cix(min(s.x0, s.x1))
        val cx1 = cix(max(s.x0, s.x1))
        val cy0 = ciy(min(s.y0, s.y1))
        val cy1 = ciy(max(s.y0, s.y1))
        for (cy in cy0..cy1) for (cx in cx0..cx1) {
            grid[cy * cols + cx].add(idx)
            if (primary) pgrid[cy * cols + cx].add(idx)
        }
    }

    /** nearest point on any crack != [ignore] within the 3x3 cell block; null if none in reach. */
    private fun nearestSeg(x: Float, y: Float, ignore: Int): Hit? {
        val gx = cix(x)
        val gy = ciy(y)
        var bestD2 = Float.MAX_VALUE
        var bestIdx = -1
        var cy = max(0, gy - 1)
        while (cy <= min(rows - 1, gy + 1)) {
            var cx = max(0, gx - 1)
            while (cx <= min(cols - 1, gx + 1)) {
                for (i in grid[cy * cols + cx]) {
                    if (segs[i].crack == ignore) continue
                    val d2 = footDist2(x, y, segs[i])
                    if (d2 < bestD2) {
                        bestD2 = d2; bestIdx = i
                    }
                }
                cx++
            }
            cy++
        }
        if (bestIdx < 0) return null
        val f = footOnSeg(x, y, segs[bestIdx])
        return Hit(f.x, f.y, sqrt(bestD2))
    }

    /** nearest crack of ANY id within [cap], ring-expanding the hash; null if none. carries the foot point. */
    private fun nearestRing(x: Float, y: Float, cap: Float): Hit? {
        val gx = cix(x)
        val gy = ciy(y)
        val maxRing = (cap / cell).toInt() + 1
        var bestD2 = cap * cap
        var bestIdx = -1
        var hitRing = -1
        var ring = 0
        while (ring <= maxRing) {
            var cy = gy - ring
            while (cy <= gy + ring) {
                if (cy in 0 until rows) {
                    var cx = gx - ring
                    while (cx <= gx + ring) {
                        if ((cx == gx - ring || cx == gx + ring || cy == gy - ring || cy == gy + ring) && cx in 0 until cols) {
                            for (i in grid[cy * cols + cx]) {
                                val d2 = footDist2(x, y, segs[i])
                                if (d2 < bestD2) {
                                    bestD2 = d2; bestIdx = i
                                }
                            }
                        }
                        cx++
                    }
                }
                cy++
            }
            if (bestIdx >= 0 && hitRing < 0) hitRing = ring
            if (hitRing in 0..ring - 1) break
            ring++
        }
        if (bestIdx < 0) return null
        val f = footOnSeg(x, y, segs[bestIdx])
        return Hit(f.x, f.y, sqrt(bestD2))
    }

    /** distance to the nearest PRIMARY (main-line) crack, capped at [cap]. drives the fineness gradient. */
    private fun distToPrimary(x: Float, y: Float, cap: Float): Float {
        val gx = cix(x)
        val gy = ciy(y)
        val maxRing = (cap / cell).toInt() + 1
        var bestD2 = cap * cap
        var hitRing = -1
        var ring = 0
        while (ring <= maxRing) {
            var cy = gy - ring
            while (cy <= gy + ring) {
                if (cy in 0 until rows) {
                    var cx = gx - ring
                    while (cx <= gx + ring) {
                        if ((cx == gx - ring || cx == gx + ring || cy == gy - ring || cy == gy + ring) && cx in 0 until cols) {
                            for (i in pgrid[cy * cols + cx]) {
                                val d2 = footDist2(x, y, segs[i])
                                if (d2 < bestD2) bestD2 = d2
                            }
                        }
                        cx++
                    }
                }
                cy++
            }
            val found = bestD2 < cap * cap
            if (found && hitRing < 0) hitRing = ring
            if (hitRing in 0..ring - 1) break
            ring++
        }
        return sqrt(bestD2)
    }

    /** graded minimum plate clearance: FINE hard against a main line, relaxing to COARSE out in the field. */
    private fun minPlateAt(x: Float, y: Float): Float {
        val d = distToPrimary(x, y, BAND)
        return lerp(FINE, COARSE, smoothstep(0f, BAND, d))
    }

    private fun fieldDir(x: Float, y: Float): Vec2 {
        val n = SimplexNoise.noise(x * FIELD_S + NZOFF, y * FIELD_S)
        val m = SimplexNoise.noise(x * FIELD_S2 + 91f, y * FIELD_S2 + 13f)
        val theta = (0.5f + 0.5f * (0.7f * n + 0.3f * m)) * PIf
        return Vec2(cos(theta), sin(theta))
    }

    /** momentum + coherent curl + a last-moment bend to meet a nearby old crack square-on (the T-junction). */
    private fun effectiveDir(px: Float, py: Float, hx: Float, hy: Float, self: Int, rng: Random): Vec2 {
        var d = Vec2(hx, hy)
        val hit = nearestSeg(px, py, self)
        if (hit != null && hit.dist < R_FEEL) {
            val tlen = max(1e-4f, hit.dist)
            val toward = Vec2((hit.fx - px) / tlen, (hit.fy - py) / tlen)
            val w = (smoothstep(R_FEEL, SNAP_EPS, hit.dist) * TURN_GAIN).coerceIn(0f, 1f)
            d = d.rotate(d.angleTo(toward) * w)
        }
        val curl = SimplexNoise.noise(px * CURL_S, py * CURL_S) * CURL + rng.nextDouble(-WOBBLE.toDouble(), WOBBLE.toDouble()).toFloat()
        return d.rotate(curl)
    }

    /** grow one head until it snaps to an older crack or clips the frame. */
    private fun growHead(id: Int, primary: Boolean, sx: Float, sy: Float, dx0: Float, dy0: Float, maxLen: Float, rng: Random) {
        var px = sx
        var py = sy
        var hx = dx0
        var hy = dy0
        var len = 0f
        var homing = false
        val maxSteps = (maxLen / STEP).toInt() + HOMING_STEPS
        var step = 0
        while (step < maxSteps) {
            step++
            val d = if (homing) Vec2(hx, hy) else effectiveDir(px, py, hx, hy, id, rng)
            val nx = px + d.x * STEP
            val ny = py + d.y * STEP

            if (nx < 0f || nx > W || ny < 0f || ny > H) {
                val cl = clipToFrame(px, py, nx, ny)
                addSeg(Seg(px, py, cl.x, cl.y, id), primary); return
            }
            val hit = nearestSeg(nx, ny, id)
            if (hit != null && hit.dist < SNAP_EPS) {
                val foot = weld(hit, id)
                addSeg(Seg(px, py, foot.x, foot.y, id), primary); return
            }
            if (hit != null && hit.dist < SLIVER_MIN) {
                val ax = (hit.fx - px)
                val ay = (hit.fy - py)
                val al = max(1e-4f, hypot(ax, ay))
                if ((ax / al) * hx + (ay / al) * hy < 0.5f) {
                    addSeg(Seg(px, py, hit.fx, hit.fy, id), primary); return
                }
            }
            addSeg(Seg(px, py, nx, ny, id), primary)
            hx = (nx - px); hy = (ny - py)
            val hl = max(1e-4f, hypot(hx, hy)); hx /= hl; hy /= hl
            px = nx; py = ny; len += STEP
            if (!homing && len >= maxLen) {
                homing = true
                val b = nearestBoundary(px, py)
                hx = b.x - px; hy = b.y - py
                val bl = max(1e-4f, hypot(hx, hy)); hx /= bl; hy /= bl
            }
        }
        val b = nearestBoundary(px, py)
        addSeg(Seg(px, py, b.x, b.y, id), primary)
    }

    private fun weld(hit: Hit, self: Int): Point {
        val near = nearestSeg(hit.fx, hit.fy, self) ?: return Point(hit.fx, hit.fy)
        return if (near.dist < NODE_MERGE) Point(near.fx, near.fy) else Point(hit.fx, hit.fy)
    }

    private fun nearestBoundary(x: Float, y: Float): Point {
        val border = min(min(x, W - x), min(y, H - y))
        val hit = nearestSeg(x, y, -999)
        if (hit != null && hit.dist <= border) return Point(hit.fx, hit.fy)
        val dl = x
        val dr = W - x
        val dt = y
        val db = H - y
        val m = min(min(dl, dr), min(dt, db))
        return when (m) {
            dl -> Point(0f, y); dr -> Point(W.toFloat(), y); dt -> Point(x, 0f); else -> Point(x, H.toFloat())
        }
    }

    private fun clearance(x: Float, y: Float): Float {
        val border = min(min(x, W - x), min(y, H - y))
        if (segs.isEmpty()) return border
        return min(border, nearestRing(x, y, border)?.dist ?: border)
    }

    /** the heart of the look: pick the dart with the most room ABOVE its local minimum. open-field gaps stop early
     *  (COARSE), gaps hugging a main line keep going (FINE) -> small cells crowd the lines. null when nothing exceeds. */
    private fun pickNucleus(rng: Random): Pair<Point, Float>? {
        var bx = 0f
        var by = 0f
        var bestClr = 0f
        var bestScore = -1e9f
        repeat(N_CAND) {
            val x = rng.nextDouble(MARGIN.toDouble(), (W - MARGIN).toDouble()).toFloat()
            val y = rng.nextDouble(MARGIN.toDouble(), (H - MARGIN).toDouble()).toFloat()
            val clr = clearance(x, y)
            val score = clr - minPlateAt(x, y)
            if (score > bestScore) {
                bestScore = score; bestClr = clr; bx = x; by = y
            }
        }
        return if (bestScore <= 0f) null else Point(bx, by) to bestClr
    }

    fun build(seed: Long) {
        val rng = Random(seed)
        while (crackCount < CRACK_BUDGET) {
            val (at, clr) = pickNucleus(rng) ?: break
            val x0 = at.x
            val y0 = at.y
            if (clrFirst < 0f) clrFirst = clr
            val id = crackCount++
            val gen = if (clrFirst <= 0f) 0 else (ln((clrFirst / clr).coerceAtLeast(1f)) / ln(2f)).toInt().coerceIn(0, 3)
            val primary = gen <= PRIMARY_GEN
            val gf = gen / 3f
            crackGape.add(lerp(GAPE_MAX, GAPE_MIN, gf))
            crackWidth.add(lerp(2.0f, 0.58f, gf))    // bold primary faults -> fine hairline secondaries
            // launch ACROSS the plate: toward the nearest existing crack, so the new crack spans wall-to-wall.
            val near = nearestRing(x0, y0, clr * 1.3f + R_TURN)
            val base = if (near != null) {
                val l = max(1e-4f, near.dist); Vec2((near.fx - x0) / l, (near.fy - y0) / l)
            } else fieldDir(x0, y0)
            val jit = rng.nextDouble(-SEED_JIT.toDouble(), SEED_JIT.toDouble()).toFloat()
            val (sx, sy) = base.rotate(jit)
            val maxLen = clr * LEN_GAIN + STEP
            growHead(id, primary, x0, y0, sx, sy, maxLen, rng)        // head A
            growHead(id, primary, x0, y0, -sx, -sy, maxLen, rng)      // head B
        }
    }
}

// EXTRACTION
// rasterize cracks -> watertight mask -> flood-fill plates -> 8SSEDT distance/payload field.

private class Extraction(net: Net) {
    val crack = BooleanArray(GW * GH)                 // true = fissure
    val label = IntArray(GW * GH) { -1 }              // -2 crack, >=0 plate id
    val dx = IntArray(GW * GH)                         // 8SSEDT offset to nearest fissure (SS px)
    val dy = IntArray(GW * GH)
    val gape = FloatArray(GW * GH) { -1f }            // crack gape, then DT-propagated to plate pixels

    var plateCount = 0; private set
    var alivePlates = 0; private set
    lateinit var plateBase: IntArray
    lateinit var plateMaxDist: FloatArray
    lateinit var dead: BooleanArray
    private lateinit var areaArr: IntArray
    private lateinit var cxArr: FloatArray
    private lateinit var cyArr: FloatArray

    init {
        rasterize(net)
        frameWall()
        floodFill()
        speckCull()
        distanceTransform()
        plateColours()
    }

    private fun stampDisc(cx: Int, cy: Int, r: Int, gp: Float) {
        var oy = -r
        while (oy <= r) {
            val yy = cy + oy
            if (yy in 0 until GH) {
                var ox = -r
                while (ox <= r) {
                    if (ox * ox + oy * oy <= r * r) {
                        val xx = cx + ox
                        if (xx in 0 until GW) {
                            val c = yy * GW + xx
                            crack[c] = true
                            if (gp > gape[c]) gape[c] = gp
                        }
                    }
                    ox++
                }
            }
            oy++
        }
    }

    private fun rasterize(net: Net) {
        for (s in net.segs) {
            val gp = net.crackGape[s.crack]
            val r = max(1, ceil(net.crackWidth[s.crack] * CRACK_W * SS * 0.5f).toInt())
            val x0 = s.x0 * SS
            val y0 = s.y0 * SS
            val x1 = s.x1 * SS
            val y1 = s.y1 * SS
            val steps = max(1, ceil(hypot(x1 - x0, y1 - y0)).toInt())
            var k = 0
            while (k <= steps) {
                val t = k.toFloat() / steps
                stampDisc((x0 + (x1 - x0) * t).roundToInt(), (y0 + (y1 - y0) * t).roundToInt(), r, gp)
                k++
            }
        }
    }

    private fun frameWall() {
        for (x in 0 until GW) {
            crack[x] = true; crack[(GH - 1) * GW + x] = true
        }
        for (y in 0 until GH) {
            crack[y * GW] = true; crack[y * GW + GW - 1] = true
        }
    }

    private fun floodFill() {
        val areaL = ArrayList<Int>()
        val sumXL = ArrayList<Long>()
        val sumYL = ArrayList<Long>()
        val stack = IntArray(GW * GH)
        for (start in 0 until GW * GH) {
            if (crack[start]) {
                label[start] = -2; continue
            }
            if (label[start] != -1) continue
            val id = areaL.size
            var a = 0
            var sx = 0L
            var sy = 0L
            var sp = 0
            stack[sp++] = start; label[start] = id
            while (sp > 0) {
                val cur = stack[--sp]
                val cx = cur % GW
                val cy = cur / GW
                a++; sx += cx; sy += cy
                if (cx > 0) {
                    val n = cur - 1; if (label[n] == -1 && !crack[n]) {
                        label[n] = id; stack[sp++] = n
                    }
                }
                if (cx < GW - 1) {
                    val n = cur + 1; if (label[n] == -1 && !crack[n]) {
                        label[n] = id; stack[sp++] = n
                    }
                }
                if (cy > 0) {
                    val n = cur - GW; if (label[n] == -1 && !crack[n]) {
                        label[n] = id; stack[sp++] = n
                    }
                }
                if (cy < GH - 1) {
                    val n = cur + GW; if (label[n] == -1 && !crack[n]) {
                        label[n] = id; stack[sp++] = n
                    }
                }
            }
            areaL.add(a); sumXL.add(sx); sumYL.add(sy)
        }
        plateCount = areaL.size
        areaArr = IntArray(plateCount) { areaL[it] }
        cxArr = FloatArray(plateCount) { sumXL[it].toFloat() / areaArr[it] }
        cyArr = FloatArray(plateCount) { sumYL[it].toFloat() / areaArr[it] }
        dead = BooleanArray(plateCount)
    }

    private fun speckCull() {
        for (id in 0 until plateCount) if (areaArr[id] < MIN_PLATE_AREA) dead[id] = true
        if (dead.none { it }) return
        for (i in 0 until GW * GH) {
            val l = label[i]
            if (l >= 0 && dead[l]) {
                crack[i] = true; label[i] = -2; gape[i] = max(gape[i], 0f)
            }
        }
    }

    /** in-place two-pass 8SSEDT: dx/dy become the offset to the nearest fissure; gape propagates along. */
    private fun distanceTransform() {
        val inf = 20000
        for (i in 0 until GW * GH) {
            if (crack[i]) {
                dx[i] = 0; dy[i] = 0; if (gape[i] < 0f) gape[i] = 0f
            } else {
                dx[i] = inf; dy[i] = inf; gape[i] = 0f
            }
        }
        for (y in 0 until GH) {
            var x = 0
            while (x < GW) {
                val i = y * GW + x
                relax(i, x, y, -1, 0); relax(i, x, y, 0, -1); relax(i, x, y, -1, -1); relax(i, x, y, 1, -1)
                x++
            }
            x = GW - 2
            while (x >= 0) {
                relax(y * GW + x, x, y, 1, 0); x--
            }
        }
        for (y in GH - 1 downTo 0) {
            var x = GW - 1
            while (x >= 0) {
                val i = y * GW + x
                relax(i, x, y, 1, 0); relax(i, x, y, 0, 1); relax(i, x, y, 1, 1); relax(i, x, y, -1, 1)
                x--
            }
            x = 1
            while (x < GW) {
                relax(y * GW + x, x, y, -1, 0); x++
            }
        }
        plateMaxDist = FloatArray(plateCount)
        for (i in 0 until GW * GH) {
            val l = label[i]
            if (l >= 0) {
                val d = sqrt((dx[i] * dx[i] + dy[i] * dy[i]).toFloat())
                if (d > plateMaxDist[l]) plateMaxDist[l] = d
            }
        }
    }

    private fun relax(i: Int, x: Int, y: Int, ox: Int, oy: Int) {
        val nx = x + ox
        val ny = y + oy
        if (nx < 0 || ny < 0 || nx >= GW || ny >= GH) return
        val n = ny * GW + nx
        val cdx = dx[n] + ox
        val cdy = dy[n] + oy
        if (cdx * cdx + cdy * cdy < dx[i] * dx[i] + dy[i] * dy[i]) {
            dx[i] = cdx; dy[i] = cdy; gape[i] = gape[n]
        }
    }

    private fun plateColours() {
        plateBase = IntArray(plateCount)
        var alive = 0
        for (id in 0 until plateCount) {
            if (dead[id]) continue
            alive++
            val cx = cxArr[id] / SS
            val cy = cyArr[id] / SS
            val t = ((SimplexNoise.noise(cx * NOISE_F + NZOFF, cy * NOISE_F) + 1f) * 0.5f).coerceIn(0f, 1f)
            var col = RAMP.safe((t * (GRAD_STEPS - 1)).toInt())
            val j = (seededHash01(id, 7, SEED.toInt()) - 0.5f) * 2f * LIGHT_JIT
            col = if (j >= 0f) lighten(col, j) else darken(col, -j)
            plateBase[id] = col
        }
        alivePlates = alive
    }
}

// RENDER

/** dome every plate into a lit tile, sink AO into the fissures, lift a top-left bevel, downsample. */
private fun shadePlates(f: Extraction, map: Gartmap) {
    val rgb = IntArray(GW * GH)
    val aoScale = AO_W * SS
    for (i in 0 until GW * GH) {
        val l = f.label[i]
        if (l < 0) {
            rgb[i] = VOID; continue
        }
        val maxd = f.plateMaxDist[l].coerceAtLeast(1f)
        val dxv = f.dx[i].toFloat()
        val dyv = f.dy[i].toFloat()
        val dd = sqrt(dxv * dxv + dyv * dyv)
        val t = (dd / maxd).coerceIn(0f, 1f)
        val inv = if (dd > 1e-3f) 1f / dd else 0f
        val tx = dxv * inv
        val ty = dyv * inv                       // unit toward fissure
        val slope = DOME_H * HALF_PI * cos(t * HALF_PI) / maxd
        var nx = slope * tx
        var ny = slope * ty
        var nz = 1f        // dome normal (peak interior, valley at crack)
        val nl = sqrt(nx * nx + ny * ny + nz * nz)
        nx /= nl; ny /= nl; nz /= nl
        val lambert = (nx * LX + ny * LY + nz * LZ).coerceIn(0f, 1f)
        val gp = f.gape[i].coerceAtLeast(0f)
        val ao = smoothstep(0f, aoScale * (0.4f + gp), dd)
        val shade = lerp(AO_MIN, 1f, ao) * lerp(1f - RIMK * 0.5f, 1f, lambert)
        rgb[i] = colorScale(f.plateBase[l], shade)
    }
    downsample(rgb, map)
}

/** box-average the SS grid down to W*H, writing opaque pixels. */
private fun downsample(rgb: IntArray, map: Gartmap) {
    val px = map.pixels
    val inv = 1f / (SS * SS)
    for (y in 0 until H) {
        val by = y * SS
        for (x in 0 until W) {
            val bx = x * SS
            var r = 0
            var gg = 0
            var b = 0
            var yy = 0
            while (yy < SS) {
                var row = (by + yy) * GW + bx
                var xx = 0
                while (xx < SS) {
                    val col = rgb[row]
                    r += (col ushr 16) and 0xFF; gg += (col ushr 8) and 0xFF; b += col and 0xFF
                    row++; xx++
                }
                yy++
            }
            px[y * W + x] = (0xFF shl 24) or ((r * inv).toInt() shl 16) or ((gg * inv).toInt() shl 8) or (b * inv).toInt()
        }
    }
}

// HELPERS, utils etc

/** squared point-to-segment distance, allocation-free (the hot proximity query). */
private fun footDist2(px: Float, py: Float, s: Seg): Float {
    val vx = s.x1 - s.x0
    val vy = s.y1 - s.y0
    val len2 = vx * vx + vy * vy
    val t = if (len2 < 1e-6f) 0f else (((px - s.x0) * vx + (py - s.y0) * vy) / len2).coerceIn(0f, 1f)
    val fx = s.x0 + vx * t
    val fy = s.y0 + vy * t
    val ddx = px - fx
    val ddy = py - fy
    return ddx * ddx + ddy * ddy
}

private fun footOnSeg(px: Float, py: Float, s: Seg): Point {
    val vx = s.x1 - s.x0
    val vy = s.y1 - s.y0
    val len2 = vx * vx + vy * vy
    if (len2 < 1e-6f) return Point(s.x0, s.y0)
    val t = (((px - s.x0) * vx + (py - s.y0) * vy) / len2).coerceIn(0f, 1f)
    return Point(s.x0 + vx * t, s.y0 + vy * t)
}

private fun clipToFrame(x0: Float, y0: Float, x1: Float, y1: Float): Point {
    var tBest = 1f
    val dx = x1 - x0
    val dy = y1 - y0
    if (dx > 1e-6f) tBest = min(tBest, (W - x0) / dx) else if (dx < -1e-6f) tBest = min(tBest, (0 - x0) / dx)
    if (dy > 1e-6f) tBest = min(tBest, (H - y0) / dy) else if (dy < -1e-6f) tBest = min(tBest, (0 - y0) / dy)
    tBest = tBest.coerceIn(0f, 1f)
    return Point(x0 + dx * tBest, y0 + dy * tBest)
}


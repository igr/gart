package lines.vitrali

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.SimplexNoise
import org.jetbrains.skia.*
import kotlin.math.*
import kotlin.random.Random

// vitrall - the sagrada familia stained glass

private val SEED = pi("seed", 4)
private val OUT = ps("out", "vitrall")
private val PRESET = ps("preset", "nou")         // nou | passion | nativity
private val W = pi("w", 1200)
private val H = pi("h", 1500)
private val SS = pi("ss", 2)
private val PANES = pi("panes", 68)              // rough pane count before slicing
private val LEAD = pf("lead", 7f)                // came width, logical px
private val SLICE = pf("slice", 0.20f)           // chance a pane is cut into parallel strips
private val ARC = pf("arc", 0.35f)               // chance a cut bows into a gentle arc
private val DRIFT = pf("drift", 0.42f)           // linear color drift across the panel
private val WARP = pf("warp", 1.35f)             // fbm share in the color field
private val UJIT = pf("ujit", 0.045f)            // per-pane wander along the ramp
private val BIAS = pf("bias", 0.50f)             // field centre: <0.5 cool, >0.5 warm
private val FLOW = ps("flow", "rise")            // rise: warm floor to cool sky | free: seed-driven currents
private val INTRUDE = pf("intrude", 0.07f)       // chance a pane grabs glass from elsewhere
private val EDGE = pf("edge", 1f)                // dark glass rim near the lead
private val GRAIN = pf("grain", 0.015f)
private val VIG = pf("vig", 0.12f)

private val NZV = (SEED and 0xffff) * 0.017f

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("vitrall", W, H)
    println(gart)
    println("seed=$SEED preset=$PRESET flow=$FLOW panes=$PANES lead=$LEAD slice=$SLICE arc=$ARC drift=$DRIFT warp=$WARP")

    val rng = Random(SEED)
    val ssf = SS.toFloat()
    val big = Gartvas(Dimension(W * SS, H * SS))

    var t0 = System.currentTimeMillis()
    val panes = buildPanes(rng)
    println("${panes.size} panes in ${System.currentTimeMillis() - t0}ms")

    t0 = System.currentTimeMillis()
    renderGlass(big.canvas, panes, ssf)
    println("render in ${System.currentTimeMillis() - t0}ms")

    val g = gart.gartvas()
    g.canvas.drawImageRect(
        big.snapshot(),
        Rect(0f, 0f, (W * SS).toFloat(), (H * SS).toFloat()),
        Rect(0f, 0f, W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL, null, true
    )
    g.canvas.drawVignette(g.d, VIG)
    if (GRAIN > 0f) addGrain(g, GRAIN, SEED)

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

// GLASS STOCk
// ramps sampled at u in [0,1], stops as (u, L, C, H) in oklch

private class Ramp(val u: FloatArray, val stops: Array<ColorOKLCH>) {
    fun at(t: Float): ColorOKLCH {
        val x = t.coerceIn(0f, 1f)
        var i = 0
        while (i < u.size - 2 && x > u[i + 1]) i++
        val f = ((x - u[i]) / (u[i + 1] - u[i])).coerceIn(0f, 1f)
        return stops[i].mix(stops[i + 1], f)
    }
}

private fun rampOf(name: String): Ramp = when (name) {
    "nou" -> Ramp(
        floatArrayOf(0f, 0.15f, 0.26f, 0.35f, 0.45f, 0.52f, 0.58f, 0.66f, 0.77f, 0.89f, 1f),
        arrayOf(
            ColorOKLCH(0.30f, 0.10f, 268f),  // navy
            ColorOKLCH(0.46f, 0.17f, 262f),  // cobalt
            ColorOKLCH(0.58f, 0.12f, 232f),  // azure
            ColorOKLCH(0.62f, 0.11f, 198f),  // teal
            ColorOKLCH(0.61f, 0.13f, 152f),  // emerald
            ColorOKLCH(0.73f, 0.14f, 118f),  // lime
            ColorOKLCH(0.86f, 0.16f, 97f),   // gold
            ColorOKLCH(0.72f, 0.17f, 62f),   // amber
            ColorOKLCH(0.59f, 0.20f, 36f),   // vermilion
            ColorOKLCH(0.46f, 0.19f, 27f),   // crimson
            ColorOKLCH(0.33f, 0.12f, 18f),   // maroon
        )
    )
    // passion facade at sunset - all fire
    "passion" -> Ramp(
        floatArrayOf(0f, 0.16f, 0.34f, 0.52f, 0.68f, 0.84f, 1f),
        arrayOf(
            ColorOKLCH(0.93f, 0.09f, 100f),  // white gold
            ColorOKLCH(0.84f, 0.16f, 88f),   // yellow
            ColorOKLCH(0.70f, 0.18f, 60f),   // amber
            ColorOKLCH(0.58f, 0.21f, 34f),   // vermilion
            ColorOKLCH(0.46f, 0.20f, 20f),   // crimson
            ColorOKLCH(0.38f, 0.15f, 355f),  // garnet
            ColorOKLCH(0.27f, 0.09f, 335f),  // deep wine
        )
    )
    // nativity facade at dawn - water and leaves
    "nativity" -> Ramp(
        floatArrayOf(0f, 0.15f, 0.33f, 0.50f, 0.66f, 0.84f, 1f),
        arrayOf(
            ColorOKLCH(0.93f, 0.08f, 105f),  // pale gold
            ColorOKLCH(0.80f, 0.14f, 120f),  // spring green
            ColorOKLCH(0.63f, 0.13f, 150f),  // emerald
            ColorOKLCH(0.60f, 0.11f, 195f),  // teal
            ColorOKLCH(0.50f, 0.15f, 245f),  // azure
            ColorOKLCH(0.40f, 0.16f, 265f),  // cobalt
            ColorOKLCH(0.27f, 0.09f, 275f),  // midnight
        )
    )

    else -> error("unknown preset: $name")
}

// panes

private class Pane(val poly: Poly, val u: Float, val sliceShift: Float)
private class Bone(val pts: FloatArray)

private const val ARCSTEP = 6f                   // arc sampling, logical px

// endpointss of the chord where the infinite cut line crosses the region
private fun chordEnds(q: Poly, px: Float, py: Float, dx: Float, dy: Float): Bone? {
    var tMin = Float.MAX_VALUE
    var tMax = -Float.MAX_VALUE
    for (i in 0 until q.n) {
        val j = (i + 1) % q.n
        val ax = q.x(i)
        val ay = q.y(i)
        val bx = q.x(j)
        val by = q.y(j)
        val ex = bx - ax
        val ey = by - ay
        val den = dx * ey - dy * ex
        if (abs(den) < 1e-6f) continue
        val s = ((ax - px) * ey - (ay - py) * ex) / den   // along the cut line
        val t = ((ax - px) * dy - (ay - py) * dx) / den        // along the edge
        if (t in -1e-4f..1.0001f) {
            if (s < tMin) tMin = s
            if (s > tMax) tMax = s
        }
    }
    if (tMax <= tMin) return null
    return Bone(floatArrayOf(px + dx * tMin, py + dy * tMin, px + dx * tMax, py + dy * tMax))
}

private val bones = ArrayList<Bone>()

private fun buildPanes(rng: Random): List<Pane> {
    val w = W.toFloat()
    val h = H.toFloat()
    val rootTA = w * h / PANES

    // two diagonal grain directions + the architectural vertical/horizontal
    val g1 = rng.rndf(0.45f, 1.1f) * (if (rng.nextBoolean()) 1f else -1f)
    val g2 = g1 + PIf / 2
    fun cutAngle(): Float {
        val r = rng.nextFloat()
        val base = when {
            r < 0.36f -> g1
            r < 0.56f -> g2
            r < 0.74f -> PIf / 2
            r < 0.88f -> 0f
            else -> rng.rndf(0f, PIf)
        }
        return base + rng.rndf(-0.09f, 0.09f)
    }

    // bsp
    fun newMul() = exp(rng.rndf(ln(0.45f), ln(3.0f)))

    val stack = ArrayDeque<Triple<Poly, Float, Int>>()
    val settled = ArrayList<Poly>()
    stack.add(Triple(rectPoly(0f, 0f, w, h), 1f, 0))
    while (stack.isNotEmpty()) {
        val (q, mul, depth) = stack.removeLast()
        if (q.area() < rootTA * mul) {
            settled.add(q)
            continue
        }
        val (cx, cy) = q.centroid()
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in 0 until q.n) {
            minX = min(minX, q.x(i))
            maxX = max(maxX, q.x(i))
            minY = min(minY, q.y(i))
            maxY = max(maxY, q.y(i))
        }
        var done = false
        var tries = 0
        while (!done && tries < 7) {
            tries++
            val a = cutAngle()
            val px = cx + rng.rndf(-0.26f, 0.26f) * (maxX - minX)
            val py = cy + rng.rndf(-0.26f, 0.26f) * (maxY - minY)
            if (rng.nextFloat() < min(1f, ARC * (if (depth <= 1) 1.6f else 1f))) {
                // bowed cut
                val ext = hypot(maxX - minX, maxY - minY)
                val rr = ext * (if (depth <= 1) rng.rndf(0.55f, 1.2f) else rng.rndf(0.85f, 2.2f))
                val side = if (rng.nextBoolean()) 1f else -1f
                val ccx = px + cos(a + PIf / 2) * rr * side
                val ccy = py + sin(a + PIf / 2) * rr * side
                val (qa, chain) = clipCircleSide(q, ccx, ccy, rr, true, ARCSTEP)
                val (qb, _) = clipCircleSide(q, ccx, ccy, rr, false, ARCSTEP)
                if (qa != null && qb != null &&
                    min(qa.area(), qb.area()) > rootTA * 0.13f
                ) {
                    if (depth <= 2 && chain != null) bones.add(Bone(chain))
                    stack.add(Triple(qa, newMul(), depth + 1))
                    stack.add(Triple(qb, newMul(), depth + 1))
                    done = true
                }
            } else {
                val (qa, qb) = split(q, px, py, cos(a), sin(a))
                if (qa != null && qb != null &&
                    min(qa.area(), qb.area()) > rootTA * 0.13f
                ) {
                    // the first cuts
                    if (depth <= 2) chordEnds(q, px, py, cos(a), sin(a))?.let { bones.add(it) }
                    stack.add(Triple(qa, newMul(), depth + 1))
                    stack.add(Triple(qb, newMul(), depth + 1))
                    done = true
                }
            }
        }
        if (!done) settled.add(q)
    }

    // color field: linear drift along a random direction + folded fbm
    val dd = rng.rndf(0f, TWO_PIf)
    val dx = cos(dd)
    val dy = sin(dd)
    fun fbm(x: Float, y: Float): Float {
        var v = 0f
        var amp = 0.55f
        var fx = x * 0.0021f
        var fy = y * 0.0021f
        repeat(3) {
            v += amp * SimplexNoise.noise(fx + NZV, fy - NZV * 0.6f)
            fx *= 2.2f
            fy *= 2.2f
            amp *= 0.5f
        }
        return v
    }

    fun uAt(x: Float, y: Float): Float {
        var v: Float
        if (FLOW == "rise") {
            v = (BIAS - 0.5f) + 0.14f + 0.74f * (y / h) + WARP * fbm(x, y) * 0.32f
        } else {
            val lin = ((x / w) * dx + (y / h) * dy) * DRIFT
            v = BIAS + lin + WARP * fbm(x, y) * 0.5f
        }
        // soft reflection
        if (v < 0f) v = -v * 0.7f
        if (v > 1f) v = 1f - (v - 1f) * 0.7f
        return v.coerceIn(0f, 1f)
    }

    // slicing pass
    val panes = ArrayList<Pane>(settled.size * 2)
    for (q in settled) {
        val (cx, cy) = q.centroid()
        var u = uAt(cx, cy) + rng.rndf(-UJIT, UJIT)
        if (rng.nextFloat() < INTRUDE) {
            u = (u + 0.3f + rng.nextFloat() * 0.4f) % 1f
        }
        val doSlice = rng.nextFloat() < SLICE && q.area() > rootTA * 1.2f
        if (!doSlice) {
            panes.add(Pane(q, u.coerceIn(0f, 1f), 0f))
            continue
        }
        // curved glazing courses
        if (rng.nextFloat() < 0.4f) {
            val dirA = rng.rndf(0f, TWO_PIf)
            var bminX = Float.MAX_VALUE
            var bmaxX = -Float.MAX_VALUE
            var bminY = Float.MAX_VALUE
            var bmaxY = -Float.MAX_VALUE
            for (i in 0 until q.n) {
                bminX = min(bminX, q.x(i))
                bmaxX = max(bmaxX, q.x(i))
                bminY = min(bminY, q.y(i))
                bmaxY = max(bmaxY, q.y(i))
            }
            val ext = hypot(bmaxX - bminX, bmaxY - bminY)
            val ccx = cx + cos(dirA) * ext * rng.rndf(0.6f, 1.4f)
            val ccy = cy + sin(dirA) * ext * rng.rndf(0.6f, 1.4f)
            var rmin = Float.MAX_VALUE
            var rmax = -Float.MAX_VALUE
            for (i in 0 until q.n) {
                val dr = hypot(q.x(i) - ccx, q.y(i) - ccy)
                rmin = min(rmin, dr)
                rmax = max(rmax, dr)
            }
            val strips = min(2 + rng.nextInt(3), (q.area() / (rootTA * 0.14f)).toInt())
            if (strips >= 2) {
                var rest: Poly? = q
                for (s in 1 until strips) {
                    val rc = rmin + (rmax - rmin) * (s.toFloat() / strips + rng.rndf(-0.05f, 0.05f))
                    val r0 = rest ?: break
                    val (innerP, _) = clipCircleSide(r0, ccx, ccy, rc, true, ARCSTEP)
                    val (outerP, _) = clipCircleSide(r0, ccx, ccy, rc, false, ARCSTEP)
                    if (innerP == null || outerP == null) continue
                    panes.add(Pane(innerP, (u + rng.rndf(-0.02f, 0.02f)).coerceIn(0f, 1f), rng.rndf(-0.05f, 0.05f)))
                    rest = outerP
                }
                rest?.let { panes.add(Pane(it, u.coerceIn(0f, 1f), rng.rndf(-0.05f, 0.05f))) }
                continue
            }
        }

          // strips run near-vertical/horizontal
        val a = if (rng.nextFloat() < 0.35f) {
            var bi = 0
            var bj = 0
            var bd = 0f
            for (vi in 0 until q.n) for (vj in vi + 1 until q.n) {
                val dd2 = (q.x(vi) - q.x(vj)).pow(2) + (q.y(vi) - q.y(vj)).pow(2)
                if (dd2 > bd) {
                    bd = dd2
                    bi = vi
                    bj = vj
                }
            }
            atan2(q.y(bj) - q.y(bi), q.x(bj) - q.x(bi)) + rng.rndf(-0.05f, 0.05f)
        } else {
            (if (rng.nextBoolean()) PIf / 2 else 0f) + rng.rndf(-0.07f, 0.07f)
        }
        val nx = cos(a + PIf / 2)
        val ny = sin(a + PIf / 2)
        var pmin = Float.MAX_VALUE
        var pmax = -Float.MAX_VALUE
        for (i in 0 until q.n) {
            val t = q.x(i) * nx + q.y(i) * ny
            pmin = min(pmin, t)
            pmax = max(pmax, t)
        }

        // don't slice into strips thinner
        val strips = min(2 + rng.nextInt(3), (q.area() / (rootTA * 0.14f)).toInt())
        if (strips < 2) {
            panes.add(Pane(q, u.coerceIn(0f, 1f), 0f))
            continue
        }
        var rest: Poly? = q
        for (s in 1 until strips) {
            val at = pmin + (pmax - pmin) * (s.toFloat() / strips + rng.rndf(-0.06f, 0.06f))
            val r = rest ?: break

            // (nx*at, ny*at) projects to `at` on the normal, so it sits on the cut line
            val (qa, qb) = split(r, nx * at, ny * at, cos(a), sin(a))
            if (qa == null || qb == null) continue
            panes.add(Pane(qa, (u + rng.rndf(-0.02f, 0.02f)).coerceIn(0f, 1f), rng.rndf(-0.05f, 0.05f)))
            rest = qb
        }
        rest?.let { panes.add(Pane(it, u.coerceIn(0f, 1f), rng.rndf(-0.05f, 0.05f))) }
    }
    return panes
}

// RENDER!

private fun renderGlass(c: Canvas, panes: List<Pane>, ssf: Float) {
    val ramp = rampOf(PRESET)
    val rng = Random(SEED * 31 + 5)
    c.clear(0xFF0F0C09.toInt())

    val leadW = LEAD * ssf
    val rimPaint = Paint().apply {
        color = 0x000000 or (((EDGE * 74f).toInt().coerceIn(0, 255)) shl 24)
        mode = PaintMode.STROKE
        strokeWidth = leadW * 2.6f
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, leadW * 0.9f)
    }

    for (p in panes) {
        val path = pathOfPoly(p.poly, ssf, 0f, 0f)
        val ok = ramp.at(p.u)
        var okj = ColorOKLCH(
            (ok.l + p.sliceShift + rng.rndf(-0.02f, 0.02f)).coerceIn(0.05f, 0.97f),
            (ok.c * rng.rndf(0.92f, 1.08f)).coerceAtMost(0.4f),
            (ok.h + rng.rndf(-4f, 4f) + 360f) % 360f
        )

        // the odd flashed pane
        if (rng.nextFloat() < 0.02f) {
            okj = ColorOKLCH((okj.l + 0.14f).coerceAtMost(0.96f), okj.c * 0.75f, okj.h)
        }

        // interior gradient - one end of the pane holds more light
        val dl = 0.035f + 0.085f * rng.nextFloat()
        val hi = ColorOKLCH((okj.l + dl).coerceAtMost(0.975f), okj.c, okj.h).toColor4f().toColor() or 0xFF000000.toInt()
        val lo = ColorOKLCH((okj.l - dl * 1.15f).coerceAtLeast(0.04f), (okj.c * 1.05f).coerceAtMost(0.4f), okj.h)
            .toColor4f().toColor() or 0xFF000000.toInt()
        val ang = rng.rndf(0f, TWO_PIf)
        val lx = cos(ang)
        val ly = sin(ang)
        var pmin = Float.MAX_VALUE
        var pmax = -Float.MAX_VALUE
        for (v in 0 until p.poly.n) {
            val t = (p.poly.x(v) * lx + p.poly.y(v) * ly) * ssf
            pmin = min(pmin, t)
            pmax = max(pmax, t)
        }
        val fill = Paint().apply {
            shader = Shader.makeLinearGradient(
                pmax * lx, pmax * ly, pmin * lx, pmin * ly,
                gradientOf(intArrayOf(hi, lo))
            )
        }
        c.drawPath(path, fill)
        // glass darkens where it meets the came
        c.save()
        c.clipPath(path)
        c.drawPath(path, rimPaint)
        c.restore()
    }

    // the lead -shared edges overlap into one clean line, joints solder themselves
    val lead = Paint().apply {
        color = 0xFF14100C.toInt()
        mode = PaintMode.STROKE
        strokeWidth = leadW
        strokeJoin = PaintStrokeJoin.ROUND
        strokeCap = PaintStrokeCap.ROUND
    }
    for (p in panes) {
        c.drawPath(pathOfPoly(p.poly, ssf, 0f, 0f), lead)
    }
    // the bones
    val heavy = Paint().apply {
        color = 0xFF14100C.toInt()
        mode = PaintMode.STROKE
        strokeWidth = leadW * 2.1f
        strokeCap = PaintStrokeCap.ROUND
    }
    for (b in bones) {
        val pb = PathBuilder()
        pb.moveTo(Point(b.pts[0] * ssf, b.pts[1] * ssf))
        for (i in 1 until b.pts.size / 2) {
            pb.lineTo(Point(b.pts[i * 2] * ssf, b.pts[i * 2 + 1] * ssf))
        }
        c.drawPath(pb.detach(), heavy)
    }
    // outer frame came, heavier
    val frame = Paint().apply {
        color = 0xFF14100C.toInt()
        mode = PaintMode.STROKE
        strokeWidth = leadW * 2.2f
    }
    c.drawRect(Rect(0f, 0f, W * ssf, H * ssf), frame)
}

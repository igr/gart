package nm

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.BwColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.ToneCurve
import dev.oblac.gart.color.blendScreen
import dev.oblac.gart.color.bluef
import dev.oblac.gart.color.greenf
import dev.oblac.gart.color.redf
import dev.oblac.gart.color.rgb
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.fx.addLens
import dev.oblac.gart.gfx.blurPaint
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.lerp
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndi
import dev.oblac.gart.math.seededRandom
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.curl
import dev.oblac.gart.noise.fbm
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.noise.ringNoise
import dev.oblac.gart.pixels.boxBlur
import dev.oblac.gart.pixels.splat
import dev.oblac.gart.vector.MutableVec2
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * nm2 - phosphenes. the second before he jacks in, seen from behind his eyes.
 *
 * the ground is the bloodlit dark. it is red where the light comes through and black at the
 * edge. silver specks boil in from the edge in clumps and burn out before the middle.
 *
 * a ring of blurred frames turns round the middle. the frames carry signs, figures, static and
 * halftone. each frame comes back seven times round the ring, so the ring is a mandala, but frames
 * are missing and some of them jerk.
 *
 * in the middle there is a gray disk of static, the color of chiba sky. its rim dissolves into
 * the dark grain by grain.
 */
fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val gart = Gart.of("nm2", 1024, 1024)
    println(gart)
    println("seed=$SEED")
    val d = gart.d
    val g = gart.gartvas()

    Gartmap(g.d).use { m ->
        bloodlit(m)
        m.drawToCanvas(g)
    }
    mandala(g.canvas, d)
    Gartmap(g).use { m ->
        phosphenes(m)
        disk(m)
        // the lens: red and blue drift apart toward the edge, a vignette, heavy grain
        addLens(m, Point(d.wf / 2, d.hf * EYE), split = 0.006f, vignette = 0.35f, grain = 46f, seed = SEED)
        m.drawToCanvas()
    }

    gart.saveImage(g, "$OUT.png")
    if (!headless) gart.window().showImage(g)
}

private val SEED = pi("seed", 1)
private val OUT = ps("out", "nm2")
private val SPECKS = pi("specks", 120000, 0..2_000_000)   // the streaks boiling in
private val FOLD = pi("fold", 7, 3..12)                   // how many times the mandala comes round

private val rng = seededRandom(SEED)
private const val EYE = 0.5f      // the middle of the vision, share of the height
private val NZ = noiseOffset(SEED.toLong()) * 100f    // a whole noise unit a seed. 0.01 left every seed alike

// the bloodlit dark, darkest first
private val blood = Palette.of(BwColors.warmBlack0, 0xFF2E0709.toInt(), 0xFF5E0B10.toInt(), 0xFF8C1915.toInt(), 0xFFB8402A.toInt())
private val colorSilver = BwColors.bg04

// the bloodlit dark ///////

private fun bloodlit(m: Gartmap) {
    val d = m.d
    val cx = d.wf / 2
    val cy = d.hf * EYE
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val v = fbm(x / 420f + NZ, y / 420f + NZ, octaves = 4, lacunarity = 2.2f, gain = 0.5f)
        val blot = SimplexNoise.noise(x / 70f + NZ * 3, y / 70f - NZ)
        // the glow falls to black before the edge on its own. when the noise had the say,
        // some seeds were red to the corners and the silver went pink
        val r = hypot(x - cx, y - cy) / cx
        val glow = 1f - smoothstep(0.15f, 1.05f, r)
        val t = (0.08f + 0.62f * glow + 0.3f * v + 0.06f * blot).coerceIn(0f, 1f)
        m.pixels[y * d.w + x] = blood.sample(t)
    }
}

// the mandala ----

// one ring of frames. a slot comes back FOLD times round the circle
private class Ring(val r: Float, val size: Float, val kinds: IntArray)

private const val STATIC = 1
private const val DOTS = 2
private const val SIGN = 3
private const val FIGURE = 4

// "symbols, figures, faces" - no words
private val rings = listOf(
    Ring(0.205f, 30f, intArrayOf(SIGN, FIGURE)),
    Ring(0.27f, 40f, intArrayOf(DOTS, STATIC, SIGN)),
    Ring(0.345f, 54f, intArrayOf(STATIC, FIGURE, DOTS)),
)

private fun mandala(c: Canvas, d: Dimension) {
    val cx = d.wf / 2
    val cy = d.hf * EYE
    for (ring in rings) {
        val per = ring.kinds.size
        val count = FOLD * per
        val phase = rng.rndf(0f, (2 * PI / count).toFloat())
        for (k in 0 until count) {
            val kind = ring.kinds[k % per]
            if (rng.rndf() < 0.22f) continue
            val a = phase + 2 * PI * k / count + rng.rndf(-0.05f, 0.05f)
            val r = d.wf * ring.r * rng.rndf(0.95f, 1.05f)
            val s = ring.size * rng.rndf(0.85f, 1.2f)
            val turn = Math.toDegrees(a).toFloat() + 90f + rng.rndf(-6f, 6f)
            frame(c, Point(cx + cos(a).toFloat() * r, cy + sin(a).toFloat() * r), s, s * 0.62f, turn, kind)
        }
    }
}

// a frame out of focus, screened on. a quarter of them jerk - three late copies along the ring
private fun frame(c: Canvas, at: Point, w: Float, h: Float, turn: Float, kind: Int) {
    val blur = 0.6f + 6f * rng.rndf().pow(2.5f)
    val smear = if (rng.rndf() < 0.25f) rng.rndf(5f, 15f) * (if (rng.nextBoolean()) 1 else -1) else 0f
    val layer = blurPaint(blur, BlendMode.SCREEN, (rng.rndf(0.55f, 0.95f) * 255).toInt())
    c.save()
    c.translate(at.x, at.y)
    c.rotate(turn)
    c.saveLayer(Rect(-w - 40f - abs(smear), -h - 40f, w + 40f + abs(smear), h + 40f), layer)
    val ghosts = if (smear != 0f) 4 else 1
    for (gh in 0 until ghosts) {
        c.save()
        c.translate(smear * gh / 3f, 0f)
        content(c, w, h, kind, 1f - gh * 0.28f)
        c.restore()
    }
    c.restore()
    c.restore()
}

private fun content(c: Canvas, w: Float, h: Float, kind: Int, fade: Float) {
    val ink = fillOf(colorSilver).apply { alpha = (255 * fade).toInt() }
    c.drawRect(Rect(-w / 2, -h / 2, w / 2, h / 2), strokeOf(colorSilver, 1.2f).apply { alpha = (180 * fade).toInt() })
    when (kind) {
        STATIC -> {
            var yy = -h / 2 + 2
            while (yy < h / 2 - 2) {
                var xx = -w / 2 + 2
                while (xx < w / 2 - 2) {
                    val v = rng.rndf()
                    if (v > 0.45f) c.drawRect(Rect(xx, yy, xx + 2.5f, yy + 2.5f), fillOf(colorSilver).apply { alpha = (255 * fade * v).toInt() })
                    xx += 2.5f
                }
                yy += 2.5f
            }
        }

        DOTS -> {
            // a soft blob in halftone
            val step = w / 9
            var yy = -h / 2 + step / 2
            while (yy < h / 2) {
                var xx = -w / 2 + step / 2
                while (xx < w / 2) {
                    val q = 1f - (xx * xx + yy * yy) / (w * w * 0.2f)
                    if (q > 0f) c.drawCircle(xx, yy, step * 0.48f * sqrt(q), ink)
                    xx += step
                }
                yy += step
            }
        }

        SIGN -> {
            val r = h * 0.3f
            val sign = strokeOf(colorSilver, 2.5f).apply { alpha = (255 * fade).toInt() }
            when (rng.rndi(3)) {
                0 -> c.drawCircle(0f, 0f, r, sign)
                1 -> {
                    c.drawLine(-r, 0f, r, 0f, sign)
                    c.drawLine(0f, -r, 0f, r, sign)
                }

                else -> c.drawRect(Rect(-r, -r, r, r), sign)
            }
        }

        else -> {
            val f = font(FontFamily.SpaceMono, 40f)
            val t = "${rng.rndi(10)}${rng.rndi(10)}"
            val tw = f.measureTextWidth(t)
            val k = h * 0.9f / 40f
            c.save()
            c.scale(k, k)
            c.drawString(t, -tw / 2, 14f, f, ink)
            c.restore()
        }
    }
}

// phosphenes ==========

// short streaks, not long trails. long ones merged into a few bright comet lines, and a field of
// warped noise read as crumpled foil. many short ones in clumps boil
private fun phosphenes(m: Gartmap) {
    val d = m.d
    val w = d.w
    val trail = FloatArray(d.area)
    val fine = FloatArray(d.area)
    val soft = FloatArray(d.area)
    val cx = d.wf / 2
    val cy = d.hf * EYE
    val v = MutableVec2()
    var made = 0
    while (made < SPECKS) {
        var x = rng.rndf(-0.05f, 1.05f) * d.wf
        var y = rng.rndf(-0.05f, 1.05f) * d.hf
        val r0 = hypot(x - cx, y - cy) / d.wf
        val clump = 0.5f + 0.5f * SimplexNoise.noise(x / 140f + NZ, y / 140f - NZ)
        if (rng.rndf() > smoothstep(0.2f, 0.62f, r0) * clump) continue
        made++
        val b0 = 0.4f + 1.4f * rng.rndf().pow(3f)
        val n = rng.rndi(6, 34)
        for (s in 0 until n) {
            val dx = x - cx
            val dy = y - cy
            val r = hypot(dx, dy)
            if (r < d.wf * 0.15f) break
            val boil = smoothstep(0.22f, 0.6f, r / d.wf)
            curl(x / 120f + NZ, y / 120f - NZ, v, eps = 0.5f)
            // half in, half round, and a lot of churn near the edge
            val inx = -dx / r
            val iny = -dy / r
            var mx = inx * 0.5f - iny * 0.5f + v.x * (0.4f + 1.6f * boil)
            var my = iny * 0.5f + inx * 0.5f + v.y * (0.4f + 1.6f * boil)
            val ml = hypot(mx, my)
            mx /= ml
            my /= ml
            x += mx * 1.3f
            y += my * 1.3f
            val life = sin(PI.toFloat() * s / n) * (0.3f + 0.7f * boil)
            splat(trail, w, x, y, 0.12f * b0 * life)
        }
        // the head is a speck, now and then a soft one
        val b = b0 * rng.rndf(0.2f, 1f)
        if (rng.rndf() < 0.85f) splat(fine, w, x, y, b) else splat(soft, w, x, y, b * 4f)
    }
    val blurred = boxBlur(soft, w, d.h, 2)
    val glow = boxBlur(blurred, w, d.h, 9)
    val haze = boxBlur(trail, w, d.h, 6)
    val tone = ToneCurve(1f)
    for (i in fine.indices) {
        val e = tone(trail[i] + haze[i] * 0.8f + fine[i] * 1.6f + blurred[i] * 1.2f + glow[i] * 2.5f)
        m.pixels[i] = blendScreen(m.pixels[i], colorSilver, e)
    }
}

// the gray disk, the color of chiba sky ////

private const val SPIN = 0.05f       // smear at the rim, radians. 0.6 turned the snow into rings

private fun disk(m: Gartmap) {
    val d = m.d
    val cx = d.wf / 2
    val cy = d.hf * EYE
    val rad = d.wf * 0.125f
    for (y in 0 until d.h) for (x in 0 until d.w) {
        val dx = x - cx
        val dy = y - cy
        val r = hypot(dx, dy)
        if (r > rad * 1.4f) continue
        val a = atan2(dy, dx)
        val edge = rad * (1f + 0.05f * ringNoise(a, 3f, NZ))
        // the rim dissolves grain by grain
        val soft = 1f - smoothstep(edge * 0.7f, edge * 1.25f, r)
        val cover = if (hash01(x, y, 17, SEED) < soft) 1f else soft * 0.3f
        if (cover <= 0f) continue
        // it starts to turn, so the snow smears a little near the rim
        val q = r / rad
        val smear = SPIN * q * q
        var sum = 0f
        val n = 6
        for (k in 0 until n) {
            val ak = a - smear * k / n
            val ix = floor((cx + cos(ak) * r) / 2.2f).toInt()
            val iy = floor((cy + sin(ak) * r) / 2.2f).toInt()
            sum += 0.8f * hash01(ix, iy, 5, SEED) + 0.2f * hash01(iy, 11, SEED)
        }
        val pale = 0.66f - 0.24f * q * q        // "becoming a sphere of paler gray"
        val tone = (0.12f + 0.85f * sum / n) * 0.6f + pale * 0.4f
        val i = y * d.w + x
        val c = m.pixels[i]
        m.pixels[i] = rgb(lerp(redf(c), tone, cover), lerp(greenf(c), tone, cover), lerp(bluef(c), tone, cover))
    }
}

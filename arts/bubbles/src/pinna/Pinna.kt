package pinna

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.hueShift
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.random.Random

private const val W = 1200
private const val H = 1500

internal data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    val canopyW: Float,
    val canopyH: Float,
    val canopyX: Float,
    val canopyY: Float,
    val canopyR: Float,
    val attractors: Int,
    val influence: Float,
    val kill: Float,
    val segLen: Float,
    val wander: Float,
    val tips: Int,
    val smooth: Int,
    val taper: Float,
    val tipR: Float,
    val trunkX: Float,
    val trunkY: Float,
    val foot: Float,
    val footRun: Float,
    val footPow: Float,
    val footSkew: Float,
    val branchGap: Float,
    val packRes: Int,
    val circMax: Float,
    val circMin: Float,
    val circSteps: Int,
    val circGap: Float,
    val circOverlap: Float,
    val folHue: Float,
    val folChroma: Float,
    val folDeep: Float,
    val folLight: Float,
    val folSteps: Int,
    val stroke: Float,
    val tremor: Float,
    val ground: Int,
    val hue: Float,
    val vignette: Float,
)

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 2026L),
        out = ps("out", "output/pinna"),
        ss = pi("ss", 2),
        canopyW = pf("canopyw", 0.737f),
        canopyH = pf("canopyh", 0.756f),
        canopyX = pf("canopyx", 0.486f),
        canopyY = pf("canopyy", 0.075f),
        canopyR = pf("canopyr", 0.06f),
        attractors = pi("attractors", 280),
        influence = pf("influence", 0.26f),
        kill = pf("kill", 0.055f),
        segLen = pf("seglen", 0.018f),
        wander = pf("wander", 0.30f),
        tips = pi("tips", 22),
        smooth = pi("smooth", 6),
        taper = pf("taper", 1.3f),
        tipR = pf("tipr", 0.0026f),
        trunkX = pf("trunkx", 0.485f),
        trunkY = pf("trunky", 0.946f),
        foot = pf("foot", 1.5f),
        footRun = pf("footrun", 0.115f),
        footPow = pf("footpow", 3.0f),
        footSkew = pf("footskew", 0.55f),
        branchGap = pf("branchgap", 0.006f),
        packRes = pi("packres", 900),
        circMax = pf("circmax", 0.055f),
        circMin = pf("circmin", 0.004f),
        circSteps = pi("circsteps", 13),
        circGap = pf("circgap", 0.002f),
        circOverlap = pf("circoverlap", 0f),
        folHue = pf("folhue", 148f),
        folChroma = pf("folchroma", 0.13f),
        folDeep = pf("foldeep", 0.38f),
        folLight = pf("follight", 0.86f),
        folSteps = pi("folsteps", 5),
        stroke = pf("stroke", 0.0011f),
        tremor = pf("tremor", 0.35f),
        ground = pi("ground", 1),
        hue = pf("hue", 0f),
        vignette = pf("vignette", 0.10f),
    )

    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    require(p.canopyW in 0.4f..0.95f) { "canopyw must be between 0.4 and 0.95" }
    require(p.canopyH in 0.4f..0.95f) { "canopyh must be between 0.4 and 0.95" }
    require(p.canopyX in 0.3f..0.7f) { "canopyx must be between 0.3 and 0.7" }
    require(p.canopyY in 0f..0.3f) { "canopyy must be between 0 and 0.3" }
    require(p.canopyR in 0f..0.5f) { "canopyr must be between 0 and 0.5" }
    require(p.attractors in 40..2000) { "attractors must be between 40 and 2000" }
    require(p.influence in 0.05f..1f) { "influence must be between 0.05 and 1" }
    require(p.kill in 0.01f..0.4f) { "kill must be between 0.01 and 0.4" }
    require(p.kill < p.influence) { "kill must be less than influence, or nothing grows" }
    require(p.segLen in 0.004f..0.08f) { "seglen must be between 0.004 and 0.08" }
    require(p.wander in 0f..1.5f) { "wander must be between 0 and 1.5" }
    require(p.tips in 3..200) { "tips must be between 3 and 200" }
    require(p.smooth in 0..40) { "smooth must be between 0 and 40" }
    require(p.taper in 1.05f..4f) { "taper must be between 1.05 and 4" }
    require(p.tipR in 0.0004f..0.01f) { "tipr must be between 0.0004 and 0.01" }
    require(p.trunkX in 0.2f..0.8f) { "trunkx must be between 0.2 and 0.8" }
    require(p.trunkY in 0.8f..1f) { "trunky must be between 0.8 and 1" }
    require(p.foot in 0f..3f) { "foot must be between 0 and 3" }
    require(p.footRun in 0.005f..0.2f) { "footrun must be between 0.005 and 0.2" }
    require(p.footPow in 1f..8f) { "footpow must be between 1 and 8" }
    require(p.footSkew in -1f..1f) { "footskew must be between -1 and 1" }
    require(p.branchGap in 0f..0.03f) { "branchgap must be between 0 and 0.03" }
    require(p.packRes in 200..2000) { "packres must be between 200 and 2000" }
    require(p.circMax in 0.005f..0.2f) { "circmax must be between 0.005 and 0.2" }
    require(p.circMin in 0.001f..0.1f) { "circmin must be between 0.001 and 0.1" }
    require(p.circMin < p.circMax) { "circmin must be less than circmax" }
    require(p.circSteps in 2..40) { "circsteps must be between 2 and 40" }
    require(p.circGap in 0f..0.03f) { "circgap must be between 0 and 0.03" }
    require(p.circOverlap in 0f..0.6f) { "circoverlap must be between 0 and 0.6" }
    require(p.folHue in -360f..360f) { "folhue must be between -360 and 360" }
    require(p.folChroma in 0f..0.4f) { "folchroma must be between 0 and 0.4" }
    require(p.folDeep in 0f..1f) { "foldeep must be between 0 and 1" }
    require(p.folLight in 0f..1f) { "follight must be between 0 and 1" }
    require(p.folSteps in 1..24) { "folsteps must be between 1 and 24" }
    require(p.stroke in 0.0004f..0.006f) { "stroke must be between 0.0004 and 0.006" }
    require(p.tremor in 0f..2f) { "tremor must be between 0 and 2" }
    require(p.ground in 0..1) { "ground must be 0 or 1" }
    require(p.hue in -180f..180f) { "hue must be between -180 and 180" }
    require(p.vignette in 0f..1.2f) { "vignette must be between 0 and 1.2" }
    return p
}

private const val INK = 0xFF161418.toInt()
private const val PAPER = 0xFFF2EFE7.toInt()
private const val OPAQUE = 0xFF000000.toInt()

internal class Colors(p: Params) {
    val ink = hueShift(INK, p.hue)
    val paper = hueShift(PAPER, p.hue)

    val greens: IntArray = IntArray(p.folSteps) { i ->
        val t = if (p.folSteps > 1) i / (p.folSteps - 1f) else 0f
        val l = p.folDeep + (p.folLight - p.folDeep) * t
        ColorOKLCH(l, p.folChroma, p.folHue + p.hue).toColor4f().toColor() or OPAQUE
    }
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()
    val gart = Gart.of("pinna", W, H)

    println(gart)
    println("seed=${p.seed} tips=${p.tips} attractors=${p.attractors} circmax=${p.circMax} hue=${p.hue}")

    val big = Gartvas(Dimension(W * p.ss, H * p.ss))
    big.canvas.scale(p.ss.toFloat(), p.ss.toFloat())
    render(big.canvas, p)

    val g = gart.gartvas()
    val snapshot = big.snapshot()
    g.canvas.drawImageRect(
        snapshot,
        Rect.makeWH((W * p.ss).toFloat(), (H * p.ss).toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    snapshot.close()

    if (p.vignette > 0f) g.canvas.drawVignette(g.d, p.vignette, color = 0x1A1510, radius = 0.95f, innerStop = 0.55f)

    val output = if (p.out.endsWith(".png", ignoreCase = true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun render(c: Canvas, p: Params) {
    val colors = Colors(p)
    c.drawRect(Rect.makeWH(W.toFloat(), H.toFloat()), fillOf(colors.paper))

    val canopy = canopyOf(p, W, H)
    val nz = (p.seed and 0xffff) * 0.01f
    val skeleton = growSkeleton(canopy, p, Random(p.seed xor 0x5EED1), nz, W, H)
    val discs = skeleton.discs(p, H)
    val field = fieldOf(canopy, discs, p)
    val dist = branchDistance(field)
    val blobs = packCircles(field, dist, p, Random(p.seed xor 0x1EAF5), H)
    println("nodes=${skeleton.nodes().size} tips=${skeleton.tips().size} circles=${blobs.size}")

    c.save()
    c.clipRect(Rect.makeLTRB(0f, 0f, W.toFloat(), p.trunkY * H))

    val canopyPath = canopy.rrect()
    c.save()
    c.clipRRect(canopyPath, ClipMode.INTERSECT, true)
    drawCircles(c, blobs, p, colors)
    c.restore()

    val branches = skeleton.branchPath(p, H)
    val inkFill = fillOf(colors.ink)
    c.drawPath(branches, inkFill)
    c.restore()
    inkFill.close()
    branches.close()
}

private fun drawCircles(c: Canvas, blobs: List<Blob>, p: Params, colors: Colors) {
    val paint = Paint().apply { isAntiAlias = true }
    for (b in blobs) {
        paint.color = colors.greens[b.tone]
        c.drawCircle(b.x, b.y, b.r, paint)
    }
    paint.close()
}

package ommatidia

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.color.hueShift
import dev.oblac.gart.color.space.ColorOKLCH
import dev.oblac.gart.color.space.color4f
import dev.oblac.gart.fx.addGrain
import dev.oblac.gart.gfx.drawVignette
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.pl
import dev.oblac.gart.io.ps
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Shader
import kotlin.random.Random

/**
 * ommatidia - a facet membrane.
 *
 * thousands of voronoi micro lenses tile the frame, packed on a height field that never
 * gets drawn. each lens refracts a hidden marbled scene along its own optical axis, so the
 * swells behave like the converging lenses they are: they magnify and fold what's behind
 * them while the flats show it more or less straight through.
 *
 * nothing here draws the picture.
 */

private const val W = 1400
private const val H = 1400

internal data class Params(
    val seed: Long,
    val out: String,
    val ss: Int,
    // the membrane
    val swells: Int,
    val swellAmp: Float,
    val swellR: Float,
    val dimples: Float,
    val ripple: Float,
    val rippleF: Float,
    val relief: Float,
    // the pack
    val pitch: Float,
    val acuity: Float,
    val aniso: Float,
    val relax: Int,
    val grout: Float,
    // the optics
    val ior: Float,
    val depth: Float,
    // the hidden scene
    val pal: Int,
    val flip: Int,
    val bandF: Float,
    val bandAng: Float,
    val bandC: Float,
    val warp: Float,
    val warpF: Float,
    val sunX: Float,
    val sunY: Float,
    val sunR: Float,
    val sunI: Float,
    val sunHue: Float,
    val sunWarm: Float,
    val sunDesat: Float,
    // the light
    val lightX: Float,
    val lightY: Float,
    val lightZ: Float,
    val ambient: Float,
    val diffuse: Float,
    val shine: Float,
    val specMin: Float,
    val specSize: Float,
    val specA: Float,
    val tremor: Float,
    val domeLift: Float,
    val domeEdge: Float,
    val domeOff: Float,
    val domeR: Float,
    // ground and post
    val inkL: Float,
    val inkC: Float,
    val inkHue: Float,
    val bloom: Float,
    val vignette: Float,
    val grain: Float,
    val hue: Float,
)

private fun resolveParams(): Params {
    val p = Params(
        seed = pl("seed", 2028L),
        out = ps("out", "output/ommatidia"),
        ss = pi("ss", 2),

        swells = pi("swells", 6),
        swellAmp = pf("swellamp", 1.0f),
        swellR = pf("swellr", 0.42f),
        dimples = pf("dimples", 0.35f),
        ripple = pf("ripple", 0.06f),
        rippleF = pf("ripplef", 2.0f),
        relief = pf("relief", 1.4f),

        pitch = pf("pitch", 0.0110f),
        acuity = pf("acuity", 0.55f),
        aniso = pf("aniso", 0.85f),
        relax = pi("relax", 18),
        grout = pf("grout", 0.11f),

        ior = pf("ior", 1.5f),
        depth = pf("depth", 0.50f),

        pal = pi("pal", 1),
        flip = pi("flip", 0),
        bandF = pf("bandf", 0.9f),
        bandAng = pf("bandang", 28f),
        bandC = pf("bandc", 0.0f),
        warp = pf("warp", 0.35f),
        warpF = pf("warpf", 1.3f),
        sunX = pf("sunx", 0.36f),
        sunY = pf("suny", 0.30f),
        sunR = pf("sunr", 0.18f),
        sunI = pf("suni", 0.90f),
        sunHue = pf("sunhue", 82f),
        sunWarm = pf("sunwarm", 0.0f),
        sunDesat = pf("sundesat", 0.35f),

        lightX = pf("lightx", -0.45f),
        lightY = pf("lighty", -0.62f),
        lightZ = pf("lightz", 0.65f),
        ambient = pf("ambient", 0.54f),
        diffuse = pf("diffuse", 0.72f),
        shine = pf("shine", 30f),
        specMin = pf("specmin", 0.18f),
        specSize = pf("specsize", 0.34f),
        specA = pf("speca", 0.70f),
        tremor = pf("tremor", 0.30f),
        domeLift = pf("domelift", 0.44f),
        domeEdge = pf("domeedge", 0.66f),
        domeOff = pf("domeoff", 0.52f),
        domeR = pf("domer", 1.10f),

        inkL = pf("inkl", 0.14f),
        inkC = pf("inkc", 0.03f),
        inkHue = pf("inkhue", 265f),
        bloom = pf("bloom", 0.07f),
        vignette = pf("vignette", 0.35f),
        grain = pf("grain", 0.05f),
        hue = pf("hue", 0f),
    )

    require(p.ss in 1..4) { "ss must be between 1 and 4" }
    return p
}

private const val OPAQUE = 0xFF000000.toInt()

internal class Colors(p: Params) {
    val ramp: IntArray
    /** the seams between lenses, and everything the pack doesn't cover */
    val ink: Int
    val sun: Int
    val glint: Int

    init {
        val cool = Palettes.coolPalette(p.pal).toIntArray()
        val ordered = if (p.flip == 1) cool.reversedArray() else cool
        ramp = IntArray(ordered.size) { hueShift(ordered[it], p.hue) }
        ink = ColorOKLCH(p.inkL, p.inkC, p.inkHue + p.hue).toColor4f().toColor() or OPAQUE
        sun = ColorOKLCH(0.97f, 0.055f, p.sunHue + p.hue).toColor4f().toColor() or OPAQUE
        glint = ColorOKLCH(0.99f, 0.020f, p.sunHue + p.hue).toColor4f().toColor() or OPAQUE
    }
}

fun main(args: Array<String>) {
    val headless = detectHeadlessFlags(args)
    val p = resolveParams()
    val colors = Colors(p)
    val gart = Gart.of("ommatidia", W, H)

    println(gart)
    println("seed=${p.seed} pitch=${p.pitch} acuity=${p.acuity} depth=${p.depth} pal=${p.pal}")

    val big = Gartvas(Dimension(W * p.ss, H * p.ss))
    big.canvas.scale(p.ss.toFloat(), p.ss.toFloat())
    render(big.canvas, p, colors)

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

    if (p.bloom > 0f) drawBloom(g.canvas, p, colors, g.d)
    if (p.vignette > 0f) g.canvas.drawVignette(g.d, p.vignette, color = 0x0A0810, radius = 0.95f, innerStop = 0.5f)
    if (p.grain > 0f) addGrain(g, p.grain, (p.seed and 0x7fffffff).toInt())

    val output = if (p.out.endsWith(".png", ignoreCase = true)) p.out else "${p.out}.png"
    gart.saveImage(g, output)
    if (!headless) gart.window().showImage(g)
}

private fun render(c: Canvas, p: Params, colors: Colors) {
    val nzOff = (p.seed and 0xffff) * 0.01f
    val rnd = Random(p.seed)
    val membrane = Membrane(p, rnd, nzOff)
    val light = lightOf(p)

    var t0 = System.currentTimeMillis()
    val facets = packFacets(membrane, p, rnd, light, W.toFloat(), H.toFloat())
    println("facets=${facets.size} pack=${System.currentTimeMillis() - t0}ms")

    val scene = Scene(p, colors, nzOff)
    c.drawRect(Rect.makeWH(W.toFloat(), H.toFloat()), fillOf(colors.ink))

    t0 = System.currentTimeMillis()
    val eta = 1f / p.ior
    val depth = p.depth * W
    val hit = FloatArray(2)
    LensPainter(p, colors).use { painter ->
        for (f in facets) {
            opticalSample(f.x, f.y, f.nx, f.ny, f.nz, eta, depth, hit)
            painter.draw(c, f, scene.sample(hit[0] / W, hit[1] / W))
        }
    }
    println("draw=${System.currentTimeMillis() - t0}ms")
}


private fun drawBloom(c: Canvas, p: Params, colors: Colors, d: Dimension) {
    val a = (255f * p.bloom).toInt().coerceIn(0, 255)
    val paint = Paint().apply {
        blendMode = BlendMode.PLUS
        shader = Shader.makeRadialGradient(
            p.sunX * d.wf, p.sunY * d.wf, d.wf * 0.6f,
            gradientOf(
                intArrayOf((a shl 24) or (colors.sun and 0xFFFFFF), 0x00000000),
                floatArrayOf(0f, 1f),
            ),
        )
    }
    c.drawPaint(paint)
    paint.close()
}

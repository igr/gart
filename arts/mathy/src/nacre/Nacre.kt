package nacre

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.fx.downsample
import dev.oblac.gart.fx.supersampled
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.degToRad
import dev.oblac.gart.math.lerp
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.noise.noiseOffset
import dev.oblac.gart.tri3d.Camera
import dev.oblac.gart.tri3d.Face
import dev.oblac.gart.tri3d.Mesh
import dev.oblac.gart.tri3d.Scene
import dev.oblac.gart.util.Stopwatch
import dev.oblac.gart.vector.Vec3
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// nacre. i wanted a shell that had forgotten which way to grow.

private const val W = 1200
private const val H = 1440
private const val HOLE = 0.24f
private const val RELIEF = 0.0055f
private const val TILT = -28f
private const val TURN = -18f

private val SEED = pi("seed", 19)
private val OUT = ps("out", "nacre")
private val SS = pi("ss", 3, 1..4)
private val RIBS = pi("ribs", 150, 40..260)
private val FOLD = pf("fold", 1f, 0.2f..1.8f)
private val CURL = pf("curl", 1.4f, -1.5f..1.5f)
private val PEARL = pf("pearl", 0.62f, 0f..1f)

private val pal = Palettes.coolPalette(182)
private val ground = Palette(0xFF101A20, 0xFF27383C, 0xFF4B4140)
private val r = Random(SEED)
private val phase = r.nextFloat() * TAUf
private val asym = 0.7f + r.nextFloat() * 0.6f
private val nz = noiseOffset(SEED.toLong())
private val light = Vec3(-0.5f, -0.65f, -1f).normalize()
private val fill = Vec3(0.85f, 0.05f, -0.6f).normalize()

private val tx = degToRad(TILT)
private val tz = degToRad(TURN)

private fun pose(p: Vec3) = p.rotateX(tx).rotateZ(tz)

private fun shell(u: Float, t: Float): Vec3 {
    val a = u + CURL * sin(PIf * t) + 0.11f * t * sin(2f * u + phase)
    val lip = t * t * t
    val radius = lerp(HOLE, 1.22f, t) +
        0.10f * lip * sin(3f * u + phase) + 0.035f * t * sin(2f * u - phase)
    val swell = sin(PIf * (0.10f + 0.82f * t))
    val fold = 0.48f * sin(3f * u + 3.5f * t + phase) * swell +
        0.13f * asym * sin(5f * u - 2f * t + phase) * t * t
    val tooth = RELIEF * cos(TAUf * RIBS * t) * (0.35f + 0.65f * swell)
    val z = FOLD * fold + 0.40f * (1f - t).pow(3) + tooth
    return pose(Vec3(radius * cos(a) * 1.03f, radius * sin(a), z))
}

private fun pigment(u: Float, t: Float, n: Vec3, view: Vec3): Int {
    val facing = n.dot(view)
    val drift = 0.5f + 0.5f * sin(u + 2.8f * t + phase + 1.3f * n.y)
    val copper = (0.5f + 0.5f * sin(2f * u - 3f * t + phase)).pow(3)
    var base = lerpColor(pal[1], pal[2], drift)
    base = lerpColor(base, pal[4], copper)
    base = lerpColor(base, pal[3], PEARL * facing.pow(2))

    val diffuse = n.dot(light).coerceAtLeast(0f)
    val bounce = n.dot(fill).coerceAtLeast(0f)
    val half = (light + view).normalize()
    val spec = n.dot(half).coerceAtLeast(0f).pow(38) * 0.75f
    val glaze = n.dot((fill + view).normalize()).coerceAtLeast(0f).pow(12) * 0.23f
    val shade = 0.19f + 0.98f * diffuse + 0.24f * bounce
    val mottle = 1f + 0.045f * SimplexNoise.noise(cos(u) * 7f + nz, sin(u) * 7f + t * 9f)
    val dark = lerpColor(pal[0], pal[5], copper * 0.6f)
    val lit = lerpColor(dark, base, (shade * mottle).coerceIn(0f, 1f))
    return lerpColor(lit, pal[3], (spec + glaze).coerceIn(0f, 0.85f))
}

private fun grow(camera: Camera): Mesh {
    val around = 1280
    val across = RIBS * 4
    val rows = Array(across + 1) { j ->
        Array(around) { i -> shell(i * TAUf / around, j.toFloat() / across) }
    }
    val faces = ArrayList<Face>(around * across * 2)

    fun facet(a: Vec3, b: Vec3, c: Vec3, u: Float, t: Float) {
        val centre = (a + b + c) / 3f
        var n = (b - a).cross(c - a).normalize()
        val view = (camera.eye - centre).normalize()
        val front = n.dot(view) < 0f
        if (front) n *= -1f
        val col = pigment(u, t, n, view)
        faces += if (front) Face(a, b, c, col) else Face(a, c, b, col)
    }

    for (j in 0 until across) {
        val t = (j + 0.5f) / across
        for (i in 0 until around) {
            val k = (i + 1) % around
            val u = (i + 0.5f) * TAUf / around
            val a = rows[j][i]
            val b = rows[j][k]
            val c = rows[j + 1][k]
            val d = rows[j + 1][i]
            facet(a, b, c, u, t)
            facet(a, c, d, u, t)
        }
    }
    return Mesh(faces)
}

private fun paper(gart: Gart): Gartmap {
    val m = Gartmap(gart.d)
    for (y in 0 until H) for (x in 0 until W) {
        val dx = (x - 0.43f * W) / W
        val dy = (y - 0.38f * H) / H
        val pool = (1f - 1.7f * (dx * dx + dy * dy)).coerceIn(0f, 1f).pow(3)
        val fog = 0.5f + 0.5f * SimplexNoise.noise(x * 0.0015f + nz, y * 0.0015f)
        val ink = lerpColor(ground[1], ground[2], fog * 0.42f)
        m[x, y] = lerpColor(ground[0], ink, pool * 0.6f)
    }
    return m
}

fun main(args: Array<String>) {
    val gart = Gart.of("nacre", W, H)
    val sw = Stopwatch()
    val sheet = gart.gartvas()
    paper(gart).use { it.drawToCanvas(sheet) }

    val big = gart.supersampled(SS)
    big.canvas.save()
    big.canvas.scale(SS.toFloat(), SS.toFloat())
    sheet.snapshot().use { big.canvas.drawImage(it, 0f, 0f) }
    big.canvas.restore()
    val camera = Camera(W * SS * 0.5f, H * SS * 0.48f, W * SS * 0.31f, 7f)
    val mesh = grow(camera)
    println("nacre: seed=$SEED, $RIBS ribs, ${mesh.faces.size} facets")
    Scene.render(big.canvas, camera, mesh, big.d.w, big.d.h)
    val g = big.downsample(SS)

    gart.saveImage(g, OUT.ensureExtension())
    println("drawn in ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

package pixelmania.light

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.stipple.stippleNoisyDotDensity
import org.jetbrains.skia.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val clLight = NipponColors.col233_SHIRONERI
private val clBack = RetroColors.black01
private val clAccent = NipponColors.col029_GINSYU

data class LightSource(
    val position: Point,
    val direction: Float,   // radians, cone center direction
    val coneAngle: Float,   // radians, full aperture
    val length: Float,      // how far the cone reaches
)

fun main() {
    val gart = Gart.of("nippon", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    draw(g)
    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(g: Gartvas) {
    val c = g.canvas
    val d = g.d
    c.clear(clBack)

    // Straight horizontal path across the canvas.
    val path = PathBuilder()
        .moveTo(d.wf * 0.15f, d.hf * 0.15f)
        .lineTo(d.wf * 0.85f, d.hf * 0.15f)
        .detach()

    val path2 = PathBuilder()
        .moveTo(d.wf * 0.15f, d.hf * 0.85f)
        //.lineTo(d.wf * 0.5f, d.hf * 0.9f)
        .lineTo(d.wf * 0.85f, d.hf * 0.85f)
        .detach()

    val lights = lightSourcesOnPath(
        path = path,
        count = 140,
        coneAngle = PIf / 1f,
        length = 420f,
    )
    val lights2 = lightSourcesOnPath(
        path = path2,
        count = 140,
        coneAngle = PIf / 1f,
        length = 420f,
        directionOffset = PIf
    )
    val all = arrayOf(lights, lights2)

    // Render the cones offscreen as DARK on WHITE so stippleNoisyDotDensity,
    // which dots dark areas, treats the cones as the subject.
    val temp = Gartvas(d)
    temp.canvas.clear(Color.WHITE)
    all.forEach { l ->
        for (ls in l) {
            drawConeParticles(temp.canvas, ls, particleCount = 1200, color = Color.BLACK)
        }
    }

    // stipple

    val tmap = Gartmap(temp)
    stippleNoisyDotDensity(tmap, backgroundColor = Color.WHITE)

    // Composite the stippled cones back onto the main canvas as clLight dots.
    val mainMap = Gartmap(g)
    val n = d.area
    for (i in 0 until n) {
        val p = tmap.pixels[i]
        val r = (p shr 16) and 0xFF
        val gc = (p shr 8) and 0xFF
        val b = p and 0xFF
        if (r + gc + b < 100) {
            mainMap.pixels[i] = clLight
        }
    }
    mainMap.drawToCanvas()

    c.drawPath(path, strokeOf(clLight, 6f).roundStroke())
    c.drawPath(path2, strokeOf(clLight, 6f).roundStroke())

    val circle = Circle.of(Point.relative(0.5f, 0.5f, d), 240f)
    val cx = circle.x
    val cy = circle.y
    val r = circle.radius
    val r2 = r * r
    val recolorMap = Gartmap(g)
    val xMin = (cx - r).toInt().coerceAtLeast(0)
    val xMax = (cx + r).toInt().coerceAtMost(d.w - 1)
    val yMin = (cy - r).toInt().coerceAtLeast(0)
    val yMax = (cy + r).toInt().coerceAtMost(d.h - 1)
    for (y in yMin..yMax) {
        for (x in xMin..xMax) {
            val dx = x - cx
            val dy = y - cy
            if (dx * dx + dy * dy <= r2 && recolorMap[x, y] == clLight) {
                recolorMap[x, y] = clAccent
            }
        }
    }
    recolorMap.drawToCanvas()

//    val dotPaint = fillOf(clLight)
//    for (ls in lights) {
//        c.drawCircle(ls.position.x, ls.position.y, 3f, dotPaint)
//    }
}

/**
 * Draws a noisy light cone as scattered particles whose density and brightness
 * fall off with distance from the source. Particles are area-sampled in the
 * cone: r is uniform along the radial axis (so per-area density ~ 1/r as the
 * cone widens), theta is uniform across the aperture with a cosine softening
 * at the edges. Size and alpha then scale with the same intensity.
 */
private fun drawConeParticles(
    c: Canvas,
    ls: LightSource,
    particleCount: Int,
    color: Int = clLight,
) {
    val halfAngle = ls.coneAngle / 2f
    val maxRadius = 3.5f

    repeat(particleCount) {
        val r = rndf() * ls.length
        val tOffset = rndf(-1f, 1f)
        val angle = ls.direction + tOffset * halfAngle

        val distFalloff = 1f - r / ls.length
        val edgeFalloff = cos(tOffset * HALF_PIf)
        val intensity = distFalloff * edgeFalloff

        val x = ls.position.x + cos(angle) * r
        val y = ls.position.y + sin(angle) * r

        val pr = maxRadius * intensity * rndf(0.4f, 1.2f)
        val pa = (255f * intensity * rndf(0.5f, 1f)).toInt().coerceIn(0, 255)

        c.drawCircle(x, y, pr, fillOf(color).alpha(pa))
    }
}

private fun lightSourcesOnPath(
    path: Path,
    count: Int,
    coneAngle: Float,
    length: Float,
    directionOffset: Float = 0f,
): List<LightSource> {
    val measure = PathMeasure(path)
    val total = measure.length
    val step = total / (count - 1)
    val sources = mutableListOf<LightSource>()
    for (i in 0 until count) {
        val dist = i * step
        val pos = measure.getPosition(dist) ?: continue
        val tan = measure.getTangent(dist) ?: continue
        // Base direction: perpendicular to tangent, 90° CCW. directionOffset
        // rotates from there: 0 = perpendicular, PIf = flipped 180°, PIf/2 =
        // along the tangent, any value in between for arbitrary tilt.
        val direction = atan2(tan.x, -tan.y) + directionOffset
        sources.add(LightSource(pos, direction, coneAngle, length))
    }
    return sources
}


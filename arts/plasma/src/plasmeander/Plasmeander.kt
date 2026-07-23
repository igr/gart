package plasmeander

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.ColorRamp
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.darken
import dev.oblac.gart.color.lerpColor
import dev.oblac.gart.color.lumOf
import dev.oblac.gart.color.shiftLuma
import dev.oblac.gart.math.hash01
import dev.oblac.gart.noise.SimplexNoise
import dev.oblac.gart.reactiondiffusion.GrayScott
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A sinuous current made from Gray-Scott (Turing) ridges.
 */
private const val W = 840
private const val H = 1260
private const val SCALE = 2
private const val SW = W / SCALE
private const val SH = H / SCALE
private const val STEPS = 2100
private const val DEFAULT_SEED = 731L
private const val DEFAULT_LINE_WIDTH = 0.75f
private const val DEFAULT_DENSITY = 1.55f
private const val DEFAULT_LINE_LENGTH = 2.45f
private const val DEFAULT_FLOW_ALIGNMENT = 0.62f
private const val FLOW_HALF_WIDTH = 132f
private const val PAPER_MOTTLE = 5.5f
private const val EDGE_DISTRESS = 0.075f
private const val FINAL_AA_SIGMA = 0.68f

private val BACKGROUND = argb(255, 7, 24, 45)
private val GROOVE = argb(255, 5, 17, 34)
private val BASE_PALETTE = Palettes.cool85
private val ART_PALETTE = (BASE_PALETTE + BASE_PALETTE.reversed()).expand(256)
private val FLOW_COLORS = ColorRamp.of(ART_PALETTE)

private data class Drifter(val x: Float, val y: Float, val radius: Float, val color: Int, val phase: Float)

fun main() {
    val seed = System.getProperty("seed")?.toLong() ?: DEFAULT_SEED
    val out = System.getProperty("out") ?: "work/plasmeander.png"
    val lineWidth = System.getProperty("lineWidth")?.toFloatOrNull()?.coerceIn(0.15f, 1f) ?: DEFAULT_LINE_WIDTH
    val density = System.getProperty("density")?.toFloatOrNull()?.coerceIn(0.70f, 2.20f) ?: DEFAULT_DENSITY
    val lineLength = System.getProperty("lineLength")?.toFloatOrNull()?.coerceIn(0.50f, 4.00f) ?: DEFAULT_LINE_LENGTH
    val flowAlignment = System.getProperty("flowAlignment")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: DEFAULT_FLOW_ALIGNMENT
    val headless = System.getProperty("headless") != null || System.getProperty("gart.headless") != null
    val rng = Random(seed)
    val drifters = makeDrifters(seed)

    // Turing wavelength scales approximately with sqrt(diffusion). Dividing
    // both coefficients by density² gives tighter ridges

    val diffusionScale = density * density
    val rd = GrayScott(
        SW, SH,
        feed = 0.055f - (lineLength - 1f) * 0.012f,
        kill = 0.062f - (lineLength - 1f) * 0.002f,
        Du = 0.066f / diffusionScale,
        Dv = 0.033f / diffusionScale,
    )
    seedPattern(rd, rng, density, lineLength)
    applyObstacles(rd, drifters)
    val alignedU = FloatArray(SW * SH)
    val alignedV = FloatArray(SW * SH)
    repeat(STEPS) { step ->
        rd.step()
        if (flowAlignment > 0f && step % 3 == 2) alignToFlow(rd, flowAlignment, alignedU, alignedV)
        applyObstacles(rd, drifters)
    }

    val small = Gartmap(Dimension(SW, SH))
    renderPattern(rd, small, seed, lineWidth)

    val gart = Gart.of("turing-flow", W, H)
    val g = gart.gartvas()
    g.canvas.clear(BACKGROUND)
    val image = small.image()
    g.canvas.drawImageRect(
        image,
        Rect.makeWH(SW.toFloat(), SH.toFloat()),
        Rect.makeWH(W.toFloat(), H.toFloat()),
        SamplingMode.MITCHELL,
        null,
        true,
    )
    drawDrifters(g.canvas, drifters)
    applyTactileFinish(g, seed)

    val output = if (out.endsWith(".png", true)) out else "$out.png"
    gart.saveImage(g, output)
    small.close()
    println("turing-flow seed=$seed lineWidth=$lineWidth density=$density lineLength=$lineLength flowAlignment=$flowAlignment steps=$STEPS -> $output")
    if (!headless) gart.window().showImage(g)
}

private fun seedPattern(rd: GrayScott, rng: Random, density: Float, lineLength: Float) {
    repeat((520 * density * density / lineLength).toInt()) {
        val radius = (rng.nextInt(4, 9) / density).toInt().coerceAtLeast(2)
        val x = rng.nextInt(radius, SW - radius)
        val y = rng.nextInt(radius, SH - radius)
        rd.stampU(x, y, radius, 0.50f)
        rd.stampV(x, y, radius, 0.25f)
    }


    var y = -8
    while (y < SH + 8) {
        val x = flowCenter(y.toFloat()).toInt()
        val radius = (17 / density).toInt().coerceAtLeast(5)
        rd.stampU(x, y, radius, 0.48f)
        rd.stampV(x, y, radius, 0.27f)
        y += (20 / density).toInt().coerceAtLeast(7)
    }
}

/** Directional diffusion that stretches the Turing concentrations along the current!!! */
private fun alignToFlow(rd: GrayScott, amount: Float, nextU: FloatArray, nextV: FloatArray) {
    for (y in 0 until SH) {
        val center = flowCenter(y.toFloat())
        val dxdy = (flowCenter((y + 1).toFloat()) - flowCenter((y - 1).toFloat())) * 0.5f
        val invLength = 1f / sqrt(dxdy * dxdy + 1f)
        val tx = dxdy * invLength
        val ty = invLength
        for (x in 0 until SW) {
            val x1 = (x + tx * 1.6f).roundToInt().coerceIn(0, SW - 1)
            val y1 = (y + ty * 1.6f).roundToInt().coerceIn(0, SH - 1)
            val x0 = (x - tx * 1.6f).roundToInt().coerceIn(0, SW - 1)
            val y0 = (y - ty * 1.6f).roundToInt().coerceIn(0, SH - 1)
            val riverDistance = (x - center) / 112f
            val influence = 0.12f + 0.88f * exp(-riverDistance * riverDistance * 1.4f)
            val blend = amount * influence * 0.24f
            val i = y * SW + x
            val u = rd.u(x, y)
            val v = rd.v(x, y)
            val tangentU = (rd.u(x0, y0) + rd.u(x1, y1)) * 0.5f
            val tangentV = (rd.v(x0, y0) + rd.v(x1, y1)) * 0.5f
            nextU[i] = u + (tangentU - u) * blend
            nextV[i] = v + (tangentV - v) * blend
        }
    }
    for (y in 0 until SH) {
        for (x in 0 until SW) {
            val i = y * SW + x
            rd.setU(x, y, nextU[i])
            rd.setV(x, y, nextV[i])
        }
    }
}

private fun renderPattern(rd: GrayScott, map: Gartmap, seed: Long, lineWidth: Float) {
    val phase = (seed % 997).toFloat() * 0.013f

    val ridgeStart = 0.58f - lineWidth * 0.40f
    val ridgeEnd = (ridgeStart + 0.18f).coerceAtMost(0.78f)
    val activatorValues = FloatArray(SW * SH)
    for (y in 0 until SH) {
        for (x in 0 until SW) {
            activatorValues[y * SW + x] = rd.v(x, y).coerceIn(0f, 0.42f) / 0.42f
        }
    }
    val smoothActivator = smoothScalarField(activatorValues)
    val ridgeValues = FloatArray(SW * SH)
    for (y in 0 until SH) {
        for (x in 0 until SW) {
            ridgeValues[y * SW + x] = smoothstep(ridgeStart, ridgeEnd, smoothActivator[y * SW + x])
        }
    }

    // Flood-fill
    val chunkColors = IntArray(SW * SH)
    val visited = BooleanArray(SW * SH)
    val queue = IntArray(SW * SH)
    val chunkBands = IntArray(SW * SH) { i ->
        val x = i % SW
        val y = i / SW
        ((y + 13f * sin(x * 0.071f + phase)) / 38f).toInt()
    }
    for (start in ridgeValues.indices) {
        if (visited[start] || ridgeValues[start] <= 0.06f) continue
        val band = chunkBands[start]
        var read = 0
        var write = 0
        var weightedFlow = 0f
        var totalWeight = 0f
        queue[write++] = start
        visited[start] = true
        while (read < write) {
            val i = queue[read++]
            val x = i % SW
            val y = i / SW
            val weight = ridgeValues[i]
            weightedFlow += flowGradientPosition(x, y, phase) * weight
            totalWeight += weight
            if (x > 0) write = enqueueRidge(i - 1, band, ridgeValues, chunkBands, visited, queue, write)
            if (x + 1 < SW) write = enqueueRidge(i + 1, band, ridgeValues, chunkBands, visited, queue, write)
            if (y > 0) write = enqueueRidge(i - SW, band, ridgeValues, chunkBands, visited, queue, write)
            if (y + 1 < SH) write = enqueueRidge(i + SW, band, ridgeValues, chunkBands, visited, queue, write)
        }
        val color = FLOW_COLORS.colorAt((weightedFlow / totalWeight.coerceAtLeast(0.001f)).coerceIn(0f, 1f))
        for (i in 0 until write) chunkColors[queue[i]] = color
    }
    val antialiasedChunkColors = antialiasFloodColors(chunkColors, ridgeValues)

    for (y in 0 until SH) {
        val center = flowCenter(y.toFloat())
        for (x in 0 until SW) {
            val i = y * SW + x
            val ridge = ridgeValues[i]
            val signed = (x - center) / FLOW_HALF_WIDTH
            val river = exp(-signed * signed * 1.75f)
            val flowColor = if (antialiasedChunkColors[i] != 0) {
                antialiasedChunkColors[i]
            } else {
                FLOW_COLORS.colorAt(flowGradientPosition(x, y, phase))
            }
            val outerColor = argb(255, 19, 52, 82)
            val ridgeColor = lerpColor(outerColor, flowColor, smoothstep(0.10f, 0.78f, river))

            // Dark substrate remains visible between bright activator ridges.
            val base = lerpColor(BACKGROUND, GROOVE, 0.38f + 0.34f * river)
            map[x, y] = lerpColor(base, ridgeColor, ridge * (0.58f + 0.42f * river))
        }
    }
}

/** Gaussian pass before contour extraction smooths the Turing geometry itself. */
private fun smoothScalarField(source: FloatArray): FloatArray {
    val output = FloatArray(source.size)
    val kernel = intArrayOf(1, 2, 1)
    for (y in 0 until SH) {
        for (x in 0 until SW) {
            var sum = 0f
            var weightSum = 0
            for (dy in -1..1) {
                val ny = (y + dy).coerceIn(0, SH - 1)
                for (dx in -1..1) {
                    val nx = (x + dx).coerceIn(0, SW - 1)
                    val weight = kernel[dx + 1] * kernel[dy + 1]
                    sum += source[ny * SW + nx] * weight
                    weightSum += weight
                }
            }
            output[y * SW + x] = sum / weightSum
        }
    }
    return output
}

/**
 * Smooth only color seams created by the flood-fill chunks. Dark gaps are
 * excluded, so neighboring Turing ridges do not bleed into one another.
 * Not sure if it works.
 */
private fun antialiasFloodColors(colors: IntArray, ridgeValues: FloatArray): IntArray {
    val output = colors.copyOf()
    for (y in 0 until SH) {
        for (x in 0 until SW) {
            val i = y * SW + x
            if (colors[i] == 0) continue
            var redSum = 0f
            var greenSum = 0f
            var blueSum = 0f
            var weightSum = 0f
            for (dy in -1..1) {
                val ny = y + dy
                if (ny !in 0 until SH) continue
                for (dx in -1..1) {
                    val nx = x + dx
                    if (nx !in 0 until SW) continue
                    val ni = ny * SW + nx
                    val color = colors[ni]
                    if (color == 0) continue
                    val kernel = if (dx == 0 && dy == 0) 4f else if (dx == 0 || dy == 0) 2f else 1f
                    val weight = kernel * ridgeValues[ni].coerceAtLeast(0.05f)
                    redSum += ((color ushr 16) and 0xFF) * weight
                    greenSum += ((color ushr 8) and 0xFF) * weight
                    blueSum += (color and 0xFF) * weight
                    weightSum += weight
                }
            }
            if (weightSum > 0f) {
                output[i] = argb(
                    255,
                    (redSum / weightSum).toInt(),
                    (greenSum / weightSum).toInt(),
                    (blueSum / weightSum).toInt(),
                )
            }
        }
    }
    return output
}

private fun enqueueRidge(
    index: Int,
    band: Int,
    ridgeValues: FloatArray,
    chunkBands: IntArray,
    visited: BooleanArray,
    queue: IntArray,
    write: Int,
): Int {
    if (visited[index] || ridgeValues[index] <= 0.06f || chunkBands[index] != band) return write
    visited[index] = true
    queue[write] = index
    return write + 1
}

private fun flowGradientPosition(x: Int, y: Int, phase: Float): Float {
    val signed = (x - flowCenter(y.toFloat())) / FLOW_HALF_WIDTH
    val across = ((signed + 1.08f) / 2.16f).coerceIn(0f, 1f)
    return (across + 0.055f * sin(y * 0.032f + phase)).coerceIn(0f, 1f)
}

private fun flowCenter(y: Float): Float {
    val n = y / SH
    return SW * (0.50f + 0.155f * sin(n * 2f * PI.toFloat() + 0.35f) + 0.045f * sin(n * 6f * PI.toFloat() - 0.8f))
}

private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun makeDrifters(seed: Long): List<Drifter> {
    val rng = Random(seed xor 0x51E4A9B7L)
    val spheres = ArrayList<Drifter>()
    repeat(13) { i ->
        val radius = rng.nextFloat() * 15f + 22f
        var candidate: Drifter? = null
        repeat(500) {
            val y = rng.nextFloat() * (H - radius * 2) + radius
            val x = if (i < 6) {
                // Put the major spheres in the current so the pattern visibly parts around themm
                (flowCenter(y / SCALE) * SCALE + rng.nextFloat() * 230f - 115f).coerceIn(radius, W - radius)
            } else {
                rng.nextFloat() * (W - radius * 2) + radius
            }
            val proposed = Drifter(x, y, radius, ART_PALETTE[rng.nextInt(ART_PALETTE.size)], rng.nextFloat() * 2f * PI.toFloat())
            val clear = spheres.all { other ->
                val dx = proposed.x - other.x
                val dy = proposed.y - other.y
                val spacing = proposed.radius + other.radius + 14f
                dx * dx + dy * dy >= spacing * spacing
            }
            if (clear) {
                candidate = proposed
                return@repeat
            }
        }
        candidate?.let(spheres::add)
    }
    return spheres
}

private fun applyObstacles(rd: GrayScott, drifters: List<Drifter>) {
    for (sphere in drifters) {
        val cx = sphere.x / SCALE
        val cy = sphere.y / SCALE
        val radius = (sphere.radius + 8f) / SCALE
        val x0 = (cx - radius).toInt().coerceAtLeast(0)
        val x1 = (cx + radius).toInt().coerceAtMost(SW - 1)
        val y0 = (cy - radius).toInt().coerceAtLeast(0)
        val y1 = (cy + radius).toInt().coerceAtMost(SH - 1)
        for (y in y0..y1) {
            for (x in x0..x1) {
                val dx = x - cx
                val dy = y - cy
                val angle = kotlin.math.atan2(dy, dx)
                val edge = radius * islandRadius(angle, sphere.phase)
                if (dx * dx + dy * dy <= edge * edge) {
                    rd.setU(x, y, 1f)
                    rd.setV(x, y, 0f)
                }
            }
        }
    }
}

private fun drawDrifters(canvas: Canvas, drifters: List<Drifter>) {
    val lightX = flowCenter(SH * 0.5f) * SCALE
    val lightY = H * 0.5f
    for (sphere in drifters) {
        val toLightX = lightX - sphere.x
        val toLightY = lightY - sphere.y
        val lightDistance = sqrt(toLightX * toLightX + toLightY * toLightY).coerceAtLeast(1f)
        val lx = toLightX / lightDistance
        val ly = toLightY / lightDistance
        val shadowPath = islandPath(sphere, -lx * 5f, -ly * 5f)
        val shadow = Paint().apply {
            isAntiAlias = true
            color = argb(105, 0, 0, 0)
        }
        canvas.drawPath(shadowPath, shadow)
        shadow.close()

        val path = islandPath(sphere)
        val body = Paint().apply {
            isAntiAlias = true
            color = sphere.color
        }
        canvas.drawPath(path, body)
        body.close()

        val edge = Paint().apply {
            isAntiAlias = true
            mode = PaintMode.STROKE
            strokeWidth = 3f
            color = darken(sphere.color, 0.52f)
        }
        canvas.drawPath(path, edge)
        edge.close()
        path.close()
        shadowPath.close()
    }
}

private fun islandRadius(angle: Float, phase: Float): Float =
    1f + 0.085f * sin(angle * 3f + phase) + 0.045f * sin(angle * 5f - phase * 1.7f)

private fun islandPath(sphere: Drifter, dx: Float = 0f, dy: Float = 0f): Path {
    val path = PathBuilder()
    val points = 40
    for (i in 0 until points) {
        val angle = i * 2f * PI.toFloat() / points
        val r = sphere.radius * islandRadius(angle, sphere.phase)
        val x = sphere.x + dx + kotlin.math.cos(angle) * r
        val y = sphere.y + dy + sin(angle) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.closePath()
    return path.detach()
}

private fun applyTactileFinish(g: Gartvas, seed: Long) {
    val map = Gartmap(g)
    val source = map.pixels.copyOf()
    val seedOffsetX = (seed % 997).toFloat() * 0.31f
    val seedOffsetY = (seed % 613).toFloat() * 0.27f

    for (y in 0 until H) {
        for (x in 0 until W) {
            val i = y * W + x
            val original = source[i]
            var treated = original

            // remove occasional bright edge pixels toward their darkest neighbor
            val neighbors = intArrayOf(
                source[y * W + (x - 1).coerceAtLeast(0)],
                source[y * W + (x + 1).coerceAtMost(W - 1)],
                source[(y - 1).coerceAtLeast(0) * W + x],
                source[(y + 1).coerceAtMost(H - 1) * W + x],
            )
            val darkest = neighbors.minBy(::lumOf)
            if (lumOf(original) - lumOf(darkest) > 34f && hash01(x, y, seed.toInt() xor 0x34D1) < EDGE_DISTRESS) {
                treated = lerpColor(original, darkest, 0.58f)
            }

            // Two broad simplex octaves create cloudy paper variation
            val broad = SimplexNoise.noise(x * 0.0065f + seedOffsetX, y * 0.0065f + seedOffsetY)
            val fiber = SimplexNoise.noise(x * 0.021f - seedOffsetY, y * 0.017f + seedOffsetX)
            val delta = (broad * 0.76f + fiber * 0.24f) * PAPER_MOTTLE
            map.pixels[i] = shiftLuma(treated, delta)
        }
    }
    map.drawToCanvas(g)
    map.close()
}

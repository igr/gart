package dev.oblac.gart.flowforce.glst

import dev.oblac.gart.Gart
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.flow.Flow2
import dev.oblac.gart.flow.FlowField
import dev.oblac.gart.flow.StreamlineTracer
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.d
import dev.oblac.gart.math.rndi
import dev.oblac.gart.noise.OpenSimplexNoise
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import kotlin.math.atan2
import kotlin.math.sqrt

//private val backColor = BgColors.warmBlack2
private val backColor = Palettes.cool9.last()

fun main() {
    val gart = Gart.of("glst", 1024, 1024)
    val d = gart.d
    val g = gart.gartvas()
    val c = g.canvas

    val simplex = OpenSimplexNoise(123L)
    val flowField = FlowField.of(d) { x, y ->
        val noiseAngle = simplex.random2D(x * 0.002, y * 0.002) * 100f
        Flow2(Degrees.of(noiseAngle), StreamlineTracer.STEP_SIZE)
    }

    // 3 tracers: sparse, medium, dense
    val paths1 = StreamlineTracer(d, flowField, 280f, 550).trace()
    val paths2 = StreamlineTracer(d, flowField, 60f, 300).trace()
    val paths3 = StreamlineTracer(d, flowField, 6f, 50).trace()

    val p = Palettes.cool9.removeLast()
//    val p = Palettes.cool10
//    val p = Palettes.cool101
//    val p = Palettes.cool115
//    val p = Palettes.cool141
    val pr = p.shuffle()

    c.clear(backColor)
    //flowField.drawField2(c, d, 50)

    val outlineGap = 6f

    // Layer 1: sparse, thickest lines
    val strokeWidth1 = 58f
    paths1.forEach { path ->
        hipline(c, path, strokeWidth1, pr)
    }
    val outlines1 = paths1.map { it.toOutline(strokeWidth1 + outlineGap).outline }

    // Layer 2: medium density, medium lines, skip overlapping
    val strokeWidth2 = 14f
    val drawnPaths2 = mutableListOf<Path>()
    paths2.forEach { path ->
        if (!overlapsAny(path, outlines1)) {
            hipline(c, path, strokeWidth2, pr)
            //c.drawPath(path, strokeOf(strokeWidth2, p.safe(1)))
            drawnPaths2.add(path)
        }
    }
    val outlines2 = outlines1 + drawnPaths2.map { it.toOutline(strokeWidth2 + outlineGap).outline }

    // Layer 3: dense, thinnest lines, skip overlapping
    val strokeWidth3 = 1.5f
    paths3.forEach { path ->
        if (!overlapsAny(path, outlines2)) {
            dottedline(c, path, strokeWidth3, p)
        }
    }

    gart.saveImage(g)
    val w = gart.window()
    w.showImage(g)
}

private fun dottedline(
    c: Canvas,
    path: Path,
    strokeWidth3: Float,
    p: Palette
) {
    c.drawPath(path, strokeOf(strokeWidth3, p.safe(1)))
    path.toPoints(20).shuffled().take(4).forEach {
        c.drawCircle(it.x, it.y, 2f, fillOf(backColor))
    }
}

private fun hipline(
    c: Canvas,
    path: Path,
    strokeWidth: Float,
    p: Palette
) {
    val pathLen = path.length()
    val colors = if (pathLen > 400) 14 else 4
    val n = 1000
    val chunks = path.toPoints(n).chunkedAt(*randomIndexes(colors, n), overlap = 10)
    chunks.forEachIndexed { index, chunk ->
        c.drawPath(chunk.toPath(), strokeOf(strokeWidth, p.shuffle() % index))
    }
}

private fun radialFlow(x: Float, y: Float, cx: Float, cy: Float, radius: Float, strength: Float, outward: Boolean): Flow2 {
    val dx = x - cx
    val dy = y - cy
    val dist = sqrt(dx * dx + dy * dy)
    if (dist >= radius) return Flow2(Degrees.of(0f), 0f)
    val radialAngle = Math.toDegrees(atan2(dx.d(), dy.d())).toFloat()
    val angle = if (outward) radialAngle else radialAngle + 180f
    val strength = strength
    return Flow2(Degrees.of(angle), strength)
}

private fun randomIndexes(n: Int, max: Int): IntArray {
    return IntArray(n) { rndi(max) }.apply { sort() }
}


/**
 * Splits list into chunks at given indices.
 * Each chunk (except the first) starts [overlap] elements before its index to prevent gaps.
 * E.g., list of 10 elements, chunkedAt(3, 7) overlap=1 -> [0..3], [2..7], [6..9]
 */
private fun <T> List<T>.chunkedAt(vararg indices: Int, overlap: Int = 1): List<List<T>> {
    val sorted = indices.sorted()
    val result = mutableListOf<List<T>>()
    var start = 0
    for (idx in sorted) {
        val end = idx.coerceAtMost(size)
        if (start < end) result.add(subList(start, end))
        start = (end - overlap).coerceAtLeast(0)
    }
    if (start < size) result.add(subList(start, size))
    return result
}

private fun overlapsAny(path: Path, outlines: List<Path>): Boolean {
    val samplePoints = path.toPoints(10)
    return outlines.any { outline ->
        samplePoints.any { point -> outline.contains(point.x, point.y) }
    }
}

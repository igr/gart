package dev.oblac.gart.lines

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.math.DOUBLE_PIf
import dev.oblac.gart.math.PIf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintStrokeCap
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CPoint(
    val x: Float,
    val y: Float,
    val dir: Float,
    val level: Int
)

data class CLine(
    val x: Float,
    val y: Float,
    val xEnd: Float,
    val yEnd: Float,
    val level: Int
)

private val pal = Palettes.cool9

fun main() {
    val gart = Gart.of("citymap", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    draw(c, d)
    gart.saveImage(g)
    w.showImage(g)
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(BgColors.coolDark)
    val streets = generateCityMap(
        r = 1.1f,
        angle = 0.06f,
        pBranch = 0.2f
    )

    c.save()
    val scale = minOf(
        d.w / 80f,
        d.h / 80f
    )
    c.scale(scale, scale)

    val lineSize = 2f

    val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = lineSize
        strokeCap = PaintStrokeCap.ROUND
        isAntiAlias = true
    }

    streets
        .sortedByDescending { it.level }
        .forEach {
            paint.strokeWidth = lineSize * (1f / (it.level + 1f))
            paint.color = pal.safe(it.level)

            c.drawLine(
                it.x, it.y,
                it.xEnd, it.yEnd,
                paint
            )
        }

    c.restore()
}


private fun generateCityMap(
    r: Float = 0.6f,                // radius, aka step
    angle: Float = DOUBLE_PIf / 180f, // how straight the lines are
    pBranch: Float = 0.1f,          // probability of branching
    n: Int = 50000,
    maxTrials: Int = 300,
    maxExceeds: Int = 60,
    width: Float = 80f,
    height: Float = 80f,
    seed: Long = System.currentTimeMillis(),
): List<CLine> {

    val rnd = Random(seed)
    val points = mutableListOf<CPoint>()
    val lines = mutableListOf<CLine>()

    // initial point is center
    points.add(CPoint(width / 2f, height / 2f, 0f, 0))

    var i = 1   // index, start with second point
    var totalExceeds = 0
    var totalTrials = 0
    var trials = 0

    while (i < n && totalExceeds < maxExceeds) {
        if (totalTrials > maxTrials) {
            totalExceeds++
        }

        var valid = false
        totalTrials = 0

        while (!valid) {
            val randomPoint = if (trials > maxTrials / 10 || i < 500) {
                points[rnd.nextInt(i)] // pick random point
            } else {
                // pick a point from last N
                val last = (1 + trials) * 20
                val startIdx = maxOf(i - last, 0)
                val endIdx = i
                points[rnd.nextInt(startIdx, endIdx)]
            }

            val branch = rnd.nextFloat() <= pBranch

//            val alpha = randomPoint.dir +
//                random.nextFloat() * (2 * angle) - angle +
//                (if (branch) (if (random.nextBoolean()) -1f else 1f) * PIf / 2f else 0f)

            // this produced more curved lines, i like it better
            val alpha = randomPoint.dir +
                rnd.nextFloat() * (2 * angle) +
                (if (branch) (if (rnd.nextBoolean()) -1f else 1f) * PIf / 2f else 0f)

            val levelFactor = 1f + 1f / (if (branch) randomPoint.level + 1 else randomPoint.level)
            val xj = randomPoint.x + cos(alpha) * r * levelFactor
            val yj = randomPoint.y + sin(alpha) * r * levelFactor
            val newLevel = if (branch) randomPoint.level + 1 else randomPoint.level

            if (xj < 0 || xj > width || yj < 0 || yj > height) {    // out of bounds
                trials++
                continue
            }

            val minDistanceToExistingPoints = points.minOf {
                sqrt((xj - it.x).pow(2) + (yj - it.y).pow(2))
            }

            if (minDistanceToExistingPoints >= r) {
                points.add(CPoint(xj, yj, alpha, newLevel))
                lines.add(CLine(xj, yj, randomPoint.x, randomPoint.y, newLevel))
                valid = true
                trials = 0
            } else {
                trials++
            }
            totalTrials = maxOf(totalTrials, trials)
        }

        i++
    }
    return lines.filter { it.level > 0 }
}

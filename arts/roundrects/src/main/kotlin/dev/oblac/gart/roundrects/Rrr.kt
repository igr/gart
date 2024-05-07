package dev.oblac.gart.roundrects

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.math.rndi
import dev.oblac.gart.toSeconds
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.RRect
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration


data class BigBox(val d: Dimension, val maxX: Int, val maxY: Int) {
    val cellW = d.wf / maxX
    val cellH = d.hf / maxY
    val inner = rndi(5, 10)

    val allCells = (0 until maxX).flatMap { x ->
        (0 until maxY).map { y ->
            Cell(this, x, y, inner)
        }
    }
}

class Cell(private val box: BigBox, private val x: Int, private val y: Int, private val inner: Int) {
    private val x1 = x * box.cellW
    private val y1 = y * box.cellH
    private val strokeSize = 2f
    private val delta = (box.cellW - inner * strokeSize) / inner / 2


    fun draw(canvas: Canvas, duration: Duration) {
        val time = duration.toSeconds()
        var xxx1 = x1 + delta
        var yyy1 = y1 + delta
        var www1 = box.cellH - 2 * delta
        var hhh1 = box.cellH - 2 * delta
        val d = delta * (sin(time + x) + 1) / 2
        val d2 = delta * ((cos(time + y) + 1) / 4 + 0.25f)

        for (i in 0..inner) {
            canvas.drawRRect(RRect.makeXYWH(xxx1, yyy1, www1, hhh1, 10f), strokeOfWhite(strokeSize))
            xxx1 += strokeSize + d
            yyy1 += strokeSize + d2
            www1 -= 2 * strokeSize + delta
            hhh1 -= 2 * strokeSize + delta
        }
    }
}

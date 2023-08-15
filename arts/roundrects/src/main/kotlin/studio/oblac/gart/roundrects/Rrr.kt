package studio.oblac.gart.roundrects

import studio.oblac.gart.Dimension
import studio.oblac.gart.gfx.strokeOfWhite
import studio.oblac.gart.math.rnd
import studio.oblac.gart.skia.Canvas
import org.jetbrains.skia.RRect
import kotlin.math.cos
import kotlin.math.sin


data class BigBox(val d: Dimension, val maxX: Int, val maxY: Int) {
    val cellW = d.wf / maxX
    val cellH = d.hf / maxY
    val inner = rnd(5, 10)

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


    fun draw(canvas: Canvas, time: Float) {
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

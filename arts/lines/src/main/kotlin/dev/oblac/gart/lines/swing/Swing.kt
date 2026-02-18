package dev.oblac.gart.lines.swing

import dev.oblac.gart.*
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("swing", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(g: Gartvas) : Drawing(g) {
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(c, d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)
    val total = 20
    repeat(total) { i ->
        val s = Swing(
            y = 300f + i * 10f,
            right = 600f + i * 10f,
            gap = 200f,
            left = 300f + i * 10f,
            radius = 200f)
        val path = topLevelPath(d, s)
        val stroke = if (i == 14) {
            strokeOf(RetroColors.red01, 34f)
        } else {
            strokeOf(RetroColors.white01, 4f + i * 0.5f)
        }.apply {
            this.alpha = 255 - total * 10 + i * 10
        }
        c.drawPath(path, stroke)
    }
}

private data class Swing(
    val y: Float,
    val right: Float,
    val gap: Float,
    val left: Float,
    val radius: Float
)

private fun topLevelPath(d: Dimension, swing: Swing): Path {
    val path = PathBuilder()
    val y = swing.y
    path.moveTo(0f, y)
    path.lineTo(swing.right, y)
    val middleY = y + swing.gap
    path.arcTo(Rect(swing.right, y, swing.right + swing.gap, middleY), 270f, 180f, false)
    path.lineTo(swing.left, middleY)
    val lastY = middleY + swing.gap
    path.arcTo(Rect(swing.left - swing.gap, middleY, swing.left, lastY), 270f, -180f, false)
    path.lineTo(d.wf, lastY)
    return path.detach()
}


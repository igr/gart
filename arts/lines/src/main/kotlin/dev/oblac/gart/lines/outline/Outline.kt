package dev.oblac.gart.lines.outline

import dev.oblac.gart.Dimension
import dev.oblac.gart.Drawing
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.jfa.Jfa
import dev.oblac.gart.util.repeatSequence
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("outline", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)

//    Line(Point(0f, d.hf + 40f), Point(d.cx, -40f)).also {
//        c.drawLine(it, strokeOf(RetroColors.red01, 200f).apply {
//            this.pathEffect = PathEffect.makeDiscrete(100f, 10f, 0)
//        })
//    }
//    Line(Point(-40f, 440f), Point(d.wf + 40f, d.hf - 40f)).also {
//        c.drawLine(it, strokeOf(RetroColors.red01, 160f).apply {
//            this.pathEffect = PathEffect.makeDiscrete(100f, 10f, 0)
//        })
//    }

    val path1 = Circle(Point(d.cx - 250f, d.cy - 250f), 80f).toPath()
    val path2 = isoscelesTriangle(Point(d.cx + 300f, d.cy - 150f), 100f, 300f, Degrees.of(220)).path
    val path3 = createNtagonPoints(4, d.cx + 250f, d.cy + 250f, 80f, 10f).toPath()
    val path4 = isoscelesTriangle(Point(d.cx - 250f, d.cy + 250f), 100f, 250f, Degrees.of(20)).path
    val path = Path().apply {
        addPath(path1)
        addPath(path2)
        addPath(path3)
        addPath(path4)
    }

    val w = 25f
    val total = (1024 / (2 * w)).toInt() + 6
    val paths = repeatSequence(total).map {
        Jfa(d).outlinePath(path, outlineWidth = 1024f - it * (2 * w), outerOnly = true)
    }.toList()

    paths.forEach {
        c.drawPath(
            it, strokeOf(RetroColors.white01, w).roundStroke()
        )
    }
    val samples = 8000
    val array = Array<Point?>(samples) { null }
    paths.forEach {
        val sampledPointsCount = it.getPoints(array, samples)
        if (sampledPointsCount == 0) return@forEach
        val randomSliceIndex = (sampledPointsCount * 0.62).toInt()
        val array2 = array.sliceArray(randomSliceIndex..<sampledPointsCount) + array.sliceArray(0..randomSliceIndex)
        array2
            .filter { p -> p != null }
            .take((sampledPointsCount * 0.5).toInt())
            .forEach { p ->
                c.drawCircle(p!!, 12.5f, fillOf(RetroColors.red01))
            }
    }

    //c.drawPath(path, strokeOf(RetroColors.red01, 20f).roundStroke())
}

private fun windowTitle(size: Int) {
    val g = Gartvas.of(size, size)
    val c = g.canvas
}



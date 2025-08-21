package dev.oblac.gart.rects.rubik

import dev.oblac.gart.*
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.shader.createNoiseGrainFilter
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PathEffect

private enum class Type {
    ONE, TWO, THREE
}
private val type = Type.THREE

fun main() {
    val gart = Gart.of("impossible-rubik-${type.toString().lowercase()}", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
//        b.updatePixelsFromCanvas()
//        b.drawToCanvas()
        c.draw(g)
    }
}

private val A = when(type) {
    Type.ONE -> 38f
    else -> 48f
}

private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.white01)
    repeat(10) {
        val x = 80 + it * 60f
        val y = 260 + it * 46f
        c.save()
        if (type == Type.THREE) {
            c.rotate(it * 8f, 512f, 512f)
        }
        cube(c, x, y, 500f - A * it, it)
        c.restore()
    }
}

private fun discreet(paint: Paint) {
    paint.pathEffect = PathEffect.makeDiscrete(1f, 2f, 10)
}

private val lineStroke = strokeOf(RetroColors.white01, 2f).apply { discreet(this) }
private val fill = fillOf(RetroColors.black01).apply { discreet(this) }.apply {
    this.imageFilter = createNoiseGrainFilter(
        1f, Dimension(1024, 1024),
    )
}
private val fill2 = fillOf(RetroColors.black01).apply { discreet(this) }.apply {
    this.imageFilter = createNoiseGrainFilter(
        1.8f, Dimension(1024, 1024),
    )
}
private val fill3 = fillOf(RetroColors.black01).apply { discreet(this) }.apply {
    this.imageFilter = createNoiseGrainFilter(
        0.9f, Dimension(1024, 1024),
    )
}
private val fillRed = fillOf(RetroColors.red01).apply { discreet(this) }.apply {
    this.imageFilter = createNoiseGrainFilter(
        1f, Dimension(1024, 1024),
    )
}

private fun cube(c: Canvas, x: Float, y: Float, size: Float, index: Int): RectIsometricTop {
    val angle = Degrees.of(30f)

    val top = RectIsometricTop(x, y, size, size, angle)
    if (index == 2) {
        c.drawPath(top.path(), fillRed)
    } else {
        c.drawPath(top.path(), fill)
    }
    c.drawPath(top.path(), lineStroke)

    val left = RectIsometricLeft(x, y, size, size, angle)
    if (index == 4) {
        c.drawPath(left.path(), fillRed)
    } else {
        c.drawPath(left.path(), fill2)
    }
    c.drawPath(left.path(), lineStroke)

    val right = RectIsometricRight(x + top.width() / 2, y + top.height() / 2, size, size, angle)
    if (index == 7) {
        c.drawPath(right.path(), fillRed)
    } else {
        c.drawPath(right.path(), fill3)
    }
    c.drawPath(right.path(), lineStroke)
    return top
}


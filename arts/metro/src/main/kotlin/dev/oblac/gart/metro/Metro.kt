package dev.oblac.gart.metro

import dev.oblac.gart.Gart
import dev.oblac.gart.gfx.Colors
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.PaintStrokeCap
import dev.oblac.gart.skia.PathEffect
import dev.oblac.gart.skia.Point

val gart = Gart.of(
    "metro",
    1024, 640, 10
)

val poly1 = RLine(
    Point(-100f, 600f),
    300f, 100f,
    Point(400f, 240f),
    160f, 40f
)
val poly2 = poly1.nextTo(
    Point(700f, 500f),
    100f, -40f
)
val poly3 = poly2.nextTo(
    Point(1200f, 300f),
    500f, 20f
)

val polyA = RLine(
    Point(-100f, 160f),
    300f, -100f,
    Point(420f, 540f),
    160f, 40f
)
val polyB = polyA.nextTo(
    Point(650f, 300f),
    100f, 40f
)
val polyC = polyB.nextTo(
    Point(1200f, 600f),
    300f, -40f
)

val polyT = RLine(
    Point(400f, -100f),
    10f, -300f,
    Point(500f, 240f),
    20f, -200f
)
val polyT2 = polyT.nextTo(
    Point(300f, 800f),
    100f, -400f
)

val polyG = RLine(
    Point(1050f, -100f),
    30f, -300f,
    Point(800f, 340f),
    -20f, -200f
)
val polyG2 = polyG.nextTo(
    Point(1000f, 800f),
    100f, -340f
)

val polyX = RLine(
    Point(500f, 400f),
    30f, -300f,
    Point(700f, 700f),
    20f, -300f
)

var indexOffset = 0

fun main() {
    println(gart)
    val g = gart.gartvas()

    g.canvas.clear(Colors.blackColor.toColor())

    drawR3(g.canvas, polyG)
    drawR3(g.canvas, polyG2)
    drawR3(g.canvas, polyX)

    drawR2(g.canvas, polyA)
    drawR(g.canvas, poly1)
    drawR(g.canvas, poly2)
    drawR2(g.canvas, polyB)
    drawR2(g.canvas, polyC)
    drawR(g.canvas, poly3)

    drawR3(g.canvas, polyT)
    drawR3(g.canvas, polyT2)

    gart.showImage(g)
}

private fun drawR(c: Canvas, rl: RLine) {
    c.drawPath(rl.rect, fillOfBlack())
    val count = 60
    rl.toLines(count).forEachIndexed { index, it ->
        c.drawLine(it.first.x, it.first.y, it.second.x, it.second.y,
//            strokeOf(Palettes.cool24.safe(index + indexOffset), 6f).apply {
//                this.strokeCap = PaintStrokeCap.ROUND
//                this.pathEffect = PathEffect.makeCorner(10f)
//            })
            strokeOf(Palettes.cool24.relative(index / count.toFloat()), 6f).apply {
                this.strokeCap = PaintStrokeCap.ROUND
                this.pathEffect = PathEffect.makeCorner(10f)
            })
    }
}
private fun drawR2(c: Canvas, rl: RLine) {
    c.drawPath(rl.rect, fillOfBlack())
    rl.toLines(9).forEachIndexed { index, it ->
        c.drawLine(it.first.x, it.first.y, it.second.x, it.second.y,
            strokeOf(Palettes.cool7.safe(index), 4f).apply {
                this.strokeCap = PaintStrokeCap.ROUND
                this.pathEffect = PathEffect.makeCorner(10f)
            })
    }
}
private fun drawR3(c: Canvas, rl: RLine) {
    c.drawPath(rl.rect, fillOfBlack())
    rl.toLines(9).forEachIndexed { index, it ->
        c.drawLine(it.first.x, it.first.y, it.second.x, it.second.y,
            strokeOf(Palettes.cool15.safe(index), 4f).apply {
                this.strokeCap = PaintStrokeCap.ROUND
                this.pathEffect = PathEffect.makeCorner(10f)
            })
    }
}

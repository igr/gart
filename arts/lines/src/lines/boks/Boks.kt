package lines.boks

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.f
import dev.oblac.gart.math.i
import dev.oblac.gart.perspective.Block3D
import dev.oblac.gart.util.forSequence
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("boks", 1024, 1024)
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
    c.clear(RetroColors.black01)
    c.save()
    c.rotate(45f, d.width / 2f, d.height / 2f)

    val pLeft = Point(0, 512f)
    val pRight = Point(1024f, 512f)
    val xCenter = 512f
    val block = Block3D.of(
        vpLeft = pLeft,
        vpRight = pRight,
        frontBottom = Point(xCenter, 1024f),
        height = 1024f,
        leftWidth = 400f,
        rightWidth = 500f
    )

    val left = block.left

    val yFrom = left.topPoint().y
    val yTo = left.bottomPoint().y

    forSequence(yFrom.i(), yTo.i(), 40).forEachIndexed { index, y ->
        val p = PathBuilder()
            .moveTo(pLeft)
            .lineTo(Point(xCenter, y))
            .lineTo(pRight)
            .lineTo(Point(xCenter, y + index))
            .lineTo(pLeft)
            .detach()
        c.drawPath(p, fillOf(RetroColors.white01))

        if (index == 8) {
            c.drawCircle(450f, y + index.f() + 36f, 180f, fillOf(RetroColors.red01))
        }
    }

    c.restore()
}

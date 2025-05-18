package dev.oblac.gart

import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.spirograph.Spirograph
import dev.oblac.gart.spirograph.createSpirograph
import dev.oblac.gart.util.circular
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathOp
import org.jetbrains.skia.Rect

private val dimension = Dimension(1024, 1024)
private var pathId = 1
private var radius = 80f
private var step = Degrees.of(20)
private var show = false
private var repetitions = 20

private fun buildSpiro(): Spirograph {
    val path = when (pathId) {
        1 -> createCircleOfPoints(dimension.center, 250f, 100).toClosedPath()
        2 -> {
            Triangle.equilateral(dimension.center, 200f, Degrees.of(45f))
                .path.toPoints(100).toClosedPath()
        }
        3 -> {
            Rect.makeXYWH(
                dimension.center.x - 200f,
                dimension.center.y - 200f,
                400f, 400f,
            ).path().toPoints(100).toClosedPath()
        }
        4 -> {
            val canvasSize = 1024f
            val radius = 200f

            val centerX = canvasSize / 2
            val centerY = canvasSize / 2

            // Position the circles to form a clover shape centered in the canvas
            val circle1 = Path().apply {
                addOval(Rect.makeXYWH(centerX - radius, centerY - radius * 1.5f, radius * 2, radius * 2))
            }
            val circle2 = Path().apply {
                addOval(Rect.makeXYWH(centerX - radius * 1.2f, centerY - radius / 2f, radius * 2, radius * 2))
            }
            val circle3 = Path().apply {
                addOval(Rect.makeXYWH(centerX + radius * 0.5f, centerY - radius / 2f, radius * 2, radius * 2))
            }
            val result = combinePathsWithOp(PathOp.UNION, circle1, circle2, circle3)
            result.toPoints(100).toClosedPath()
        }

        else -> {
            throw IllegalArgumentException()
        }
    }
    println("pathId: $pathId")
    println("radius: $radius")
    println("step: $step")
    println("repetitions: $repetitions")
    println("---")
    return createSpirograph(
        dimension,
        path,
        radius,
        step,
        samples = 120,
        repetitions = repetitions,
    )
}


fun main() {
    val gart = Gart.of("ExampleSpirograph", 1024, 1024)
    println(gart)

    val g = gart.gartvas()
    val c = g.canvas

    draw(c, buildSpiro())
    var image = g.snapshot()

    // show image
    gart.window()
        .showImage{ image }
        .onKey {
            when (it) {
                Key.KEY_1 -> pathId = 1
                Key.KEY_2 -> pathId = 2
                Key.KEY_3 -> pathId = 3
                Key.KEY_4 -> pathId = 4
                Key.KEY_Q -> radius -= 5f
                Key.KEY_A -> radius += 5f
                Key.KEY_W -> step -= Degrees.of(1f)
                Key.KEY_S -> step += Degrees.of(1f)
                Key.KEY_E -> repetitions -= 1
                Key.KEY_D -> repetitions += 1
                Key.KEY_SPACE -> show = !show
                else -> {}
            }
            draw(c, buildSpiro())
            image = g.snapshot()
        }
}

private fun draw(c: Canvas, spiro: Spirograph) {
    c.clear(Colors.black)
    val ps = spiro.points.circular()
    ps.forEachIndexed { ndx, p ->
        val line = Line(ps[ndx - 1], p)
        c.drawLine(line, strokeOfWhite(1f))
    }
    if (show) {
        spiro.tangents.circular().forEach {
            c.drawDLine(it, 20f, strokeOfRed(2f))
            c.drawPoint(it.p, strokeOfYellow(4f))
        }
    }
}

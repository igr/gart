package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.fillOfBlue
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.*
import org.jetbrains.skia.BlendMode.SRC_OVER

fun main() {
    val gart = Gart.of("exampleCanvas", 512, 512)
    println(gart)

    val d = gart.d
    val w = gart.window()

    val g = gart.gartvas()
    val c = g.canvas
    c.clear(Colors.white)

    // compose path effects
    c.drawLine(0f, 0f, d.wf, d.hf, strokeOf(Colors.red, 5f).apply {
        this.pathEffect = PathEffect.makeDash(floatArrayOf(10f, 10f), 10f)
            .makeCompose(PathEffect.makeDiscrete(4f, 4f, 20))
    })

    // draw patch
    drawPatch(c)

    c.drawTriangles(
        arrayOf(
            Point(300f, 300f), Point(400f, 300f), Point(350f, 400f),
            Point(350f, 200f), Point(450f, 200f), Point(400f, 300f)
        ),
        null,
        null,
        null,
        SRC_OVER,
        fillOfBlue()
    )

    w.showImage(g)
}

private fun drawPatch(c: Canvas) {
    val pts = arrayOf(
        Point(100f / 4f, 0f),
        Point(3f * 100f / 4f, 100f)
    )

    val p = Paint().apply {
        isAntiAlias = true
    }

    p.shader = Shader.makeLinearGradient(
        pts[0], pts[1], intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW), null, GradientStyle.DEFAULT
    )

    val cubics = arrayOf(
        Point(100f, 100f), Point(150f, 50f), Point(250f, 150f), Point(300f, 100f),
        Point(250f, 150f), Point(350f, 250f), Point(300f, 300f), Point(250f, 250f),
        Point(150f, 350f), Point(100f, 300f), Point(50f, 250f), Point(150f, 150f)
    )

    val texCoords = arrayOf(
        Point(0.0f, 0.0f), Point(100.0f, 0.0f),
        Point(100.0f, 100.0f), Point(0.0f, 100.0f)
    )

    c.drawPatch(
        cubics, intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW), texCoords, SRC_OVER, p
    )
}

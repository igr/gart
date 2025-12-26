package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.whfast.NBodySystem2D
import org.jetbrains.skia.Canvas

fun main() {
    val gart = Gart.of("ExampleWhfast", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    w.show(OrbitrDraw(g))
}

private class OrbitrDraw(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

val system = NBodySystem2D.innerSolarSystem()

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)
    repeat(1000) {
        system.step(0.01f)
        val bodies = system.bodies

        for (body2D in bodies) {
            val x1 = 512 + body2D.position.x * 200f
            val y1 = 512 + body2D.position.y * 200f
            c.drawCircle(x1, y1, 1f, fillOfRed())
        }
    }
}

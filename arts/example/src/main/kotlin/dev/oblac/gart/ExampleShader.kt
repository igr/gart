package dev.oblac.gart

import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.shader.createNeuroShader
import dev.oblac.gart.shader.toPaint

fun main() {
    val gart = Gart("ExampleShader")
    println(gart.name)

    val d = gart.dimension(400, 400)
    val w = gart.window(d, 60)
    val m = gart.movie(d)

    var tick = 0f

    m.record(w).show { c, _, f ->
        val p = createNeuroShader(tick, 0.1f).toPaint()
        c.drawPaint(p)
        c.drawCircle(200f, 200f, 100f, fillOfRed())

        if (f.new) {
            tick += 0.01f
        }
    }
}

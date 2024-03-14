package dev.oblac.gart

import dev.oblac.gart.gfx.BgColors
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.shader.createNeuroShader
import dev.oblac.gart.shader.toPaint

fun main() {
    val gart = Gart.of(
        "ExampleShader",
        400, 400
    )
    println("Example Shader")

    // show image
    with(gart) {
        w.show()
        m.draw {
            g.canvas.clear(BgColors.bg01)
            g.canvas.drawCircle(200f, 200f, 100f, fillOfRed())

            val s = createNeuroShader(f.time.inWholeMilliseconds.toFloat() / 1000f, 0.1f)
            val p = s.toPaint()
            g.canvas.drawPaint(p)
        }

    }
}

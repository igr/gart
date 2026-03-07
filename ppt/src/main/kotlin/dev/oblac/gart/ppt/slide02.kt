package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import kotlin.math.sin

val slide02 = DrawFrame { c, d, f ->
    c.clear(NipponColors.col016_KURENAI)
    val radius = if (f.frameTimeSeconds < 1.6f) {
        d.hf * (0.4f + sin(f.frameTimeSeconds))
    } else {
        d.hf * (0.4f + sin(1.6f))
    }
    c.drawCircle(Point(d.w3x2, d.hf - d.hf * 0.05f), radius, fillOf(NipponColors.col149_TOKIWA))
    c.drawTitle("What is Skiko?")

    val text = """
        • Skia bindings for Kotlin/Native, JVM, and JS
        • Maintained by JetBrains, open source
        • Low-level API, verbose, but powerful
    """.trimIndent()

    c.drawContent(text)
}

package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf

val slide02 = DrawFrame { c, d, f ->
    c.clear(CssColors.darkRed)
    c.drawCircle(Point(d.w3x2, d.hf - d.hf * 0.05f), d.hf * 0.4f, fillOf(CssColors.green))
    c.drawTitle("What is Skiko?")

    val text = """
        • Skia bindings for Kotlin/Native, JVM, and JS
        • Maintained by JetBrains, open source
        • Low-level API, verbose, but powerful
    """.trimIndent()

    c.drawContent(text)
}

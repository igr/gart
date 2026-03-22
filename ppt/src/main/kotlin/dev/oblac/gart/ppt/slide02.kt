package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.EaseFn
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf

val slide02 = DrawFrame { c, d, f ->
    c.clear(NipponColors.col016_KURENAI)
    val t = (f.frameTimeSeconds / 1.6f).coerceIn(0f, 1f)
    val radius = d.hf * (0.4f + EaseFn.QuadOut(t))
    c.drawCircle(Point(d.w3x2, d.hf - d.hf * 0.05f), radius, fillOf(NipponColors.col149_TOKIWA))
    c.drawTitle("What is Skiko?")

    val text = """
        • Skia bindings for Kotlin/Native, JVM, and JS
        • Maintained by JetBrains, open source
        • Low-level API, verbose, but powerful
        • Same domain as painting tools      
    """.trimIndent()

    c.drawContent(text)
}

package dev.oblac.gart.ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.Point
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import kotlin.math.sin

val slide01 = DrawFrame { c, d, f ->
    c.clear(NipponColors.col149_TOKIWA)
    val radius = if (f.frameTimeSeconds < 1.6f) {
        d.hf * (0.2f + sin(f.frameTimeSeconds))
    } else {
        d.hf * (0.2f + sin(1.6f))
    }
    c.drawCircle(Point(d.w3x2, d.hf * 0.08f), radius, fillOf(NipponColors.col016_KURENAI))
    c.drawTitle("What is Skia?")

    val text = """
        • 2D graphic engine for drawing Text, Geometries, and Images
        • Compact C++ OS graphics library, New BSD
        • Developed by Skia Inc., acquired by Google in 2005
        • Chrome browser, Chrome OS, Firefox, Firefox OS, and Android
        • Back-ends: CPU-based rasterization, PDF output, GPU OpenGL
        • Front-ends: SVG, PostScript, PDF, SWF and Adobe
    """.trimIndent()

    c.drawContent(text)
}

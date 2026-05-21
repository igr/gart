package bubbles.holes

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.addCircle
import dev.oblac.gart.gfx.shrink
import dev.oblac.gart.pack.simpleCirclePacker
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ClipMode
import org.jetbrains.skia.PathBuilder

fun main() {
    val gart = Gart.of("holes", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    // Hot reload requires a real class to be created, not a lambda!

    // save image
    draw(g.canvas, g.d)
    gart.saveImage(g)

    w.showImage(g)
}

private val pal = Palettes.cool97
//private val pal = Palettes.cool111

private fun draw(c: Canvas, d: Dimension) {
    c.clear(pal[0])

    val r = d.rect
    val box = d.rect.shrink(20f)
    repeat(pal.size - 1) {
        val path = PathBuilder()
        val templates = simpleCirclePacker(
            r,
            attempts = 800_000,
            minRadius = 20f, maxRadius = 60f,
            growth = 2,
            padding = 10,
            isInside = { it.isInsideOf(box) }
        )
        templates.forEach {
            path.addCircle(it)
        }
        c.clipPath(path.detach(), ClipMode.INTERSECT, true)
        c.clear(pal[it + 1])
    }
}

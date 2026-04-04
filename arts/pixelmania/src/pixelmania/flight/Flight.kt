package pixelmania.flight

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.SampleMode
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.rndf
import dev.oblac.gart.pixels.conformalWarp
import org.jetbrains.skia.Color

fun main() {
    val plt = Palettes.colormap016.expandReversed().expand(40)
    val gart = Gart.of("flight", 1024, 1024)

    val g = gart.gartvas()
    val c = g.canvas
    val d = gart.d
    c.clear(Color.WHITE)

    gridOfDimension(d, 40, 44).forEach {
        val pth = it.rect.shrink(-20f).offset(0f, rndf(-20f, 20f)).path()
        c.drawPath(pth, fillOf(plt.safe(it.row * 5 + it.col)))

//        c.drawLine(
//            it.rect.left / 2, it.rect.center().x, it.rect.right, it.rect.center().y / 2,
//            strokeOf(CssColors.white, 1.5f).alpha(100)
//        )
        c.drawLine(
            it.rect.left / 4, it.rect.center().x, it.rect.right, it.rect.center().y / 1.9f,
            strokeOf(CssColors.white, 1.5f).alpha(100)
        )
//        if (it.rect.left > 100) {
//            c.drawLine(
//                it.rect.left * 1.3f, it.rect.center().x * 2.1f, it.rect.right / 100, it.rect.center().y * 1.3f,
//                strokeOf(CssColors.white, 1.5f).alpha(100)
//            )
//        }
    }

    val b = Gartmap(g)
    val result = conformalWarp(
        src = b,
        outDimension = d,
        sampleMode = SampleMode.TILE,
        rInner = 0.01,
        rOuter = 8.0,
        background = CssColors.white
    ).image()

    c.drawImage(result)

    gart.saveImage(g)
    gart.window().showImage(g)
//    gart.window().showImage(result)
}

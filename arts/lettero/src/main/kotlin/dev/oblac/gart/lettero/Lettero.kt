package dev.oblac.gart.lettero

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.Media
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.math.doubleLoop
import dev.oblac.gart.skia.Font
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.skia.TextLine
import dev.oblac.gart.skia.Typeface
import dev.oblac.gart.util.loadResourceAsData
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

val jbMono = Typeface.makeFromData(loadResourceAsData("/JetBrainsMono-Bold.ttf"))
val jbMonoFont = Font(jbMono, 22.0f)

private val fonts = mutableMapOf<Float, Font>()
fun fontOf(size: Float): Font {
    return fonts.getOrPut(size) { Font(jbMono, size) }
}

val gart = Gart.of(
    "lettero",
    768, 1024,
    1
)

fun main() {
    with(gart) {
        println(name)
        
        val tick = f.marker().onEvery(1.seconds)

        val letters = listOf('I', 'G', 'O', '.', 'R', 'S')
        var count = 0;

        val p = Palettes.cool9
        val pdelta = 0

        w.show()
        a.record()
        a.draw {
            if (tick.now()) {
                g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(p[count + pdelta]))
                drawLetters(g, letters[count])
                count++
                if (count == letters.size) {
                    a.stop()
                }
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}

fun drawLetters(g: Gartvas, text: Char) {
    doubleLoop(44f to 64f, g.d.wf, g.d.hf, 20 to 20) { (i, j) ->
        val textLine = TextLine.make(text.toString(), fontOf(Random.nextDouble(8.0, 36.0).toFloat()))
        val deltax = textLine.width / 2
        val deltay = textLine.capHeight / 2
        g.canvas.drawTextLine(textLine, i - deltax, j + deltay, fillOfWhite())
        //g.canvas.drawPoint(i, j, strokeOfRed(2f))
    }
}

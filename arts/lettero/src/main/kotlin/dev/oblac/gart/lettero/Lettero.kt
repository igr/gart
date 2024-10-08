package dev.oblac.gart.lettero

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.font.FontFamily.JetBrainsMonoBold
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.math.doubleLoop
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.jetbrains.skia.TextLine
import kotlin.random.Random

val gart = Gart.of(
    "lettero",
    768, 1024, 1
)

fun main() {
    println(gart)

    val letters = listOf('I', 'G', 'O', '.', 'R', 'S')
    var count = 0

    val p = Palettes.cool9
    val pdelta = 0

    val w = gart.window()
    val m = gart.movie()

    m.record(w).show { c, d, f ->
        c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(p.safe(count + pdelta)))
        drawLetters(c, d, letters[count % letters.size])
        f.tick {
            count++
            if (count == letters.size) {
                m.stopRecording()
            }
        }
    }
}

fun drawLetters(c: Canvas, d: Dimension, text: Char) {
    doubleLoop(44f to 64f, d.wf, d.hf, 20 to 20) { (i, j) ->
        val textLine = TextLine.make(text.toString(), font(JetBrainsMonoBold, Random.nextDouble(8.0, 36.0).toFloat()))
        val deltax = textLine.width / 2
        val deltay = textLine.capHeight / 2
        c.drawTextLine(textLine, i - deltax, j + deltay, fillOfWhite())
        //g.canvas.drawPoint(i, j, strokeOfRed(2f))
    }
}

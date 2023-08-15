package studio.oblac.gart.lettero

import studio.oblac.gart.*
import studio.oblac.gart.gfx.Palettes
import studio.oblac.gart.gfx.fillOf
import studio.oblac.gart.gfx.fillOfWhite
import studio.oblac.gart.math.doubleLoop
import studio.oblac.gart.skia.Font
import studio.oblac.gart.skia.Rect
import studio.oblac.gart.skia.TextLine
import studio.oblac.gart.skia.Typeface
import studio.oblac.gart.util.loadResourceAsData
import kotlin.random.Random

val jbMono = Typeface.makeFromData(loadResourceAsData("/JetBrainsMono-Bold.ttf"))
val jbMonoFont = Font(jbMono, 22.0f)

private val fonts = mutableMapOf<Float, Font>()
fun fontOf(size: Float): Font {
    return fonts.getOrPut(size) { Font(jbMono, size) }
}

fun main() {
    val name = "lettero"
    println(name)

    val d = Dimension(768, 1024)
    val g = Gartvas(d)
    val w = Window(g).show()
    val v = GartvasVideo(g, "$name.mp4", 1)
    val tick = w.frames.marker().onEverySecond(1)

    val letters = listOf('I', 'G', 'O', '.', 'R', 'S')
    var count = 0;

    val p = Palettes.cool9
    val pdelta = 0

    w.paint2 {
        if (tick.now()) {
            g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(p[count + pdelta]))
            drawLetters(g, letters[count])
            v.addFrame()
            count++
            if (count == letters.size) {
                v.stopAndSaveVideo()
                return@paint2 false
            }

        }
        return@paint2 true
    }

    writeGartvasAsImage(g, "LetterO.png")
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

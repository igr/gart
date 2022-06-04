package ac.obl.gart.lettero

import ac.obl.gart.*
import ac.obl.gart.gfx.Palettes
import ac.obl.gart.gfx.fillOf
import ac.obl.gart.gfx.fillOfWhite
import ac.obl.gart.math.doubleLoop
import ac.obl.gart.skia.Font
import ac.obl.gart.skia.Rect
import ac.obl.gart.skia.TextLine
import ac.obl.gart.skia.Typeface
import ac.obl.gart.util.loadResourceAsData
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

    val box = Box(768, 1024)
    val g = Gartvas(box)
    val w = Window(g).show()
    val v = VideoGartvas(g).start("$name.mp4", 1)
    val tick = w.frames.marker().onEverySecond(1)

    val letters = listOf('I', 'G', 'O', '.', 'R', 'S')
    var count = 0;

    val p = Palettes.cool9
    val pdelta = 0

    w.paint2 {
        if (tick.now()) {
            g.canvas.drawRect(Rect(0f, 0f, box.wf, box.hf), fillOf(p[count + pdelta]))
            drawLetters(g, letters[count])
            v.addFrameIfRunning()
            count++
            if (count == letters.size) {
                v.save()
                return@paint2 false
            }

        }
        return@paint2 true
    }

    ImageWriter(g).save("LetterO.png")
}

fun drawLetters(g: Gartvas, text: Char) {
    doubleLoop(44f to 64f, g.box.wf, g.box.hf, 20 to 20) { (i, j) ->
        val textLine = TextLine.make(text.toString(), fontOf(Random.nextDouble(8.0, 36.0).toFloat()))
        val deltax = textLine.width / 2
        val deltay = textLine.capHeight / 2
        g.canvas.drawTextLine(textLine, i - deltax, j + deltay, fillOfWhite())
        //g.canvas.drawPoint(i, j, strokeOfRed(2f))
    }
}

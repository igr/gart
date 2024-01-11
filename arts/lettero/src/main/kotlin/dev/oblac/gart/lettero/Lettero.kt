package dev.oblac.gart.lettero

import dev.oblac.gart.*
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

fun main() {
    val name = "lettero"
    println(name)

    val d = Dimension(768, 1024)
    val g = Gartvas(d)
    val a = Animation(g)
    val f = a.frames
    val w = Window(a).show()
    val v = GartvasVideo(g, "$name.mp4", 1)
    val tick = f.marker().onEvery(1.seconds)

    val letters = listOf('I', 'G', 'O', '.', 'R', 'S')
    var count = 0;

    val p = Palettes.cool9
    val pdelta = 0

    w.drawWhile {
        if (tick.now()) {
            g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(p[count + pdelta]))
            drawLetters(g, letters[count])
            v.addFrame()
            count++
            if (count == letters.size) {
                v.stopAndSaveVideo()
                return@drawWhile false
            }

        }
        return@drawWhile true
    }

    g.writeSnapshotAsImage("LetterO.png")
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

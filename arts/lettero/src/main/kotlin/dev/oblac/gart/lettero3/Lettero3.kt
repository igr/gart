package dev.oblac.gart.lettero3

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.drawSprite
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.TextLine
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.math.sin

val text = Files.readString(
    Paths.get("arts/lettero/src/main/kotlin/dev/oblac/gart/lettero3/Lettero3.kt")
).replace("\n", "")
    .substringAfter(".substringAfter(")
    .trim().replace(Regex("\\s+"), " ").substring(31)

private fun draw(c: Canvas, d: Dimension) {
    val font = font(FontFamily.IBMPlexMono, 50f)
    val textLines = mutableMapOf<Char, TextLine>()
    var index = 0
    val amp = 20
    val off = 10
    val frequency = 1 / 80f
    val palette = Palettes.cool37

    c.clear(NipponColors.col248_SUMI)
    for (y in 0 until d.h step 50) {
        var x = 10.0
        while (x < d.w - 5) {
            val char = text[index]
            val width = 30 + amp * sin(x * frequency + y + off)
            val tlc = textLines.computeIfAbsent(char) { TextLine.make("$it", font) }
            val sprite = Gartvas.of(tlc.width, tlc.height)
            sprite.canvas.clear(Colors.transparent)
            sprite.canvas.drawTextLine(tlc, 0f, tlc.capHeight, fillOf(palette.safe(index)))

            c.drawSprite(sprite.sprite()) { it.scaleX(width / tlc.width).at(x, y - 10f) }

            index++
            x += width
        }
        println(y)
    }
}

fun main() {
    val gart = Gart.of("lettero3", 1024, 1024)
    val g = gart.gartvas()
    val c = g.canvas
    val w = gart.window()
    println(gart)
    draw(c, gart.d)
    gart.saveImage(g)
    w.showImage(g)
}

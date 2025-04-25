package dev.oblac.gart.lettero3

import dev.oblac.gart.*
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palette
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
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
    val sprites = mutableMapOf<Char, Sprite>()
    var index = 0
    val palette = Palettes.cool37
    val magic = true

    fun makeSpriteForChar(
        ch: Char, font: Font, palette: Palette, index: Int
    ): Sprite {
        val tlc = textLines.computeIfAbsent(ch) { TextLine.make("$it", font) }
        val gg = Gartvas.of(tlc.width, tlc.height)
        gg.canvas.clear(Colors.transparent)
        gg.canvas.drawTextLine(tlc, 0f, tlc.capHeight, fillOf(palette.safe(index)))
        return gg.sprite()
    }

    c.clear(NipponColors.col248_SUMI)
    for (y in 0 until d.h step 50) {
        var x = 10.0
        while (x < d.w - 5) {
            val char = text[index]
            val width = if (magic) {
                30 + 20 * sin(x / 80f + y + 0.4)
            } else {
                30 + 20 * sin(x / 84f + y + 10)
            }

            val sprite = if (magic) {
                sprites.computeIfAbsent(char) { makeSpriteForChar(it, font, palette, index) }
            } else {
                makeSpriteForChar(char, font, palette, index)
            }

            c.drawSprite(sprite) { it.scaleX(width / sprite.d.w).at(x, y - 10f) }

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

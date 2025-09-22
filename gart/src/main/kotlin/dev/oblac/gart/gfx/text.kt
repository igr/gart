package dev.oblac.gart.gfx

import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.TextLine

/**
 * Draws black text at the specified (x, y) position on the canvas, for debugging purposes.
 */
fun Canvas.drawBlackText(text: String, x: Float, y: Float) {
    this.drawTextLine(TextLine.make(text, font(FontFamily.IBMPlexMono, 16f)), x, y, fillOfBlack())
}

/**
 * Draws white text at the specified (x, y) position on the canvas, for debugging purposes.
 */
fun Canvas.drawWhiteText(text: String, x: Float, y: Float) {
    this.drawTextLine(TextLine.make(text, font(FontFamily.IBMPlexMono, 16f)), x, y, fillOfWhite())
}

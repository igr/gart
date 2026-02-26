package dev.oblac.gart.ppt

import dev.oblac.gart.Screen
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.dimension
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.drawMultilineStringInRect
import dev.oblac.gart.text.drawStringInRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect

val slides = listOf(
    slide01,
    slide02,
    slide03,
    slide04,
    slide05,
    slide06,
    slide07,
    slide08,
    slide09,
    slide10,
    slide11,
    slide12,
    slide13,
)

val screen = Screen.dimension()
const val aspectRatio = 1.55

// The rectangle for the content of the presentation, excluding borders,
// keep the aspect ratio, and centered on the screen.
val activeRect: Rect = run {
    val borderH = 200f
    val borderTop = 160f
    val borderBottom = 100f
    val availW = screen.wf - 2 * borderH
    val availH = screen.hf - borderTop - borderBottom

    val activeW: Float
    val activeH: Float
    if (availW / availH > aspectRatio) {
        activeH = availH
        activeW = (activeH * aspectRatio).toFloat()
    } else {
        activeW = availW
        activeH = (activeW / aspectRatio).toFloat()
    }

    val left = (screen.wf - activeW) / 2f
    val top = borderTop + (availH - activeH) / 2f
    Rect(left, top, left + activeW, top + activeH)
}

val titleBox = Rect(
    activeRect.left,
    activeRect.top,
    activeRect.right,
    activeRect.top + activeRect.dimension().ofH(0.2f)
)
val contentBox = Rect(
    activeRect.left,
    titleBox.bottom,
    activeRect.right,
    activeRect.bottom
)

val titleFont = font(FontFamily.Alice, screen.height * 0.1f)
const val titleColor = CssColors.white
val textFont = font(FontFamily.RethinkSans, screen.height * 0.044f)
const val textColor = CssColors.white

fun Canvas.drawTitle(text: String) {
    this.drawStringInRect(text, titleBox, titleFont, titleColor.toFillPaint())
}

fun Canvas.drawContent(text: String) =
    this.drawMultilineStringInRect(
        text,
        contentBox,
        textFont,
        textColor.toFillPaint(),
        HorizontalAlign.LEFT
    )

val codeFont = font(FontFamily.IBMPlexMono, screen.height * 0.03f)
val codePaint = CssColors.white.toFillPaint()

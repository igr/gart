package dev.oblac.gart.presentation

import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.Screen
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import org.jetbrains.skia.Rect

private val slides = listOf(
    slide01,
    slide02,
)
private var currentSlide = 1

/**************************
 * PRESENTATION DEFAULTS
 **************************/

internal val screen = Screen.dimension()

internal val borderH = 40f
internal val borderW = 100f

// The rectangle for the content of the presentation, excluding borders.
internal val activeRect = Rect(borderW, borderH, screen.wf - borderW, screen.hf - borderH)
internal val titleBox = Rect(activeRect.left, activeRect.top, activeRect.right, activeRect.top + 300f)

val fontTitle = font(FontFamily.OdibeeSans, 240f)
internal val fontText = font(FontFamily.SpaceMono, 80f)


fun main() {
    val gart = Gart.of("presentation", screen)
    println(gart)

    val w = gart.fullScreenWindow()
//    val w = gart.window()

    w.show { canvas, draw, f ->
        if (f.new) {
            if (f.frame % 100 == 0L) {
                println("Frame: ${f.frame}")
            }
        }
        slides[currentSlide - 1](canvas, draw)
    }.onKey {
        when (it) {
            Key.KEY_ESCAPE -> w.close()
            Key.KEY_SPACE, Key.KEY_RIGHT -> {
                if (currentSlide < slides.size) {
                    currentSlide++
                }
            }

            Key.KEY_LEFT, Key.KEY_BACKSPACE -> {
                if (currentSlide > 1) {
                    currentSlide--
                }
            }

            else -> {}
        }
    }
}

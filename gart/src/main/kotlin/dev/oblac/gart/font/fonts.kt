package dev.oblac.gart.font

import dev.oblac.gart.util.loadResourceAsData
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.Typeface

fun Typeface.Companion.makeFromData(data: Data): Typeface =
    FontMgr.default.makeFromData(data)
        ?: error("Failed to make from data")


private data class F(val typeface: Typeface, val size: Float)

private val fonts = mutableMapOf<F, Font>()

enum class FontFamily(internal val typeface: Typeface) {
    JetBrainsMonoBold(Typeface.makeFromData(loadResourceAsData("/fonts/JetBrainsMono-Bold.ttf"))),
    IBMPlexMonoBold(Typeface.makeFromData(loadResourceAsData("/fonts/IBMPlexMono-Bold.ttf"))),
    OdibeeSans(Typeface.makeFromData(loadResourceAsData("/fonts/OdibeeSans-Regular.ttf")));
}

fun font(family: FontFamily, size: Float): Font {
    return fonts.getOrPut(F(family.typeface, size)) { Font(family.typeface, size) }
}

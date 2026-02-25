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
    IBMPlexMono(Typeface.makeFromData(loadResourceAsData("/fonts/IBMPlexMono-Regular.ttf"))),
    IBMPlexMonoBold(Typeface.makeFromData(loadResourceAsData("/fonts/IBMPlexMono-Bold.ttf"))),
    OdibeeSans(Typeface.makeFromData(loadResourceAsData("/fonts/OdibeeSans-Regular.ttf"))),
    NotoSans(Typeface.makeFromData(loadResourceAsData("/fonts/NotoSans-Regular.ttf"))),
    NotoSansBold(Typeface.makeFromData(loadResourceAsData("/fonts/NotoSans-Bold.ttf"))),
    SpaceMono(Typeface.makeFromData(loadResourceAsData("/fonts/SpaceMono-Regular.ttf"))),
    SpaceMonoBold(Typeface.makeFromData(loadResourceAsData("/fonts/SpaceMono-Bold.ttf"))),
    RethinkSans(Typeface.makeFromData(loadResourceAsData("/fonts/RethinkSans-Regular.ttf"))),
    Alice(Typeface.makeFromData(loadResourceAsData("/fonts/Alice-Regular.ttf"))),
    Literata(Typeface.makeFromData(loadResourceAsData("/fonts/Literata-Regular.ttf"))),
}

fun font(family: FontFamily, size: Float): Font {
    return fonts.getOrPut(F(family.typeface, size)) { Font(family.typeface, size) }
}

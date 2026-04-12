package dev.oblac.gart.color

import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Gradient

fun gradientOf(colors: IntArray, positions: FloatArray? = null): Gradient {
    return Gradient(
        colors = Gradient.Colors(
            colors = colors.map { Color4f.of(it) }.toTypedArray(),
            positions = positions,
            tileMode = FilterTileMode.CLAMP
        )
    )
}

fun gradientOf(colors: Array<Color4f>, positions: FloatArray? = null): Gradient {
    return Gradient(
        colors = Gradient.Colors(
            colors = colors,
            positions = positions,
            tileMode = FilterTileMode.CLAMP
        )
    )
}

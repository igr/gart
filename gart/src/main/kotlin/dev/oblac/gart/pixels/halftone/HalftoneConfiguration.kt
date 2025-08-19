package dev.oblac.gart.pixels.halftone

import dev.oblac.gart.color.RGBA

/**
 * Configuration for halftone rendering.
 */
data class HalftoneConfiguration(
    val dotSize: Int = 10,
    val dotResolution: Int = 5,
    val yellowChannel: YellowChannel = YellowChannel(82.5),
    val cyanChannel: CyanChannel = CyanChannel(112.5),
    val magentaChannel: MagentaChannel = MagentaChannel(52.5),
    val keyChannel: KeyChannel = KeyChannel(22.5),
)

data class YellowChannel(
    override val angle: Double,
) : ColorChannel {
    override val color: RGBA = RGBA.YELLOW
}

data class CyanChannel(
    override val angle: Double,
) : ColorChannel {
    override val color: RGBA = RGBA.CYAN
}

data class MagentaChannel(
    override val angle: Double,
) : ColorChannel {
    override val color: RGBA = RGBA.MAGENTA
}

data class KeyChannel(
    override val angle: Double,
) : ColorChannel {
    override val color: RGBA = RGBA.BLACK
}

/**
 * Color channel information for CMYK processing.
 */
sealed interface ColorChannel {
    val angle: Double
    val color: RGBA
}


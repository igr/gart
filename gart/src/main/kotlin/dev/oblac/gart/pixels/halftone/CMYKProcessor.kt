package dev.oblac.gart.pixels.halftone

import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.color.space.ColorCMYK
import dev.oblac.gart.color.space.RGBA
import kotlin.math.max

/**
 * Extracts a specific color channel from source image and
 * creates a grayscale representation.
 */
fun extractColorChannel(
    source: Pixels,
    channel: ColorChannel
): Pixels {
    val result = MemPixels(source.d)

    source.forEach { x, y, color ->
        val grayscaleValue = when {
            channel is KeyChannel -> extractKeyChannel(color)
            else -> extractColorChannelValue(color, channel)
        }

        // Convert to grayscale pixel (all channels same value)
        val grayscalePixel = argb(255, grayscaleValue, grayscaleValue, grayscaleValue)
        result[x, y] = grayscalePixel
    }
    return result
}

/**
 * Extracts the key (black) channel using K = 255 - max(R,G,B).
 */
private fun extractKeyChannel(color: Int): Int {
    val r = red(color)
    val g = green(color)
    val b = blue(color)
    val keyValue = 255 - max(r, max(g, b))
    return 255 - keyValue // Invert for grayscale representation
}

/**
 * Extracts a specific RGB channel with CMYK processing.
 */
private fun extractColorChannelValue(color: Int, channel: ColorChannel): Int {
    val r = red(color)
    val g = green(color)
    val b = blue(color)

    // Calculate key value (black)
    val keyValue = 255 - max(r, max(g, b))

    // Get the complement of the specific channel
    val channelValue = when (channel) {
        is CyanChannel -> r     // Red channel for cyan separation
        is MagentaChannel -> g  // Green channel for magenta separation
        is YellowChannel -> b   // Blue channel for yellow separation
        else -> 0
    }

    val complement = 255 - channelValue

    // Apply CMYK separation formula: complement - key
    val result = complement - keyValue

    // Return inverted result for proper grayscale representation
    return 255 - result.coerceIn(0, 255)
}

fun joinGrayscaleCMYKChannels(cyanChannel: Pixels, magentaChannel: Pixels, yellowChannel: Pixels, keyChannel: Pixels, target: MemPixels) {
    target.forEach { x, y, _ ->
        // Extract grayscale intensity from each channel (stored as R component, since its grayscale)
        val yellowIntensity = red(yellowChannel[x, y])
        val magentaIntensity = red(magentaChannel[x, y])
        val cyanIntensity = red(cyanChannel[x, y])
        val keyIntensity = red(keyChannel[x, y])

        // The extracted grayscale values represent the "darkness" for each CMYK component
        // We need to invert them to get the actual CMYK ink densities
        // Higher grayscale value = more ink needed for that component
        val colorCMYK = ColorCMYK.of(
            c = (255 - cyanIntensity) / 255f,      // Invert: dark areas need more cyan ink
            m = (255 - magentaIntensity) / 255f,   // Invert: dark areas need more magenta ink
            y = (255 - yellowIntensity) / 255f,    // Invert: dark areas need more yellow ink
            k = (255 - keyIntensity) / 255f        // Invert: dark areas need more black ink
        )
        //val value = colorCMYK.toRGBA().value
        val value = colorCMYK.toPureInk().value

        target[x, y] = value
    }
}

class YellowChannel() : ColorChannel {
    override val color: RGBA = RGBA.YELLOW
}

class CyanChannel() : ColorChannel {
    override val color: RGBA = RGBA.CYAN
}

class MagentaChannel : ColorChannel {
    override val color: RGBA = RGBA.MAGENTA
}

class KeyChannel : ColorChannel {
    override val color: RGBA = RGBA.BLACK
}

/**
 * Color channel information for CMYK processing.
 */
sealed interface ColorChannel {
    val color: RGBA

    companion object {
        val YELLOW = YellowChannel()
        val CYAN = CyanChannel()
        val MAGENTA = MagentaChannel()
        val KEY = KeyChannel()
    }
}

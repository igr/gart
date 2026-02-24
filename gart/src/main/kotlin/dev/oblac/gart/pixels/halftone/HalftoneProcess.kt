package dev.oblac.gart.pixels.halftone

import dev.oblac.gart.MemPixels
import dev.oblac.gart.Pixels
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.red
import dev.oblac.gart.color.space.ColorCMYK
import dev.oblac.gart.color.space.RGBA
import dev.oblac.gart.color.space.RGBA.Companion.BLACK

/**
 * Processes an input image to create a CMYK halftone effect.
 *
 * @param source The input image pixels
 * @param config Configuration for halftone processing
 * @return The processed halftone image pixels
 */
fun halftoneProcess(source: Pixels, config: HalftoneConfiguration = HalftoneConfiguration()): Pixels {
    val target = MemPixels(source.d)
    target.fill(CssColors.white) // Initialize with white background for CMYK processing
    with(config) {
        processSingleChannel(source, target, dotSize, dotResolution, yellowAngle, ColorChannel.YELLOW, true)
        processSingleChannel(source, target, dotSize, dotResolution, magentaAngle, ColorChannel.MAGENTA, false)
        processSingleChannel(source, target, dotSize, dotResolution, cyanAngle, ColorChannel.CYAN, false)
        processSingleChannel(source, target, dotSize, dotResolution, keyAngle, ColorChannel.KEY, false)
    }
    return target
}

/**
 * Processes a single CMYK color channel.
 */
private fun processSingleChannel(
    source: Pixels,
    target: Pixels,
    dotSize: Int,
    dotResolution: Int,
    angle: Float,
    channel: ColorChannel,
    isFirstChannel: Boolean
) {
    // Extract the color channel into a grayscale representation
    val channelPixels = extractColorChannel(source, channel)

    // Create a separate buffer for this CMYK channel's halftone pattern
    val channelHalftone = MemPixels(source.d)
    channelHalftone.fill(CssColors.white)

    // Apply halftone pattern to this channel (always renders as black dots)
    renderHalftone(
        source = channelPixels,
        target = channelHalftone,
        angle = angle,
        dotSize = dotSize,
        dotResolution = dotResolution,
        color = BLACK, // Always use black dots to represent ink density
        isLayer = !isFirstChannel
    )

    // Combine this channel with the result using CMYK logic
    combineChannelWithResult(channelHalftone, target, channel)
}

/**
 * Combines a single CMYK channel halftone with the result pixels.
 */
private fun combineChannelWithResult(
    channelHalftone: Pixels,
    target: Pixels,
    channel: ColorChannel
) {
    target.forEach { x, y, existingColor ->
        val inkDensity = 1.0f - (red(channelHalftone[x, y]) / 255.0f)

        if (inkDensity > 0) {
            // Apply this channel's ink to the existing color
            val currentCMYK = ColorCMYK.of(RGBA.of(existingColor).toColor4f())
            val newCMYK = when (channel) {
                is KeyChannel -> currentCMYK.blendK(inkDensity)
                is CyanChannel -> currentCMYK.blendC(inkDensity)
                is MagentaChannel -> currentCMYK.blendM(inkDensity)
                is YellowChannel -> currentCMYK.blendY(inkDensity)
            }
            target[x, y] = RGBA.of(newCMYK.toColor4f()).value
        }
    }
}

/**
 * Processes an input image to create a CMYK halftone effect with channel joining.
 * It is less memory efficient than `halftoneProcess` as it creates separate buffers for each channel
 * and then joins them at the end.
 */
fun processHalftoneWithJoin(source: Pixels, config: HalftoneConfiguration = HalftoneConfiguration()): Pixels {
    val yellowChannel = extractColorChannel(source, ColorChannel.YELLOW)
    val magentaChannel = extractColorChannel(source, ColorChannel.MAGENTA)
    val cyanChannel = extractColorChannel(source, ColorChannel.CYAN)
    val keyChannel = extractColorChannel(source, ColorChannel.KEY)

    val memC = MemPixels(source.d)
    renderHalftone(
        cyanChannel, memC,
        angle = config.cyanAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = BLACK
    )
    val memK = MemPixels(source.d)
    renderHalftone(
        keyChannel, memK,
        angle = config.keyAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = BLACK
    )
    val memM = MemPixels(source.d)
    renderHalftone(
        magentaChannel, memM,
        angle = config.magentaAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = BLACK
    )
    val memY = MemPixels(source.d)
    renderHalftone(
        yellowChannel, memY,
        angle = config.yellowAngle,
        dotSize = config.dotSize,
        dotResolution = config.dotResolution,
        color = BLACK
    )
    val target = MemPixels(source.d)
    joinGrayscaleCMYKChannels(
        memC, memM, memY, memK,
        target,
    )
    return target
}


package dev.oblac.gart.noise

import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Shader

class NoiseColor(
    baseX: Float = 0.5f,
    baseY: Float = 0.5f,
    octaves: Int = 4,
    seed: Float = 2.0f,
    noiseType: NoiseType = NoiseType.FRACTAL
) {

    enum class NoiseType {
        FRACTAL,
        TURBULENCE
    }

    enum class BlendType(val blendMode: BlendMode) {
        /**
         * Colors become darker, and the noise is RGB
         */
        DARKER_RGB(BlendMode.SRC_OVER),

        /**
         * Colors become gray, and the noise is RGB
         */
        GRAY_TV_RGB(BlendMode.SRC_IN),

        /**
         * Same color, just noise.
         */
        HARD_NOISE(BlendMode.DST_IN),   // DST_OUT

        /**
         * Colors become almost gray.
         */
        DARK_NOIR(BlendMode.MODULATE),

        /**
         * Just beautiful noise.
         */
        NOISE(BlendMode.SCREEN),    // COLOR_DODGE

        /**
         * Almost no noise.
         */
        LIGHT_NOISE(BlendMode.OVERLAY),
        DARKEN(BlendMode.DARKEN),
        SATURATED(BlendMode.SATURATION),
        PRINT(BlendMode.LUMINOSITY)
    }

    private val noiseShader = when (noiseType) {
        NoiseType.TURBULENCE -> Shader.makeTurbulence(
            baseFrequencyX = baseX,
            baseFrequencyY = baseY,
            numOctaves = octaves,
            seed = seed
        )

        NoiseType.FRACTAL -> Shader.makeFractalNoise(
            baseFrequencyX = baseX,
            baseFrequencyY = baseY,
            numOctaves = octaves,
            seed = seed
        )
    }

    fun composeShader(color: Int, blend: BlendType = BlendType.PRINT) = Shader.makeBlend(
        blend.blendMode,
        dst = Shader.makeColor(color),
        src = noiseShader
    )
}

package dev.oblac.gart.shader

import dev.oblac.gart.Dimension
import org.jetbrains.skia.ImageFilter

// https://www.shadertoy.com/view/3sGGR
private val sksl = """
uniform float2 iResolution;
uniform float intensity;
uniform shader image;

half4 main(float2 coord) {
    // Normalized pixel coordinates (from 0 to 1)
    float2 uv = coord / iResolution.xy;

    float mdf = intensity;  // increase for noise amount 
    float noise = fract(sin(dot(uv, half2(12.9898,78.233)*2.0)) * 43758.5453);
    half4 tex = image.eval(coord);
    
    half4 col = tex - noise * mdf;
    return col;
}
""".sksl()

fun createNoiseGrainFilter(intensity: Float, d: Dimension): ImageFilter {
    sksl.uniform("intensity", intensity)
    sksl.uniform("iResolution", d.wf, d.hf)
    return ImageFilter.makeRuntimeShader(
        sksl,
        shaderName = "image",
        input = null
    )
}

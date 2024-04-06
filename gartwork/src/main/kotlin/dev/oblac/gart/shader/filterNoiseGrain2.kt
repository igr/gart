package dev.oblac.gart.shader

import dev.oblac.gart.Dimension
import dev.oblac.gart.skia.ImageFilter

class filterNoiseGrain2 {
}

private val sksl = """
uniform float2 resolution;
uniform shader image;
uniform float intensity;

float random( vec2 p )
{
    vec2 K1 = vec2(
        23.14069263277926, // e^pi (Gelfond's constant)
        2.665144142690225 // 2^sqrt(2) (Gelfond–Schneider constant)
    );
    return fract( cos( dot(p,K1) ) * 43758.5453 ); // 43758.5453
}

vec4 main( vec2 fragCoord )  {
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/resolution.xy;
    
    // Check if pixel is inside viewport bounds
    if (fragCoord.x < 0.0 || fragCoord.x > resolution.x || fragCoord.y < 0.0 || fragCoord.y > resolution.y) {
        return vec4(image.eval(fragCoord));
    }
    
    vec2 uvRandom = uv;
    float amount = 0.2;
    uvRandom.y *= random(vec2(uvRandom.y,amount));
    vec4 tex = vec4(image.eval(fragCoord));
    tex.rgb += random(uvRandom)*intensity;

    return vec4(tex);
}
""".sksl()

fun createNoiseGrain2Filter(intensity: Float, d: Dimension): ImageFilter {
    sksl.uniform("intensity", intensity)
    sksl.uniform("resolution", d.wf, d.hf)
    return ImageFilter.makeRuntimeShader(
        sksl,
        shaderName = "image",
        input = null
    )
}

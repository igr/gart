package dev.oblac.gart.shader

import dev.oblac.gart.Dimension
import dev.oblac.gart.skia.ImageFilter

private val sksl = """
uniform float2 resolution;
uniform shader image; 
uniform float randomization;
uniform float randomizationOffset;

float random( vec2 p )
{
    vec2 K1 = vec2(
        23.14069263277926, // e^pi (Gelfond's constant)
        2.665144142690225 // 2^sqrt(2) (Gelfond–Schneider constant)
    );
    return fract( cos( dot(p,K1) ) * 43758.5453 );
}

float noise( vec2 uv )
{
  vec2 K1 = vec2(12.9898,78.233);
    return (fract(sin(dot(uv, K1*2.0)) * 43758.5453));
}

vec4 main( vec2 fragCoord )  {
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = fragCoord/resolution.xy;
        
    vec2 uvRandom = uv;
    float amount = 0.8;
    uvRandom.y *= noise(vec2(uvRandom.y,amount));
    vec4 tex = vec4(image.eval(fragCoord));
    vec4 originalTex = tex;
    tex.rgb += random(uvRandom) * randomization + randomizationOffset;
    
    float r = max(tex.r, originalTex.r);
    float g = max(tex.g, originalTex.g);
    float b = max(tex.b, originalTex.b);
    float a = 1.0;
  
    return vec4(r, g, b, a);
}
""".sksl()

fun createRisographFilter(
    intensity: Float,
    randomization: Float = 0.5f,
    randomizationOffset: Float = 0.1f,
    d: Dimension
): ImageFilter {
    sksl.uniform("intensity", intensity)
    sksl.uniform("randomization", randomization)
    sksl.uniform("randomizationOffset", randomizationOffset)
    sksl.uniform("resolution", d.wf, d.hf)
    return ImageFilter.makeRuntimeShader(
        sksl,
        shaderName = "image",
        input = null
    )
}

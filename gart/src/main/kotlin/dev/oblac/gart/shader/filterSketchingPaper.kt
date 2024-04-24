package dev.oblac.gart.shader

import dev.oblac.gart.Dimension
import dev.oblac.gart.skia.ImageFilter

private val sksl = """
uniform float2 resolution;
uniform shader image; 
uniform float contrast1;
uniform float contrast2;
uniform float amount; // 0.15

float mod289(float x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float2 mod289(float2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float3 mod289(float3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
float4 mod289(float4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }

float permute(float x) { return mod289((34.0 * x + 1.0) * x); }
float3 permute(float3 x) { return mod289((34.0 * x + 1.0) * x); }

float4 permute(float4 x) {
  return mod289(((x * 34.0) + 1.0) * x);
}

float snoise(float2 v) {
  const float4 C = float4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
  float2 i  = floor(v + dot(v, C.yy));
  float2 x0 = v - i + dot(i, C.xx);
  float2 i1;
  i1 = (x0.x > x0.y) ? float2(1.0, 0.0) : float2(0.0, 1.0);
  float4 x12 = x0.xyxy + C.xxzz;
  x12.xy -= i1;
  i = mod289(i);
  float3 p = permute(permute(i.y + float3(0.0, i1.y, 1.0)) + i.x + float3(0.0, i1.x, 1.0));
  float3 m = max(0.5 - float3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
  m = m * m;
  m = m * m;
  float3 x = 2.0 * fract(p * C.www) - 1.0;
  float3 h = abs(x) - 0.5;
  float3 ox = floor(x + 0.5);
  float3 a0 = x - ox;
  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
  float3 g;
  g.x = a0.x * x0.x + h.x * x0.y;
  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
  return 130.0 * dot(m, g);
}

half4 main( vec2 fragCoord )  {
    vec2 uv = fragCoord.xy / resolution.xy;
    half4 baseColor = image.eval(fragCoord);

    // Generate Simplex noise
    float noise = snoise(uv * 200.0) * 0.5 + 0.5;
    noise = pow(noise, contrast1); // Increase contrast

    // Create a dot pattern
    float dotPattern = (sin(uv.x * 800.0) * sin(uv.y * 800.0)) * 0.5 + 0.5;
    dotPattern = pow(dotPattern, contrast2); // Increase contrast

    // Combine the noise and dot pattern
    float combinedTexture = mix(noise, dotPattern, 0.6);

    // Apply the texture to the base color
    half4 outputColor = baseColor + half4(combinedTexture, combinedTexture, combinedTexture, 0.0) * amount;

    return outputColor;
}
""".sksl()

fun createSketchingPaperFilter(
    contrast1: Float,
    contrast2: Float,
    amount: Float,
    d: Dimension
): ImageFilter {
    sksl.uniform("contrast1", contrast1)
    sksl.uniform("contrast2", contrast2)
    sksl.uniform("amount", amount)
    sksl.uniform("resolution", d.wf, d.hf)
    return ImageFilter.makeRuntimeShader(
        sksl,
        shaderName = "image",
        input = null
    )
}

package dev.oblac.gart.shader

import org.jetbrains.skia.Shader

private val sksl = """
uniform float time;
uniform float thickness;

float f(vec3 p) {
    p.z -= 10. + time;
    float a = p.z * .1;
    p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
    return thickness - length(cos(p.xy) + sin(p.yz));
}

half4 main(vec2 fragcoord) { 
    vec3 d = .5 - fragcoord.xy1 / 500;
    vec3 p=vec3(0);
    for (int i = 0; i < 32; i++) p += f(p) * d;
    return ((sin(p) + vec3(2, 5, 9)) / length(p)).xyz1;
}
""".sksl()

fun createNeuroShader(time: Float, thickness: Float = 0.1f): Shader {
    sksl.uniform("time", time)
    sksl.uniform("thickness", thickness)
    return sksl.makeShader()
}

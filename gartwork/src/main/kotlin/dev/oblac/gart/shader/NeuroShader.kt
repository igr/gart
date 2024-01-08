package dev.oblac.gart.shader

import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Neuro shader.
 */
fun makeNeuroShader(thickness: Float = 0.1f): (Float) -> Shader {
    return NeuroShader(thickness)
}

/**
 * @param thickness thickness of the white lines, try 0.01 for thin and 0.5 for thick
 */
private class NeuroShader(thickness: Float) : (Float) -> Shader {

    private val sksl = """
uniform float time;

float f(vec3 p) {
    p.z -= 10. + time;
    float a = p.z * .1;
    p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
    return $thickness - length(cos(p.xy) + sin(p.yz));
}

half4 main(vec2 fragcoord) { 
    vec3 d = .5 - fragcoord.xy1 / 500;
    vec3 p=vec3(0);
    for (int i = 0; i < 32; i++) p += f(p) * d;
    return ((sin(p) + vec3(2, 5, 9)) / length(p)).xyz1;
}
        """

    private val runtimeEffect = RuntimeEffect.makeForShader(sksl)

    // just one argument
    private val byteBuffer = ByteBuffer.allocate(4 * 1).order(ByteOrder.LITTLE_ENDIAN)

    override operator fun invoke(time: Float): Shader {
        // load argument
        val timeBits = byteBuffer.clear().putFloat(time).array()

        return runtimeEffect.makeShader(
            uniforms = Data.makeFromBytes(timeBits),
            children = null,
            localMatrix = null,
        )
    }

}

package ac.obl.gart.shader

import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Neuro shader.
 */
class MakeNeuroShader(startTime: Float = 0f) {

    private val sksl = """
            uniform float time;
            
            float f(vec3 p) {
                p.z -= 10. + time;
                float a = p.z * .1;
                p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
                return .1 - length(cos(p.xy) + sin(p.yz));
            }
            
            half4 main(vec2 fragcoord) { 
                vec3 d = .5 - fragcoord.xy1 / 500;
                vec3 p=vec3(0);
                for (int i = 0; i < 32; i++) p += f(p) * d;
                return ((sin(p) + vec3(2, 5, 9)) / length(p)).xyz1;
            }
        """

    var time = startTime

    operator fun invoke(): Shader {
        val runtimeEffect = RuntimeEffect.makeForShader(sksl)
        val byteBuffer = ByteBuffer.allocate(4 * 1).order(ByteOrder.LITTLE_ENDIAN)  // just one argument

        // load argument

        val timeBits = byteBuffer.clear().putFloat(time).array()
        return runtimeEffect.makeShader(
            uniforms = Data.makeFromBytes(timeBits),
            children = null,
            localMatrix = null,
            isOpaque = false
        )
    }

}

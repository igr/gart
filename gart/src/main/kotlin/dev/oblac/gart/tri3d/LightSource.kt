package dev.oblac.gart.tri3d

import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.vector.Vec3

/**
 * A point light source in 3D space.
 */
data class LightSource(val position: Vec3)

/**
 * Shading function: given a face and its normal, returns the pixel color.
 */
fun interface Shading {
    fun color(face: Face, normal: Vec3): Int

    companion object {
        /**
         * No shading — uses the face color directly.
         */
        val flat = Shading { face, _ -> face.color }

        /**
         * Diffuse (Lambertian) shading from a point light source.
         * Faces pointing toward the light are fully lit;
         * faces pointing away are darkened down to [ambient].
         *
         * @param light   the light source position
         * @param ambient minimum brightness factor (0..1), default 0.2
         */
        fun diffuse(light: LightSource, ambient: Float = 0.2f) = Shading { face, normal ->
            val centroid = (face.a + face.b + face.c) / 3f
            val toLight = (light.position - centroid).normalize()
            val n = normal.normalize()
            val dot = n.dot(toLight).coerceIn(0f, 1f)
            val brightness = ambient + (1f - ambient) * dot
            scaleColor(face.color, brightness)
        }
    }
}

private fun scaleColor(color: Int, factor: Float): Int {
    val r = (red(color) * factor).toInt().coerceIn(0, 255)
    val g = (green(color) * factor).toInt().coerceIn(0, 255)
    val b = (blue(color) * factor).toInt().coerceIn(0, 255)
    return argb(alpha(color), r, g, b)
}

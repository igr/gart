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
         * Diffuse (Lambertian) shading from a point light source, using the
         * same lighting model as [VolumetricLight.render] so a scene
         * rendered by [ZBuffer] and by [VolumetricLight] shares consistent
         * surface shading.
         *
         * `brightness = clamp(ambient + lambert × strength × falloff, 0, 1)`.
         *
         * The face is sampled at its centroid; this is per-face shading
         * (consistent with [ZBuffer]'s raster cadence), not per-pixel.
         *
         * @param light    the light source position
         * @param ambient  baseline brightness factor (0..1)
         * @param strength multiplier on the direct (Lambert × falloff) term
         * @param falloff  distance-based attenuation model
         */
        fun diffuse(
            light: LightSource,
            ambient: Float = 0.2f,
            strength: Float = 1f,
            falloff: Falloff = Falloff.NONE,
        ) = Shading { face, normal ->
            val centroid = (face.a + face.b + face.c) / 3f
            val toLight = light.position - centroid
            val distToLight = toLight.length()
            // Light sitting on (or essentially at) the surface: skip the
            // entire direct calculation. Otherwise INVERSE/INVERSE_SQUARE
            // falloff at d≈0 returns +Infinity, lambert=0 → 0 * Inf = NaN,
            // and the pixel collapses to black instead of pure ambient.
            val direct = if (distToLight < 1e-5f) {
                0f
            } else {
                val n = normal.normalize()
                val lambert = n.dot(toLight / distToLight).coerceAtLeast(0f)
                lambert * strength * falloffFactor(distToLight, falloff)
            }
            val brightness = (ambient + direct).coerceIn(0f, 1f)
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

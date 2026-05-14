package dev.oblac.gart.tri3d

import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.vector.Vec3

/**
 * A point light source in 3D space.
 *
 * [color] tints both the Lambertian surface contribution and the volumetric
 * scattering produced by [VolumetricLight]. Defaults to opaque white.
 */
data class LightSource(
    val position: Vec3,
    val color: Int = 0xFFFFFFFF.toInt(),
)

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
            val lr = red(light.color) / 255f
            val lg = green(light.color) / 255f
            val lb = blue(light.color) / 255f
            val fr = (ambient + direct * lr).coerceIn(0f, 1f)
            val fg = (ambient + direct * lg).coerceIn(0f, 1f)
            val fb = (ambient + direct * lb).coerceIn(0f, 1f)
            argb(
                alpha(face.color),
                (red(face.color) * fr).toInt().coerceIn(0, 255),
                (green(face.color) * fg).toInt().coerceIn(0, 255),
                (blue(face.color) * fb).toInt().coerceIn(0, 255),
            )
        }
    }
}

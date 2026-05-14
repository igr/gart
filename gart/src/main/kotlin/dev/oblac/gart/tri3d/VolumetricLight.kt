package dev.oblac.gart.tri3d

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blue
import dev.oblac.gart.color.green
import dev.oblac.gart.color.red
import dev.oblac.gart.vector.Vec3
import java.util.Random

enum class VolumetricBlend { ADD, SCREEN, REPLACE }
enum class Falloff { NONE, INVERSE, INVERSE_SQUARE }

internal fun falloffFactor(d: Float, falloff: Falloff): Float = when (falloff) {
    Falloff.NONE -> 1f
    Falloff.INVERSE -> 1f / d
    Falloff.INVERSE_SQUARE -> 1f / (d * d)
}

/**
 * Volumetric light post-pass adapted from inconvergent.net/2021/volumetric-light/.
 *
 * For every pixel the camera ray is marched from the eye to the ZBuffer hit
 * point (or to [maxDistance] for background pixels). [samples] points along the
 * ray cast shadow rays toward each light in [lights]; those with a clear path
 * contribute to the pixel intensity (scaled by [strength] / samples and
 * falloff). Each light's [LightSource.color] tints its own scattering and
 * surface contribution. The accumulated colour is composited onto the surface
 * using [blendMode].
 *
 * Deterministic given a fixed [seed]; the default seed of 0 also produces
 * stable output across runs.
 */
data class VolumetricLight(
    val lights: List<LightSource>,
    val samples: Int = 10,
    val strength: Float = 1.0f,
    val blendMode: VolumetricBlend = VolumetricBlend.ADD,
    val falloff: Falloff = Falloff.INVERSE_SQUARE,
    val maxDistance: Float = 100f,
    val antiAlias: Int = 1,
    val seed: Long = 0L,
    val ambient: Float = 0.1f,
    val background: Int = 0,
) {
    /**
     * Standalone render: ray-traces primary rays against [mesh], shades the
     * hit surface (Lambert + ambient + falloff + shadow rays toward each
     * light, tinted by per-light color), and composites volumetric scattering
     * along each camera ray.
     *
     * Returns a fresh [Gartvas] containing the rendered image. The caller is
     * responsible for drawing/saving it.
     *
     * **Two-sided geometry:** unlike [ZBuffer], which culls faces failing
     * `Camera.isFrontFacing`, this renderer treats every face as two-sided
     * — it returns whichever face the ray hits, regardless of normal
     * orientation. For closed meshes viewed from outside this makes no
     * visible difference (the nearest hit is the front face). For meshes
     * viewed from the *inside* (e.g., a room or box where normals point
     * inward), VL still renders the walls while ZB would cull them.
     */
    fun render(camera: Camera, mesh: Mesh, width: Int, height: Int): Gartvas {
        val gartvas = Gartvas(Dimension(width, height))
        val gartmap = Gartmap(gartvas)

        val rng = Random(seed)
        val eye = camera.eye
        val aa = antiAlias.coerceAtLeast(1)
        val invNorm = 1f / (samples.coerceAtLeast(1).toFloat() * aa.toFloat())

        for (y in 0 until height) {
            for (x in 0 until width) {
                var accumVolR = 0f
                var accumVolG = 0f
                var accumVolB = 0f
                var accumR = 0
                var accumG = 0
                var accumB = 0
                var accumA = 0

                for (aaStep in 0 until aa) {
                    val jx = if (aa == 1) 0.5f else rng.nextFloat()
                    val jy = if (aa == 1) 0.5f else rng.nextFloat()
                    val sx = x + jx
                    val sy = y + jy
                    val rayDir = camera.rayDirection(sx, sy)

                    // Primary rays are uncapped — maxDistance only bounds
                    // the volumetric march when the camera ray misses geometry.
                    // Capping primary rays here would invisibly clip surfaces
                    // ZB still rasterizes, breaking parity between renderers.
                    val hit = RayMesh.firstHit(eye, rayDir, mesh)
                    val endT: Float
                    val basePixel: Int
                    if (hit == null) {
                        endT = maxDistance
                        basePixel = background
                    } else {
                        endT = (hit.t - SURFACE_BIAS).coerceAtLeast(0f)
                        val p = eye + rayDir * endT
                        val n = hit.face.normal().normalize()
                        var directR = 0f
                        var directG = 0f
                        var directB = 0f
                        for (light in lights) {
                            val toLight = light.position - p
                            val distToLight = toLight.length()
                            if (distToLight < 1e-5f) continue
                            val lightDir = toLight / distToLight
                            val lambert = n.dot(lightDir).coerceAtLeast(0f)
                            if (lambert == 0f) continue
                            val occluded = RayMesh.isOccluded(
                                p, lightDir, mesh, distToLight, epsilon = 1e-6f,
                            )
                            if (!occluded) {
                                val w = lambert * falloffFactor(distToLight, falloff) * strength
                                directR += w * red(light.color) / 255f
                                directG += w * green(light.color) / 255f
                                directB += w * blue(light.color) / 255f
                            }
                        }
                        val fa = alpha(hit.face.color)
                        val fr = (ambient + directR).coerceIn(0f, 1f)
                        val fg = (ambient + directG).coerceIn(0f, 1f)
                        val fb = (ambient + directB).coerceIn(0f, 1f)
                        basePixel = argb(
                            fa,
                            (red(hit.face.color) * fr).toInt().coerceIn(0, 255),
                            (green(hit.face.color) * fg).toInt().coerceIn(0, 255),
                            (blue(hit.face.color) * fb).toInt().coerceIn(0, 255),
                        )
                    }

                    if (samples > 0 && strength != 0f && endT > 0f) {
                        val vol = marchOnce(eye, rayDir, endT, mesh, rng)
                        accumVolR += vol.x
                        accumVolG += vol.y
                        accumVolB += vol.z
                    }

                    accumA += alpha(basePixel)
                    accumR += red(basePixel)
                    accumG += green(basePixel)
                    accumB += blue(basePixel)
                }

                val baseA = accumA / aa
                val baseR = accumR / aa
                val baseG = accumG / aa
                val baseB = accumB / aa
                val baseColor = argb(baseA, baseR, baseG, baseB)

                val volTotal = accumVolR + accumVolG + accumVolB
                val out = if (samples > 0 && strength != 0f && volTotal > 0f) {
                    val tintR = (accumVolR * invNorm).toInt().coerceIn(0, 255)
                    val tintG = (accumVolG * invNorm).toInt().coerceIn(0, 255)
                    val tintB = (accumVolB * invNorm).toInt().coerceIn(0, 255)
                    blend(baseColor, tintR, tintG, tintB)
                } else {
                    baseColor
                }

                gartmap.pixels[y * width + x] = out
            }
        }
        gartmap.drawToCanvas()
        return gartvas
    }

    /**
     * Applies the volumetric pass in place on [zBuffer]. The depth buffer is
     * read but never modified; only the colour buffer is updated.
     */
    fun apply(zBuffer: ZBuffer, camera: Camera, mesh: Mesh) {
        if (samples <= 0 || strength == 0f) return

        val rng = Random(seed)
        val eye = camera.eye
        val width = zBuffer.width
        val height = zBuffer.height
        val aa = antiAlias.coerceAtLeast(1)
        val invNorm = 1f / (samples.toFloat() * aa.toFloat())

        for (y in 0 until height) {
            for (x in 0 until width) {
                val depthIdx = y * width + x
                val storedDepth = zBuffer.depth[depthIdx]

                var accumR = 0f
                var accumG = 0f
                var accumB = 0f
                for (aaStep in 0 until aa) {
                    val jx = if (aa == 1) 0.5f else rng.nextFloat()
                    val jy = if (aa == 1) 0.5f else rng.nextFloat()
                    val sx = x + jx
                    val sy = y + jy

                    val rayDir = camera.rayDirection(sx, sy)
                    val endT = if (storedDepth >= Float.MAX_VALUE) {
                        maxDistance
                    } else {
                        val hit = camera.unproject(sx, sy, storedDepth)
                        ((hit - eye).length() - SURFACE_BIAS).coerceAtLeast(0f)
                    }
                    if (endT <= 0f) continue

                    val vol = marchOnce(eye, rayDir, endT, mesh, rng)
                    accumR += vol.x
                    accumG += vol.y
                    accumB += vol.z
                }

                if (accumR + accumG + accumB <= 0f) continue
                val tintR = (accumR * invNorm).toInt().coerceIn(0, 255)
                val tintG = (accumG * invNorm).toInt().coerceIn(0, 255)
                val tintB = (accumB * invNorm).toInt().coerceIn(0, 255)

                val surface = zBuffer.gartmap.pixels[depthIdx]
                zBuffer.gartmap.pixels[depthIdx] = blend(surface, tintR, tintG, tintB)
            }
        }
        zBuffer.gartmap.drawToCanvas()
    }

    private fun marchOnce(
        rayOrigin: Vec3,
        rayDir: Vec3,
        endT: Float,
        mesh: Mesh,
        rng: Random,
    ): Vec3 {
        var sumR = 0f
        var sumG = 0f
        var sumB = 0f
        for (s in 1..samples) {
            val tNorm = (s - rng.nextFloat()) / samples
            val t = tNorm * endT
            val p = rayOrigin + rayDir * t

            for (light in lights) {
                val toLight = light.position - p
                val distToLight = toLight.length()
                if (distToLight < 1e-5f) continue

                val lightDir = toLight / distToLight

                // SURFACE_BIAS guarantees `p` is geometrically off every triangle,
                // so we can use a much smaller t-min epsilon than RayMesh's default.
                // Otherwise grazing camera angles produce shadow-ray hits at tiny t
                // that get falsely rejected as self-intersections.
                if (!RayMesh.isOccluded(p, lightDir, mesh, distToLight, epsilon = 1e-6f)) {
                    val w = strength * falloffFactor(distToLight, falloff)
                    sumR += w * red(light.color)
                    sumG += w * green(light.color)
                    sumB += w * blue(light.color)
                }
            }
        }
        return Vec3(sumR, sumG, sumB)
    }

    private fun blend(surface: Int, vR: Int, vG: Int, vB: Int): Int {
        val sA = alpha(surface)
        val sR = red(surface)
        val sG = green(surface)
        val sB = blue(surface)
        return when (blendMode) {
            VolumetricBlend.ADD -> argb(
                sA,
                (sR + vR).coerceAtMost(255),
                (sG + vG).coerceAtMost(255),
                (sB + vB).coerceAtMost(255),
            )
            VolumetricBlend.SCREEN -> argb(
                sA,
                255 - ((255 - sR) * (255 - vR)) / 255,
                255 - ((255 - sG) * (255 - vG)) / 255,
                255 - ((255 - sB) * (255 - vB)) / 255,
            )
            VolumetricBlend.REPLACE -> argb(sA, vR, vG, vB)
        }
    }

    companion object {
        // Pull camera-ray sample range slightly off rasterized surfaces so the
        // last jittered sample can't land on (or numerically inside) the wall
        // the camera ray just hit. RayMesh.isOccluded's own t > 1e-4 self-int
        // guard handles the shadow-ray side without an extra origin bias.
        private const val SURFACE_BIAS = 0.01f
    }
}

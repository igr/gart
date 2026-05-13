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
 * ray cast shadow rays toward the light; those with a clear path contribute
 * to the pixel intensity (scaled by [strength] / samples and falloff). The
 * accumulated colour is composited onto the surface using [blendMode].
 *
 * Deterministic given a fixed [seed]; the default seed of 0 also produces
 * stable output across runs.
 */
data class VolumetricLight(
    val light: LightSource,
    val samples: Int = 10,
    val strength: Float = 1.0f,
    val color: Int = 0xFFFFFFFF.toInt(),
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
     * hit surface (Lambert + ambient + falloff + shadow rays toward the
     * light), and composites volumetric scattering along each camera ray.
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
        val vColorR = red(color)
        val vColorG = green(color)
        val vColorB = blue(color)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var accumVol = 0f
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
                        val toLight = light.position - p
                        val distToLight = toLight.length()
                        val direct = if (distToLight < 1e-5f) {
                            0f
                        } else {
                            val lightDir = toLight / distToLight
                            val n = hit.face.normal().normalize()
                            val lambert = n.dot(lightDir).coerceAtLeast(0f)
                            val occluded = RayMesh.isOccluded(
                                p, lightDir, mesh, distToLight, epsilon = 1e-6f,
                            )
                            if (occluded) 0f
                            else lambert * falloffFactor(distToLight, falloff) * strength
                        }
                        val litFactor = (ambient + direct).coerceIn(0f, 1f)
                        basePixel = shadeFaceColor(hit.face.color, litFactor)
                    }

                    if (samples > 0 && strength != 0f && endT > 0f) {
                        accumVol += marchOnce(eye, rayDir, endT, mesh, rng)
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

                val out = if (samples > 0 && strength != 0f && accumVol > 0f) {
                    val intensity = (accumVol * invNorm).coerceIn(0f, 1f)
                    val tintR = (vColorR * intensity).toInt()
                    val tintG = (vColorG * intensity).toInt()
                    val tintB = (vColorB * intensity).toInt()
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

    private fun shadeFaceColor(c: Int, factor: Float): Int {
        val a = alpha(c)
        val r = (red(c) * factor).toInt().coerceIn(0, 255)
        val g = (green(c) * factor).toInt().coerceIn(0, 255)
        val b = (blue(c) * factor).toInt().coerceIn(0, 255)
        return argb(a, r, g, b)
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

        val vColorR = red(color)
        val vColorG = green(color)
        val vColorB = blue(color)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val depthIdx = y * width + x
                val storedDepth = zBuffer.depth[depthIdx]

                var accum = 0f
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

                    accum += marchOnce(eye, rayDir, endT, mesh, rng)
                }

                if (accum <= 0f) continue
                val intensity = (accum * invNorm).coerceIn(0f, 1f)

                val tintR = (vColorR * intensity).toInt()
                val tintG = (vColorG * intensity).toInt()
                val tintB = (vColorB * intensity).toInt()

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
    ): Float {
        var sum = 0f
        for (s in 1..samples) {
            val tNorm = (s - rng.nextFloat()) / samples
            val t = tNorm * endT
            val p = rayOrigin + rayDir * t

            val toLight = light.position - p
            val distToLight = toLight.length()
            if (distToLight < 1e-5f) continue

            val lightDir = toLight / distToLight

            // SURFACE_BIAS guarantees `p` is geometrically off every triangle,
            // so we can use a much smaller t-min epsilon than RayMesh's default.
            // Otherwise grazing camera angles produce shadow-ray hits at tiny t
            // that get falsely rejected as self-intersections.
            if (!RayMesh.isOccluded(p, lightDir, mesh, distToLight, epsilon = 1e-6f)) {
                sum += strength * falloffFactor(distToLight, falloff)
            }
        }
        return sum
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

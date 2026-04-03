package ppt

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.smoothstep
import dev.oblac.gart.text.drawStringInRect
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.VertexMode
import java.util.*
import kotlin.math.*

// FSS (Flat Surface Shader) animated triangle mesh

// --- Color palette (tweak these to change the look) ---
private val ambient = Color4f(0.00f, 0.15f, 0.20f)    // base: dark HANADA (0,98,132)
private val diffuse1 = Color4f(0.00f, 0.24f, 0.32f)   // light 1: HANADA blue
private val diffuse2 = Color4f(0.85f, 0.20f, 0.15f)   // light 2: GINSYU/BENIHI red
private const val clearColor = 0xFF001822.toInt()      // canvas clear: very dark teal

private class FSSPlane(
    screenW: Float,
    screenH: Float,
    val segments: Int,
    val slices: Int,
) {
    private val padding = 100f
    private val gridW = segments + 1
    private val gridH = slices + 1
    private val gridSize = gridW * gridH
    private val triCount = segments * slices * 2

    private val segW = (screenW + 2 * padding) / segments
    private val segH = (screenH + 2 * padding) / slices
    private val centerX = screenW / 2f
    private val centerY = screenH / 2f
    private val maxDist = sqrt(centerX * centerX + centerY * centerY)

    // Per grid-vertex persistent data
    private val anchorX = FloatArray(gridSize)
    private val anchorY = FloatArray(gridSize)
    private val v0 = Array(gridSize) { Vec3.ZERO }
    private val phi = FloatArray(gridSize)

    // Animated state per grid vertex
    private val animX = FloatArray(gridSize)
    private val animY = FloatArray(gridSize)
    private val animZ = FloatArray(gridSize)

    // Output arrays for drawVertices
    val positions = FloatArray(triCount * 6)  // 3 verts * 2 coords per tri
    val colors = IntArray(triCount * 3)       // 3 colors per tri

    init {
        val rng = Random(42)
        for (i in 0..segments) {
            for (j in 0..slices) {
                val idx = i * gridH + j
                anchorX[idx] = -padding + i * segW
                anchorY[idx] = -padding + j * segH
                v0[idx] = Vec3(
                    rng.nextFloat() * 2f - 1f,
                    rng.nextFloat() * 2f - 1f,
                    rng.nextFloat() * 2f - 1f,
                )
                phi[idx] = rng.nextFloat() * PIf * 2f
            }
        }
    }

    fun update(time: Float) {
        val amplitude = segW * 0.25f
        val speedScale = 0.7f

        // Step 3: animate vertex positions
        for (idx in 0 until gridSize) {
            val speed = v0[idx].normalize() * speedScale
            val phase = phi[idx]
            animX[idx] = anchorX[idx] + amplitude * sin(speed.x * time + phase)
            animY[idx] = anchorY[idx] + amplitude * cos(speed.y * time + phase)
            animZ[idx] = amplitude * 0.5f * sin(speed.z * time + phase)
        }

        // Step 4: two orbiting lights
        val ls = 0.15f
        val light1 = Vec3(
            centerX + centerX * 0.8f * cos(3f * time * ls),
            centerY + centerY * 0.8f * sin(2f * time * ls),
            300f * sin(time * ls),
        )
        val light2 = Vec3(
            centerX + centerX * 1.0f * cos(2f * time * ls + PIf),
            centerY + centerY * 1.0f * sin(3f * time * ls + PIf),
            200f * cos(time * ls),
        )

        // Build triangles: tessellate each quad into 2 triangles
        var triIdx = 0
        for (i in 0 until segments) {
            for (j in 0 until slices) {
                val a = i * gridH + j
                val b = i * gridH + j + 1
                val c = (i + 1) * gridH + j
                val d = (i + 1) * gridH + j + 1
                emitTriangle(triIdx++, a, b, c, light1, light2)
                emitTriangle(triIdx++, c, b, d, light1, light2)
            }
        }
    }

    private fun illum(light: Vec3, cx: Float, cy: Float, cz: Float, nx: Float, ny: Float, nz: Float): Float {
        var dx = light.x - cx
        var dy = light.y - cy
        var dz = light.z - cz
        val dl = sqrt(dx * dx + dy * dy + dz * dz)
        if (dl > 0f) { dx /= dl; dy /= dl; dz /= dl }
        return max(nx * dx + ny * dy + nz * dz, 0f)
    }

    private fun emitTriangle(
        triIdx: Int,
        i0: Int, i1: Int, i2: Int,
        light1: Vec3, light2: Vec3,
    ) {
        // Vertex positions
        val base = triIdx * 6
        positions[base] = animX[i0]; positions[base + 1] = animY[i0]
        positions[base + 2] = animX[i1]; positions[base + 3] = animY[i1]
        positions[base + 4] = animX[i2]; positions[base + 5] = animY[i2]

        // Step 2: centroid
        val cx = (animX[i0] + animX[i1] + animX[i2]) / 3f
        val cy = (animY[i0] + animY[i1] + animY[i2]) / 3f
        val cz = (animZ[i0] + animZ[i1] + animZ[i2]) / 3f

        // Step 2: normal via cross product
        val ux = animX[i1] - animX[i0];
        val uy = animY[i1] - animY[i0];
        val uz = animZ[i1] - animZ[i0]
        val vx = animX[i2] - animX[i0];
        val vy = animY[i2] - animY[i0];
        val vz = animZ[i2] - animZ[i0]
        var nx = uy * vz - uz * vy
        var ny = uz * vx - ux * vz
        var nz = ux * vy - uy * vx
        val nl = sqrt(nx * nx + ny * ny + nz * nz)
        if (nl > 0f) {
            nx /= nl; ny /= nl; nz /= nl
        }

        // Step 4: per-triangle lighting
        var r = ambient.r
        var g = ambient.g
        var b = ambient.b

        // Light 1
        illum(light1, cx, cy, cz, nx, ny, nz).let { i ->
            r += diffuse1.r * i; g += diffuse1.g * i; b += diffuse1.b * i
        }
        // Light 2
        illum(light2, cx, cy, cz, nx, ny, nz).let { i ->
            r += diffuse2.r * i; g += diffuse2.g * i; b += diffuse2.b * i
        }

        // Shadow based on z-depth
        val shadow = 1f - abs(cz) * 0.003f
        r *= shadow; g *= shadow; b *= shadow

        // Step 5: vignette darkening toward edges
        val distFromCenter = sqrt((cx - centerX) * (cx - centerX) + (cy - centerY) * (cy - centerY))
        val vignetteT = smoothstep(0f, maxDist * 1.1f, distFromCenter)
        r *= (1f - vignetteT * 0.4f)
        g *= (1f - vignetteT * 0.4f)
        b *= (1f - vignetteT * 0.3f)

        // Flat color for all 3 vertices of this triangle
        val color = argb(1f, r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
        val ci = triIdx * 3
        colors[ci] = color; colors[ci + 1] = color; colors[ci + 2] = color
    }
}

// Step 1: build the triangle mesh (lazy to avoid static init ordering issues with `screen`)
private val plane by lazy { FSSPlane(screen.wf, screen.hf, 20, 14) }

private val meshPaint = 0xFFFFFFFF.toInt().toFillPaint()

val slide00 = DrawFrame { c, d, f ->
    c.clear(clearColor)

    plane.update(f.frameTimeSeconds)

    c.drawVertices(
        VertexMode.TRIANGLES,
        plane.positions,
        plane.colors,
        null, null,
        BlendMode.MODULATE,
        meshPaint
    )

    c.drawStringInRect("Skiko", activeRect, introFont, titleColor.toFillPaint())
}

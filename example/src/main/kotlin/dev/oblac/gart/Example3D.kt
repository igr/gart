package dev.oblac.gart

import dev.oblac.gart.color.CssColors.darkSlateGray
import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.fillOfWhite
import dev.oblac.gart.tri3d.*
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Image

private enum class RenderMode { ZB, VL_STANDALONE, ZB_PLUS_VL }

fun main() {
    val gart = Gart.of("Example3D", 800, 800)
    val d = gart.d
    val w = gart.window()

    val stacks = 24
    val slices = 32
    val sphereMesh = sphere(stacks, slices) { i, j ->
        val hue = (j.toFloat() / slices * 360f + i.toFloat() / stacks * 120f) % 360f
        val sat = 0.7f + 0.3f * (i.toFloat() / stacks)
        hsvToColor(hue, sat, 1f)
    }

    val light = LightSource(Vec3(-2f, -3f, 2f), color = 0xFFFFE0B0.toInt())
    val labelFont = font(FontFamily.OdibeeSans, 18f)

    var angleX = 0f
    var angleY = 0f
    val step = 0.08f
    var renderMode = RenderMode.ZB
    var ambientOn = true

    var cachedImage: Image? = null
    var dirty = true

    fun currentAmbient(): Float = if (ambientOn) 0.2f else 0f

    fun renderScene(camera: Camera, mesh: Mesh): Image {
        val ambient = currentAmbient()
        println("Rendering scene with mode $renderMode, ambient ${if (ambientOn) "ON" else "OFF"}")
        return when (renderMode) {
            RenderMode.ZB -> {
                val zb = Scene.rasterize(
                    camera, mesh, d.w, d.h,
                    background = darkSlateGray,
                    shading = Shading.diffuse(light, ambient = ambient),
                )
                println("ZB render complete")
                zb.toImage()
            }
            RenderMode.VL_STANDALONE -> {
                // strength controls BOTH the surface direct-lighting term and
                // the per-sample volumetric contribution (VL.render shares one
                // falloff/strength). 5f gives a clearly warm scattering halo
                // while keeping the surface lit enough to read against the bg.
                val gv = VolumetricLight(
                    lights = listOf(light),
                    samples = 8,
                    strength = 5f,
                    blendMode = VolumetricBlend.ADD,
                    falloff = Falloff.INVERSE_SQUARE,
                    maxDistance = 8f,
                    ambient = ambient,
                    background = darkSlateGray,
                ).render(camera, mesh, d.w, d.h)
                println("VL render complete")
                gv.snapshot()
            }
            RenderMode.ZB_PLUS_VL -> {
                val zb = Scene.rasterize(
                    camera, mesh, d.w, d.h,
                    background = darkSlateGray,
                    shading = Shading.diffuse(light, ambient = ambient),
                )
                // VL.apply only contributes scattering here (surface comes from
                // ZB). strength can be higher without dimming the sphere.
                VolumetricLight(
                    lights = listOf(light),
                    samples = 12,
                    strength = 4f,
                    blendMode = VolumetricBlend.SCREEN,
                    falloff = Falloff.INVERSE_SQUARE,
                    maxDistance = 8f,
                ).apply(zb, camera, mesh)
                println("ZB+VL render complete")
                zb.toImage()
            }
        }
    }

    w.show { c, _, _ ->
        if (dirty || cachedImage == null) {
            // Clear the flag BEFORE rendering so a key press fired during the
            // (potentially slow) renderScene call survives — the next frame
            // will see dirty=true and re-render.
            dirty = false
            val camera = Camera(d.cx, d.cy, d.hf * 0.35f, 4f)
            val rotated = Mesh(sphereMesh.faces.map { face ->
                face.rotateY(angleY).rotateX(angleX)
            })
            cachedImage = renderScene(camera, rotated)
        }
        c.drawImage(cachedImage, 0f, 0f)

        val label = "mode: ${renderMode.name} | ambient: ${if (ambientOn) "ON" else "OFF"}"
        c.drawString(label, 12f, 26f, labelFont, fillOfBlack())
        c.drawString(label, 10f, 24f, labelFont, fillOfWhite())
    }.onKey { key ->
        when (key) {
            Key.KEY_LEFT  -> { angleY -= step; dirty = true }
            Key.KEY_RIGHT -> { angleY += step; dirty = true }
            Key.KEY_UP    -> { angleX -= step; dirty = true }
            Key.KEY_DOWN  -> { angleX += step; dirty = true }
            Key.KEY_SPACE -> {
                renderMode = when (renderMode) {
                    RenderMode.ZB -> RenderMode.VL_STANDALONE
                    RenderMode.VL_STANDALONE -> RenderMode.ZB_PLUS_VL
                    RenderMode.ZB_PLUS_VL -> RenderMode.ZB
                }
                dirty = true
            }
            Key.KEY_A -> { ambientOn = !ambientOn; dirty = true }
            else -> {}
        }
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float): Int {
    val hi = ((h / 60f) % 6).toInt()
    val f = h / 60f - hi
    val p = v * (1 - s)
    val q = v * (1 - f * s)
    val t = v * (1 - (1 - f) * s)
    val (r, g, b) = when (hi) {
        0 -> Triple(v, t, p)
        1 -> Triple(q, v, p)
        2 -> Triple(p, v, t)
        3 -> Triple(p, q, v)
        4 -> Triple(t, p, v)
        else -> Triple(v, p, q)
    }
    return (0xFF shl 24) or
        ((r * 255).toInt() shl 16) or
        ((g * 255).toInt() shl 8) or
        (b * 255).toInt()
}

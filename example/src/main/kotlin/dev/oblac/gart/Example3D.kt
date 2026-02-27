package dev.oblac.gart

import dev.oblac.gart.color.CssColors.darkSlateGray
import dev.oblac.gart.tri3d.Camera
import dev.oblac.gart.tri3d.Mesh
import dev.oblac.gart.tri3d.Scene
import dev.oblac.gart.tri3d.rotateX
import dev.oblac.gart.tri3d.rotateY
import dev.oblac.gart.tri3d.sphere

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

    var angleX = 0f
    var angleY = 0f
    val step = 0.08f

    w.show { c, _, _ ->
        c.clear(darkSlateGray)

        val rotated = Mesh(sphereMesh.faces.map { face ->
            face.rotateY(angleY).rotateX(angleX)
        })

        val camera = Camera(d.cx, d.cy, d.hf * 0.35f, 4f)
        Scene.render(c, camera, rotated, d.w, d.h)
    }.onKey { key ->
        when (key) {
            Key.KEY_LEFT -> angleY -= step
            Key.KEY_RIGHT -> angleY += step
            Key.KEY_UP -> angleX -= step
            Key.KEY_DOWN -> angleX += step
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

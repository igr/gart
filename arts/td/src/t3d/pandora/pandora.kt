package t3d.pandora

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.tri3d.*
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color

fun main() {
    val gart = Gart.of("pandora", 1024, 1024)
    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val wall = 0xFF000000.toInt()
    val lightColor = 0xFFFFFFFF.toInt()

    // Cube corners
    val v = arrayOf(
        Vec3(-1f, -1f, -1f), Vec3(1f, -1f, -1f),
        Vec3(1f, 1f, -1f), Vec3(-1f, 1f, -1f),
        Vec3(-1f, -1f, 1f), Vec3(1f, -1f, 1f),
        Vec3(1f, 1f, 1f), Vec3(-1f, 1f, 1f),
    )

    // Two crossed rectangular panels at y = -1 forming a "+" shape, partially
    // closing the open top.
    val rLong = 0.55f
    val rShort = 0.18f
    // Bar A: long along x, short along z.
    val a0 = Vec3(-rLong, -1f, -rShort)
    val a1 = Vec3( rLong, -1f, -rShort)
    val a4 = Vec3(-rLong, -1f,  rShort)
    val a5 = Vec3( rLong, -1f,  rShort)
    // Bar B: short along x, long along z (perpendicular to A).
    val b0 = Vec3(-rShort, -1f, -rLong)
    val b1 = Vec3( rShort, -1f, -rLong)
    val b4 = Vec3(-rShort, -1f,  rLong)
    val b5 = Vec3( rShort, -1f,  rLong)

    val box = Mesh(
        listOf(
            // front (z = -1)
            Face(v[0], v[1], v[2], wall),
            Face(v[0], v[2], v[3], wall),
            // back (z = +1)
            Face(v[5], v[4], v[7], wall),
            Face(v[5], v[7], v[6], wall),
            // left (x = -1)
            Face(v[4], v[0], v[3], wall),
            Face(v[4], v[3], v[7], wall),
            // right (x = +1)
            Face(v[1], v[5], v[6], wall),
            Face(v[1], v[6], v[2], wall),
            // floor (y = +1, visual bottom of screen)
            Face(v[3], v[2], v[6], wall),
            Face(v[3], v[6], v[7], wall),
            // partial top panels (y = -1) — two crossed bars forming a "+".
            Face(a0, a5, a1, wall),
            Face(a0, a4, a5, wall),
            Face(b0, b5, b1, wall),
            Face(b0, b4, b5, wall),
        )
    )

    var angleX = 0.77f
    var angleY = 1.08f
    val step = 0.08f
    draw(g.canvas, box, angleY, angleX, d, lightColor)
    gart.saveImage(g)
    w.show { c, _, _ ->
        draw(c, box, angleY, angleX, d, lightColor)
    }.onKey { key ->
        when (key) {
            Key.KEY_LEFT -> angleY -= step
            Key.KEY_RIGHT -> angleY += step
            Key.KEY_UP -> angleX -= step
            Key.KEY_DOWN -> angleX += step
            else -> {}
        }
        println("angleX: $angleX | angleY: $angleY")
    }
}

private fun draw(
    c: Canvas,
    box: Mesh,
    angleY: Float,
    angleX: Float,
    d: Dimension,
    lightColor: Int
) {
    c.clear(Color.BLACK)

    val rotated = Mesh(box.faces.map { face ->
        face.rotateY(angleY).rotateX(angleX)
    })

    val camera = Camera(d.cx, d.cy + 300f, d.hf * 0.25f, 8f)

    // Light inside the box, slightly off-center so the radial gradient is asymmetric
    val light = LightSource(Vec3(0.3f, 0.3f, 0.3f))

    val gv = VolumetricLight(
        light = light,
        samples = 5,
        strength = 4.0f,
        color = lightColor,
        blendMode = VolumetricBlend.ADD,
        falloff = Falloff.INVERSE_SQUARE,
        maxDistance = 12f,
        seed = 1L,
        ambient = 0f,
        background = Color.BLACK,
    ).render(camera, rotated, d.w, d.h)

    gv.snapshotTo(c)
}

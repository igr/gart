package t3d.mesh

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.tri3d.*
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import java.util.*

fun main() {
    val gart = Gart.of("mesh", 1024, 1024)
    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val wall = 0xFF101010.toInt()

    val mesh = buildMesh(
        n = 12,
        extent = 1.4f,
        xyJitter = 0.2f,
        zJitter = 0.4f,
        color = wall,
        seed = 7L,
    )

    val angleX = 0f
    val angleY = 0f
    draw(g.canvas, mesh, angleY, angleX, d)
    gart.saveImage(g)
    w.showImage(g)
}

private fun buildMesh(
    n: Int,
    extent: Float,
    xyJitter: Float,
    zJitter: Float,
    color: Int,
    seed: Long,
): Mesh {
    val p = Palettes.cool34
    val cellSize = (extent * 2f) / n
    val xyMax = cellSize * xyJitter
    val zMax = cellSize * zJitter
    val rng = Random(seed)
    fun jit(max: Float) = (rng.nextFloat() * 2f - 1f) * max

    val faces = mutableListOf<Face>()
    for (i in 0 until n) {
        for (j in 0 until n) {
            val x0 = -extent + i * cellSize
            val x1 = x0 + cellSize
            val y0 = -extent + j * cellSize
            val y1 = y0 + cellSize

            // Each corner jitters independently — adjacent squares no longer
            // share vertices, so they pull apart and leave gaps.
            val p00 = Vec3(x0 + jit(xyMax), y0 + jit(xyMax), jit(zMax))
            val p10 = Vec3(x1 + jit(xyMax), y0 + jit(xyMax), jit(zMax))
            val p11 = Vec3(x1 + jit(xyMax), y1 + jit(xyMax), jit(zMax))
            val p01 = Vec3(x0 + jit(xyMax), y1 + jit(xyMax), jit(zMax))

            // Winding orients normals toward -Z (the camera side).
            val color = p.safe(i+j)
            faces.add(Face(p00, p11, p10, color))
            faces.add(Face(p00, p01, p11, color))
        }
    }
    return Mesh(faces)
}

private fun draw(
    c: Canvas,
    mesh: Mesh,
    angleY: Float,
    angleX: Float,
    d: Dimension,
) {
    c.clear(Color.BLACK)

    val rotated = Mesh(mesh.faces.map { face ->
        face.rotateY(angleY).rotateX(angleX)
    })

    val camera = Camera(d.cx, d.cy, d.hf * 0.4f, 8f)

    // Camera sits at z = -distance, the mesh sits around z ≈ 0, and the light
    // sits on the far side at z = +0.5 — i.e. "below" the mesh, shining up
    // through the gaps toward the camera.
    val light1 = LightSource(Vec3(0f, 0f, 0.5f), NipponColors.col192_GUNJYO)
    val light2 = LightSource(Vec3(-0.2f, -0.3f, 0.5f), NipponColors.col102_TAMAMOROKOSHI)

    val gv = VolumetricLight(
        lights = listOf(light1, light2),
        samples = 6,
        strength = 3.0f,
        blendMode = VolumetricBlend.ADD,
        falloff = Falloff.INVERSE_SQUARE,
        maxDistance = 12f,
        seed = 1L,
        ambient = 0.04f,
        background = Color.BLACK,
    ).render(camera, rotated, d.w, d.h)

    gv.snapshotTo(c)
}

package t3d.backbone

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.math.toRadians
import dev.oblac.gart.tri3d.*
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color

fun main() {
    val gart = Gart.of("backbone", 1024, 1024)
    val g = gart.gartvas()
    val d = gart.d
    val w = gart.window()

    val wall = 0xFF000000.toInt()
    val lightColor = 0xFFFFFFFF.toInt()

    val numLayers = 14 // OK
    val baseHalfSize = 1.8f
    val topHalfSize = 0.25f
    val layerHalfHeight = 0.25f
    val layerGap = 0.03f
    val pyramidMidY = (numLayers - 1) * (layerHalfHeight * 2 + layerGap) / 2f

    val pyramid = buildPyramid(
        numLayers = numLayers,
        baseHalfSize = baseHalfSize,
        topHalfSize = topHalfSize,
        layerHalfHeight = layerHalfHeight,
        layerGap = layerGap,
        wall = wall,
        twistPerLayer = 8f,
    )

    // Look-at pyramid center; pitch negative tilts camera down from above
    val pose = CameraPose(
        position = Vec3(0f, pyramidMidY, 0f),
        yaw = 0.4f,
        pitch = -1.0f,
    )

    draw(g.canvas, pyramid, pose, pyramidMidY, d, lightColor)
    gart.saveImage(g)
    w.showImage(g)
}

/**
 * @param twistPerLayer Degrees of rotation around Y added to each successive
 *   layer. Layer 0 stays unrotated; layer i is rotated by `i * twistPerLayer`.
 *   Each layer's center lies on the Y axis, so this rotates each layer
 *   around its own vertical axis.
 */
private fun buildPyramid(
    numLayers: Int,
    baseHalfSize: Float,
    topHalfSize: Float,
    layerHalfHeight: Float,
    layerGap: Float,
    wall: Int,
    twistPerLayer: Float = 0f,
): Mesh {
    val faces = mutableListOf<Face>()
    val layerStep = layerHalfHeight * 2 + layerGap
    for (i in 0 until numLayers) {
        val t = if (numLayers <= 1) 0f else i.toFloat() / (numLayers - 1)
        val halfSize = baseHalfSize + (topHalfSize - baseHalfSize) * t
        val cy = i * layerStep
        val angle = (i * twistPerLayer).toRadians()
        val v = arrayOf(
            rotateY(Vec3(-halfSize, cy - layerHalfHeight, -halfSize), angle),
            rotateY(Vec3( halfSize, cy - layerHalfHeight, -halfSize), angle),
            rotateY(Vec3( halfSize, cy + layerHalfHeight, -halfSize), angle),
            rotateY(Vec3(-halfSize, cy + layerHalfHeight, -halfSize), angle),
            rotateY(Vec3(-halfSize, cy - layerHalfHeight,  halfSize), angle),
            rotateY(Vec3( halfSize, cy - layerHalfHeight,  halfSize), angle),
            rotateY(Vec3( halfSize, cy + layerHalfHeight,  halfSize), angle),
            rotateY(Vec3(-halfSize, cy + layerHalfHeight,  halfSize), angle),
        )
        // 4 side walls; top (+Y) and bottom (-Y) faces are open so light passes through
        faces += Face(v[0], v[1], v[2], wall)
        faces += Face(v[0], v[2], v[3], wall)
        faces += Face(v[5], v[4], v[7], wall)
        faces += Face(v[5], v[7], v[6], wall)
        faces += Face(v[4], v[0], v[3], wall)
        faces += Face(v[4], v[3], v[7], wall)
        faces += Face(v[1], v[5], v[6], wall)
        faces += Face(v[1], v[6], v[2], wall)
    }
    return Mesh(faces)
}

private fun draw(
    c: Canvas,
    pyramid: Mesh,
    pose: CameraPose,
    pyramidMidY: Float,
    d: Dimension,
    lightColor: Int,
) {
    c.clear(Color.BLACK)

    val transformed = pose.toCameraSpace(pyramid)

    val camera = Camera(d.cx, d.cy, d.hf * 0.20f, 8f)

    val lightA = LightSource(pose.toCameraSpace(Vec3(0f, pyramidMidY * 0.4f, 0f)), lightColor)
    val lightB = LightSource(pose.toCameraSpace(Vec3(0f, pyramidMidY * 1.6f, 0f)), NipponColors.col033_BENIKABA)

    val gv = VolumetricLight(
        lights = listOf(lightA, lightB),
        samples = 6,
        strength = 4.0f,
        blendMode = VolumetricBlend.ADD,
        falloff = Falloff.INVERSE_SQUARE,
        maxDistance = 14f,
        seed = 1L,
        ambient = 0f,
        background = Color.BLACK,
    ).render(camera, transformed, d.w, d.h)

    gv.snapshotTo(c)
}

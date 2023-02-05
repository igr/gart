package studio.oblac.gart.kaleiircle

import studio.oblac.gart.skia.Color4f

class DHCircle(
    val radius: Float,
    val width: Float,
    val colors: Pair<Color4f, Color4f>,
    val innerAngle: Float,
    val angle: Float = 0f,
    val type: DHType = DHType.FULL
)

enum class DHType {
    TRIANGLE, CIRCLE, FULL
}


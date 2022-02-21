package ac.obl.gart.kaleiircle

import ac.obl.gart.skia.Color4f

class DHCircle(
    val radius: Float,
    val width: Float,
    val colors: Pair<Color4f, Color4f>,
    val angle: Float = 0f,
    val type: DHType = DHType.FULL
)

enum class DHType {
    TRIANGLE, CIRCLE, FULL
}


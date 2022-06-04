package ac.obl.gart.math

fun doubleLoop(imax: Int, jmax: Int, consumer: (Pair<Int, Int>) -> Unit) =
    (0 until jmax).flatMap { (0 until imax).map { j -> j to it } }.forEach(consumer)

fun doubleLoop(
    gap: Pair<Float, Float>,
    w: Float,
    h: Float,
    step: Pair<Float, Float>,
    consumer: (Pair<Float, Float>) -> Unit,
) {
    val gapx = gap.first
    val gapy = gap.second

    var x = gapx
    var y = gapy
    while (y < h - gapy) {
        while (x < w - gapx) {
            consumer(x to y)
            x += step.first
        }
        y += step.second
        x = gapx
    }
}

@JvmName("doubleLoop_count")
fun doubleLoop(
    gap: Pair<Float, Float>,
    w: Float,
    h: Float,
    count: Pair<Int, Int>,
    consumer: (Pair<Float, Float>) -> Unit,
) {
    val gapx = gap.first
    val gapy = gap.second

    val stepx: Float = (w - 2 * gapx) / count.first
    val stepy: Float = (h - 2 * gapy) / count.second

    var x = gapx
    var y = gapy
    while (y <= h - gapy) {
        while (x <= w - gapx) {
            consumer(x to y)
            x += stepx
        }
        y += stepy
        x = gapx
    }
}

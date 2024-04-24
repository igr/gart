package dev.oblac.gart.palecircles

import dev.oblac.gart.color.Palette
import dev.oblac.gart.math.rnd

const val STARTING_R = 6f

data class Circle(val x: Int, val y: Int, val r: Float, val color: Int = colors.random(), var off: Float = 0f) {
    fun rOff() = r + off
}

fun createCircleSet(size: Int, count: Int) : MutableList<Circle> {
    var x = size
    var y = 0
    val step = size / (2 * count)
    var r = STARTING_R
    val rStep = (size/1.6f) / count

    val rndOffset = 5

    val circles = mutableListOf<Circle>()
    for (i in 0..count) {
        circles.add(Circle(x + rnd(-rndOffset, rndOffset), y + rnd(-rndOffset, rndOffset), r))
        x -= step
        y += step
        r += rStep
    }
    return circles
}

data class CircleSet(
    val circles: MutableList<Circle>,
    val x: Int,
    val y: Int
)

val colors = Palette(
    0xFFFFC6AC,
    0xFFFFF6DC,
    0xFFFFA194,
    0xFF1E1F15,
    0xFFFFFFFF
)

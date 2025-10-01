package dev.oblac.gart.pack

import dev.oblac.gart.gfx.Circle
import dev.oblac.gart.gfx.distanceTo
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Rect

/**
 * A very simple circle packer that randomly places circles within a box, ensuring they don't overlap.
 * @param rect The bounding box within which to pack circles.
 * @param attempts The number of random placement attempts to make.
 * @param minRadius The minimum radius of the circles. It is the beginning radius for growth.
 * @param maxRadius The maximum radius of the circles.
 * @param growth The amount by which to grow the circles after placement.
 * @param padding The minimum distance between circles.
 */
fun simpleCirclePacker(
    rect: Rect,
    attempts: Int = 100_000,
    minRadius: Float = 5f,
    maxRadius: Float = 20f,
    growth: Int = 1,
    padding: Int = 20,
    isInside: (Circle) -> Boolean = { true },
): List<Circle> {
    val circles = mutableListOf<Circle>()
    fun Circle.isValid(): Boolean {
        return isInside(this) &&
            circles.none { it.center.distanceTo(this.center) < (it.radius + this.radius + padding) }
    }

    repeat(attempts) {
        val x = rndf(rect.left, rect.right)
        val y = rndf(rect.top, rect.bottom)
        var newCircle = Circle(x, y, minRadius)
        if (!newCircle.isValid()) {
            return@repeat
        }
        // newCircle inside, start growing
        var radius = minRadius + growth
        while (radius < maxRadius) {
            val grownCircle = Circle(x, y, radius)
            if (!grownCircle.isValid()) {
                break
            }
            newCircle = grownCircle
            radius += growth
        }
        circles.add(newCircle)
    }
    return circles

}

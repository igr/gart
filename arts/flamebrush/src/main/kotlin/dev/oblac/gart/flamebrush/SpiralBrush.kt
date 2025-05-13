package dev.oblac.gart.flamebrush

import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.gfx.createSpiral
import org.jetbrains.skia.Point

class SpiralBrush(
    private val steps: Int
) {
    private var offset: Angle = Degrees.ZERO
    private var delta: Angle = Degrees(1f)

    fun tick() {
        offset += delta
    }

    fun points(center: Point, radius: Float): List<Point> {
        return createSpiral(center, radius, steps, offset, 3)
    }
}

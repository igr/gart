package dev.oblac.gart.noise

import dev.oblac.gart.Dimension
import dev.oblac.gart.openrndr.toSkikoPoint
import org.jetbrains.skia.Point
import org.openrndr.extra.noise.poissonDiskSampling

fun poissonDiskSamplingNoise(d: Dimension, r: Double = 30.0): List<Point> {
    return poissonDiskSampling(
        org.openrndr.shape.Rectangle(0.0, 0.0, d.wd, d.hd),
        r
    ).map { it.toSkikoPoint() }
}

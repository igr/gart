package dev.oblac.gart.gfx

import org.jetbrains.skia.PathBuilder

fun PathBuilder.addCircle(circle: Circle) =
    this.addCircle(circle.center.x, circle.center.y, circle.radius)

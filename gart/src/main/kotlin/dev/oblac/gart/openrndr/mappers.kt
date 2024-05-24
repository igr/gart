package dev.oblac.gart.openrndr

import org.jetbrains.skia.Point
import org.openrndr.math.Vector2

fun Vector2.toSkikoPoint() = Point(x.toFloat(), y.toFloat())

fun Point.toOpenrndrVector2() = Vector2(x.toDouble(), y.toDouble())

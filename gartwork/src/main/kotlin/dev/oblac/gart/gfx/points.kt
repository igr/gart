package dev.oblac.gart.gfx

import dev.oblac.gart.Dimension
import dev.oblac.gart.skia.Point

fun Point.isInside(dimension: Dimension) = x.toInt() in 0 until dimension.w && y.toInt() in 0 until dimension.h
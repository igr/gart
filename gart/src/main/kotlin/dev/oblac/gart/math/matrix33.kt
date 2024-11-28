package dev.oblac.gart.math

import org.jetbrains.skia.Matrix33

fun Matrix33.Companion.multiply(a: Matrix33, b: Matrix33): Matrix33 = IDENTITY.makeConcat(a).makeConcat(b)

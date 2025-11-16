package dev.oblac.gart.math

fun <T> cond(b: Boolean, x: T, y: T): T = if (b) x else y

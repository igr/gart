package dev.oblac.gart

import kotlin.time.Duration

fun Duration.toSeconds(): Float {
    return this.inWholeMilliseconds.toFloat() / 1000f
}

fun Duration.toFrames(fps: Int): Long {
    return this.inWholeMilliseconds * fps / 1000
}

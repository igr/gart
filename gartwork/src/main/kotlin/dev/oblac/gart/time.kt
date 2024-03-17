package dev.oblac.gart

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Duration.toSeconds(): Float {
    return this.inWholeMilliseconds.toFloat() / 1000f
}

fun Duration.toFrames(fps: Int): Long {
    return this.inWholeMilliseconds * fps / 1000
}

/**
 * FRAME -> DURATION
 */
fun Long.toTime(frameTimeNanos: Long): Duration = (frameTimeNanos * this).toDuration(DurationUnit.NANOSECONDS)

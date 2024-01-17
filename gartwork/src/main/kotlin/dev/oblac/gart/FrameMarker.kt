package dev.oblac.gart

import kotlin.time.Duration

enum class FrameMarkerType {
    SINGLE_FRAME,
    REPEATED_FRAME,
}

data class FrameMarker(val value: FramesCount, val frameMarkerType: FrameMarkerType)

fun before(currentFrame: FramesCount, marker: FrameMarker): Boolean {
    return when (marker.frameMarkerType) {
        FrameMarkerType.SINGLE_FRAME -> currentFrame < marker.value
        FrameMarkerType.REPEATED_FRAME -> true
    }
}

fun after(currentFrame: FramesCount, marker: FrameMarker): Boolean {
    return when (marker.frameMarkerType) {
        FrameMarkerType.SINGLE_FRAME -> currentFrame > marker.value
        FrameMarkerType.REPEATED_FRAME -> false
    }
}

fun isNow(currentFrame: FramesCount, marker: FrameMarker): Boolean {
    return when (marker.frameMarkerType) {
        FrameMarkerType.SINGLE_FRAME -> currentFrame == marker.value
        FrameMarkerType.REPEATED_FRAME -> currentFrame.mod(marker.value)
    }
}

class FrameMarkerBuilder(private val frames: Frames) {
    fun atFrame(number: Number): FrameMarker {
        return FrameMarker(FramesCount.of(number), FrameMarkerType.SINGLE_FRAME)
    }

    fun atTime(duration: Duration): FrameMarker {
        return FrameMarker(FramesCount.of(duration, frames.fps), FrameMarkerType.SINGLE_FRAME)
    }

    fun onEveryFrame(number: Number): FrameMarker {
        return FrameMarker(FramesCount.of(number), FrameMarkerType.REPEATED_FRAME)
    }

    fun onEvery(duration: Duration): FrameMarker {
        return FrameMarker(FramesCount.of(duration, frames.fps), FrameMarkerType.REPEATED_FRAME)
    }
}

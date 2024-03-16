package dev.oblac.gart

enum class FrameMarkerType {
    SINGLE_FRAME,
    REPEATED_FRAME,
}

//data class FrameMarker(val value: FramesCount, val frameMarkerType: FrameMarkerType)
//
//internal fun before(currentFrame: FramesCount, marker: FrameMarker): Boolean {
//    return when (marker.frameMarkerType) {
//        FrameMarkerType.SINGLE_FRAME -> currentFrame < marker.value
//        FrameMarkerType.REPEATED_FRAME -> true
//    }
//}
//
//internal fun after(currentFrame: FramesCount, marker: FrameMarker): Boolean {
//    return when (marker.frameMarkerType) {
//        FrameMarkerType.SINGLE_FRAME -> currentFrame > marker.value
//        FrameMarkerType.REPEATED_FRAME -> false
//    }
//}
//
//internal fun isNow(currentFrame: FramesCount, marker: FrameMarker): Boolean {
//    return when (marker.frameMarkerType) {
//        FrameMarkerType.SINGLE_FRAME -> currentFrame == marker.value
//        FrameMarkerType.REPEATED_FRAME -> currentFrame.mod(marker.value)
//    }
//}
//
//class FrameMarkerBuilder(private val frames: Frames) {
//    fun atFrame(number: Number): FrameMarker {
//        return FrameMarker(FramesCount.of(number), FrameMarkerType.SINGLE_FRAME)
//    }
//
//    fun atTime(duration: Duration): FrameMarker {
//        return FrameMarker(FramesCount.of(duration, frames.fps), FrameMarkerType.SINGLE_FRAME)
//    }
//
//    fun onEveryFrame(number: Number): FrameMarker {
//        return FrameMarker(FramesCount.of(number), FrameMarkerType.REPEATED_FRAME)
//    }
//
//    fun onEvery(duration: Duration): FrameMarker {
//        return FrameMarker(FramesCount.of(duration, frames.fps), FrameMarkerType.REPEATED_FRAME)
//    }
//}

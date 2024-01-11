package dev.oblac.gart

import kotlin.time.Duration

fun before(frameMarker: FrameMarker): Boolean = frameMarker.before()

interface FrameMarker {
    /**
     * Returns `true` if current frame is before the marker.
     */
    fun before(): Boolean

    /**
     * Returns `true` if current frame is after the marker.
     */
    fun after(): Boolean

    /**
     * Returns true if current frame equals the marker.
     */
    fun now(): Boolean
}


class FrameMarkerBuilder(private val frames: Frames) {
    fun atFrame(number: Number): FrameMarker {
        return FrameMarkerOnSingleFrame(frames, FramesCount(number.toLong()))
    }

    fun atTime(duration: Duration): FrameMarker {
        return FrameMarkerOnSingleFrame(frames, FramesCount.of(duration, frames.fps))
    }

    fun onEveryFrame(number: Number): FrameMarker {
        return FrameMarkerOnRepeatedFrame(frames, FramesCount(number.toLong()))
    }

    fun onEvery(duration: Duration): FrameMarker {
        return FrameMarkerOnRepeatedFrame(frames, FramesCount.of(duration, frames.fps))
    }

    /**
     * Creates a marker after a certain duration passed from the current moment.
     */
    fun after(duration: Duration): FrameMarker {
        return FrameMarkerOnSingleFrame(
            frames, frames.count + FramesCount.of(duration, frames.fps)
        )
    }
}

internal class FrameMarkerOnSingleFrame(private val frames: Frames, private val frameValue: FramesCount) : FrameMarker {
    override fun before() = frames.count < frameValue
    override fun after() = frames.count > frameValue
    override fun now() = frames.count == frameValue
}

internal class FrameMarkerOnRepeatedFrame(private val frames: Frames, private val frameValue: FramesCount) : FrameMarker {
    override fun before() = true
    override fun after() = false
    override fun now() = frames.count.mod(frameValue)
}

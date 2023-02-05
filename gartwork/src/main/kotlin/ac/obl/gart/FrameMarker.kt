package ac.obl.gart

import kotlin.time.Duration

interface FrameMarker {
    /**
     * Returns true if current frame is before the marker.
     */
    fun before(): Boolean

    /**
     * Returns true if current frame is after the marker.
     */
    fun after(): Boolean

    /**
     * Returns true if current frame equals the marker.
     */
    fun now(): Boolean
}


class FrameMarkerBuilder(private val frames: Frames) {
    fun atNumber(number: Number): FrameMarker {
        return FrameMarkerSingle(frames, number.toLong())
    }

    fun atSecond(seconds: Number): FrameMarker {
        return FrameMarkerSingle(frames, (frames.rate() * seconds.toFloat()).toLong())
    }

    fun onEveryFrame(number: Number): FrameMarker {
        return FrameMarkerRepeated(frames, number.toLong())
    }

    fun onEverySecond(seconds: Number): FrameMarker {
        return FrameMarkerRepeated(frames, (frames.rate() * seconds.toFloat()).toLong())
    }

    fun onEvery(duration: Duration): FrameMarker {
        return FrameMarkerRepeated(frames, (frames.rate() * duration.inWholeSeconds.toFloat()).toLong())
    }
}

internal class FrameMarkerSingle(private val frames: Frames, private val frameValue: Long) : FrameMarker {
    override fun before() = frames.count() < frameValue
    override fun after() = frames.count() > frameValue
    override fun now() = frames.count() == frameValue
}

internal class FrameMarkerRepeated(private val frames: Frames, private val frameValue: Long) : FrameMarker {
    override fun before() = true
    override fun after() = false
    override fun now() = frames.count().mod(frameValue) == 0L
}

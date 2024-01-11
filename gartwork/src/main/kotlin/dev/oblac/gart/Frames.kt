package dev.oblac.gart

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@JvmInline
value class FramesCount(val value: Long) {
    fun time(fps: Int) = (value * 1000 / fps).milliseconds

    operator fun inc() = FramesCount(value + 1)
    operator fun compareTo(frameValue: FramesCount): Int {
        return value.compareTo(frameValue.value)
    }

    fun mod(frameValue: FramesCount): Boolean {
        return value % frameValue.value == 0L
    }

    operator fun plus(of: FramesCount): FramesCount = FramesCount(value + of.value)

    companion object {
        val ZERO = FramesCount(0)
        fun of(time: Duration, fps: Int) = FramesCount(time.inWholeMilliseconds * fps / 1000)
    }
}

interface Frames {
    /**
     * Framerate in frames per second.
     */
    val fps: Int

    /**
     * Elapsed time.
     */
    val time: Duration

    /**
     * Elapsed number of frames.
     */
    val count: FramesCount

    /**
     * Duration of a single frame.
     */
    val frameDuration: Duration

    /**
     * Starts marker creation.
     */
    fun marker() = FrameMarkerBuilder(this)

}

class FramesCounter(override val fps: Int) : Frames {

    private val singleFrameDuration = 1000.milliseconds / fps
    private var total: FramesCount = FramesCount.ZERO
    private var elapsed: Duration = 0.milliseconds
    /**
     * Increments frame counter.
     */
    fun tick(): FramesCounter {
        total = total++
        elapsed = total.time(fps)
        return this
    }

    override val count: FramesCount
        get() = total

    override val time: Duration
        get() = elapsed

    override val frameDuration: Duration
        get() = singleFrameDuration

    override fun toString(): String {
        return "Count: $total. Time: ${time.toString(DurationUnit.SECONDS, 2)}s."
    }
}

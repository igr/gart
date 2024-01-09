package dev.oblac.gart

import dev.oblac.gart.math.format

interface Frames {
    /**
     * Framerate (fps).
     */
    val rate: Int

    /**
     * Elapsed time in seconds.
     */
    val time: Float
        get() = count / rate.toFloat()

    /**
     * Returns elapsed number of frames.
     */
    val count: Long

    /**
     * Converts frames count to time in seconds.
     */
    fun framesToTime(frames: Int): Float {
        return frames / rate.toFloat()
    }

    fun timeToFrames(time: Float): Long {
        return (time * rate).toLong()
    }

    /**
     * Creates a marker on a certain position.
     */
    fun marker() = FrameMarkerBuilder(this)
}

class FramesCounter(private val fps: Int) : Frames {

    private var total: Long = 0
    //private var callbacks: Array<(Frames)->Unit> = emptyArray()

    /**
     * Increments frame counter.
     */
    fun tick(): FramesCounter {
        total++
        return this
    }

    override val rate: Int
        get() = fps

    override val count: Long
        get() = total

    override fun toString(): String {
        return "Count: $total. Time: ${time.format(2)}."
    }
}

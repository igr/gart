package studio.oblac.gart

import studio.oblac.gart.math.format

interface Frames {
    /**
     * Returns a framerate.
     */
    fun rate(): Int
    /**
     * Returns elapsed time in seconds.
     */
    fun time(): Float
    /**
     * Returns elapsed number of frames.
     */
    fun count(): Long
    /**
     * Creates a marker on certain position.
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

    /**
     * Converts frames count to time in seconds.
     */
    fun framesToTime(frames: Int) : Float {
        return frames / fps.toFloat()
    }
    fun timeToFrames(time: Float) : Long {
        return (time * fps).toLong()
    }

    override fun time(): Float {
        return total / fps.toFloat()
    }

    override fun count() = total

    override fun rate(): Int = fps

    override fun toString(): String {
        return "Count: $total. Time: ${time().format(2)}."
    }
}

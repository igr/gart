package ac.obl.gart

import ac.obl.gart.math.format

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

class FramesCount(private val rate: Int) : Frames {

    private var total: Long = 0
    //private var callbacks: Array<(Frames)->Unit> = emptyArray()
    /**
     * Increments frame counter.
     */
    fun tick(): FramesCount {
        total++
        return this
    }

    /**
     * Converts frames count to time in seconds.
     */
    fun framesToTime(frames: Int) : Float {
        return frames / rate.toFloat()
    }
    fun timeToFrames(time: Float) : Long {
        return (time * rate).toLong()
    }

    override fun time(): Float {
        return total / rate.toFloat()
    }

    override fun count() = total

    override fun rate(): Int = rate

    override fun toString(): String {
        return "Count: $total. Time: ${time().format(2)}."
    }
}

package dev.oblac.gart

import org.jetbrains.skiko.FPSCounter

//data class FramesCount(val value: Long) {
//    fun time(fps: Int) = (value * 1000 / fps).milliseconds
//
//    operator fun compareTo(frameValue: FramesCount): Int {
//        return value.compareTo(frameValue.value)
//    }
//
//    fun mod(frameValue: FramesCount): Boolean {
//        return value % frameValue.value == 0L
//    }
//
//    operator fun plus(fc: FramesCount): FramesCount = FramesCount(value + fc.value)
//    operator fun plus(i: Int): FramesCount = FramesCount(value + i)
//
//    companion object {
//        val ZERO = FramesCount(0)
//        fun of(time: Duration, fps: Int) = FramesCount(time.inWholeMilliseconds * fps / 1000)
//        fun of(number: Number) = FramesCount(number.toLong())
//    }
//}

interface Frames {
    /**
     * Frame rate i.e. Frames per second.
     */
    val fps: Int

    /**
     * Time of the current frame in nanoseconds.
     */
    val frametime: Long

    /**
     * Current frame, i.e. total number of elapsed frames.
     */
    val frame: Long

    /**
     * Returns true if the new frame is drawn.
     * It is called FPS times per second.
     */
    val new: Boolean

    /**
     * Calculates the time of the current frame, elapsed duration.
     */
    val time get() = frame.toTime(frametime)

    /**
     * Called on each tick, with given FPS.
     */
    fun tick(callback: () -> Unit) {
        if (new) {
            callback()
        }
    }

    /**
     * Invokes the callback f the current frame is the target frame.
     */
    fun onFrame(targetFrame: Long, callback: () -> Unit) {
        if (new && frame == targetFrame) {
            callback()
        }
    }

    fun onBeforeFrame(targetFrame: Long, callback: () -> Unit) {
        if (new && frame < targetFrame) {
            callback()
        }
    }

    fun onAfterFrame(targetFrame: Long, callback: () -> Unit) {
        if (new && frame > targetFrame) {
            callback()
        }
    }

    fun onEveryFrame(targetFrame: Long, callback: () -> Unit) {
        if (new && frame % targetFrame == 0L) {
            callback()
        }
    }
}

/**
 * Simple frames counter for manually controlled movies.
 */
internal class FrameCounter(override val fps: Int) : Frames {
    override val frametime = 1000000000L / fps   // frame time in nanoseconds
    private var totalFrames: Long = 0
    override val frame get() = totalFrames
    private var drawNew: Boolean = false
    override val new get() = drawNew

    /**
     * Increments frame counter.
     */
    fun tick() {
        totalFrames += 1
        drawNew = true
    }

    fun tock() {
        drawNew = false
    }

    fun set(frame: Long) {
        totalFrames = frame
    }
}

/**
 * Frames drawing FPS guard
 */
internal class FpsGuard(fps: Int, private val printFps: Boolean = false) {
    private val framesCounter = FrameCounter(fps)
    val frames get() = framesCounter

    private var last = System.nanoTime()

    private val fpsCounterMax = FPSCounter()
    private val fpsCounterReal = FPSCounter()
    private val activeTicker = ActiveTicker()

    fun withFps(now: Long) {
        fpsCounterMax.tick()

        if (now - last > framesCounter.frametime) {
            fpsCounterReal.tick()
            framesCounter.tick()
            last = now
        } else {
            framesCounter.tock()
        }

        if (printFps) {
            print("frame: ${frames.frame} • fps: ${fpsCounterReal.average} / ${fpsCounterMax.average} ${activeTicker.str()} \r")
        }
    }

    private class ActiveTicker {
        private val chars = charArrayOf('¦', '/', '—', '\\')
        private var index = 0
        fun str(): Char {
            val c = chars[index]
            index = (index + 1) % chars.size
            return c
        }
    }

}

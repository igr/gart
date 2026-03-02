package dev.oblac.gart

import dev.oblac.gart.math.format
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
     * Frame rate i.e. Frames per second (constant).
     */
    val fps: Int

    /**
     * Duration of the single frame in nanoseconds (constant).
     */
    val frameDurationNanos: Long

    /**
     * Duration of the single frame in seconds (constant).
     */
    val frameDurationSeconds: Float

    /**
     * Current frame number, i.e. total number of elapsed frames.
     */
    val frame: Long

    /**
     * Returns true if the new frame is drawn.
     * It is called FPS times per second.
     */
    val new: Boolean

    /**
     * Calculates the time of the current frame, elapsed duration.
     * Depends on fhe frame count.
     * @see frame
     */
    val frameTime get() = frame.toTime(frameDurationNanos)

    /**
     * Calculates the time of the current frame in seconds, elapsed duration.
     * Depends on fhe frame count.
     * @see frame
     */
    val frameTimeSeconds get() = frame / fps.toFloat()

    /**
     * Elapsed time in nanoseconds since the start.
     */
    val time: Long

    /**
     * Elapsed time in seconds since the start.
     * @see time
     */
    val timeSeconds get() = time / 1_000_000_000f

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

    /**
     * Prints all frames info in one line, for debugging purposes.
     */
    fun print() {
        print("frame: $frame | fps: $fps | frameTime: ${frameTime.toSeconds().format(3)} | new: $new | time: ${timeSeconds.format(3)}s\r")
    }

    companion object {
        val ZERO = object : Frames {
            override val fps = 0
            override val frameDurationNanos = 0L
            override val frameDurationSeconds = 0f
            override val frame = 0L
            override val new = false
            override val time = 0L
        }
    }
}

/**
 * Simple frames counter for manually controlled movies.
 */
internal class FrameCounter(override val fps: Int) : Frames {
    private var _time = 0L
    override val frameDurationNanos = 1000000000L / fps   // frame time in nanoseconds
    override val frameDurationSeconds = 1f / fps    // frame time in seconds
    private var totalFrames: Long = 0
    override val frame get() = totalFrames
    private var drawNew: Boolean = false
    override val new get() = drawNew
    override val time get() = _time

    fun updateTime(now: Long) {
        _time = now
    }

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

    /**
     * Counts how many times the onRender was called (including the times when the frame was not drawn because of FPS guard).
     */
    private val fpsCounterMax = FPSCounter()

    /**
     * Counts how many times the frame was actually drawn.
     * It is expected to be close to the target FPS, but may be lower if the rendering is too heavy or higher if the rendering is very light and the system can keep up with the target FPS.
     */
    private val fpsCounterReal = FPSCounter()
    private val activeTicker = ActiveTicker()

    fun withFps(now: Long) {
        fpsCounterMax.tick()
        framesCounter.updateTime(now)

        if (now - last > framesCounter.frameDurationNanos) {
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

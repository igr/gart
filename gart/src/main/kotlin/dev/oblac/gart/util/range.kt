package dev.oblac.gart.util

class FloatRange(val start: Float, val end: Float, val steps: Int) : Iterable<Float> {
    init {
        require(steps >= 2) { "steps must be at least 2" }
    }

    override fun iterator(): Iterator<Float> {
        val step = (end - start) / (steps - 1)
        return (0 until steps).asSequence().map { start + it * step }.iterator()
    }

    companion object {
        fun of(start: Number, end: Number, steps: Int) =
            FloatRange(start.toFloat(), end.toFloat(), steps)
    }
}

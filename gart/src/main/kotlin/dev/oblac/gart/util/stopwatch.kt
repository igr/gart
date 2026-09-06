package dev.oblac.gart.util

/**
 * Wall-clock timer for the progress prints in a piece's main. `val sw = Stopwatch()`, then
 * `"... in ${sw.lap()}ms"` for the time since the previous lap (or the start), or `sw.ms` for the
 * time since the start without resetting anything. Milliseconds, as the prints have always been.
 */
class Stopwatch {
    private val start = System.currentTimeMillis()
    private var last = start

    /** Milliseconds since the start. */
    val ms: Long get() = System.currentTimeMillis() - start

    /** Milliseconds since the previous lap (or the start), and the next lap begins now. */
    fun lap(): Long {
        val now = System.currentTimeMillis()
        val d = now - last
        last = now
        return d
    }
}

/** Runs [block], prints `"<label> in <ms>ms"`, and hands back whatever the block returned. */
inline fun <T> timed(label: String, block: () -> T): T {
    val sw = Stopwatch()
    val result = block()
    println("$label in ${sw.ms}ms")
    return result
}

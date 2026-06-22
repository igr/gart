package dev.oblac.gart.io

import java.io.File

// knobs (overridable via -Dkey=value for sweeping; defaults are the chosen values)
// every read is recorded with its RESOLVED value (default or override). if -Dparams.out is set,
// the whole resolved set is dumped to that file on exit - lets a sweeper capture exactly what an
// art ran with, defaults and all, so it can later sweep around any knob (not just the ones passed).

private val recorded = LinkedHashMap<String, String>()
private var hooked = false

@Synchronized
private fun record(k: String, v: Any?) {
    recorded[k] = v.toString()
    if (!hooked) {
        hooked = true
        System.getProperty("params.out")?.let { out ->
            Runtime.getRuntime().addShutdownHook(Thread {
                runCatching { File(out).writeText(recorded.entries.joinToString("\n") { "${it.key}=${it.value}" } + "\n") }
            })
        }
    }
}

fun pi(k: String, d: Int) = (System.getProperty(k)?.toInt() ?: d).also { record(k, it) }
fun pf(k: String, d: Float) = (System.getProperty(k)?.toFloat() ?: d).also { record(k, it) }
fun ps(k: String, d: String) = (System.getProperty(k) ?: d).also { record(k, it) }

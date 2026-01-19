package dev.oblac.gart

import dev.oblac.gart.math.rndb
import dev.oblac.gart.math.rndf
import java.io.File

/**
 * Gart random number generator with replay capability.
 *
 * When created with `replay = false`, it generates random numbers and stores them.
 * When created with `replay = true`, it reads the stored numbers from a file and replays them.
 *
 * The generated or replayed values can be saved to a file using the `save()` method.
 *
 * @param name The name used for the storage file.
 * @param replay If true, the generator will replay stored values instead of generating new ones.
 */
class GartRand(private val name: String, private val replay: Boolean = false) {
    private val storage = mutableListOf<String>()
    private var index = 0

    init {
        if (replay) {
            load()
        }
    }

    fun f(min: Float = 0f, max: Float = 1f): Float =
        if (replay) storage[index++].toFloat()
        else rndf(min, max).also { storage.add(it.toString()) }

    fun b(): Boolean =
        if (replay) storage[index++].toBoolean()
        else rndb().also { storage.add(it.toString()) }

    fun save() {
        File("$name.rand").writeText(storage.joinToString("\n"))
    }

    private fun load() {
        val file = File("$name.rand")
        if (file.exists()) {
            storage.addAll(file.readLines())
        } else {
            throw IllegalStateException("Rand file not found: ${file.absolutePath}")
        }
    }
}

package dev.oblac.gart.util

/**
 * Returns a random element from the list, except the one provided.
 */
fun <E> List<E>.randomExcept(exception: E): E {
    while (true) {
        val random = this.random()
        if (random != exception) return random
    }
}

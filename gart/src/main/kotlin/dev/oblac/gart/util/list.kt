package dev.oblac.gart.util

import java.lang.Math.floorMod
import kotlin.math.abs

/**
 * Returns a random element from the list, except the one provided.
 */
fun <E> List<E>.randomExcept(exception: E): E {
    while (true) {
        val random = this.random()
        if (random != exception) return random
    }
}

fun <T> List<T>.circular(): List<T> {
    return object : List<T> {
        override val size: Int
            get() = this@circular.size

        override fun isEmpty(): Boolean {
            return this@circular.isEmpty()
        }

        override fun contains(element: T): Boolean {
            return this@circular.contains(element)
        }

        override fun iterator(): Iterator<T> {
            return this@circular.iterator()
        }

        override fun containsAll(elements: Collection<T>): Boolean {
            return this@circular.containsAll(elements)
        }

        override fun get(index: Int): T {
            return this@circular[floorMod(index, this@circular.size)]
//            return this@circular[index % this@circular.size]
        }

        override fun indexOf(element: T): Int {
            return this@circular.indexOf(element)
        }

        override fun lastIndexOf(element: T): Int {
            return this@circular.lastIndexOf(element)
        }

        override fun listIterator(): ListIterator<T> {
            return this@circular.listIterator()
        }

        override fun listIterator(index: Int): ListIterator<T> {
            return this@circular.listIterator(index)
        }

        override fun subList(fromIndex: Int, toIndex: Int): List<T> {
            return this@circular.subList(fromIndex, toIndex)
        }
    }
}

operator fun <T> List<T>.rem(index: Int): T {
    return this[abs(index % this.size)]
}

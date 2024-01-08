package dev.oblac.gart.gfx

import kotlin.math.abs

class Palette(internal val colors: IntArray) {
    constructor(vararg values: Long) : this(values.map { it.toInt() }.toIntArray())

    val size = colors.size

	operator fun get(position: Int): Int {
		return colors[position]
	}

    fun getSafe(position: Int): Int {
        return colors[abs(position) % size]
    }

    operator fun plus(otherPalette: Palette): Palette {
        return Palette(this.colors + otherPalette.colors)
    }

    fun random(): Int {
        return colors.random()
    }

    fun <R> map(transform: (Int) -> R): List<R> {
        return colors.map(transform)
    }

    fun reversed(): Palette {
        return Palette(colors.reversedArray())
    }
}

package dev.oblac.gart.color

import kotlin.math.abs

class Palette(internal val colors: IntArray) {
    constructor(vararg values: Long) : this(values.map { it.toInt() }.toIntArray())

    val size = colors.size

	operator fun get(position: Int): Int {
		return colors[position]
	}

    fun at(position: Int): Int {
        return colors[position]
    }

    fun safe(position: Number): Int {
        return colors[abs(position.toInt()) % size]
    }

    fun relative(offset: Float): Int {
        val index = (offset * size).toInt()
        return colors[index % size]
    }

    fun last(): Int {
        return colors[size - 1]
    }

    operator fun plus(otherPalette: Palette): Palette {
        return Palette(this.colors + otherPalette.colors)
    }

    operator fun plus(color: Int): Palette {
        return Palette(this.colors + color)
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

    /**
     * Grows (expands) palette by adding gradients between each pair of colors.
     */
    fun expand(steps: Int): Palette {
        val delta = steps.toFloat() / (this.size - 1)
        var i = 0

        var gradient = Palette()
        var colorCounter = 0f
        while (i < this.size - 1) {
            // since delta is float, some gradients will have more steps than others
            val gradientSteps = (colorCounter + delta).toInt() - colorCounter.toInt()

            gradient += Palettes.gradient(this[i], this[i + 1], gradientSteps)
            i++
            colorCounter += delta
        }

        if (gradient.size == steps - 1) {
            gradient += this.last()
        }
        if (gradient.size != steps) {
            throw IllegalStateException("Gradient size is ${gradient.size}, expected $steps")
        }

        return gradient
    }

    fun sequence(): Sequence<Int> = colors.asSequence()
}

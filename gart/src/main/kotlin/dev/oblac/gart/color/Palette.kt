package dev.oblac.gart.color

import org.jetbrains.skia.Color4f
import kotlin.math.abs

class Palette(internal val colors: IntArray) {
    constructor(vararg values: Long) : this(values.map { it.toInt() }.toIntArray())

    val size = colors.size

    val indices: IntRange
        get() = colors.indices

	operator fun get(position: Int): Int {
		return colors[position]
	}

    fun at(position: Int): Int {
        return colors[position]
    }

    fun safe(position: Number): Int {
        return colors[abs(position.toInt()) % size]
    }

    fun bound(position: Number): Int {
        val index = position.toInt().coerceIn(0, size - 1)
        return colors[index]
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

    fun randomExclude(vararg color: Int): Int {
        val filtered = colors.filter { it !in color }
        if (filtered.isEmpty()) {
            throw IllegalArgumentException("No colors left in the palette after excluding the given colors.")
        }
        return filtered.random()
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

    fun expandReversed() = this + this.reversed()

    fun shuffle(): Palette {
        val clone = colors.clone()
        clone.shuffle()
        return Palette(clone)
    }

    /**
     * Splits the palette into [numberOfSplits] smaller palettes.
     * If the palette can't be split evenly, the last palette will have less colors.
     */
    fun split(numberOfSplits: Int): Array<Palette> {
        if (numberOfSplits == 1) {
            return arrayOf(this)
        }

        val baseSize = size / numberOfSplits
        val remainder = size % numberOfSplits

        return Array(numberOfSplits) { splitIndex ->
            val start = splitIndex * baseSize + minOf(splitIndex, remainder)
            val end = start + baseSize + if (splitIndex < remainder) 1 else 0
            Palette(colors.sliceArray(start until end))
        }
    }

    /**
     * Splits the palette into [numberOfPalettes] and returns them as a list for easier access.
     * This is a convenience method that calls split() and converts the result to a list.
     */
    fun splitIn(numberOfPalettes: Int): List<Palette> {
        return split(numberOfPalettes).toList()
    }

    /**
     * Shifts the colors in the palette by the given number of [steps].
     */
    fun shifted(steps: Int): Palette {
        val shiftedColors = IntArray(size)
        for (i in colors.indices) {
            val newIndex = (i + steps).mod(size)
            shiftedColors[newIndex] = colors[i]
        }
        return Palette(shiftedColors)
    }

    companion object {
        fun of(values: Collection<Int>) = Palette(values.toIntArray())
        fun of(vararg values: Int) = Palette(values)
        fun of(vararg values: Color4f) = Palette(values.map { it.toColor() }.toIntArray())
        fun of(vararg values: java.awt.Color) = Palette(values.map { rgb(it.red, it.blue, it.green) }.toIntArray())
        fun of(vararg values: String) = Palette(values.map { it.parseColor() }.toIntArray())
    }
}

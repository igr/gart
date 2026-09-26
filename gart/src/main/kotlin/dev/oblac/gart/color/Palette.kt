package dev.oblac.gart.color

import org.jetbrains.skia.Color4f
import kotlin.math.abs
import kotlin.random.Random

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

    /**
     * Samples the palette as one continuous ramp: [t] runs `0f..1f` across the whole palette and
     * the result is interpolated between the two entries it falls between. [relative] snaps to a
     * slot, this one blends - so a value walked over a short palette comes out as a smooth
     * gradient instead of a staircase. [t] is clamped, so the ends hold.
     */
    fun sample(t: Float): Int = lerpColors(colors, t)

    /**
     * [sample], but the blend between two entries is done in OKLCH: hue turns the short way round
     * and lightness moves evenly, so a vivid palette stays vivid between its stops instead of
     * dipping through grey. Slower, the two ends are converted per call.
     */
    fun sampleOklch(t: Float): Int = lerpColorsOklch(colors, t)

    /**
     * [sample], but the blend between two entries is a straight line in OKLab: lightness moves
     * evenly and hue does not turn, so two far-apart hues meet in a muted colour between them
     * (coral and navy give a dusty maroon, not the magenta [sampleOklch] swings through).
     */
    fun sampleOklab(t: Float): Int = lerpColorsOklab(colors, t)

    /**
     * The palette resampled to [n] colours, evenly spaced along [sampleOklab] from the first entry
     * to the last. A short palette gains blends between its entries, a long one is thinned out.
     */
    fun stretchOklab(n: Int): Palette {
        require(n >= 2) { "need at least 2 colours, got $n" }
        return Palette(IntArray(n) { sampleOklab(it / (n - 1f)) })
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

    operator fun rem(index: Int) = safe(index)

    fun random(): Int {
        return colors.random()
    }

    /** [random] drawing from [rng] - one `nextInt(size)` - so a seeded piece gets the same pick every run. */
    fun random(rng: Random): Int = colors[rng.nextInt(size)]

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

    /** [shuffle] drawing from [rng], so a seeded piece gets the same order every run. */
    fun shuffle(rng: Random): Palette {
        val clone = colors.clone()
        clone.shuffle(rng)
        return Palette(clone)
    }

    /** The colours that pass [predicate], in their original order. */
    fun filter(predicate: (Int) -> Boolean): Palette = Palette(colors.filter(predicate).toIntArray())

    /** The first [n] colours, or all of them when there are fewer. */
    fun take(n: Int): Palette = Palette(colors.take(n).toIntArray())

    /** The colour with the lowest [lumOf]; ties go to the earlier one. */
    fun darkest(): Int = colors.minBy { lumOf(it) }

    /** The colour with the highest [lumOf]; ties go to the earlier one. */
    fun lightest(): Int = colors.maxBy { lumOf(it) }

    /** All but the first [n] colours, in order; empty when there are no more. */
    fun drop(n: Int): Palette = Palette(colors.drop(n).toIntArray())

    /**
     * The colours at [indices], in the order given. An index may repeat; one outside the
     * palette throws.
     */
    fun pick(vararg indices: Int): Palette {
        indices.forEach { require(it in colors.indices) { "no colour $it, have 0..${size - 1}" } }
        return Palette(IntArray(indices.size) { colors[indices[it]] })
    }

    /** The colours ordered by [selector], lowest first. A stable sort, so ties keep their order. */
    fun <R : Comparable<R>> sortedBy(selector: (Int) -> R): Palette = Palette(colors.sortedBy(selector).toIntArray())

    /** The colour with the lowest [selector]; the earlier one wins a tie. Throws when empty. */
    fun <R : Comparable<R>> minBy(selector: (Int) -> R): Int = colors.minBy(selector)

    /** The colour with the highest [selector]; the earlier one wins a tie. Throws when empty. */
    fun <R : Comparable<R>> maxBy(selector: (Int) -> R): Int = colors.maxBy(selector)

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

    fun removeLast(): Palette {
        return Palette(colors.sliceArray(0 until size - 1))
    }

    fun toIntArray(): IntArray {
        return colors.clone()
    }

    companion object {
        fun of(values: Collection<Int>) = Palette(values.toIntArray())
        fun of(vararg values: Int) = Palette(values)
        fun of(vararg values: Color4f) = Palette(values.map { it.toColor() }.toIntArray())
        fun of(vararg values: java.awt.Color) = Palette(values.map { rgb(it.red, it.green, it.blue) }.toIntArray())
        fun of(vararg values: String) = Palette(values.map { it.parseColor() }.toIntArray())
    }
}

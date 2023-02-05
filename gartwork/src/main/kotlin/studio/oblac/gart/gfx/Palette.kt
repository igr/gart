package studio.oblac.gart.gfx

class Palette(private val colors: IntArray) {
    constructor(vararg values: Long) : this(values.map { it.toInt() }.toIntArray())

    val size = colors.size

	operator fun get(position: Int): Int {
		return colors[position]
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
}

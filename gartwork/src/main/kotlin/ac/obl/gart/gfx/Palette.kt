package ac.obl.gart.gfx

class Palette(private val colors: IntArray) {
	val size = colors.size

	operator fun get(position: Int): Int {
		return colors[position]
	}
}

interface PaletteSetter {
	operator fun set(position: Int, color: Int) : PaletteSetter
}

class PaletteBuilder(totalColors: Int) : PaletteSetter {
	private val colors = IntArray(totalColors)

	override operator fun set(position: Int, color: Int) : PaletteBuilder {
		colors[position] = color
		return this
	}

	fun apply(fn: (PaletteSetter) -> Unit): PaletteBuilder {
		fn(this)
		return this
	}

	fun get(): Palette {
		return Palette(colors)
	}

}

fun gradientFill(p: PaletteSetter, indexFrom: Int, indexTo: Int, colorFrom: Int, colorTo: Int) {
	var index = indexFrom
	var color = alpha(colorFrom, 0xFF)
	var i = indexTo - index + 1
	var r = red(colorFrom).toFloat()
	var g = green(colorFrom).toFloat()
	var b = blue(colorFrom).toFloat()
	val deltaR = (red(colorTo) - r) / i.toFloat()
	val deltaG = (green(colorTo) - g) / i.toFloat()
	val deltaB = (blue(colorTo) - b) / i.toFloat()
	while (i > 0) {
		p[index] = color

		r += deltaR
		g += deltaG
		b += deltaB

		color = rgb(
			(r + 0.5).toInt(),
			(g + 0.5).toInt(),
			(b + 0.5).toInt()
		)
		++index
		--i
	}
}

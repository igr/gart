package dev.oblac.gart.color

/**
 * Navigator for ALL color palettes.
 */
class PalettesNavigator {
    data class PaletteInfo(
        val name: String,
        val size: Int,
        val palette: (Int) -> Palette
    )

    // size is the highest valid number for the set, not a count-from-zero. keep these in step with
    // the when-branches in Palettes, or the navigator walks off the end and throws
    private val sets = listOf(
        PaletteInfo(name = "mix", size = 15, Palettes::mixPalette),
        PaletteInfo(name = "cool", size = 181, Palettes::coolPalette),
        PaletteInfo(name = "colormap", size = 133, Palettes::colormapPalette),
    )

    private var set = 0
    private var index = 1

    fun palette(): Palette = sets[set].palette(index)
    fun name() = sets[set].name + index

    fun nextPalette(): Palette {
        index++
        if (index > sets[set].size) {
            index = 1
        }
        return palette()
    }

    fun previousPalette(): Palette {
        index--
        if (index < 1) {
            index = sets[set].size
        }
        return palette()
    }

    fun nextSet() {
        set++
        if (set >= sets.size) {
            set = 0
        }
        index = 1
    }

    fun previousSet() {
        set--
        if (set < 0) {
            set = sets.size - 1
        }
        index = 1
    }
}

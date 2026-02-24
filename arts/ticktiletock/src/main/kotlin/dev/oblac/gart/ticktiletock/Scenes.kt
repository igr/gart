package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Dimension
import dev.oblac.gart.Draw
import dev.oblac.gart.Gartmap
import dev.oblac.gart.Pixel
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.math.rndi
import dev.oblac.gart.pixels.floodFill
import dev.oblac.gart.pixels.matchSimilarColor
import org.jetbrains.skia.Canvas

private fun clear(canvas: Canvas) {
    canvas.clear(CssColors.white)
}

object Scenes {
    private val scenes = mutableListOf<Draw>()

    fun add(count: Int, scene: () -> Draw): Scenes {
        repeat(count) { scenes.add(scene()) }
        return this
    }

    fun tick() {
        if (scenes.isNotEmpty()) {
            scenes.removeFirst()
        }
    }

    fun draw(canvas: Canvas, d: Dimension) {
        if (scenes.isNotEmpty()) {
            scenes[0](canvas, d)
        }
    }

    fun isEnd(): Boolean {
        return scenes.isEmpty()
    }
}

//class SceneAWithFill(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
//    private val r: Array<Pixel> = Array(10){ Pixel(nextInt(d.w), nextInt(d.h)) }
//    override fun invoke(canvas: Canvas, d: Dimension) {
//        super.invoke(canvas, d)
//        m.updatePixelsFromCanvas()
//        r.forEach { floodFill(m, it, 0xFF000000.toInt()) }
//        m.drawToCanvas()
//    }
//}

class SceneAWithFill2(private val d: Dimension, split: Int, private val m: Gartmap) : SceneX(d, split, paintTile2) {
    private val r: Array<Pair<Pixel, Int>> = Array(32) { Pixel(rndi(d.w), rndi(d.h)) to Palettes.cool9.random() }
    override fun invoke(canvas: Canvas, d: Dimension) {
        super.invoke(canvas, d)
        m.updatePixelsFromCanvas()
        r.forEach {
            floodFill(
                m, it.first, fillColor = it.second, matchSimilarColor(CssColors.white, 160)
            )
        }
        m.drawToCanvas()
    }
}

open class SceneX(d: Dimension, split: Int, tilePainter: (Tile) -> Draw) : Draw {
    private val drawable: Draw

    init {
        val matrix = splitBox(d, split)
        drawable = calcMatrix(matrix) { tilePainter(it) }
    }

    override fun invoke(canvas: Canvas, d: Dimension) {
        clear(canvas)
        drawable(canvas, d)
    }
}

private fun calcMatrix(matrix: Array<Array<Tile>>, tilePainter: (Tile) -> Draw): Draw {
    val tiles = matrix
        .flatten()
        .map { tilePainter(it) }
    return Draw { canvas, d -> tiles.forEach { it.invoke(canvas, d) } }
}

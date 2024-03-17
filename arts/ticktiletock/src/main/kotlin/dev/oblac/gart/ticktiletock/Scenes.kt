package dev.oblac.gart.ticktiletock

import dev.oblac.gart.Dimension
import dev.oblac.gart.Draw
import dev.oblac.gart.Gartmap
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.skia.Canvas
import kotlin.random.Random.Default.nextInt

private fun clear(canvas: Canvas) {
    canvas.clear(0xFFFFFFFF.toInt())
}

object Scenes {
    private val scenes = mutableListOf<Draw>()

    fun add(count: Int, scene: () -> Draw): Scenes {
        repeat(count){ scenes.add(scene()) }
        return this
    }

    fun tick() {
        if (scenes.isNotEmpty()) {scenes.removeFirst()}
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

class SceneAWithFill(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pixel> = Array(10){ Pixel(nextInt(d.w), nextInt(d.h)) }
    override fun invoke(canvas: Canvas, d: Dimension) {
        super.invoke(canvas, d)
        m.updatePixelsFromCanvas()
        r.forEach { floodFill(m, it, 0xFF000000.toInt()) }
        m.drawToCanvas()
    }
}

class SceneAWithFill2(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pair<Pixel, Int>> = Array(24){ Pixel(nextInt(d.w), nextInt(d.h)) to Palettes.cool1.random() }
    override fun invoke(canvas: Canvas, d: Dimension) {
        super.invoke(canvas, d)
        m.updatePixelsFromCanvas()
        r.forEach { floodFill(m, it.first, it.second, 0xFFFFFFFF.toInt()) }
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

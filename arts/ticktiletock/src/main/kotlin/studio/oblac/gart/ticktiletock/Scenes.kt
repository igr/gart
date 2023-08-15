package studio.oblac.gart.ticktiletock

import studio.oblac.gart.Dimension
import studio.oblac.gart.Gartmap
import studio.oblac.gart.Shape
import studio.oblac.gart.gfx.Palettes
import studio.oblac.gart.skia.Canvas
import kotlin.random.Random.Default.nextInt

private fun clear(canvas: Canvas) {
    canvas.clear(0xFFFFFFFF.toInt())
}

object Scenes {
    private val scenes = mutableListOf<Shape>()

    fun add(count: Int, scene: () -> Shape): Scenes {
        repeat(count){ scenes.add(scene()) }
        return this
    }

    fun tick() {
        if (scenes.isNotEmpty()) {scenes.removeFirst()}
    }

    fun draw(canvas: Canvas) {
        if (scenes.isNotEmpty()) {
            scenes[0].draw(canvas)
        }
    }

    fun isEnd(): Boolean {
        return scenes.isEmpty()
    }
}

class SceneAWithFill(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pixel> = Array(10){ Pixel(nextInt(d.w), nextInt(d.h)) }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        m.update()
        r.forEach { floodFill(m, it, 0xFF000000.toInt()) }
        m.draw()
    }
}

class SceneAWithFill2(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pair<Pixel, Int>> = Array(24){ Pixel(nextInt(d.w), nextInt(d.h)) to Palettes.cool1.random() }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        m.update()
        r.forEach { floodFill(m, it.first, it.second, 0xFFFFFFFF.toInt()) }
        m.draw()
    }
}

open class SceneX(d: Dimension, split: Int, tilePainter: (Tile) -> Shape) : Shape {
    private val shape: Shape
    init {
        val matrix = splitBox(d, split)
        shape = calcMatrix(matrix) { tilePainter(it) }
    }

    override fun draw(canvas: Canvas) {
        clear(canvas)
        shape.draw(canvas)
    }
}

private fun calcMatrix(matrix: Array<Array<Tile>>, tilePainter: (Tile) -> Shape): Shape {
    val tiles = matrix
        .flatten()
        .map { tilePainter(it) }
    return object : Shape {
        override fun draw(canvas: Canvas) {
            tiles.forEach { it.draw(canvas) }
        }
    }
}

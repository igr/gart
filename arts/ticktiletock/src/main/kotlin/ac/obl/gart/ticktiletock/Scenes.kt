package ac.obl.gart.ticktiletock

import ac.obl.gart.Box
import ac.obl.gart.Gartmap
import ac.obl.gart.Shape
import ac.obl.gart.gfx.Palettes
import ac.obl.gart.skia.Canvas
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

class SceneAWithFill(private val box: Box, split: Int, private val m: Gartmap): SceneX(box, split, paintTile2) {
    private val r: Array<Pixel> = Array(10){ Pixel(nextInt(box.w), nextInt(box.h)) }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        m.update()
        r.forEach { floodFill(m, it, 0xFF000000.toInt()) }
        m.draw()
    }
}

class SceneAWithFill2(private val box: Box, split: Int, private val m: Gartmap): SceneX(box, split, paintTile2) {
    private val r: Array<Pair<Pixel, Int>> = Array(24){ Pixel(nextInt(box.w), nextInt(box.h)) to Palettes.cool1.random() }
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        m.update()
        r.forEach { floodFill(m, it.first, it.second, 0xFFFFFFFF.toInt()) }
        m.draw()
    }
}

open class SceneX(box: Box, split: Int, tilePainter: (Tile) -> Shape) : Shape {
    private val shape: Shape
    init {
        val matrix = splitBox(box, split)
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

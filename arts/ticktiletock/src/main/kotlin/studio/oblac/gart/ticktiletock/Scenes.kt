package studio.oblac.gart.ticktiletock

import studio.oblac.gart.Dimension
import studio.oblac.gart.Drawable
import studio.oblac.gart.Gartmap
import studio.oblac.gart.gfx.Palettes
import studio.oblac.gart.skia.Canvas
import kotlin.random.Random.Default.nextInt

private fun clear(canvas: Canvas) {
    canvas.clear(0xFFFFFFFF.toInt())
}

object Scenes {
    private val scenes = mutableListOf<Drawable>()

    fun add(count: Int, scene: () -> Drawable): Scenes {
        repeat(count){ scenes.add(scene()) }
        return this
    }

    fun tick() {
        if (scenes.isNotEmpty()) {scenes.removeFirst()}
    }

    fun draw(canvas: Canvas) {
        if (scenes.isNotEmpty()) {
            scenes[0](canvas)
        }
    }

    fun isEnd(): Boolean {
        return scenes.isEmpty()
    }
}

class SceneAWithFill(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pixel> = Array(10){ Pixel(nextInt(d.w), nextInt(d.h)) }
    override fun invoke(canvas: Canvas) {
        super.invoke(canvas)
        m.update()
        r.forEach { floodFill(m, it, 0xFF000000.toInt()) }
        m.draw()
    }
}

class SceneAWithFill2(private val d: Dimension, split: Int, private val m: Gartmap): SceneX(d, split, paintTile2) {
    private val r: Array<Pair<Pixel, Int>> = Array(24){ Pixel(nextInt(d.w), nextInt(d.h)) to Palettes.cool1.random() }
    override fun invoke(canvas: Canvas) {
        super.invoke(canvas)
        m.update()
        r.forEach { floodFill(m, it.first, it.second, 0xFFFFFFFF.toInt()) }
        m.draw()
    }
}

open class SceneX(d: Dimension, split: Int, tilePainter: (Tile) -> Drawable) : Drawable {
    private val drawable: Drawable
    init {
        val matrix = splitBox(d, split)
        drawable = calcMatrix(matrix) { tilePainter(it) }
    }

    override fun invoke(canvas: Canvas) {
        clear(canvas)
        drawable(canvas)
    }
}

private fun calcMatrix(matrix: Array<Array<Tile>>, tilePainter: (Tile) -> Drawable): Drawable {
    val tiles = matrix
        .flatten()
        .map { tilePainter(it) }
    return Drawable { canvas -> tiles.forEach { it.invoke(canvas) } }
}

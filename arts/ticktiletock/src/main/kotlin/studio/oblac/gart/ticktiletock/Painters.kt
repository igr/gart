package studio.oblac.gart.ticktiletock

import studio.oblac.gart.Drawable
import studio.oblac.gart.gfx.Palettes
import studio.oblac.gart.gfx.fillOfBlack
import studio.oblac.gart.gfx.strokeOf
import studio.oblac.gart.gfx.strokeOfBlack
import studio.oblac.gart.skia.*
import kotlin.random.Random

val paintTile2: (Tile) -> Drawable = { tile ->
    val line = when (Random.nextBoolean()) {
        false -> Path().moveTo(tile.x, tile.y).lineTo(tile.x + tile.d, tile.y + tile.d)
        true -> Path().moveTo(tile.x, tile.y + tile.d).lineTo(tile.x + tile.d, tile.y)
    }

    val stroke = strokeOfBlack(4f).apply {
        strokeCap = PaintStrokeCap.SQUARE
    }
    Drawable { canvas -> canvas.drawPath(line, stroke) }
}
val paintTile4: (Tile) -> Drawable = { tile ->
    val line = when (Random.nextInt(4)) {
        0 -> Path().moveTo(tile.x, tile.y).lineTo(tile.x + tile.d, tile.y + tile.d)
        1 -> Path().moveTo(tile.x, tile.y + tile.d).lineTo(tile.x + tile.d, tile.y)
        2 -> Path().moveTo(tile.x, tile.y + tile.d / 2).lineTo(tile.x + tile.d, tile.y + tile.d / 2)
        else -> Path().moveTo(tile.x + tile.d / 2, tile.y).lineTo(tile.x + tile.d / 2, tile.y + tile.d)
    }

    val stroke = strokeOf(Palettes.cool1.random(), 4f).apply {
        strokeCap = PaintStrokeCap.SQUARE
    }

    Drawable { canvas -> canvas.drawPath(line, stroke) }
}
val paintCircle: (Tile) -> Drawable = { tile ->
    val rnd = Random.nextInt(4)
    val circle = Path().addCircle(tile.x + tile.d / 2, tile.y + tile.d / 2, rnd * tile.d / 8)
    val stroke = strokeOf(Palettes.cool1.random(), 4f)

    Drawable { canvas -> canvas.drawPath(circle, stroke) }
}
val paintCircleBW: (Tile) -> Drawable = { tile ->
    val rnd = Random.nextInt(4)
    val circle = Path().addCircle(tile.x + tile.d/2, tile.y + tile.d/2, rnd * tile.d/8)
    val stroke = fillOfBlack()

    Drawable { canvas -> canvas.drawPath(circle, stroke) }
}

val paintSquares: (Tile) -> Drawable = { paintSquares(it, 0) }
val paintSquaresFill1: (Tile) -> Drawable = { paintSquares(it, 1) }
val paintSquaresFill2: (Tile) -> Drawable = { paintSquares(it, 2) }

private fun paintSquares(tile: Tile, type: Int): Drawable {
    val finalSize = 3
    val directions = arrayOf(-1, 0, 1)

    val totalSteps = 3 + Random.nextInt(3)
    val xDirection = directions.random()
    val yDirection = directions.random()

    val p = when(type) {
        2 -> {
            Palettes.cool1.map {
                Paint().apply{
                    color = it
                    strokeWidth = 2f
                }
            }
        }
        1 -> {
            Palettes.cool4.map {
                Paint().apply{
                    color = it
                    strokeWidth = 2f
                }
            }
        }
        else -> {
            Palettes.gradient(0xFF000000, 0xFF000000, totalSteps + 1).map {
                Paint().apply {
                    color = it
                    strokeWidth = 2f
                }.setStroke(true)
            }
        }
    }

    return object : Drawable {

        fun draw(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, xMovement: Int, yMovement: Int, step: Int) {
            canvas.drawRect(Rect(x, y, x + width, y + height), p[step + 1])
            if (step < 0) return

            val size = tile.d * (step.toFloat() / totalSteps) + finalSize

            var newX = x + (width - size) / 2
            var newY = y + (height - size) / 2

            newX -= ((x - newX) / (step + 2)) * xMovement
            newY -= ((y - newY) / (step + 2)) * yMovement

            draw(canvas, newX, newY, size, size, xMovement, yMovement, step - 1)
        }

        override fun invoke(canvas: Canvas) {
            draw(canvas, tile.x, tile.y, tile.d, tile.d, xDirection, yDirection, totalSteps - 1)
        }
    }
}

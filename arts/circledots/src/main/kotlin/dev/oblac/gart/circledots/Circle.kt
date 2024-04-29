package dev.oblac.gart.circledots

import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.math.subDeg
import org.jetbrains.skia.Canvas

data class Circle(private val ctx: Context, val x: Float, val y: Float, val r: Float, var deg: Float, var speed: Float = 3f) {
	private var rOffset = 4

    fun draw(canvas: Canvas, drawCircle: Boolean) {
		val rr = r + ctx.msin[deg] * rOffset

		if (drawCircle) {
            canvas.drawCircle(x, y, rr, blackStroke)
		}

		val x2 = x + ctx.mcos[deg] * rr
		val y2 = y - ctx.msin[deg] * rr

        canvas.drawCircle(x2, y2, 4f, blackFill)
		deg = deg.subDeg(speed)
	}

	companion object {
		private val blackStroke = strokeOfBlack(1f)
		private val blackFill = fillOfBlack()
	}
}

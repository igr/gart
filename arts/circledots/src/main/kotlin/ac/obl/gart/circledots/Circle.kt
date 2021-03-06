package ac.obl.gart.circledots

import ac.obl.gart.gfx.fillOfBlack
import ac.obl.gart.gfx.strokeOfBlack
import ac.obl.gart.math.subDeg

data class Circle(private val ctx: Context, val x: Float, val y: Float, val r: Float, var deg: Float, var speed: Float = 3f) {
	private var rOffset = 4

	fun draw(drawCircle: Boolean) {
		val rr = r + ctx.msin[deg] * rOffset

		if (drawCircle) {
			ctx.g.canvas.drawCircle(x, y, rr, blackStroke)
		}

		val x2 = x + ctx.mcos[deg] * rr
		val y2 = y - ctx.msin[deg] * rr

		ctx.g.canvas.drawCircle(x2, y2, 4f, blackFill)
		deg = deg.subDeg(speed)
	}

	companion object {
		private val blackStroke = strokeOfBlack(1f)
		private val blackFill = fillOfBlack()
	}
}
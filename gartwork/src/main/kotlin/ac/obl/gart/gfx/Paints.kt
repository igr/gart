package ac.obl.gart.gfx

import io.github.humbleui.skija.Paint
import io.github.humbleui.skija.PaintMode

fun strokeOf(color: Long, width: Float) = Paint()
	.setAntiAlias(true)
	.setColor(color.toInt())
	.setMode(PaintMode.STROKE)
	.setStrokeWidth(width)

fun strokeOf(color: Int, width: Float) = Paint()
	.setAntiAlias(true)
	.setColor(color)
	.setMode(PaintMode.STROKE)
	.setStrokeWidth(width)

fun fillOf(color: Int) = Paint()
	.setAntiAlias(true)
	.setColor(color)

fun fillOf(color: Long) = Paint()
	.setAntiAlias(true)
	.setColor(color.toInt())


fun strokeOfBlack(width: Number) = strokeOf(0xFF000000, width.toFloat())
fun strokeOfWhite(width: Number) = strokeOf(0xFFFFFFFF, width.toFloat())
fun strokeOfRed(width: Number) = strokeOf(0xFFFF0000, width.toFloat())
fun strokeOfGreen(width: Number) = strokeOf(0xFF00FF00, width.toFloat())
fun strokeOfBlue(width: Number) = strokeOf(0xFF0000FF, width.toFloat())

fun fillOfBlack() = fillOf(0xFF000000)
fun fillOfWhite() = fillOf(0xFFFFFFFF)
fun fillOfYellow() = fillOf(0xFFFFFF00)
fun fillOfRed() = fillOf(0xFFFF0000)

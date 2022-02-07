package ac.obl.gart.gfx

import ac.obl.gart.skia.Paint
import ac.obl.gart.skia.PaintMode

fun strokeOf(color: Long, width: Float) = Paint().apply {
    this.isAntiAlias = true
    this.color = color.toInt()
    this.mode = PaintMode.STROKE
    this.strokeWidth = width
}

fun strokeOf(color: Int, width: Float) = Paint().apply {
    this.isAntiAlias = true
    this.color = color
    this.mode = PaintMode.STROKE
    this.strokeWidth = width
}

fun fillOf(color: Int) = Paint().apply {
    this.isAntiAlias = true
    this.color = color
}

fun fillOf(color: Long) = Paint().apply {
    this.isAntiAlias = true
    this.color = color.toInt()
}

fun strokeOfBlack(width: Number) = strokeOf(0xFF000000, width.toFloat())
fun strokeOfWhite(width: Number) = strokeOf(0xFFFFFFFF, width.toFloat())
fun strokeOfRed(width: Number) = strokeOf(0xFFFF0000, width.toFloat())
fun strokeOfGreen(width: Number) = strokeOf(0xFF00FF00, width.toFloat())
fun strokeOfBlue(width: Number) = strokeOf(0xFF0000FF, width.toFloat())

fun fillOfBlack() = fillOf(0xFF000000)
fun fillOfWhite() = fillOf(0xFFFFFFFF)
fun fillOfYellow() = fillOf(0xFFFFFF00)
fun fillOfRed() = fillOf(0xFFFF0000)

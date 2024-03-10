package dev.oblac.gart.gfx

import dev.oblac.gart.skia.Color
import dev.oblac.gart.skia.Color4f
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.PaintMode

fun strokeOf(color: Color4f, width: Float) = Paint().apply {
    this.isAntiAlias = true
    this.color4f = color
    this.mode = PaintMode.STROKE
    this.strokeWidth = width
}

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

fun fillOf(color: Color4f) = Paint().apply {
    this.isAntiAlias = true
    this.color4f = color
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
fun strokeOfRed(width: Number) = strokeOf(Color.RED, width.toFloat())
fun strokeOfGreen(width: Number) = strokeOf(0xFF00FF00, width.toFloat())
fun strokeOfBlue(width: Number) = strokeOf(Color.BLUE, width.toFloat())

fun fillOfBlack() = fillOf(0xFF000000)
fun fillOfWhite() = fillOf(0xFFFFFFFF)
fun fillOfYellow() = fillOf(Color.YELLOW)
fun fillOfRed() = fillOf(Color.RED)
fun fillOfBlue() = fillOf(Color.BLUE)
fun fillOfGreen() = fillOf(Color.GREEN)

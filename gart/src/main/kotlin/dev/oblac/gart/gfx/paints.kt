package dev.oblac.gart.gfx

import dev.oblac.gart.math.multiply
import org.jetbrains.skia.*

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
fun strokeOf(width: Float, color: Long) = strokeOf(color, width)

fun strokeOf(color: Int, width: Float) = Paint().apply {
    this.isAntiAlias = true
    this.color = color
    this.mode = PaintMode.STROKE
    this.strokeWidth = width
}
fun strokeOf(width: Float, color: Int) = strokeOf(color, width)

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

fun strokeOfBlack(width: Number) = strokeOf(Color.BLACK, width.toFloat())
fun strokeOfWhite(width: Number) = strokeOf(Color.WHITE, width.toFloat())
fun strokeOfRed(width: Number) = strokeOf(Color.RED, width.toFloat())
fun strokeOfGreen(width: Number) = strokeOf(Color.GREEN, width.toFloat())
fun strokeOfBlue(width: Number) = strokeOf(Color.BLUE, width.toFloat())
fun strokeOfYellow(width: Number) = strokeOf(Color.YELLOW, width.toFloat())
fun strokeOfMagenta(width: Number) = strokeOf(Color.MAGENTA, width.toFloat())

fun fillOfBlack() = fillOf(Color.BLACK)
fun fillOfWhite() = fillOf(0xFFFFFFFF)
fun fillOfYellow() = fillOf(Color.YELLOW)
fun fillOfRed() = fillOf(Color.RED)
fun fillOfBlue() = fillOf(Color.BLUE)
fun fillOfGreen() = fillOf(Color.GREEN)


/**
 * Returns a Paint object that represents a hatch (dotted) pattern.
 */
fun hatchPaint(color: Int, density: Float = 5f, dotWidth: Float = 1f, strokeWidth: Float = 3f) = Paint().apply {
    val hatch = Path().addCircle(0f, 0f, dotWidth)
    this.pathEffect = PathEffect.makePath2D(Matrix33.makeScale(density, density), hatch)
    this.color = color
    this.strokeWidth = strokeWidth
}

fun dashPaint(color: Int, density: Float = 6f, angle: Float = -45f, strokeWidth: Float = 2f) = Paint().apply {
    val diagLinesPath = PathEffect.makeLine2D(strokeWidth, Matrix33.multiply(Matrix33.makeScale(density, density), Matrix33.makeRotate(angle)))
    val paint = Paint().apply {
        this.color = color
        this.strokeWidth = strokeWidth
        this.pathEffect = diagLinesPath
    }
    return paint
}

/**
 * Creates a Paint object with anti-aliasing enabled.
 * This is a common utility function to create a Paint object with default settings.
 */
fun paint() = Paint().apply {
    this.isAntiAlias = true
}

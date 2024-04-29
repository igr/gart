package dev.oblac.gart.skyscraper

import dev.oblac.gart.Dimension
import dev.oblac.gart.gfx.RectIsometricLeft
import dev.oblac.gart.gfx.RectIsometricRight
import dev.oblac.gart.gfx.RectIsometricTop
import dev.oblac.gart.gfx.fillOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path
import kotlin.random.Random

data class Colors(
	val topColor: Int,
	val leftColor: Int,
	val rightColor: Int,
	val leftWindowsColor: Int,
	val rightWindowsColor: Int)

class Building(x: Float, y: Float, a: Float, b: Float, alpha: Float, private val colors: Colors, private val d: Dimension) {

    val roofRect: RectIsometricTop
	val rightRect: RectIsometricRight
	private val roof: Path
	private val leftSide: Path
	private val rightSide: Path

	val leftWindows: WindowsConsumer
	val rightWindows: WindowsConsumer

	init {
		roofRect = RectIsometricTop(x, y, a, b, alpha)
        rightRect = RectIsometricRight(roofRect.bottom.x, roofRect.bottom.y, d.h - y + a + 40, b, alpha)

		roof = roofRect.path()
        leftSide = RectIsometricLeft(x, y, d.h - y, a, alpha).path()
		rightSide = rightRect.path()

		val useNarrowWindows = Random.nextInt(10) < 3

		leftWindows = if (useNarrowWindows) {
			WindowsOne(roofRect.left, roofRect.bottom, windowSize, 10f, alpha)
		} else {
			WindowsMany(roofRect.left, roofRect.bottom, windowSize, 10f, 10f, alpha)
		}
		rightWindows = if (useNarrowWindows) {
			WindowsOne(rightRect.left, rightRect.top, windowSize, 10f, -alpha)
		} else {
			WindowsMany(rightRect.left, rightRect.top, windowSize, 10f, 10f, -alpha)
		}
	}

	operator fun invoke(c: Canvas) {
		c.drawPath(roof, fillOf(colors.topColor))
		c.drawPath(leftSide, fillOf(colors.leftColor))
		c.drawPath(rightSide, fillOf(colors.rightColor))

		leftWindows {
            c.drawPath(it.path(), fillOf(colors.leftWindowsColor))
		}
		rightWindows {
            c.drawPath(it.path(), fillOf(colors.rightWindowsColor))
		}

//		g.canvas.drawPoint(roofRect.left.x, roofRect.left.y, strokeOfGreen(4f))
//		g.canvas.drawPoint(roofRect.bottom.x, roofRect.bottom.y, strokeOfGreen(4f))
	}
}

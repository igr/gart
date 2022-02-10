package ac.obl.gart.circledots

import ac.obl.gart.Gartvas
import ac.obl.gart.ImageWriter
import ac.obl.gart.VideoGartvas
import ac.obl.gart.Window
import ac.obl.gart.gfx.fillOfWhite
import ac.obl.gart.skia.Rect

const val w: Int = 640
const val h: Int = w

const val rowCount = 25
const val gap = w / (rowCount - 2)

const val frames = 50

val g = Gartvas(w, h)
val ctx = Context(g)
val window = Window(g, frames).show()

val circles = Array(rowCount * rowCount) {
	val row = it.div(rowCount)
	val column = it.mod(rowCount)
	Circle(
		ctx = ctx,
		x = (column * gap).toFloat() - gap/2,
		y = (row * gap).toFloat() - gap/2,
		r = gap * 0.8f,
		deg = ((column + row) * 10).toFloat(),
		speed = 8f
	)
}

var drawCircle = true
private fun paint(change: Boolean) {
	if (change) drawCircle = !drawCircle
	g.canvas.drawRect(Rect(0f, 0f, w.toFloat(), h.toFloat()), fillOfWhite())
	for (circle in circles) {
		circle.draw(drawCircle)
	}
}

fun main() {
	println("CircleDots")

	val v = VideoGartvas(g).start("circledots.mp4", frames)

	var tick = 0
    val everySecondMarker = window.frames.marker().onEverySecond(1)

	window.paint {
		tick = if (everySecondMarker.now()) tick + 1 else tick
		paint(tick.mod(2) == 0)

		if (v.frames.count() < frames * 8) {
			v.addFrame()
		} else {
			if (v.running) {
				v.save()
				println("Video saved.")
			}
		}

	}

	ImageWriter(g).save("circledots.png")
	println("Done")
}

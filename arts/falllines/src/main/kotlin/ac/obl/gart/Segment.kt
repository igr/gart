package ac.obl.gart

import ac.obl.gart.gfx.argb
import ac.obl.gart.gfx.strokeOf
import ac.obl.gart.math.GaussianFunction
import kotlin.math.abs

data class Segment(val w: Int, val color: Int, val drawOrder: Int) {
    private var nextX: Float = 0f
    private var gf: GaussianFunction? = null
    private var x: Float = 0f
    private var stroke = strokeOf(color, 2f)
    private val strokeShadow = strokeOf(argb(0x80, 0, 0, 0), 1f)

    fun initialOffset(x: Float) {
        this.x = x
        this.nextX = x
    }

    fun setNextX(nextX: Float) {
        this.nextX = nextX

        val center = (x + nextX) / 2f
        val delta = abs(nextX - x)

        this.gf = GaussianFunction(1, center, delta / 4.2f)

        this.inMotion = true

        when {
            x < nextX -> {
                incrementer = { it ->
                    x += it
                    if (x > nextX) {
                        inMotion = false
                    }
                }
            }
            x > nextX -> {
                incrementer = { it ->
                    x -= it
                    if (x < nextX) {
                        inMotion = false
                    }
                }
            }
            else -> incrementer = {
                inMotion = false
            }
        }
    }

    private var incrementer: (Float) -> Unit = {
        inMotion = false
    }

    fun draw() {
        g.canvas.drawLine(
            x + w, g.box.bf - 1,
            x + w + 4, g.box.bf - 1,
            strokeShadow)
        g.canvas.drawLine(
            x, g.box.bf - 1,
            x + w, g.box.bf - 1,
	        stroke)
    }

    var inMotion = false

    fun tickNextX() {
        if (!inMotion) {
            return
        }

        val delta = gf?.let { it(x) } ?: 0f

        incrementer(delta)
    }
}

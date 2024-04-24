package dev.oblac.gart.circledots.v2

import dev.oblac.gart.circledots.Context
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.skia.Canvas

data class Circle2Anim(private val ctx: Context, val x: Float, val y: Float, val r: Float, var deg: Float, var speed: Float = 3f, val from: Int) {
    var tick = 0
    private val fill = fillOf(palette.safe(from))
    fun draw(canvas: Canvas) {
        tick++
        //ctx.g.canvas.drawCircle(x, y, r, blackStroke)

        val x2 = x + ctx.mcos[deg] * r
        val y2 = y - ctx.msin[deg] * r

        canvas.drawCircle(x2, y2, 20f, fill)

        if (tick > from) {
            deg += speed
        }
    }

    companion object {
        private val blackFill = fillOfBlack()
        private val palette = Palettes.gradient(Colors.black, Colors.white, 110) +
            Palettes.gradient(Colors.white, Colors.black, 110)

    }
}

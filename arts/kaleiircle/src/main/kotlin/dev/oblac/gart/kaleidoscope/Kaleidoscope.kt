package dev.oblac.gart.kaleidoscope

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Sprite
import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.sinf
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.drawSprite
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Path

fun main() {
    val gart = Gart.Companion.of("kaleidoscope", 1024, 1024)
    println(gart)

    val d = gart.d
    val w = gart.window()

    // window 1 - source
    val g = gart.gartvas()
    val c = g.canvas

    drawTemplateImage(c, d)

    // template image
    w.showImage(g)

    val tSize = 300f

    val sprite = g.sprite().cropTriangle(d.center, tSize).let {
        val hypotenuse = tSize / 2 * 1.5f
        val side = hypotenuse * 2 / 1.73f
        val gap = (tSize - side) / 2
        it.cropRect(gap, 0f, tSize - gap * 2, tSize * 1.5f / 2)
    }

    c.clear(Colors.white)

    val max = ((d.w / tSize * 2) + 1).toInt()
    for (j in -max..max) {
        for (i in -max..max) {
            val x = d.cx + i * sprite.d.w * 1.5f - 3f * i
            val y = d.cy + j * sprite.d.h * 2f - 3f * j - i * sprite.d.h
            drawRosetta(c, sprite, x, y)
        }
    }

    gart.saveImage(g)
    w.showImage(g)

}

private fun drawTemplateImage(c: Canvas, d: Dimension) {
    val pal = Palettes.cool1

    c.clear(pal[1])
    c.drawCircle(d.center, 100f, fillOf(NipponColors.col249_KURO))

    // draw sins
    var y = 0f
    while (y <= d.hf) {
        y += 42f
        val step = 1f
        val amplitude = 20f
        val frequency = 0.05f
        val offset = 100f
        val path = Path()
        path.moveTo(0f, y)
        var x = 0f
        while (x <= d.wf) {
            val yy = y + amplitude * sinf(Radians.of(frequency * x + offset))
            path.lineTo(x, yy)
            x += step
        }
        c.drawPath(path, strokeOf(pal.safe(y * 2), 8f))
    }

}

private fun drawRosetta(c: Canvas, sprite: Sprite, cx: Float, cy: Float) {
    // the center of rosetta is the center of the top-left triangle
    val x = cx - sprite.d.cx
    val y = cy - sprite.d.cy

    // top-left
    c.drawSprite(sprite) { it.at(x, y) }
    // top
    c.drawSprite(sprite) { it.rotateRB(60f).flipHorizontal().right(sprite.d.w).at(x - 1f, y) }
    // top-right
    c.drawSprite(sprite) { it.rotateRB(120f).at(x - 2f, y) }

    // down-left
    c.drawSprite(sprite) { it.flipVertical().down(sprite.d.h).at(x, y - 1f) }
    // down
    c.drawSprite(sprite) { it.rotateRB(60).flipVertical().flipHorizontal().down(sprite.d.h).right(sprite.d.w).at(x - 1f, y - 1f) }
    // down-right
    c.drawSprite(sprite) { it.rotateRB(120).flipVertical().down(sprite.d.h).at(x - 2f, y - 1f) }
}

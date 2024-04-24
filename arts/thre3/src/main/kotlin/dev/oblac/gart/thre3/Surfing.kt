package dev.oblac.gart.thre3

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.color.BgColors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.force.ForceField
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.*
import dev.oblac.gart.skia.Image
import dev.oblac.gart.skia.Point
import dev.oblac.gart.skia.Rect
import dev.oblac.gart.toFrames
import kotlin.time.Duration.Companion.seconds

fun main() {
    val gart = Gart.of(
        "temp",
        1024, 1024
    )
    val d = gart.d
    val g = gart.gartvas()

    val forceField = force1(d)
    val forceField2 = force2(d)

    val startDrawing = 0.seconds.toFrames(gart.fps)
    val stopDrawing1 = 20.seconds.toFrames(gart.fps)
    val theEnd = 42.seconds.toFrames(gart.fps)

    val palette = Palettes.cool2

//    var randomPoints = Array(10000) {
//        Point(rnd.nextFloat(d.wf), rnd.nextFloat(d.hf))
//    }.toList()

    val n = 10000
    var line1 = Array(n) {
        Point(d.wf * it / n, 122f)
    }.toList()
    var line2 = Array(n) {
        Point(d.wf * it / n, 344f)
    }.toList()
    var line3 = Array(n) {
        Point(d.wf * it / n, 566f)
    }.toList()
    var line4 = Array(n) {
        Point(d.wf * it / n, 788f)
    }.toList()
    var line5 = Array(n) {
        Point(d.wf * it / n, 988f)
    }.toList()

    var line11 = Array(n) {
        Point(d.wf * it / n, 640f)
    }.toList()
    var line12 = Array(n) {
        Point(d.wf * it / n, 560f)
    }.toList()
    var line13 = Array(n) {
        Point(d.wf * it / n, 700f)
    }.toList()

    g.canvas.clear(BgColors.sand)
    //g.canvas.clear(palette[3])

    val w = gart.window()
    var image = g.snapshot()

    var image1 = image
    var image2 = image

    w.show { c, d, f ->
        f.onBeforeFrame(startDrawing) {
            g.canvas.clear(BgColors.sand)
        }
        f.onBeforeFrame(stopDrawing1) {
            line1 = line1
                .filter { it.isInside(d) }
                .map { forceField[it.x.toInt(), it.y.toInt()].offset(it) }
            line2 = line2
                .filter { it.isInside(d) }
                .map { forceField[it.x.toInt(), it.y.toInt()].offset(it) }
            line3 = line3
                .filter { it.isInside(d) }
                .map { forceField[it.x.toInt(), it.y.toInt()].offset(it) }
            line4 = line4
                .filter { it.isInside(d) }
                .map { forceField[it.x.toInt(), it.y.toInt()].offset(it) }
            line5 = line5.asSequence()
                .filter { it.isInside(d) }
                .map { forceField[it.x.toInt(), it.y.toInt()].offset(it) }
                .toList()

            // draw line
            g.canvas.drawPointsAsCircles(line1, strokeOf(palette[0].alpha(0x23), 1f))
            g.canvas.drawPointsAsCircles(line2, strokeOf(palette[1].alpha(0x23), 1f))
            g.canvas.drawPointsAsCircles(line3, strokeOf(palette[1].alpha(0x23), 1f))
            g.canvas.drawPointsAsCircles(line4, strokeOf(palette[3].alpha(0x23), 1f))
            g.canvas.drawPointsAsCircles(line5, strokeOf(palette[4].alpha(0x23), 8f))
            image = g.snapshot()
        }
        f.onFrame(stopDrawing1) {
            println("Done - image #1")
//            g.canvas.drawRect(Rect(370f, 160f, 370f + 400, 160f + 400f), strokeOfBlack(2))
            image1 = g.snapshot()

            // prepare for the next drawing
            g.canvas.clear(palette[3])
        }

        f.onAfterFrame(stopDrawing1) {
            line11 = line11
                .filter { it.isInside(d) }
                .map { forceField2[it.x.toInt(), it.y.toInt()].offset(it) }
            line12 = line12
                .filter { it.isInside(d) }
                .map { forceField2[it.x.toInt(), it.y.toInt()].offset(it) }
            line13 = line13
                .filter { it.isInside(d) }
                .map { forceField2[it.x.toInt(), it.y.toInt()].offset(it) }
            g.canvas.drawPointsAsCircles(line11, strokeOf(palette[1].alpha(0x23), 1f))
            g.canvas.drawPointsAsCircles(line12, strokeOf(palette[2].alpha(0x23), 1f))

            image = g.snapshot()
        }
        f.onFrame(theEnd) {
//            g.canvas.drawRect(Rect(420f, 310f, 420f + 400, 310f + 400f), strokeOfBlack(2))
            image2 = g.snapshot()
            drawFinalImage(image1, image2)
        }

        c.drawImage(image, 0f, 0f)
    }
    //.fastForwardTo(stopDrawing1)

}

fun drawFinalImage(image1: Image, image2: Image) {
    val gart = Gart.of("surfing", 600, 1100)
    println(gart)

    val g = gart.gartvas()

    g.canvas.clear(BgColors.sand)

    val rect1 = Rect(100f, 100f, 100f + 400, 100f + 400f)
    val rect2 = Rect(100f, 600f, 100f + 400, 600f + 400f)
    g.canvas.drawImageRect(image1, Rect(370f, 160f, 370f + 400, 160f + 400f), rect1)
    g.canvas.drawImageRect(image2, Rect(420f, 310f, 420f + 400, 310f + 400f), rect2)

    g.canvas.drawRect(rect1.grow(-10f), strokeOfWhite(10f))
    g.canvas.drawRect(rect2.grow(-10f), strokeOfWhite(10f))
    gart.saveImage(g)
}

//fun grayscaleToColor(gartvas: Gartvas, palette: Palette) {
//    val pixels = gart.gartmap(gartvas)
//    pixels.forEach{ x, y, color ->
//        val c = pixels[x, y]
//        val gray = (red(c) + green(c) + blue(c)) / 3
//        val color = palette[gray]
//        pixels[x, y] = color
//    }
//    pixels.drawToCanvas()
//}


private fun force1(d: Dimension): ForceField {
    val poles = arrayOf(Complex(0.5, 0.5))
    val holes = arrayOf(Complex(0.2, -0.4))

    val complexField = ComplexField.of(d) { x, y ->
        val z = x + i * y
        ComplexFunctions.polesAndHoles(poles, holes)(z)
    }
    return ForceField.from(d) { x, y ->
        complexField[x, y].let { c -> Vector(c.real, c.img).normalize() }
    }
}

private fun force2(d: Dimension): ForceField {
    val poles = arrayOf(Complex(0.2, -0.8))
    val holes = arrayOf(Complex(-0.2, 0.1), Complex(0, 0))

    val complexField = ComplexField.of(d) { x, y ->
        val z = x + i * y
        ComplexFunctions.polesAndHoles(poles, holes)(z)
    }
    return ForceField.from(d) { x, y ->
        complexField[x, y].let { c -> Vector(c.real, c.img).normalize() }
    }
}

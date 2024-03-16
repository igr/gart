package dev.oblac.gart.plasma

import dev.oblac.gart.Gart
import dev.oblac.gart.Pixels
import dev.oblac.gart.gfx.Palette
import dev.oblac.gart.gfx.Palettes.gradient
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val w = 320
const val w_2 = w / 2
const val h = 240
const val h_2 = h / 2
val tab1 = IntArray(w * h)
val tab2 = IntArray(w * h)

var circle1 = 0f
var circle2 = 0f
var circle3 = 0f
var circle4 = 0f
var circle5 = 0f
var circle6 = 0f
var circle7 = 0f
var circle8 = 0f
var roll = 0

val p: Palette = gradient(0xff174185, 0xffFFFFFF, 128) + gradient(0xffFFFFFF, 0xff174185, 128)

private fun init() {
    for (i in 0 until h) {
        for (j in 0 until w) {
            tab1[i * w + j] = ((sqrt(16.0 + (h_2 - i) * (h_2 - i) + (w_2 - j) * (w_2 - j)) - 4) * 5).toInt()
        }
    }
    for (i in 0 until h) {
        for (j in 0 until w) {
            val temp = sqrt(16.0 + (h_2 - i) * (h_2 - i) + (w_2 - j) * (w_2 - j)) - 4
            tab2[i * w + j] = ((sin(temp / 9.5) + 1) * 90).toInt()
        }
    }
    circle8 = 0f
    circle7 = circle8
    circle6 = circle7
    circle5 = circle6
    circle4 = circle5
    circle3 = circle4
    circle2 = circle3
    circle1 = circle2
    roll = 0
}


private fun drawNext(dest: Pixels, p: Palette) {
    circle1 += 0.085f / 6
    circle2 -= 0.1f / 6
    circle3 += 0.3f / 6
    circle4 -= 0.2f / 6
    circle5 += 0.4f / 6
    circle6 -= 0.15f / 6
    circle7 += 0.35f / 6
    circle8 -= 0.05f / 6

    val x2: Int = (w / 4 + w / 4 * sin(circle1)).toInt() // 0 - 160
    val y2: Int = (h / 4 + h / 4 * cos(circle2)).toInt() // 0 - 120
    val x1: Int = (w / 4 + w / 4 * cos(circle3)).toInt()
    val y1: Int = (h / 4 + h / 4 * sin(circle4)).toInt()
    val x3: Int = (w / 4 + w / 4 * cos(circle5)).toInt()
    val y3: Int = (200 / 4 + 200 / 4 * sin(circle6)).toInt()
    val x4: Int = (w / 4 + w / 4 * cos(circle7)).toInt()
    val y4: Int = (200 / 4 + 200 / 4 * sin(circle8)).toInt()
    roll += 5
    var k = 0

    var i = 0
    while (i < h_2) {
        var j = 0
        while (j < w_2) {
            var c =
                tab1[w * (i + y1) + j + x1] +
                        tab1[w * (i + y2) + j + x2] +
                        tab2[w * (i + y3) + j + x3] +
                        tab2[w * (i + y4) + j + x4] + roll

            c = p[c and 0xFF]
            dest[k + 2 * j] = c
            dest[k + 2 * j + 1] = c
            dest[k + 2 * j + w] = c
            dest[k + 2 * j + w + 1] = c
            j++
        }
        k += w + w
        i++
    }
}

val gart = Gart.of("Plasma", w, h)

fun main() {
    println(gart.name)

    init()

    val g = gart.gartvas()
    val b = gart.gartmap(g)
    val m = gart.movie()
    val w = gart.window(fps = 30)

    m.record(w).show { c, _, f ->
        f.tick {
            drawNext(b, p)
        }

        b.drawToCanvas()
        c.drawImage(b.image(), 0f, 0f)

        f.onFrame(757) {
            m.stopRecording()
        }
    }
}

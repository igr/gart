package dev.oblac.gart.moire

import dev.oblac.gart.Gart
import dev.oblac.gart.Gartmap
import dev.oblac.gart.gfx.drawBitmap
import dev.oblac.gart.math.HALF_PIf
import dev.oblac.gart.math.Lissajous
import dev.oblac.gart.math.PIf
import dev.oblac.gart.pixels.applyGaussianBlur
import dev.oblac.gart.pixels.drawBlackWhiteMoire

private val gart = Gart.of(
    "moire", 1024, 1024,
    30
)

fun main() {
    val g = gart.gartvas()
    val b = gart.gartmap(g)
    val m = gart.movieGif()

    println(gart)
    val w = gart.window()
//    m.record(w).show { c, _, f ->
    w.show { c, _, f ->
        draw(b)
        c.drawBitmap(b)

        f.onFrame(200) {
            gart.saveImage(c)
        }

//        f.onFrame(628) {
//            m.stopRecording()
//            gart.saveMovie(m)
//        }
    }
}

val l1 = Lissajous(
    gart.d.center,
    200f,
    200f,
    3f,
    4f
)
val l2 = Lissajous(
    gart.d.center,
    200f,
    200f,
    4f,
    3f,
    PIf,
    HALF_PIf
)

fun draw(b: Gartmap) {
    val pp1 = l1.step(0.005f)
    val pp2 = l2.step(0.005f)
    drawBlackWhiteMoire(b, pp1, pp2)
    applyGaussianBlur(b)
}


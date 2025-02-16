package dev.oblac.gart.blob

import dev.oblac.gart.*
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.drawImage
import dev.oblac.gart.math.Lissajous
import dev.oblac.gart.math.fastSqrt
import dev.oblac.gart.math.map
import kotlin.math.pow

val p = Palettes.cool9.expand(255).reversed()

fun main() {
    val gart = Gart.of("blob", 1024, 1024)
    val g = gart.gartvas()
    val b = gart.gartmap(g)
    val w = gart.window()
    val d = gart.d
    val m = gart.movieGif()

    println(gart)

    val l1 = Lissajous(d.center, 100f, 200f, 3f, 4f)
    val l2 = Lissajous(d.center, 300f, 100f, 3f, 4f)
    val l3 = Lissajous(d.center, 500f, 400f, 2f, 6f)

    val blobs = prepareBlobify(d, listOf(l1, l2, l3))

    val two = Gartvas(d)
    val mem = Gartmap(two)
    m.record(w).show { c, _, f ->
//    w.show { c, _, _ ->
        //blobify(b, blobs)
        blobifyWeird(mem, blobs)
        //transparentRightImage(b, mem)
        c.drawImage(mem.image())


//        blobs.blobs.forEach{ l ->
//            c.drawPoint(l.position(), strokeOfRed(20f))
//        }
        f.onFrame(90) {
            m.stopRecording()
        }
    }
}



/// BLOBS

private fun transparentRightImage(target:Pixels, pixels: Pixels) {
    val fromX = pixels.d.cx.toInt()
    for (y in 0 until pixels.d.h) {
        for (x in fromX until pixels.d.w) {
            target[x, y] = pixels[x, y]
        }
    }
}

data class Blobs(
    val blobs: List<Lissajous>,
    val maxDsq: Float
)

private fun prepareBlobify(d: Dimension, blobs: List<Lissajous>): Blobs {
    val maxDsq = fastSqrt(d.wf * d.wf + d.hf * d.hf).toDouble().pow(blobs.size.toDouble())
    return Blobs(blobs, maxDsq.toFloat())
}

var weird = 0.0001f

private fun blobifyWeird(b: Pixels, blobs: Blobs, step: Float = 0.01f) {
    for (b in blobs.blobs) {
        b.step(step)
    }
    val times = blobs.blobs.map { it.t }.toFloatArray()

    b.forEach { x, y, _ ->
        var dSq = 1.0
        for (b in blobs.blobs) {

            b.step(weird)           // weirdness!!!

            val p = b.position()
            val xSq = (x - p.x) * (x - p.x)
            val ySq = (y - p.y) * (y - p.y)
            dSq *= fastSqrt(xSq + ySq)
        }

        // max dsq with the given number of blobs:
        val maxDsq = blobs.maxDsq
        val pix = map(maxDsq - dSq * 50, 0f, maxDsq, 0f, 255f).toInt().coerceIn(0 until 255)

        //b[x, y] = rgb(pix, pix, pix)
        b[x, y] = p[pix]
    }

    // reset times for weirdness
//    blobs.blobs.forEachIndexed { i, b ->
//        b.t = times[i]
//    }
    weird += 0.001f
}

private fun blobify(b: Pixels, blobs: Blobs, step: Float = 0.01f) {
    for (b in blobs.blobs) {
        b.step(step)
    }
    val times = blobs.blobs.map { it.t }.toFloatArray()

    b.forEach { x, y, _ ->
        var dSq = 1.0
        for (b in blobs.blobs) {
            val p = b.position()
            val xSq = (x - p.x) * (x - p.x)
            val ySq = (y - p.y) * (y - p.y)
            dSq *= fastSqrt(xSq + ySq)
        }

        // max dsq with the given number of blobs:
        val maxDsq = blobs.maxDsq
        val pix = map(maxDsq - dSq * 50, 0f, maxDsq, 0f, 255f).toInt().coerceIn(0 until 255)

        //b[x, y] = rgb(pix, pix, pix)
        b[x, y] = p[pix]
    }

    // reset times for weirdness
    blobs.blobs.forEachIndexed { i, b ->
        b.t = times[i]
    }
}

package dev.oblac.gart

import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.gfx.alpha
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.rgb
import dev.oblac.gart.math.Constants
import dev.oblac.gart.pixels.scrollPixelsUp
import dev.oblac.gart.skia.Rect
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

const val w = 600
const val h = (w * Constants.goldenRatio).toInt()

val gart = Gart.of(
    "falllines", w, h,
)

val g = gart.g
val b = gart.b

fun main() {
    with(gart) {
        println(name)

        w.show()
        val changeMarker = f.marker().onEvery(8.seconds)

        val markerStart = f.marker().atTime(40.seconds)   // window marker (!)
        val markerEnd = f.marker().atTime(20.seconds)          // video marker (!)

        g.canvas.drawRect(Rect(0f, 0f, g.d.wf, g.d.hf), fillOf(0xFF000000))
        a.draw {
            draw()
            when {
                changeMarker.now() -> randomizeSegments()
                markerStart.now() -> a.record()
                markerEnd.now() -> a.stop()
            }
        }

        Media.saveImage(this)
        Media.saveVideo(this)
    }
}

const val gap = 10

// segments
val input = arrayOf(40, 40, 40, 8, 40, 20, 40)
val drawOrder = (input.indices).map { it }.shuffled()

val segmentsTotalWidth = input.sumOf { it + gap } - gap
val offset = (w - segmentsTotalWidth) / 2f

// create segments
fun colorOfIndex(index: Int): Int {
    if (input.size > Palettes.cool6.size) {
        throw IllegalArgumentException("Palette too small")
    }
    val color = Palettes.cool6[Palettes.cool6.size * index / input.size]
    return alpha(color, 255)
}

val segments = input
    .mapIndexed { index, value -> Segment(value, colorOfIndex(index), drawOrder[index]) }
    .toTypedArray()
    .apply { updateStartingPositions(this) }

fun randomizeSegments() {
    val inMotion = segments.find { it.inMotion }?.inMotion ?: false
    if (inMotion) {
        return
    }

    val cloned = segments.clone()
    val size = cloned.size

    val pair = (0 until size)
        .filter { !segments[it].inMotion }
        .toTypedArray()
        .apply { this.shuffle() }

    if (pair.size < 2) {
        return
    }
    println("${pair[0]} <-> ${pair[1]}")
    cloned
        .switch(pair[0], pair[1])
        .apply { updateNextPositions(segments, this) }
}

private fun <T> Array<T>.switch(a: Int, b: Int): Array<T> {
    val temp = this[a]
    this[a] = this[b]
    this[b] = temp
    return this
}

fun draw() {
    b.update()
    scrollPixelsUp(b, 1)

    for (x in 0 until w) {
        val g = Random.nextInt(30)
        b[x, h - 1] = rgb(g, g, g)
    }
    b.draw()
    segments
        .sortedBy { it.drawOrder }
        .forEach {
            it.draw()
            it.tickNextX()
        }
}

fun updateStartingPositions(segments: Array<Segment>) {
    var x = offset
    segments.forEach {
        it.initialOffset(x)
        x += it.w + gap
    }
}

fun updateNextPositions(segments: Array<Segment>, nextSegments: Array<Segment>) {
    var x = offset
    nextSegments.forEach {
        val currentSegment = segments.find { next -> it == next }
        currentSegment!!.setNextX(x)
        x += currentSegment.w + gap
    }
}

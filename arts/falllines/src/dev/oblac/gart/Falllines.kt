package dev.oblac.gart

import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.rgb
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.pixels.scrollUp
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

const val w = 600
const val h = (w * GOLDEN_RATIO).toInt()
val gart = Gart.of(
    "falllines", w, h, 25
)

val g = gart.gartvas()
val b = gart.gartmap(g)

fun main() {
    println(gart)

    val changeMarker = 8.seconds.toFrames(gart.fps)
    val markerStart = 40.seconds.toFrames(gart.fps)   // window marker (!)
    val markerEnd = 20.seconds.toFrames(gart.fps)     // video marker (!)

    g.draw { c, d -> c.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOf(0xFF000000)) }

    val w = gart.window()
    val m = gart.movie()

    m.record(w, recording = false).show { c, _, f ->
        draw(g.canvas)
        g.writeBitmap(b.bitmap)
        c.drawImage(b.image(), 0f, 0f)

        f.onEveryFrame(changeMarker) {
            randomizeSegments()
        }
        f.onFrame(markerStart) {
            m.startRecording()
        }
        f.onFrame(markerEnd) {
            m.stopRecording()
        }
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

fun draw(canvas: Canvas) {
    b.updatePixelsFromCanvas()
    b.scrollUp(1)

    for (x in 0 until w) {
        val g = Random.nextInt(30)
        b[x, h - 1] = rgb(g, g, g)
    }
    b.drawToCanvas()
    segments
        .sortedBy { it.drawOrder }
        .forEach {
            it.draw(canvas)
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

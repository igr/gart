package ac.obl.gart

import ac.obl.gart.gfx.*
import ac.obl.gart.math.GOLDEN_RATIO
import ac.obl.gart.skia.Rect
import kotlin.random.Random

const val w = 600
const val h = (w * GOLDEN_RATIO).toInt()

val g = Gartvas(w, h)
val b = Gartmap(g)
val scroll = PixelScroller(b)

fun main() {
    val name = "falllines"
    println(name)

    val window = Window(g).show()
    val changeMarker = window.frames.marker().onEverySecond(8)

    val v = VideoGartvas(g).start("$name.mp4", 50)
    val markerStart = window.frames.marker().atSecond(40)   // window marker (!)
    val markerEnd = v.frames.marker().atSecond(20)          // video marker (!)

    g.canvas.drawRect(Rect(0f, 0f, g.wf, g.hf), fillOf(0xFF000000))

    window.paint {
        draw()
        when {
            changeMarker.now() -> randomizeSegments()
            markerStart.after() && markerEnd.before() -> v.addFrame()
            markerEnd.now() -> v.save()
        }
    }

    ImageWriter(g).save("$name.png")
}

const val gap = 10

// segments
val input = arrayOf(40, 40, 40, 8, 40, 20, 40)
val drawOrder = (input.indices).map { it }.shuffled()

val segmentsTotalWidth = input.sumOf { it + gap } - gap
val offset = (w - segmentsTotalWidth) / 2f

// create segments
fun colorOfIndex(index: Int): Int {
    val color = coolColorOf(index, input.size, COOL_COL_6)
    return alpha(color, 255)
}

val segments = input
        .mapIndexed{ index, value -> Segment(value, colorOfIndex(index), drawOrder[index]) }
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
    scroll.up(1)

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

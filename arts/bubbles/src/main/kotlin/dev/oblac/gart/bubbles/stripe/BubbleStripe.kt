package dev.oblac.gart.bubbles.stripe

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.angles.Radians
import dev.oblac.gart.angles.cos
import dev.oblac.gart.angles.sin
import dev.oblac.gart.color.NipponColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIO
import dev.oblac.gart.math.PIf
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.PoissonDiskSamplingNoise
import dev.oblac.gart.pack.CirclePacker
import org.jetbrains.skia.Point
import kotlin.math.pow
import kotlin.math.sqrt

val fillBack = fillOf(NipponColors.col234_GOFUN)

fun main() {
    val gart = Gart.of("BubbleStripe", 1024 * GOLDEN_RATIO, 1024)

    val d = gart.d
    val g = gart.gartvas()
    val w = gart.window()
    val c = g.canvas

    val bigBubbles = packBigCircles(d)
    val smallBubbles = bigBubbles.flatMap { packSmallCircles(it) }

    c.clear(fillBack.color)

    bigBubbles.forEach {
        c.drawCircle(it.x, it.y, it.radius, fillOfBlack())
    }
    smallBubbles.forEach {
        c.drawCircle(it.x, it.y, it.radius - 2, fillBack)
    }

    c.drawBorder(d, strokeOf(fillBack.color, 40f))

    //gart.saveImage(g)
    //w.showImage(g)

    // create all different periods
    data class Period(val circle: Circle, val offset: Float, val amplitude: Float, val frequency: Float)

    val bigs = bigBubbles.map {
        Period(it, rndf(0, PIf), rndf(10, 25), rndf(0.04f, 0.1f))
    }

    val smalls = smallBubbles.map {
        Period(it, rndf(0, PIf), rndf(2, 8), rndf(0.1f, 0.2f))
    }

    val m = gart.movie()
//    w.show { c, _, f ->
    m.record(w).show { c, _, f ->
        c.clear(fillBack.color)

        bigs.forEach {
            val circle = it.circle
            val offsetX = sin(Radians(it.offset + f.frame * it.frequency)) * it.amplitude
            val offsetY = cos(Radians(it.offset + f.frame * it.frequency)) * it.amplitude
            c.drawCircle(circle.x + offsetX.toFloat(), circle.y + offsetY.toFloat(), circle.radius, fillOfBlack())
        }
        smalls.forEach {
            val circle = it.circle
            val offset = sin(Radians(it.offset + f.frame * it.frequency)) * it.amplitude
            c.drawCircle(circle.x, circle.y, circle.radius - 2 + offset.toFloat(), fillBack)
        }

        c.drawBorder(d, strokeOf(fillBack.color, 40f))
    }.onKey {
        when (it) {
            Key.KEY_S -> m.stopRecording()
            else -> println("Key: $it")
        }
    }
}

private fun packBigCircles(dimension: Dimension): List<Circle> {
    val packer = CirclePacker(dimension.wf, dimension.hf, growth = 10, padding = 50)
    val circles = packer.pack(100_000, 50f, 300f)
    return circles
}


private fun packSmallCircles(target: Circle): List<Circle> {
    val circles = if (target.radius > 200) {
        val packer = CirclePacker(target.radius * 2, target.radius * 2, growth = 2)
        packer.pack(500_000, 20f, 50f)
    } else {
        val packer = CirclePacker(target.radius * 2, target.radius * 2, growth = 1)
        packer.pack(500_000, 10f, 30f)
    }
    return circles
        .map { Circle(target.center.x - target.radius + it.x, target.center.y - target.radius + it.y, it.radius) }
        .filter { it.center.isInside(target) }
}

private fun growBubbles(list: List<BubbleX>, maxR: Float): List<BubbleX> {
    var bubbles = list
    while (true) {
        val newList = bubbles.map { it -> growSingleBubble(it, bubbles, maxR) }

        val wasAnyBubbleGrow = newList.indices.any { newList[it] != bubbles[it] }
        if (!wasAnyBubbleGrow) {
            break
        }
        bubbles = newList
    }
    return bubbles
}

private fun growSingleBubble(bubble: BubbleX, existingBubbles: List<BubbleX>, maxR: Float): BubbleX {
    val moveStep = 2f

    val canGrow = existingBubbles
        .filter { it != bubble }
        .any { it.collideWith(bubble) }
        .not()

    // bubble can grow, so grow it
    if (canGrow) {
        return bubble.grow()
    }

    if (bubble.r >= maxR) {
        // don't grow anymore
        return bubble
    }

    // bubble can't grow, so move it
    val directions = listOf(
        Pair(-moveStep, 0f), Pair(moveStep, 0f),  // Left, Right
        Pair(0f, -moveStep), Pair(0f, moveStep),  // Up, Down
        Pair(-moveStep, -moveStep), Pair(moveStep, moveStep), // Diagonals
        Pair(-moveStep, moveStep), Pair(moveStep, -moveStep)
    ).shuffled()  // Randomize movement direction

    for ((dx, dy) in directions) {
        val newX = bubble.x + dx
        val newY = bubble.y + dy

        val movedBubble = BubbleX(newX, newY, bubble.r)

        if (existingBubbles.none { it != bubble && movedBubble.collideWith(it) }) {
            return movedBubble
        }
    }

    // bubble can't grow and can't move, so return the same
    return bubble
}

private fun randomBubbles(d: Dimension): List<BubbleX> {
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(
        0.0, 0.0, d.w.toDouble(), d.h.toDouble(),
        20
    )
    return samples
        .map { Point(it.x, it.y) }
        .map { BubbleX(it.x, it.y, 10f) }
}

private fun randomSmallBubbles(b: BubbleX): List<BubbleX> {
    val noise = PoissonDiskSamplingNoise()
    val samples = noise.generate(
        (b.x - b.r).toDouble(),
        (b.y - b.r).toDouble(),
        (b.x + b.r).toDouble(),
        (b.y + b.r).toDouble(),
        10
    )
    return samples
        .filter { b.circle.contains(it.x, it.y) }
        .map { BubbleX(it.x, it.y, 2f) }
}

private data class BubbleX(
    val x: Float,
    val y: Float,
    val r: Float,
) {

    val circle = Circle(x, y, r)

    fun grow(): BubbleX {
        return BubbleX(x, y, r + 1)
    }

    private fun distance(b: BubbleX): Float {
        return sqrt((x - b.x).pow(2) + (y - b.y).pow(2))
    }

    fun collideWith(b: BubbleX): Boolean {
        val d = distance(b)
        return r + b.r > d
    }
}

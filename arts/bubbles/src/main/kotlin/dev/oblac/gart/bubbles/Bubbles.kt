package dev.oblac.gart.bubbles

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Window
import dev.oblac.gart.gfx.Palettes
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.fillOfBlack
import dev.oblac.gart.math.cosDeg
import dev.oblac.gart.math.nextFloat
import dev.oblac.gart.math.sinDeg
import dev.oblac.gart.math.toDegree
import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.Rect
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random.Default.nextInt
import kotlin.random.Random.Default.nextLong

const val name = "Bubbles"

fun main() {
    println(name)

    val d = Dimension(1024, 1024)
    val g = Gartvas(d)
    val w = Window(g, 10).show()

//    // load
    var list = Array(100) {
        Bubble(
            d, nextFloat(d.w), nextFloat(d.h), 2.0f,
            nextLong(100, 1000),
            fillOf(Palettes.cool2.random())
        )
    }.toList()

//    var list = listOf(
//        Bubble(box, 150f, 200f, 100.0f),
//        Bubble(box, 900f, 200f, 50.0f),
//        Bubble(box, 600f, 100f, 50.0f),
//        Bubble(box, 600f, 900f, 50.0f),
//    )
//    var list = listOf(
//        Bubble(box, 200f, 800f, 20.0f, "A"),
//        Bubble(box, 400f, 1000f, 20.0f, "B"),
////        Bubble(box, 600f, 800f, 20.0f),
//    )

    fun paintAll() {
        list.forEach {
            //g.canvas.drawCircle(it.x, it.y, 2f, strokeOfGreen(2f))
            g.canvas.drawCircle(it.x, it.y, it.r, it.paint)
        }
    }

    // draw
    g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOfBlack())
    paintAll()

    w.paint { frames ->
        g.canvas.drawRect(Rect(0f, 0f, d.wf, d.hf), fillOfBlack())
        list = list
            .filter { !it.isExpired(frames.count()) }
            .map { move(list, it) }
            .toList()
        paintAll()
    }

    g.writeSnapshotAsImage("$name.png")
}

fun move(
    list: List<Bubble>,
    b: Bubble,
): Bubble {
    val pushers =
        list
            .filter { it != b }
            .mapNotNull { b.pushedBy(it) }
            .toMutableList()

    if (b.pushedByLeft() != null) {
        pushers.add(b.pushedByLeft()!!)
    }
    if (b.pushedByRight() != null) {
        pushers.add(b.pushedByRight()!!)
    }
    if (b.pushedByUp() != null) {
        pushers.add(b.pushedByUp()!!)
    }
    if (b.pushedByDown() != null) {
        pushers.add(b.pushedByDown()!!)
    }

    var newB = b
    if (pushers.isNotEmpty()) {
        newB = newB.push(pushers)
    }
    if (list.filter { it != b }.all { !newB.collide(it) }) {
        newB = newB.grow()
    }

    // detect overlapping!
//    if (!list.any { !newB.growable(it) }) {
//        newB = b
//    }
    return newB
}

data class Bubble(
    val d: Dimension,
    val x: Float,
    val y: Float,
    val r: Float,
    val life: Long,
    val paint: Paint,
    val name: String = nextInt().toString()
) {


    fun isExpired(time: Long): Boolean {
        return time > life
    }

    fun grow(): Bubble {
        return Bubble(d, x, y, r + 1, life, paint, name)
    }

    fun push(angles: List<Float>): Bubble {
        var dx = 0f
        var dy = 0f

        angles.forEach {
            dx += 1.41f * cosDeg(180 - it)
            dy += 1.41f * sinDeg(180 - it)
        }
        return Bubble(d, x + dx, y + dy, r, life, paint, name)
    }

    private fun distance(b: Bubble): Float {
        return sqrt((x - b.x).pow(2) + (y - b.y).pow(2))
    }

    fun pushedByLeft(): Float? {
        return if (x - r <= 0) 180f else null
    }

    fun pushedByRight(): Float? {
        return if (x + r >= d.rf) 0f else null
    }

    fun pushedByUp(): Float? {
        return if (y - r <= 0) 90f else null
    }

    fun pushedByDown(): Float? {
        return if (y + r >= d.bf) 270f else null
    }

    fun pushedBy(b: Bubble): Float? {
        val d = distance(b)

        if (r + b.r < d) {
            return null     // no collision
        }

        var alpha = asin(abs(b.y - y) / d).toDegree()

        alpha = if (x >= b.x) {
            // this bubble is right, so move to right, i.e. the push is on the left
            if (y <= b.y) {
                180 + alpha // Q1
            } else {
                180 - alpha // Q4
            }
        } else {
            // this bubble is left, so move to left, i.e. the push is on the right
            if (y <= b.y) {
                -alpha // Q1
            } else {
                alpha  // Q2
            }
        }

        if (alpha < 0) alpha += 360
        if (alpha > 360) alpha -= 360
        return alpha
    }

    fun collide(b: Bubble): Boolean {
        val d = distance(b)
        return r + b.r > d
    }

    fun contains(x: Float, y: Float): Boolean {
        return sqrt((this.x - x).pow(2) + (this.y - y).pow(2)) < this.r
    }
}


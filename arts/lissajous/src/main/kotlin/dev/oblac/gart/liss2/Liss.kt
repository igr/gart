package dev.oblac.gart.liss2

import dev.oblac.gart.Dimension
import dev.oblac.gart.Frames
import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.color.toIntColor
import dev.oblac.gart.gfx.drawCircle
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.pointOf
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.map
import org.jetbrains.skia.Canvas
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val gart = Gart.of(
    "Liss", 1024, 1024,
    30
)
val d = gart.d
const val xrad = 100f
const val yrad = 100f

var c1 = 0xFF1a1a1a.toIntColor()
var c2 = 0xFFe5e5e5.toIntColor()

data class Config(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int, val f: Int, val g: Int)

val configs = listOf(
    Config(7, 6, 14, 5, 50, 2, 5),
    Config(9, 6, 12, 6, 50, 2, 11),
    Config(9, 4, 11, 6, 50, 5, 9),
    Config(6, 6, 12, 6, 50, 10, 10),
    Config(5, 6, 12, 6, 50, 7, 2),
    Config(4, 3, 14, 8, 50, 4, 7),
    Config(9, 9, 12, 7, 50, 5, 11),
    Config(6, 7, 11, 7, 50, 7, 5),
    Config(6, 6, 13, 5, 50, 2, 2),
    Config(7, 7, 12, 7, 50, 3, 4),
    Config(7, 6, 14, 8, 50, 3, 6),
    Config(7, 8, 13, 8, 50, 10, 7),
    Config(8, 8, 13, 7, 50, 11, 10),
    Config(9, 8, 13, 7, 50, 5, 9),
    Config(7, 6, 12, 6, 50, 5, 9),
    Config(8, 7, 14, 8, 50, 5, 5),
    Config(7, 7, 13, 7, 50, 10, 11),
    Config(6, 7, 11, 6, 50, 8, 5),
    Config(2, 3, 14, 8, 50, 5, 2),
    Config(6, 7, 10, 6, 50, 8, 4),
    Config(7, 7, 11, 6, 50, 3, 7),
    Config(7, 9, 13, 7, 50, 9, 6),
    Config(8, 9, 11, 8, 50, 10, 5),
    Config(9, 9, 14, 5, 50, 10, 7)
)

data class Params(
    var lx: Int,
    var ly: Int,
    var coef: Int,
    var divAngle: Int,
    var numP: Int,
    var a1: Int,
    var a2: Int
)

val params = Params(
    lx = 9,
    ly = 9,
    coef = 14,
    divAngle = 5,
    numP = 50,
    a1 = 10,
    a2 = 7
)

fun select(configs: List<Config>, params: Params) {
    // 6, 12, 17
    val cfg = configs[17]
    params.lx = cfg.a
    params.ly = cfg.b
    params.coef = cfg.c
    params.divAngle = cfg.d
    params.numP = cfg.e
    params.a1 = cfg.f
    params.a2 = cfg.g
}

fun main() {
    select(configs, params)
//    changeConfigX()
//    changeConfigY()

    val m = gart.movieGif()
    val w = gart.window()
    val movieEnds = 360L
    m.record(w).show { c, _, f ->
        println(f.frame)
        draw(c, f)
        f.onFrame(movieEnds) {
            m.stopRecording()
            gart.saveMovie(m)
        }
    }
}

fun changeConfigX() {
    params.lx = Random.nextInt(3, 9)
    params.ly = Random.nextInt(3, 9)
}

fun changeConfigY() {
    params.a1 = Random.nextInt(2, 12)
    params.a2 = Random.nextInt(2, 12)
    params.coef = Random.nextInt(10, 15)
    params.divAngle = Random.nextInt(4, 9)
}

fun draw(c: Canvas, f: Frames) {
    val t = f.frame

    c.clear(c1)
    val leeeen = 30
    for (i in 0 until leeeen) {
        val p = (i + t).toFloat() / leeeen
        computePos(c, d, p)
    }
}

fun computePos(c: Canvas, d: Dimension, p: Float) {
    val angle = map(p, 0f, 1f, 0f, TAUf / params.divAngle)

    for (i in 0 until params.numP) {
        val a = map(i.toFloat(), 0f, params.numP, 0f, TAUf)
        val rad = 0.35
        val x = sin(a + angle * params.lx) * d.hf / 1.5 * rad
        val y = cos(a + angle * params.ly) * d.hf / 1.5 * rad
        val r = 1 + sin((angle * params.coef) / 2) * 2
        c.save()
        c.translate(x.toFloat() * 1.4f, y.toFloat() * 1.4f)
        c.translate(sin(a + angle * params.a1) * xrad, cos(a + angle * params.a2) * yrad)
        c.drawCircle(pointOf(d.cx, d.cy), r * 2, fillOf(pal.safe(r * 3)))
        c.restore()
    }
}

val pal = Palettes.cool32

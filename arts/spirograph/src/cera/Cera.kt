package cera

import dev.oblac.gart.Gart
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.io.ensureExtension
import dev.oblac.gart.io.pf
import dev.oblac.gart.io.pi
import dev.oblac.gart.io.ps
import dev.oblac.gart.math.between
import dev.oblac.gart.util.Stopwatch
import kotlin.math.min
import kotlin.random.Random

/*
 * cera. rings of spirograph loops round one core, nested into a medallion.
 *
 * every bath has its own wheel, and the rings of a bath fall on top of each other. so the medallion
 * shows one ring a bath. a later bath has a bigger wheel, and its loops reach further from the core.
 * a later ring gets its wax after more baths. its color is darker, and the older rings cover it.
 *
 * the wheel, the wax and the baths are in batik.kt.
 */

private val SEED = pi("seed", 1)
private val OUT = ps("out", "cera")
private val SS = pi("ss", 2, 1..4)
private val PAL = pi("pal", 43, 1..Palettes.COOL_COUNT) // sogan browns
private val WAX = pf("wax", 2.2f, 1f..8f) // the tjanting line at the usual pen speed, px
private val CRACK = pf("crack", 0.5f, 0f..2f) // the lace is almost solid wax. 0.9 scribbled all over it
private val BORDER = pf("border", 70f, 0f..200f) // the hem band, px. 0 is none
private val TRI = pf("tri", 0.09f, 0f..0.25f) // how much the core is a triangle. 0.1 is flat sides, more dents them
private val LUMPY = pf("lumpy", 0.04f, 0f..0.5f) // the lumps on top. tri 0 with lumpy 0.3 is a pebble

private const val WMAX = 9f // the fattest the line gets, where the pen nearly stops
private const val RINGS = 14
private const val CORE = 240f // half the long side of the core's box, px. the wheels go with it
private const val HEM = 2 // the hem goes in with the third ring. in bath 0 it was bare cloth, brighter than anything else

fun main(args: Array<String>) {
    val gart = Gart.of("cera", W, H)
    val sw = Stopwatch()
    val pen = Tjanting(SEED, WAX, WMAX)
    val batik = Batik(SEED, PAL, SS, CRACK)
    val rnd = Random(SEED)

    // the old walk took the first 8 numbers. I burn them, or every seed gets another core
    repeat(8) { rnd.nextFloat() }
    val core = Blob(rnd, LUMPY, TRI)
    // one wheel a bath, like a stamp. 14 wheel sizes gave a soft glow, thinner laps a loose net
    val K = batik.K
    val rhos = FloatArray(K) { rnd.between(0.98f, 1.12f) }
    val gaps = FloatArray(K) { rnd.between(9f, 16f) }

    // the hem and every ring get their own Random, so the rings of one bath lay their wax a bit differently
    val traces = ArrayList<Trace>()
    if (BORDER > 0f) traces += pen.hem(BORDER, min(HEM, K - 1), Random(SEED * 7919L + 1))
    fun wheel(stage: Int) = CORE * (0.1f + 0.4f * stage / (K - 1).coerceAtLeast(1))
    val path = core.path(W / 2f, H / 2f, CORE, wheel(K - 1))
    for (i in 0 until RINGS) {
        val stage = i * K / RINGS
        traces += pen.rolled(path, wheel(stage), rhos[stage], gaps[stage], stage, Random(SEED * 7919L + 10 + i))
    }
    println("cera: ${traces.sumOf { it.ws.size }} strokes, ${sw.ms}ms")
    val g = batik.dye(gart, traces)
    gart.saveImage(g, OUT.ensureExtension())
    println("cera: seed=$SEED pal=$PAL, ${sw.ms}ms")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

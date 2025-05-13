package dev.oblac.gart.skyscraper

import dev.oblac.gart.Gart
import dev.oblac.gart.toTime
import kotlin.random.Random

const val w = 1280
const val h = 800
const val windowSize = 10f

val gart = Gart.of(
    "skyscraper",
    w, h, 1
)

typealias BuildingFunction = (x: Float, y: Float) -> Building

fun towerBuilding(side: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, side, side, 20f, clr, gart.d)
}

fun squareBuilding(side: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, side, side, 20f, clr, gart.d)
}

fun largeBuilding(a: Float, ratio: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, a, a * ratio, 20f, clr, gart.d)
}

fun largeBuildingOpposite(a: Float, ratio: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, a * ratio, a, 20f, clr, gart.d)
}

fun nextFloatStep(value: Number, step: Int): Float {
    return (Random.nextInt(value.toInt()).div(step) * step).toFloat()
}

fun rowTop(clr: Colors): Array<Building> {
    val list = mutableListOf<Building>()
    var x = -200
    while (x < w + 200) {
        val y = 0
        val fn: BuildingFunction = when (Random.nextInt(100)) {
            in 0 until 20 -> towerBuilding(60f, clr)
            in 20 until 35 -> largeBuilding(100f, 4f, clr)
            in 35 until 50 -> largeBuildingOpposite(100f, 4f, clr)
            else -> squareBuilding(100f, clr)
        }
        val b: Building = fn(x.toFloat(), y.toFloat())
        list.add(b)

        x = Random.nextInt(b.roofRect.top.x.toInt() - 100, b.roofRect.top.x.toInt())
    }
    list.shuffle()
    return list.toTypedArray()
}

fun rowMiddle(clr: Colors): Array<Building> {
    val list = mutableListOf<Building>()
    var x = -200
    while (x < w + 200) {
        val y = h * 0.3 + nextFloatStep(h * 0.4f, 6)
        val fn: BuildingFunction = when (Random.nextInt(100)) {
            in 0 until 30 -> towerBuilding(30f, clr)
            in 30 until 45 -> largeBuilding(80f, 3f, clr)
            in 45 until 60 -> largeBuildingOpposite(80f, 3f, clr)
            else -> squareBuilding(80f, clr)
        }
        val b: Building = fn(x.toFloat(), y.toFloat())
        list.add(b)

        x = Random.nextInt(b.roofRect.top.x.toInt() - 100, b.roofRect.top.x.toInt())
    }
    list.shuffle()
    return list.toTypedArray()
}

fun rowMiddleSpread(clr: Colors): Array<Building> {
    val list = mutableListOf<Building>()
    var x = -200
    while (x < w + 200) {
        val y = h * 0.3 + nextFloatStep(h * 0.4f, 6)
        val fn: BuildingFunction = when (Random.nextInt(100)) {
            in 0 until 30 -> towerBuilding(30f, clr)
            in 30 until 45 -> largeBuilding(80f, 3f, clr)
            in 45 until 60 -> largeBuildingOpposite(80f, 3f, clr)
            else -> squareBuilding(80f, clr)
        }
        val b: Building = fn(x.toFloat(), y.toFloat())
        list.add(b)

        x = Random.nextInt(b.roofRect.top.x.toInt() - 100, b.roofRect.top.x.toInt() + 100) + w / 12
    }
    list.shuffle()
    return list.toTypedArray()
}

fun rowBottom(clr: Colors): Array<Building> {
    val list = mutableListOf<Building>()
    var x = -200
    while (x < w + 200) {
        val y = h * 0.8
        val fn: BuildingFunction = when (Random.nextInt(100)) {
            in 0 until 20 -> towerBuilding(50f, clr)
            in 20 until 35 -> largeBuilding(80f, 2f, clr)
            in 35 until 50 -> largeBuildingOpposite(80f, 4f, clr)
            else -> squareBuilding(100f, clr)
        }
        val b: Building = fn(x.toFloat(), y.toFloat())
        list.add(b)

        x = Random.nextInt(b.roofRect.top.x.toInt() + 100, b.roofRect.top.x.toInt() + 400)
    }
    list.shuffle()
    return list.toTypedArray()
}

fun main() {
    println(gart)

    val endMarker = colors.size * 3L // repeat all colors 3 times

//    towerBuilding(30f)(100f, 100f)(g.canvas)
//    squareBuilding(80f)(100f, 100f)(g.canvas)

    val w = gart.window()
    val m = gart.movie()

    m.record(w).show { c, _, f ->
        val color = colors[f.frame.toTime(f.frametime).inWholeSeconds.mod(colors.size)]

        rowTop(color).forEach { it(c) }

        if (Random.nextBoolean()) {
            rowMiddleSpread(color)
        } else {
            rowMiddle(color)
        }.forEach { it(c) }

        rowBottom(color).forEach { it(c) }

        f.onFrame(endMarker) {
            m.stopRecording()
        }

        f.onFrame(1) {
            gart.saveImage(c)
        }
    }
}

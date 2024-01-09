package dev.oblac.gart.skyscraper

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.GartvasVideo
import dev.oblac.gart.Window
import kotlin.random.Random

const val w = 1280
const val h = 800
const val windowSize = 10f

val d = Dimension(w, h)
val g = Gartvas(d)
val window = Window(g, 1).show()

typealias BuildingFunction = (x: Float, y: Float) -> Building

fun towerBuilding(side: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, side, side, 20f, clr)
}

fun squareBuilding(side: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, side, side, 20f, clr)
}

fun largeBuilding(a: Float, ratio: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, a, a * ratio, 20f, clr)
}

fun largeBuildingOpposite(a: Float, ratio: Float, clr: Colors): BuildingFunction = { x, y ->
    Building(x, y, a * ratio, a, 20f, clr)
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
    val name = "skyscraper"
    println(name)
    val v = GartvasVideo(g, "$name.mp4", 1)
    val endMarker = v.frames.marker().atNumber(colors.size * 3) // repeat all colors 3 times

//    towerBuilding(30f)(100f, 100f)(g.canvas)
//    squareBuilding(80f)(100f, 100f)(g.canvas)

    window.paint {
        val color = colors[it.count.mod(colors.size)]

        rowTop(color).forEach { it(g.canvas) }

        if (Random.nextBoolean()) {
            rowMiddleSpread(color)
        } else {
            rowMiddle(color)
        }.forEach { it(g.canvas) }

        rowBottom(color).forEach { it(g.canvas) }

        when {
            endMarker.before() -> v.addFrame()
            endMarker.now() -> v.stopAndSaveVideo()
        }
    }

	g.writeSnapshotAsImage("$name.png")
}

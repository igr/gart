package dev.oblac.gart.skyscraper

import kotlin.random.Random

fun main() {
    println(gart)

//    towerBuilding(30f)(100f, 100f)(g.canvas)
//    squareBuilding(80f)(100f, 100f)(g.canvas)

    val g = gart.gartvas()

    g.draw { c, _ ->
        val color = colors[2]

        rowTop(color).forEach { it(c) }

        if (Random.nextBoolean()) {
            rowMiddleSpread(color)
        } else {
            rowMiddle(color)
        }.forEach { it(c) }

        rowBottom(color).forEach { it(c) }
    }

    gart.window().showImage(g)

}


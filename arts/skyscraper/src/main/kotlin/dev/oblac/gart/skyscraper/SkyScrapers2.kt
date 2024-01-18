package dev.oblac.gart.skyscraper

import dev.oblac.gart.Media
import kotlin.random.Random

fun main() {
    with(gart) {
        println(name)

//    towerBuilding(30f)(100f, 100f)(g.canvas)
//    squareBuilding(80f)(100f, 100f)(g.canvas)

        w.show()
        a.draw {
            val color = colors[2]

            rowTop(color).forEach { it(g.canvas) }

            if (Random.nextBoolean()) {
                rowMiddleSpread(color)
            } else {
                rowMiddle(color)
            }.forEach { it(g.canvas) }

            rowBottom(color).forEach { it(g.canvas) }

            a.stop()
        }
        Media.saveImage(this)
    }
}

package dev.oblac.gart.flowforce

import dev.oblac.gart.Dimension
import dev.oblac.gart.Gartvas
import dev.oblac.gart.Window

val d = Dimension(1024, 1024)
val g = Gartvas(d)
val w = Window(g).show()


fun main() {
    val name = "flowforce"

    //one("${name}1")
    //two("${name}2")
    three("${name}3")


}

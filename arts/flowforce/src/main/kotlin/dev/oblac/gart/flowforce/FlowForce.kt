package dev.oblac.gart.flowforce

import dev.oblac.gart.Gart

val gart = Gart.of(
    "flowforce",
    1024, 1024
)

fun main() {
    val name = gart.name

//    one("${name}1")
//    two("${name}2")
    three("${name}3")
}

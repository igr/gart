package dev.oblac.gart.util

import dev.oblac.gart.skia.Data

fun loadResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()

fun loadResourceAsBytes(path: String): ByteArray? =
    object {}.javaClass.getResource(path)?.readBytes()

fun loadResourceAsData(path: String): Data =
    Data.makeFromBytes(loadResourceAsBytes(path)!!, 0)

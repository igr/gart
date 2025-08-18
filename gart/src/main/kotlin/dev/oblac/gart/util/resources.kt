package dev.oblac.gart.util

import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skiko.toImage
import java.io.InputStream
import javax.imageio.ImageIO

fun loadResourceAsText(path: String): String? =
    object {}.javaClass.getResource(path)?.readText()

fun loadResourceAsBytes(path: String): ByteArray? =
    object {}.javaClass.getResource(path)?.readBytes()

fun loadResourceAsData(path: String): Data =
    Data.makeFromBytes(loadResourceAsBytes(path)!!, 0)

fun loadResourceAsStream(path: String): InputStream =
    object {}.javaClass.getResourceAsStream(path)!!

fun loadResourceAsImage(path: String): Image {
    val stream = loadResourceAsStream(path)
    return ImageIO.read(stream).toImage()
}

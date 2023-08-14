package studio.oblac.gart

import studio.oblac.gart.skia.EncodedImageFormat
import java.io.File

/**
 * Writes [Gartvas] snapshot as an image.
 */
fun writeGartvasAsImage(g: Gartvas, name: String) {
    g.snapshot()
        .encodeToData(EncodedImageFormat.valueOf(name.substringAfterLast('.').uppercase()))
        .let { it!!.bytes }
        .also {
            File(name).writeBytes(it)
            println("Image saved: $name")
        }
}

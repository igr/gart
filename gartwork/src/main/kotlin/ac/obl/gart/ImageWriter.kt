package ac.obl.gart

import io.github.humbleui.skija.EncodedImageFormat
import java.io.File

class ImageWriter(private var g: Gartvas) {
	fun save(name: String) {
		g.snapshot()
			.encodeToData(EncodedImageFormat.valueOf(name.substringAfterLast('.').uppercase()))
			.let { it!!.bytes }
			.also {
				File(name).writeBytes(it)
				println("Image saved: $name")
			}
	}
}
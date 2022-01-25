package ac.obl.gart

import io.github.humbleui.skija.*
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.*

fun Image.toBufferedImage(): BufferedImage {
	val bitmap = Bitmap()
	bitmap.allocPixelsFlags(ImageInfo.makeS32(this.width, this.height, ColorAlphaType.PREMUL), false)
	Canvas(bitmap).drawImage(this, 0f, 0f)

	val bytes = bitmap.readPixels(bitmap.imageInfo, (this.width * 4L), 0, 0)!!
	val buffer = DataBufferByte(bytes, bytes.size)
	val raster = Raster.createInterleavedRaster(
		buffer,
		this.width,
		this.height,
		this.width * 4, 4,
		intArrayOf(2, 1, 0, 3),     // BGRA order
		null
	)
	val colorModel = ComponentColorModel(
		ColorSpace.getInstance(ColorSpace.CS_sRGB),
		true,
		false,
		Transparency.TRANSLUCENT,
		DataBuffer.TYPE_BYTE
	)

	return BufferedImage(colorModel, raster!!, false, null)
}
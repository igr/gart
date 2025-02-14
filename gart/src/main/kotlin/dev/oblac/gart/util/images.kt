package dev.oblac.gart.util

import org.jetbrains.skia.*
import org.jetbrains.skiko.toBufferedImage
import java.awt.image.BufferedImage

internal fun Image.toBufferedImage(): BufferedImage {
    val bitmap = Bitmap()
    bitmap.allocPixelsFlags(ImageInfo.makeS32(this.width, this.height, ColorAlphaType.PREMUL), false)
    Canvas(bitmap).drawImage(this, 0f, 0f)

    return bitmap.toBufferedImage()

//    val bytes = bitmap.readPixels(bitmap.imageInfo, this.width * 4, 0, 0)!!
//    val buffer = DataBufferByte(bytes, bytes.size)
//    val raster = Raster.createInterleavedRaster(
//        buffer,
//        this.width,
//        this.height,
//        this.width * 4, 4,
//        intArrayOf(2, 1, 0, 3),     // BGRA order
//        null
//    )
//    val colorModel = ComponentColorModel(
//        ColorSpace.getInstance(ColorSpace.CS_sRGB),
//        true,
//        false,
//        Transparency.TRANSLUCENT,
//        DataBuffer.TYPE_BYTE
//    )
//
//    return BufferedImage(colorModel, raster!!, false, null)
}

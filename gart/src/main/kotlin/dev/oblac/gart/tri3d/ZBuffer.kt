package dev.oblac.gart.tri3d

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/**
 * Per-pixel depth buffer for accurate triangle visibility.
 *
 * Unlike the painter's algorithm, a z-buffer correctly handles
 * intersecting triangles and complex depth orderings.
 *
 * Usage:
 * ```
 * val zb = ZBuffer(width, height)
 * zb.clear()
 * zb.rasterize(camera, mesh)
 * zb.drawTo(canvas, x, y)
 * ```
 */
class ZBuffer(val width: Int, val height: Int) {

    private val depth = FloatArray(width * height)
    private val color = IntArray(width * height)

    /**
     * Clears the buffer: depth to +infinity, color to transparent.
     */
    fun clear(background: Int = 0) {
        depth.fill(Float.MAX_VALUE)
        color.fill(background)
    }

    /**
     * Rasterizes all front-facing triangles from [mesh] into the buffer.
     */
    fun rasterize(camera: Camera, mesh: Mesh) {
        for (face in mesh.faces) {
            if (!camera.isFrontFacing(face)) continue
            rasterizeFace(camera, face)
        }
    }

    private fun rasterizeFace(camera: Camera, face: Face) {
        val pa = camera.project(face.a)
        val pb = camera.project(face.b)
        val pc = camera.project(face.c)

        val za = camera.depth(face.a)
        val zb = camera.depth(face.b)
        val zc = camera.depth(face.c)

        // bounding box, clipped to screen
        val minX = maxOf(0, minOf(pa.x, pb.x, pc.x).toInt())
        val maxX = minOf(width - 1, maxOf(pa.x, pb.x, pc.x).toInt())
        val minY = maxOf(0, minOf(pa.y, pb.y, pc.y).toInt())
        val maxY = minOf(height - 1, maxOf(pa.y, pb.y, pc.y).toInt())

        if (minX > maxX || minY > maxY) return

        // edge function coefficients for barycentric coordinates
        val dx0 = pb.x - pa.x; val dy0 = pb.y - pa.y
        val dx1 = pc.x - pa.x; val dy1 = pc.y - pa.y
        val area = dx0 * dy1 - dx1 * dy0
        if (area == 0f) return  // degenerate triangle
        val invArea = 1f / area

        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val px = x + 0.5f
                val py = y + 0.5f

                // barycentric coordinates via edge functions
                val ex = px - pa.x
                val ey = py - pa.y
                val w1 = (ex * dy1 - dx1 * ey) * invArea
                val w2 = (dx0 * ey - ex * dy0) * invArea
                val w0 = 1f - w1 - w2

                if (w0 < 0f || w1 < 0f || w2 < 0f) continue

                // interpolate depth
                val z = w0 * za + w1 * zb + w2 * zc
                val idx = y * width + x

                if (z < depth[idx]) {
                    depth[idx] = z
                    color[idx] = face.color
                }
            }
        }
    }

    /**
     * Creates a Skia [Image] from the color buffer.
     */
    fun toImage(): Image {
        val bitmap = Bitmap()
        val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
        bitmap.allocPixels(info)
        bitmap.installPixels(info, colorToBytes(), width * 4)
        return Image.makeFromBitmap(bitmap)
    }

    /**
     * Draws the z-buffer result onto [canvas] at the given position.
     */
    fun drawTo(canvas: Canvas, x: Float = 0f, y: Float = 0f) {
        canvas.drawImage(toImage(), x, y)
    }

    private fun colorToBytes(): ByteArray {
        val bytes = ByteArray(width * height * 4)
        for (i in color.indices) {
            val c = color[i]
            val off = i * 4
            bytes[off] = (c and 0xFF).toByte()              // B
            bytes[off + 1] = ((c shr 8) and 0xFF).toByte()  // G
            bytes[off + 2] = ((c shr 16) and 0xFF).toByte() // R
            bytes[off + 3] = ((c shr 24) and 0xFF).toByte() // A
        }
        return bytes
    }

    private fun minOf(a: Float, b: Float, c: Float): Float = kotlin.math.min(a, kotlin.math.min(b, c))
    private fun maxOf(a: Float, b: Float, c: Float): Float = kotlin.math.max(a, kotlin.math.max(b, c))
}

package dev.oblac.gart.painter

import dev.oblac.gart.color.*
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.painter.SprayPainter.Companion.loadPng
import org.jetbrains.skia.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random

/**
 * High-precision spray painter for accumulating many tiny paint deposits.
 *
 * `SprayPainter` keeps one mutable RGBA float buffer with four channels per
 * pixel: R, G, B, A, each in `[0, 1]`. A call to [pixel] alpha-composites the
 * current foreground color [fg] over the target pixel. Higher-level helpers
 * such as [pixels], [circle], [stroke], [strokes], and [path] choose sampled
 * positions and feed them into [pixel].
 *
 * The public color API uses packed ARGB `Int` values, matching gart's color
 * convention. The internal storage stays as a primitive `FloatArray` instead
 * of per-pixel color objects so thousands of low-alpha samples can accumulate
 * without 8-bit clipping or object churn.
 *
 * Pass a seeded [kotlin.random.Random] when deterministic spray placement is needed. Use
 * [toBitmap] or [drawTo] to render the buffer, and [loadPng] to initialize a
 * painter from an existing image.
 *
 * @param width buffer width in pixels
 * @param height buffer height in pixels; defaults to a square buffer
 * @param bg initial background color, packed as ARGB
 * @param fg foreground color used by paint operations, packed as ARGB
 */
class SprayPainter(
    val width: Int,
    val height: Int = width,
    val bg: Int = 0xFFFFFFFF.toInt(),
    val fg: Int = 0xFF000000.toInt(),
    private val rng: Random = Random.Default,
) {
    internal val vals = FloatArray(width * height * 4)

    init {
        clear(bg)
    }

    /** Paints every pixel with [color] (overwrite, not blended). */
    fun clear(color: Int = bg) {
        val r = redf(color)
        val g = greenf(color)
        val b = bluef(color)
        val a = alphaf(color)
        var i = 0
        while (i < vals.size) {
            vals[i] = r
            vals[i + 1] = g
            vals[i + 2] = b
            vals[i + 3] = a
            i += 4
        }
    }

    /** Reads the pixel at integer coordinates, packed back to ARGB. */
    fun sample(x: Int, y: Int): Int {
        if (x !in 0 until width || y !in 0 until height) return 0
        val i = (y * width + x) * 4
        return argb(vals[i + 3], vals[i], vals[i + 1], vals[i + 2])
    }

    fun sample(p: Point): Int = sample(p.x.toInt(), p.y.toInt())

    /**
     * Paints one pixel at `(x, y)` with the current foreground using the
     * Porter-Duff "over" composite. Out-of-bounds samples are silently dropped.
     */
    fun pixel(x: Float, y: Float) {
        val ix = x.roundToInt()
        val iy = y.roundToInt()
        if (ix !in 0 until width || iy !in 0 until height) return
        composeOver(ix, iy, fg)
    }

    fun pixel(p: Point) = pixel(p.x, p.y)

    private fun composeOver(ix: Int, iy: Int, srcArgb: Int) {
        val sa = alphaf(srcArgb)
        if (sa == 0f) return
        val sr = redf(srcArgb)
        val sg = greenf(srcArgb)
        val sb = bluef(srcArgb)

        val i = (iy * width + ix) * 4
        val dr = vals[i];
        val dg = vals[i + 1]
        val db = vals[i + 2];
        val da = vals[i + 3]
        val outA = sa + da * (1f - sa)
        if (outA <= 0f) {
            vals[i] = 0f; vals[i + 1] = 0f; vals[i + 2] = 0f; vals[i + 3] = 0f
            return
        }
        val invOutA = 1f / outA
        vals[i] = (sr * sa + dr * da * (1f - sa)) * invOutA
        vals[i + 1] = (sg * sa + dg * da * (1f - sa)) * invOutA
        vals[i + 2] = (sb * sa + db * da * (1f - sa)) * invOutA
        vals[i + 3] = outA
    }

    /**
     * Paints either every point in [points] (when [n] < 0) or [n] random
     * samples drawn from [points]. Uses current [fg].
     */
    fun pixels(points: Iterable<Point>, n: Int = -1) {
        if (n < 0) {
            for (p in points) pixel(p.x, p.y)
            return
        }
        val arr: List<Point> = points as? List<Point> ?: points.toList()
        if (arr.isEmpty()) return
        repeat(n) {
            val p = arr[rng.nextInt(arr.size)]
            pixel(p.x, p.y)
        }
    }

    /**
     * Paints [n] uniformly distributed random points inside the disc of radius
     * [radius] centered at `(cx, cy)`. Uses current [fg].
     */
    fun circle(cx: Float, cy: Float, radius: Float, n: Int) {
        repeat(n) {
            val r = radius * sqrt(rng.nextFloat())
            val a = rng.nextFloat() * 2f * Math.PI.toFloat()
            pixel(cx + cos(a) * r, cy + sin(a) * r)
        }
    }

    fun circle(c: Point, radius: Float, n: Int) = circle(c.x, c.y, radius, n)

    /**
     * Paints [n] random points along the line segment from [a] to [b]. Uses
     * current [fg].
     */
    fun stroke(a: Point, b: Point, n: Int) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        repeat(n) {
            val t = rng.nextFloat()
            pixel(a.x + dx * t, a.y + dy * t)
        }
    }

    fun line(line: Line, n: Int) = stroke(line.a, line.b, n)

    /** Paints [n] random points across each segment in [segments]. */
    fun strokes(segments: Iterable<Pair<Point, Point>>, n: Int) {
        for ((a, b) in segments) stroke(a, b, n)
    }

    fun lines(lines: Iterable<Line>, n: Int) {
        for (line in lines) line(line, n)
    }

    /**
     * Paints [n] random points distributed across the polyline given by
     * [points]. Uniform along the parametric chain (each segment gets
     * samples in proportion to its length).
     */
    fun path(points: List<Point>, n: Int) {
        if (points.size < 2 || n <= 0) return
        val segCount = points.size - 1
        val cum = FloatArray(segCount + 1)
        for (i in 0 until segCount) {
            val p = points[i];
            val q = points[i + 1]
            val dx = q.x - p.x;
            val dy = q.y - p.y
            cum[i + 1] = cum[i] + sqrt(dx * dx + dy * dy)
        }
        val total = cum[segCount]
        if (total == 0f) return
        repeat(n) {
            val s = rng.nextFloat() * total
            var lo = 0;
            var hi = segCount
            while (lo < hi) {
                val mid = (lo + hi) ushr 1
                if (cum[mid + 1] <= s) lo = mid + 1 else hi = mid
            }
            val i = lo
            val segLen = cum[i + 1] - cum[i]
            val t = if (segLen > 0f) (s - cum[i]) / segLen else 0f
            val p = points[i];
            val q = points[i + 1]
            pixel(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
        }
    }

    /**
     * Converts the float buffer to an 8-bit Skia [org.jetbrains.skia.Bitmap] (RGBA_8888, sRGB).
     * Optionally applies gamma, values are raised to `gamma` per channel
     * before quantization. `gamma=1f` (default)
     * is identity.
     */
    fun toBitmap(gamma: Float = 1f): Bitmap {
        fun floatPow(v: Float, p: Float) =
            if (v <= 0f) 0f else v.toDouble().pow(p.toDouble()).toFloat()

        val info = ImageInfo.makeN32(width, height, ColorAlphaType.UNPREMUL)
        val bmp = Bitmap()
        bmp.allocPixels(info)
        val out = ByteArray(width * height * 4)
        for (p in 0 until width * height) {
            val si = p * 4
            val r = if (gamma == 1f) vals[si] else floatPow(vals[si], gamma)
            val g = if (gamma == 1f) vals[si + 1] else floatPow(vals[si + 1], gamma)
            val b = if (gamma == 1f) vals[si + 2] else floatPow(vals[si + 2], gamma)
            val a = vals[si + 3]
            // N32 on JVM is BGRA byte order
            val di = p * 4
            out[di] = (b.coerceIn(0f, 1f) * 255f).toInt().toByte()
            out[di + 1] = (g.coerceIn(0f, 1f) * 255f).toInt().toByte()
            out[di + 2] = (r.coerceIn(0f, 1f) * 255f).toInt().toByte()
            out[di + 3] = (a.coerceIn(0f, 1f) * 255f).toInt().toByte()
        }
        bmp.installPixels(out)
        return bmp
    }

    /**
     * Renders the current spray-paint buffer onto a Skia [org.jetbrains.skia.Canvas] at origin (0, 0).
     * Use this to compose spray-paint output with the rest of a gart drawing.
     */
    fun drawTo(canvas: Canvas, gamma: Float = 1f) {
        val bmp = toBitmap(gamma)
        canvas.drawImage(Image.makeFromBitmap(bmp), 0f, 0f)
    }

    companion object {
        /**
         * Reads a PNG into a fresh [SprayPainter]. Pixel values are decoded to
         * `[0, 1]` floats; alpha is preserved.
         */
        fun loadPng(path: String): SprayPainter {
            val bytes = File(path).readBytes()
            val img = Image.makeFromEncoded(bytes)
            val info = ImageInfo.makeN32(img.width, img.height, ColorAlphaType.UNPREMUL)
            val bmp = Bitmap()
            bmp.allocPixels(info)
            img.readPixels(bmp)
            val sp = SprayPainter(img.width, img.height)
            val pixels = bmp.readPixels(info)
                ?: error("SprayPainter.loadPng: readPixels failed for $path")
            for (p in 0 until img.width * img.height) {
                val di = p * 4
                val b = (pixels[di].toInt() and 0xFF) / 255f
                val g = (pixels[di + 1].toInt() and 0xFF) / 255f
                val r = (pixels[di + 2].toInt() and 0xFF) / 255f
                val a = (pixels[di + 3].toInt() and 0xFF) / 255f
                val vi = p * 4
                sp.vals[vi] = r
                sp.vals[vi + 1] = g
                sp.vals[vi + 2] = b
                sp.vals[vi + 3] = a
            }
            return sp
        }
    }
}

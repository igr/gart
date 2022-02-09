package ac.obl.gart.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.random.Random

private const val YWRAPB = 4
private const val WRAP = 1 shl YWRAPB
private const val ZWRAPB = 8
private const val ZWRAP = 1 shl ZWRAPB

class PerlinNoise(
    // default to medium smooth
    private val perlinOctaves: Int = 4,
    // 50% reduction/octave
    private val perlinAmpFalloff: Float = 0.5f
) {

    private val perlinSize = 4095

    private fun scaledCosine(i: Float): Float {
        return 0.5f * (1.0f - cos(i * Math.PI)).toFloat()
    }

    private val perlin = FloatArray(perlinSize + 1) { Random.nextFloat() }

    /**
     * Generate noise.
     */
    fun noise(x: Number, y: Number = 0, z: Number = 0): Float {
        val xAbs = abs(x.toFloat())
        val yAbs = abs(y.toFloat())
        val zAbs = abs(z.toFloat())

        var xi = floor(xAbs).toInt()
        var yi = floor(yAbs).toInt()
        var zi = floor(zAbs).toInt()
        var xf = xAbs - xi
        var yf = yAbs - yi
        var zf = zAbs - zi

        var ampl = 0.5f
        var r = 0.0f

        for (o in 0 until perlinOctaves) {
            var of = xi + (yi shl YWRAPB) + (zi shl ZWRAPB)

            val rxf = scaledCosine(xf)
            val ryf = scaledCosine(yf)

            var n1 = perlin[of and perlinSize]
            n1 += rxf * (perlin[(of + 1) and perlinSize] - n1)
            var n2 = perlin[(of + WRAP) and perlinSize]
            n2 += rxf * (perlin[(of + WRAP + 1) and perlinSize] - n2)
            n1 += ryf * (n2 - n1)

            of += ZWRAP
            n2 = perlin[of and perlinSize]
            n2 += rxf * (perlin[(of + 1) and perlinSize] - n2)
            var n3 = perlin[(of + WRAP) and perlinSize]
            n3 += rxf * (perlin[(of + WRAP + 1) and perlinSize] - n3)
            n2 += ryf * (n3 - n2)

            n1 += scaledCosine(zf) * (n2 - n1)

            r += n1 * ampl
            ampl *= perlinAmpFalloff
            xi = xi shl 1
            xf *= 2
            yi = yi shl 1
            yf *= 2
            zi = zi shl 1
            zf *= 2

            if (xf >= 1.0) {
                xi++
                xf--
            }
            if (yf >= 1.0) {
                yi++
                yf--
            }
            if (zf >= 1.0) {
                zi++
                zf--
            }
        }
        return r
    }
}

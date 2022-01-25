package ac.obl.gart.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.random.Random

// todo make an object and use bellow as arguments

const val PERLIN_YWRAPB = 4
const val PERLIN_YWRAP = 1 shl PERLIN_YWRAPB
const val PERLIN_ZWRAPB = 8
const val PERLIN_ZWRAP = 1 shl PERLIN_ZWRAPB
const val PERLIN_SIZE = 4095
const val perlin_octaves = 4            // default to medium smooth
const val perlin_amp_falloff = 0.5f     // 50% reduction/octave

private fun scaledCosine(i: Float): Float {
	return 0.5f * (1.0f - cos(i * Math.PI)).toFloat()
}
private val perlin = FloatArray(PERLIN_SIZE + 1) { Random.nextFloat() }

fun perlinNoise(_x: Number, _y: Number = 0, _z: Number = 0): Float {
	val x = abs(_x.toFloat())
	val y = abs(_y.toFloat())
	val z = abs(_z.toFloat())

	var xi = floor(x).toInt()
	var yi = floor(y).toInt()
	var zi = floor(z).toInt()
	var xf = x - xi
	var yf = y - yi
	var zf = z - zi

	var ampl = 0.5f
	var r = 0.0f

	for (o in 0 until  perlin_octaves) {
		var of = xi + (yi shl PERLIN_YWRAPB) + (zi shl PERLIN_ZWRAPB)

		val rxf = scaledCosine(xf)
		val ryf = scaledCosine(yf)

		var n1 = perlin[of and PERLIN_SIZE]
		n1 += rxf * (perlin[(of + 1) and PERLIN_SIZE] - n1)
		var n2 = perlin[(of + PERLIN_YWRAP) and PERLIN_SIZE]
		n2 += rxf * (perlin[(of + PERLIN_YWRAP + 1) and PERLIN_SIZE] - n2)
		n1 += ryf * (n2 - n1)

		of += PERLIN_ZWRAP
		n2 = perlin[of and PERLIN_SIZE]
		n2 += rxf * (perlin[(of + 1) and PERLIN_SIZE] - n2)
		var n3 = perlin[(of + PERLIN_YWRAP) and PERLIN_SIZE]
		n3 += rxf * (perlin[(of + PERLIN_YWRAP + 1) and PERLIN_SIZE] - n3)
		n2 += ryf * (n3 - n2)

		n1 += scaledCosine(zf) * (n2 - n1)

		r += n1 * ampl
		ampl *= perlin_amp_falloff
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
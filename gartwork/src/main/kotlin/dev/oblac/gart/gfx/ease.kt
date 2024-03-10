package dev.oblac.gart.gfx

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

interface EaseFn : (Float) -> Float {
    override fun invoke(x: Float): Float

    companion object {
        val Linear = object : EaseFn {
            override fun invoke(x: Float) = x
        }
        val QuadIn = object : EaseFn {
            override fun invoke(x: Float) = x * x
        }
        val QuadOut = object : EaseFn {
            override fun invoke(x: Float) = x * (2 - x)
        }
        val QuadInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x < 0.5f) 2 * x * x else -1 + (4 - 2 * x) * x
        }
        val CubicIn = object : EaseFn {
            override fun invoke(x: Float) = x * x * x
        }
        val CubicOut = object : EaseFn {
            override fun invoke(x: Float) = 1 + (x - 1) * x * x
        }
        val CubicInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x < 0.5f) 4 * x * x * x else 1 + (2 * x - 2) * x * x
        }
        val QuartIn = object : EaseFn {
            override fun invoke(x: Float) = x * x * x * x
        }
        val QuartOut = object : EaseFn {
            override fun invoke(x: Float) = 1 - (x - 1) * x * x * x
        }
        val QuartInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x < 0.5f) 8 * x * x * x * x else 1 - 8 * (x - 1) * x * x * x
        }
        val SineIn = object : EaseFn {
            override fun invoke(x: Float) = 1 - cos(x * Math.PI / 2).toFloat()
        }
        val SineOut = object : EaseFn {
            override fun invoke(x: Float) = sin(x * Math.PI / 2).toFloat()
        }
        val SineInOut = object : EaseFn {
            override fun invoke(x: Float) = (-(cos(Math.PI * x) - 1) / 2).toFloat()
        }
        val ExpoIn = object : EaseFn {
            override fun invoke(x: Float) = if (x == 0f) 0f else 2.0.pow(10.0 * (x - 1)).toFloat()
        }
        val ExpoOut = object : EaseFn {
            override fun invoke(x: Float) = if (x == 1f) 1f else 1 - 2.0.pow(-10.0 * x).toFloat()
        }
        val ExpoInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x == 0f || x == 1f) x else if (x < 0.5f) 2.0.pow(20.0 * x - 10).toFloat() / 2 else (2 - 2.0.pow(-20.0 * x + 10)).toFloat() / 2
        }
        val CircIn = object : EaseFn {
            override fun invoke(x: Float) = 1 - sqrt(1 - x * x)
        }
        val CircOut = object : EaseFn {
            override fun invoke(x: Float) = sqrt(1 - (x - 1) * (x - 1))
        }
        val CircInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x < 0.5f) (1 - sqrt(1 - 4 * x * x)) / 2 else (sqrt(-((2 * x - 3) * (2 * x - 1)) + 1) + 1) / 2
        }
        val BackIn = object : EaseFn {
            override fun invoke(x: Float) = (x * x * x - x * sin(x * Math.PI)).toFloat()
        }
        val ElasticIn = object : EaseFn {
            override fun invoke(x: Float): Float {
                val c4 = (2 * Math.PI / 3).toFloat()
                return if (x == 0f) 0f else if (x == 1f) 1f else -2.0.pow(10.0 * x - 10).toFloat() * sin((x * 10 - 10.75) * c4).toFloat()
            }
        }
        val ElasticOut = object : EaseFn {
            override fun invoke(x: Float): Float {
                val c4 = (2 * Math.PI / 3).toFloat()
                return if (x == 0f) 0f else if (x == 1f) 1f else 2.0.pow(-10.0 * x).toFloat() * sin((x * 10 - 0.75) * c4).toFloat() + 1
            }
        }
        val ElasticInOut = object : EaseFn {
            override fun invoke(x: Float): Float {
                val c5 = (2 * Math.PI / 4.5).toFloat()
                return if (x == 0f) 0f else if (x == 1f) 1f else if (x < 0.5f) -(2.0.pow(20.0 * x - 10).toFloat() * sin((20 * x - 11.125) * c5).toFloat()) / 2 else 2.0.pow(-20.0 * x + 10).toFloat() * sin((20 * x - 11.125) * c5).toFloat() / 2 + 1
            }
        }
        val BounceIn = object : EaseFn {
            override fun invoke(x: Float) = 1 - BounceOut(1 - x)
        }
        val BounceOut = object : EaseFn {
            override fun invoke(x: Float): Float {
                val n1 = 7.5625f
                val d1 = 2.75f
                return if (x < 1 / d1) n1 * x * x else if (x < 2 / d1) n1 * (x - 1.5f / d1) * (x - 1.5f / d1) + 0.75f else if (x < 2.5 / d1) n1 * (x - 2.25f / d1) * (x - 2.25f / d1) + 0.9375f else n1 * (x - 2.625f / d1) * (x - 2.625f / d1) + 0.984375f
            }
        }
        val BounceInOut = object : EaseFn {
            override fun invoke(x: Float) = if (x < 0.5f) (1 - BounceOut(1 - 2 * x)) / 2 else (1 + BounceOut(2 * x - 1)) / 2

        }
    }
}

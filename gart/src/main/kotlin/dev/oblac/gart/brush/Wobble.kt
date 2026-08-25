package dev.oblac.gart.brush

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.math.TAUf
import dev.oblac.gart.math.hash01
import dev.oblac.gart.math.rndf
import dev.oblac.gart.noise.SimplexNoise
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/**
 * A field that bends brush strokes off their path.
 *
 * At every step the walker asks the wobble for an angle at the spot the hand is at right now,
 * turns the step direction by it, and keeps the drift. Nothing pulls the stroke back, so a long
 * path can end up well off where it was headed; that is the point, a hand never quite holds its
 * line either and that is where the hand-drawn look comes from. Small angles (a few degrees)
 * make a line tremble, large ones bend it.
 */
fun interface Wobble {
    /** Angle to add to the stroke direction at `(x, y)`, radians. */
    fun at(x: Float, y: Float): Float

    companion object {
        /**
         * Hand tremor: one random angle in `±amount` per [cell] px grid cell. The angle jumps
         * from cell to cell, which integrates into the wavy line of a quick sketch.
         */
        fun hand(rnd: Random, amount: Float = Degrees(3f).radians, cell: Float = 10f): Wobble {
            val seed = rnd.nextInt()
            return Wobble { x, y ->
                (hash01(floor(x / cell).toInt(), floor(y / cell).toInt(), 0, seed) * 2f - 1f) * amount
            }
        }

        /**
         * Smooth bends: simplex noise over the canvas, `±amount` radians, features about
         * `1 / scale` px across.
         */
        fun curved(rnd: Random, amount: Float = 0.5f, scale: Float = 0.004f): Wobble {
            val ox = rnd.rndf(-1000f, 1000f)
            val oy = rnd.rndf(-1000f, 1000f)
            return Wobble { x, y -> amount * SimplexNoise.noise(x * scale + ox, y * scale + oy) }
        }

        /** Regular waves across the canvas, `±amount` radians. */
        fun waves(rnd: Random, amount: Float = 0.4f, fx: Float = 0.05f, fy: Float = 0.02f): Wobble {
            val px = rnd.rndf(0f, TAUf)
            val py = rnd.rndf(0f, TAUf)
            return Wobble { x, y -> amount * sin(x * fx + px) * cos(y * fy + py) }
        }
    }
}

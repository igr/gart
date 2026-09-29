package dev.oblac.gart.flow2

import dev.oblac.gart.vector.MutableVec2
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import dev.oblac.gart.noise.curl as curlAt

/**
 * A vector at every point of the plane, in canvas coordinates: x grows to the right, y grows down.
 *
 * [at] writes the vector into `out` and allocates nothing, so a tracer can call it at every
 * step. Use [invoke] outside of hot loops, where a new [Vec2] per call does not matter.
 *
 * A field is a plain function and it is sampled exactly where it is asked. When the function is
 * slow and a tracer asks it many times, [bake] it onto a grid first.
 *
 * The length of the vectors is up to the field. The tracers use only the direction and take
 * the step length as their own parameter.
 */
fun interface VectorField {

    /** Writes the vector at (`x`, `y`) into [out]. */
    fun at(x: Float, y: Float, out: MutableVec2)

    /** The vector at [p], as a new [Vec2]. */
    operator fun invoke(p: Point): Vec2 {
        val out = MutableVec2()
        at(p.x, p.y, out)
        return out.toVec2()
    }

    companion object {

        /**
         * Unit vectors at the [angle] given for each point, in radians. 0 points right and a
         * quarter turn points down the canvas, the same as [Vec2.angle].
         */
        fun angles(angle: (x: Float, y: Float) -> Float) = VectorField { x, y, out ->
            val a = angle(x, y)
            out.set(cos(a), sin(a))
        }

        /**
         * The curl of a scalar [potential], `(dp/dy, -dp/dx)`. The vectors run along the contour
         * lines of the potential, so the field has no sources and no sinks and lines drawn
         * through it do not bunch up. Its streamlines are closed loops around each high and
         * each low of the potential.
         *
         * [eps] is the half-step of the central difference, in the coordinates of the potential.
         * A large step gives a smoothed curl, see [dev.oblac.gart.noise.curl].
         */
        fun curl(potential: (x: Float, y: Float) -> Float, eps: Float = 1f) =
            VectorField { x, y, out -> curlAt(x, y, out, eps, potential) }

        /**
         * A whirl around (`cx`, `cy`). The [spin] part turns around the centre, clockwise on
         * the screen when it is positive. The [pull] part points to the centre, or away from it
         * when it is negative. Each is the length of its part, so spin 1 and pull 0 give unit
         * vectors on circles.
         *
         * [reach] fades the whirl with the distance r, by `1 / (1 + r² / reach²)`. Use it when
         * you add vortices together, so that each one keeps to its own area. The default reach
         * never fades. The vector is zero at the centre.
         */
        fun vortex(
            cx: Float,
            cy: Float,
            spin: Float = 1f,
            pull: Float = 0f,
            reach: Float = Float.POSITIVE_INFINITY,
        ): VectorField {
            val reach2 = reach * reach
            return VectorField { x, y, out ->
                val dx = x - cx
                val dy = y - cy
                val r2 = dx * dx + dy * dy
                if (r2 == 0f) {
                    out.zero()
                } else {
                    val w = 1f / (1f + r2 / reach2) / sqrt(r2)
                    out.set((-dy * spin - dx * pull) * w, (dx * spin - dy * pull) * w)
                }
            }
        }
    }
}

/** The sum of two fields. */
operator fun VectorField.plus(other: VectorField) = VectorField { x, y, out ->
    this@plus.at(x, y, out)
    val ax = out.x
    val ay = out.y
    other.at(x, y, out)
    out.add(ax, ay)
}

/** The field with each vector scaled by [k]. */
operator fun VectorField.times(k: Float) = VectorField { x, y, out ->
    this@times.at(x, y, out)
    out *= k
}

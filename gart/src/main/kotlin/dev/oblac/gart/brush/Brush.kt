package dev.oblac.gart.brush

import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * A brush: what goes down at every step along a path, and how it varies.
 *
 * Stroking walks the path in [spacing]-sized steps and puts a [tip] down at each one, so a line
 * is really thousands of tiny marks. [weight], [scatter] and [spacing] are px at size 1; the
 * `size` given when drawing scales all three together, a uniform zoom of the brush. At size 1 a
 * [Brushes.pen] is a hairline and a [Brushes.pastel] already a fat smear; sketch lines sit
 * around 2 to 4.
 *
 * Every random decision (skipped steps, wander, dot size, alpha) goes through the `Random` the
 * draw call gets, so a seeded piece renders the same stroke twice.
 *
 * @property tip       what one stamp is
 * @property weight    diameter of one dot / stamp, px at size 1
 * @property scatter   how far a dot may wander off the path, px at size 1
 * @property sharpness 0..1; 1 keeps the wander even, 0 lets it spike (gaussian, worse under light pressure)
 * @property grain     chance a step actually lands a dot, times pressure; 1 or more never skips
 * @property opacity   alpha of one stamp, 0..1; strokes build up from many faint marks
 * @property spacing   step between stamps, px at size 1
 * @property pressure  how pressure runs along a stroke; scales dot size, alpha and wander
 */
data class Brush(
    val tip: Tip = Tip.Dots,
    val weight: Float,
    val scatter: Float,
    val sharpness: Float = 1f,
    val grain: Float = 1f,
    val opacity: Float,
    val spacing: Float,
    val pressure: Pressure = Pressure.Bell(),
) {
    init {
        require(weight > 0f) { "weight must be > 0" }
        require(scatter >= 0f) { "scatter must be >= 0" }
        require(sharpness in 0f..1f) { "sharpness must be in 0..1" }
        require(grain > 0f) { "grain must be > 0" }
        require(opacity in 0f..1f) { "opacity must be in 0..1" }
        require(spacing > 0f) { "spacing must be > 0" }
    }
}

/**
 * What one stamp of a [Brush] is.
 */
sealed interface Tip {
    /** A scatter of small dots around the path: pencils, pens, charcoal. */
    data object Dots : Tip

    /**
     * [specks] tiny dots thrown inside a disc of radius `scatter` at every step, more of them
     * under light pressure.
     */
    data class Spray(val specks: Int = 40) : Tip {
        init {
            require(specks > 0) { "specks must be > 0" }
        }
    }

    /** One soft disc of diameter `weight`, stacking up with its neighbours: felt markers. */
    data object Marker : Tip

    /**
     * An image stamped along the path, scaled so its longer side is `weight` px (at size 1).
     * The image's alpha channel is the shape; the stroke colour is the ink.
     */
    class Image(val image: org.jetbrains.skia.Image, val rotate: Rotate = Rotate.NATURAL) : Tip

    /**
     * Anything you can draw. [draw] is called per stamp with the canvas transformed so the unit
     * box `[-0.5, 0.5]²` is the stamp (scaled to `weight`, rotated per [rotate]), and a paint
     * that already carries the stroke colour and this stamp's alpha.
     */
    class Custom(val rotate: Rotate = Rotate.NATURAL, val draw: (Canvas, Paint) -> Unit) : Tip
}

/** How an image or custom tip is turned at each stamp. */
enum class Rotate {
    /** Never. */
    NONE,

    /** Along the stroke direction (wobble included). */
    NATURAL,

    /** A fresh random angle per stamp. */
    RANDOM,
}

/**
 * How pressure runs along a stroke. Pressure scales the dot size (squared), the stamp alpha
 * and, for soft brushes, the wander off the path.
 */
sealed interface Pressure {
    /**
     * Simulated pressure: a randomised bell along the stroke. [peak] is what it reaches near
     * the middle, [ends] what it falls to where the stroke lifts off; the landing end only gets
     * part of the way there, a stroke lands slower than it lifts. The peak sits at
     * `0.5 ± drift` of the length and the bell is about `1 - spread` of the length wide, both
     * re-rolled per stroke, so no two strokes match. Ends heavier than the middle is the usual
     * setting: the hand slows down landing and lifting and the ink pools there.
     */
    data class Bell(
        val ends: Float = 1.2f,
        val peak: Float = 1f,
        val drift: Float = 0.15f,
        val spread: Float = 0.2f,
    ) : Pressure

    /** Your own profile over `t` in 0..1 along the stroke, 1 being nominal. */
    class Curve(val fn: (t: Float) -> Float) : Pressure

    companion object {
        /** Constant pressure 1. */
        val Flat: Pressure = Curve { 1f }
    }
}

/**
 * Fixes the per-stroke randoms and returns the pressure as a function of the distance along a
 * stroke of [length].
 */
internal fun Pressure.along(rnd: Random, length: Float): (Float) -> Float = when (this) {
    is Pressure.Curve -> { s -> fn(if (length > 0f) (s / length).coerceIn(0f, 1f) else 0f) }
    is Pressure.Bell -> {
        val a = 0.5f + drift * rnd.rndf(-1f, 1f)
        val b = 1f - spread * rnd.rndf(1f, 1.5f)
        val c = rnd.rndf(3f, 3.5f)
        val peakAt = a * length
        val fn: (Float) -> Float = { s ->
            // a super-gaussian: flat on top, steep flanks. the flank before the peak is wider
            // than the one after, the stroke lands slower than it lifts
            val hw = abs(if (s < peakAt) 1.2f * b else 0.8f * b) * length / 2f
            val v = if (hw <= 0f) 0f else 1f / (1f + abs((s - peakAt) / hw).pow(2f * c))
            ends + v * (peak - ends)
        }
        fn
    }
}

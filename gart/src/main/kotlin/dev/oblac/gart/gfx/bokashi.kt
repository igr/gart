package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import dev.oblac.gart.color.alpha
import dev.oblac.gart.color.gradientOf
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import org.jetbrains.skia.Shader
import kotlin.math.abs
import kotlin.math.pow

/**
 * Bokashi - the graded wash of a woodblock print: one colour wiped onto the block along an edge
 * rather than cut into it, full at [y0] and gone by [y1]. A sky fades down from the top edge,
 * deep water darkens up from the bottom; either way round works, [y1] just sits on the other
 * side of [y0].
 *
 * Lay it with [Canvas.drawBokashi]; put [Canvas.drawBokashiScreen] over it if the grade should
 * read as printed rather than airbrushed.
 *
 * @property color    the wash colour. Its own alpha is ignored - [strength] sets it
 * @property y0       where the wash is full
 * @property y1       where it has let go
 * @property strength 0..1, opacity at [y0]
 * @property fall     power of the fade. 1 is linear, high hugs the edge, low keeps colour most of the way to [y1]
 */
data class Bokashi(
    val color: Int,
    val y0: Float,
    val y1: Float,
    val strength: Float = 1f,
    val fall: Float = 1.7f,
) {
    init {
        require(strength in 0f..1f) { "strength must be in 0..1" }
        require(fall > 0f) { "fall must be > 0" }
    }

    // signed, and never under a pixel, so a wash with y1 == y0 cant divide by zero
    private val span: Float = (y1 - y0).let { if (abs(it) < 1f) (if (it < 0f) -1f else 1f) else it }

    /** The fade for a position [t] along the wash: 1 at [y0] (`t = 0`), 0 at [y1] (`t = 1`) and beyond. */
    fun fade(t: Float): Float = (1f - t.coerceIn(0f, 1f)).pow(fall)

    /**
     * The fade at [y]. [waver] scales the distance the wash runs over, so where it gives out
     * can wander across the page the way a hand-wiped one does: 1 is a ruler-straight edge,
     * 1.3 lets go 30% further along.
     */
    fun at(y: Float, waver: Float = 1f): Float = fade((y - y0) / (span * waver))
}

/**
 * Lays [wash] into [region] as a vertical gradient, full at [Bokashi.y0] and clear at
 * [Bokashi.y1]. The fade is sampled at [stops] points and linear between them - skia only does
 * straight ramps, so the power curve is a polyline, and seven stops is already smooth to the eye.
 */
fun Canvas.drawBokashi(region: Path, wash: Bokashi, stops: Int = 7) {
    if (wash.strength <= 0f) return
    require(stops >= 2) { "stops must be >= 2" }
    val ramp = IntArray(stops) {
        val a = (wash.fade(it / (stops - 1f)) * wash.strength * 255f).toInt().coerceIn(0, 255)
        wash.color.alpha(a)
    }
    drawPath(
        region,
        paint().apply {
            shader = Shader.makeLinearGradient(
                0f, wash.y0, 0f, wash.y1,
                gradientOf(ramp, FloatArray(stops) { it / (stops - 1f) }),
            )
        },
    )
}

/**
 * A dot screen following [wash]: the same fade, but carried by dot size instead of opacity, so
 * the grade reads as something that went through a press. Usually drawn over [drawBokashi] in
 * the wash colour; on its own it is a halftone of the wash.
 *
 * [pitch], [angle], [strength] and [origin] are the screen, see [drawDotScreen]. [waver] gives
 * the span multiplier at a given x (see [Bokashi.at]), so the edge of the fade can wander across
 * the page; the screen is what the eye reads the fade off, so it is enough to waver just this.
 */
fun Canvas.drawBokashiScreen(
    region: Path,
    wash: Bokashi,
    paint: Paint,
    pitch: Float,
    angle: Angle = Radians(0f),
    strength: Float = 1f,
    origin: Point = Point(region.bounds.left, wash.y0),
    waver: (x: Float) -> Float = { 1f },
) = drawDotScreen(region, paint, pitch, angle, strength, origin) { x, y -> wash.at(y, waver(x)) }

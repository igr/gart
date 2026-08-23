package dev.oblac.gart.gfx

import dev.oblac.gart.angle.Angle
import dev.oblac.gart.angle.Radians
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.Point
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A dot screen over [region]: a square lattice of dots [pitch] px apart, tipped by [angle], each
 * dot sized so that its *area* follows [tone] at its centre (hence the square root on the
 * radius). The analytic cousin of the image-based `halftone` package - the tone is a function,
 * not a bitmap, so it draws straight into the canvas as vector circles.
 *
 * [strength] is the dot diameter at full tone as a fraction of [pitch]: 1 touches the neighbours,
 * above it the dots merge into a solid. [origin] anchors the lattice (a dot sits exactly there);
 * keep it fixed while tuning so the screen doesn't swim. Dots under 0.12 px are skipped.
 *
 * The lattice is not set square to the page by default for a reason - a screen aligned with a
 * rectangular frame beats visibly against everything else on it. Pick something like 0.48 rad.
 */
fun Canvas.drawDotScreen(
    region: Path,
    paint: Paint,
    pitch: Float,
    angle: Angle = Radians(0f),
    strength: Float = 1f,
    origin: Point = Point(region.bounds.left, region.bounds.top),
    tone: (x: Float, y: Float) -> Float,
) {
    if (strength <= 0f || pitch <= 0f) return
    val bounds = region.bounds
    val ca = cos(angle.radians)
    val sa = sin(angle.radians)
    val reach = (hypot(bounds.width, bounds.height) / pitch).toInt() + 2

    save()
    clipPath(region)
    for (j in -reach..reach) {
        for (i in -reach..reach) {
            val u = i * pitch
            val v = j * pitch
            val x = origin.x + u * ca - v * sa
            val y = origin.y + u * sa + v * ca
            if (x < bounds.left - 2f || x > bounds.right + 2f || y < bounds.top - 2f || y > bounds.bottom + 2f) continue
            val r = sqrt(tone(x, y).coerceIn(0f, 1f)) * pitch * 0.5f * strength
            if (r > 0.12f) drawCircle(x, y, r, paint)
        }
    }
    restore()
}

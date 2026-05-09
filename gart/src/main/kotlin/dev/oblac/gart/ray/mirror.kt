package dev.oblac.gart.ray

import dev.oblac.gart.gfx.DLine
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.intersectionOf
import dev.oblac.gart.vector.Vec2
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.abs

data class Mirror(
    val p1: Point,
    val p2: Point,
    val reflectivity: Float = 1.0f
) {
    val line = Line(p1, p2)

    fun reflect(ray: Ray): Pair<Point, Ray>? {
        // Convert the ray's DLine to a Line for intersection calculation
        // Use a large distance to simulate an infinite ray
        val rayLine = ray.dline.toLine(ray.dline.p, 10000f)

        // Find intersection point between ray and mirror
        val intersectionPoint = intersectionOf(rayLine, line) ?: return null

        // Calculate mirror normal vector (perpendicular to mirror line)
        val mirrorVector = Vec2(line.b.x - line.a.x, line.b.y - line.a.y).normalize()
        val normalVector = Vec2(-mirrorVector.y, mirrorVector.x) // Perpendicular to mirror

        // Get incident ray direction (normalized)
        val incidentDirection = ray.dline.dvec.normalize()

        // Calculate reflected direction using: R = I - 2(I·N)N
        // where I is incident direction, N is normal, R is reflected direction
        val dotProduct = incidentDirection.dot(normalVector)
        val reflectedDirection = incidentDirection - (normalVector * (2f * dotProduct))

        // Create new reflected ray starting from intersection point
        val reflectedRay = Ray(
            dline = DLine(intersectionPoint, reflectedDirection),
            intensity = ray.intensity * reflectivity
        )

        return intersectionPoint to reflectedRay
    }
}

/**
 * Builds a [Path] connecting consecutive mirror segments. When the next mirror's
 * start point matches the previous mirror's end point, segments are joined into
 * a continuous sub-path; otherwise a new sub-path is started.
 */
fun List<Mirror>.toPath(): Path {
    if (isEmpty()) return Path()
    val builder = PathBuilder().moveTo(first().p1)
    var prev = first().p1
    val eps = 0.5f
    for (m in this) {
        if (abs(m.p1.x - prev.x) > eps || abs(m.p1.y - prev.y) > eps) {
            builder.moveTo(m.p1)
        }
        builder.lineTo(m.p2)
        prev = m.p2
    }
    return builder.detach()
}

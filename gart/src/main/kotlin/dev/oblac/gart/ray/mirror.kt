package dev.oblac.gart.ray

import dev.oblac.gart.gfx.DLine
import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.intersectionOf
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Point

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
        val mirrorVector = Vector2(line.b.x - line.a.x, line.b.y - line.a.y).normalize()
        val normalVector = Vector2(-mirrorVector.y, mirrorVector.x) // Perpendicular to mirror

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

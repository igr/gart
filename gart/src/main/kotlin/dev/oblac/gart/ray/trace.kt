package dev.oblac.gart.ray

import dev.oblac.gart.gfx.intersectionOf
import org.jetbrains.skia.Point

fun traceRayWithReflections(ray: Ray, mirrors: List<Mirror>, maxReflections: Int): List<RayTrace> {
    val result = mutableListOf<RayTrace>()
    var currentRay = ray
    var currentFrom = ray.dline.p

    for (iteration in 0..(maxReflections)) {
        // Find the closest mirror that intersects with the current ray
        var closestMirror: Mirror? = null
        var closestIntersection: Point? = null
        var shortestDistance = Float.MAX_VALUE

        mirrors.forEach { mirror ->
            val intersection = intersectionOf(currentRay.dline, mirror.line)

            if (intersection != null) {
                // Calculate distance from ray origin to intersection
                val dx = intersection.x - currentRay.dline.p.x
                val dy = intersection.y - currentRay.dline.p.y
                val distance = dx * dx + dy * dy // squared distance for comparison

                if (distance > 1f && distance < shortestDistance) { // > 1f to avoid self-intersection
                    shortestDistance = distance
                    closestMirror = mirror
                    closestIntersection = intersection
                }
            }
        }

        // Create RayTrace for current ray segment
        val rayTrace = RayTrace(
            iteration = iteration,
            ray = currentRay,
            from = currentFrom,
            to = closestIntersection
        )
        result.add(rayTrace)

        // If we found a mirror to reflect off and we haven't reached max reflections, create the reflected ray
        if (closestMirror != null && closestIntersection != null && iteration < maxReflections) {
            val reflected = closestMirror.reflect(currentRay)
            val reflectedRay = reflected?.second
            if (reflectedRay != null && reflectedRay.intensity > 0.01) { // Minimum intensity threshold
                currentRay = reflectedRay
                currentFrom = closestIntersection
            } else {
                break // Stop tracing if ray is too weak
            }
        } else {
            break // Stop tracing if no intersection found or max reflections reached
        }
    }

    return result
}

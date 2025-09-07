package dev.oblac.gart.rayz.mirror

import dev.oblac.gart.*
import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.color.RetroColors
import dev.oblac.gart.gfx.*
import dev.oblac.gart.vector.Vector2
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Point

fun main() {
    val gart = Gart.of("mirrorz", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()
    val draw = MyDraw(g)

    // save image
    //g.draw(draw)
    //gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * Hot reload requires a real class to be created, not a lambda.
 */
private class MyDraw(val g: Gartvas) : Drawing(g) {
    val b = Gartmap(g)
    override fun draw(c: Canvas, d: Dimension, f: Frames) {
        draw(g.canvas, d)
        //b.updatePixelsFromCanvas()
        // draw pixels
        //b.drawToCanvas()
        c.draw(g)
    }
}


private fun draw(c: Canvas, d: Dimension) {
    c.clear(RetroColors.black01)


    val lightSource = Point(d.w3x2, d.h3x2)
    val rays = List(100) {
        val angle = it * (360f / 100f)
        Ray(DLine.of(lightSource, Degrees.of(angle)))
    }

    val mirrors = listOf(
        Mirror(Point(d.w / 4f, d.h / 4f), Point(d.w3x2, d.h / 4f)), // top
    )

    // Draw mirrors
    debugDrawMirrors(c, mirrors)

    // Trace rays with reflections
    val allRays = mutableListOf<Ray>()
    rays.forEach { ray ->
        allRays.addAll(traceRayWithReflections(ray, mirrors, maxReflections = 1))
    }

    // Draw all rays (original and reflected)
    debugDrawRays(c, allRays)
}

private fun debugDrawRays(c: Canvas, rays: List<Ray>) {
    rays.forEach { ray ->
        val alpha = (ray.intensity * 255).toInt().coerceIn(0, 255)
        val color = strokeOfWhite(1f).apply { this.alpha = alpha }
        c.drawLine(ray.dline.toLine(ray.dline.p, 1000f), color)
    }
}

private fun debugDrawMirrors(c: Canvas, mirrors: List<Mirror>) {
    mirrors.forEach { mirror ->
        c.drawLine(mirror.line, strokeOfRed(4f))
    }
}

private fun traceRayWithReflections(ray: Ray, mirrors: List<Mirror>, maxReflections: Int): List<Ray> {
    val result = mutableListOf<Ray>()
    var currentRay = ray
    
    result.add(currentRay) // Add the original ray
    
    repeat(maxReflections) {
        // Find the closest mirror that intersects with the current ray
        var closestMirror: Mirror? = null
        var shortestDistance = Float.MAX_VALUE
        
        mirrors.forEach { mirror ->
            val rayLine = currentRay.dline.toLine(currentRay.dline.p, 10000f)
            val intersection = intersectionOf(rayLine, mirror.line)
            
            if (intersection != null) {
                // Calculate distance from ray origin to intersection
                val dx = intersection.x - currentRay.dline.p.x
                val dy = intersection.y - currentRay.dline.p.y
                val distance = dx * dx + dy * dy // squared distance for comparison
                
                if (distance > 1f && distance < shortestDistance) { // > 1f to avoid self-intersection
                    shortestDistance = distance
                    closestMirror = mirror
                }
            }
        }
        
        // If we found a mirror to reflect off, create the reflected ray
        closestMirror?.let { mirror ->
            val reflected = mirror.reflect(currentRay)
            val reflectedRay = reflected?.second
            if (reflectedRay != null && reflectedRay.intensity > 0.01) { // Minimum intensity threshold
                result.add(reflectedRay)
                currentRay = reflectedRay
            } else {
                return@repeat // Stop tracing if ray is too weak
            }
        } ?: return@repeat // Stop tracing if no intersection found
    }
    
    return result
}

private data class Mirror(
    val p1: Point,
    val p2: Point,
    val reflectivity: Double = 1.0
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

private data class Ray(
    val dline: DLine,
    val intensity: Double = 1.0
)
private data class RayTrace(
    val ray: Ray,
    val from: Point,
    val to: Point?
)

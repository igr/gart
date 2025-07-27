package dev.oblac.gart.gfx

import dev.oblac.gart.math.rndb
import org.jetbrains.skia.Point
import kotlin.math.pow

/**
 * Applies the midpoint algorithm to the specified segment and
 * returns the obtained list of points.
 * Final number of points = (2^iterations)+1
 */
fun midpointDisplacementY(
    start: Point,
    end: Point,
    roughness: Float,
    verticalDisplacement: Float = (start.y + end.y) / 2f,
    numOfIterations: Int = 16
): List<Point> {
    var displacement = verticalDisplacement
    
    // always sorted from smallest to biggest x-value
    val points = mutableListOf(start, end)
    
    repeat(numOfIterations) {
        val pointsCopy = points.toList()
        
        for (i in 0 until pointsCopy.size - 1) {
            val midpointX = (pointsCopy[i].x + pointsCopy[i + 1].x) / 2f
            val midpointY = (pointsCopy[i].y + pointsCopy[i + 1].y) / 2f
            
            // displace midpoint y-coordinate
            val displacedY = midpointY + if (rndb()) displacement else -displacement
            val midpoint = Point(midpointX, displacedY)
            
            // insert the displaced midpoint maintaining x-order
            val insertIndex = points.indexOfFirst { it.x > midpoint.x }
            if (insertIndex == -1) {
                points.add(midpoint)
            } else {
                points.add(insertIndex, midpoint)
            }
        }
        
        // reduce displacement range
        displacement *= 2f.pow(-roughness)
    }
    
    return points
}

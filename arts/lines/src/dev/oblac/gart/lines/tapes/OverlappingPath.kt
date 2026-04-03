package dev.oblac.gart.lines.tapes

import dev.oblac.gart.gfx.Line
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates a random closed path with overlapping lines using Skiko Path.
 * Algorithm: Starting from one point, either jumps over the center to another point
 * or chooses a nearby point (creating short lines). This creates natural overlapping patterns.
 *
 * @param numPoints Number of points to generate around the perimeter
 * @param radius Base radius for point generation
 * @param centerX Center X coordinate
 * @param centerY Center Y coordinate
 * @param jumpProbability Probability of jumping over center vs choosing nearby point (0.0-1.0)
 * @param nearbyRange How many adjacent points to consider as "nearby" (1 = only adjacent, 2 = next two, etc.)
 * @param randomSeed Random seed for reproducible results
 * @return List of Line objects representing the overlapping closed path
 */
fun generateOverlappingClosedPath(
    numPoints: Int,
    radius: Float = 100f,
    centerX: Float = 0f,
    centerY: Float = 0f,
    jumpProbability: Float = 0.4f,
    nearbyRange: Int = 3,
    randomSeed: Long = System.currentTimeMillis()
): List<Line> {
    require(numPoints >= 3) { "Need at least 3 points to create a closed path" }
    require(jumpProbability in 0f..1f) { "Jump probability must be between 0.0 and 1.0" }
    require(nearbyRange >= 1) { "Nearby range must be at least 1" }

    val random = Random(randomSeed)
    val points = mutableListOf<Point>()

    // Generate points in a rough circle with random variation
    for (i in 0 until numPoints) {
        val angle = (2 * PI * i / numPoints).toFloat()
        val radiusVariation = radius * (0.6f + random.nextFloat() * 0.4f)
        val x = centerX + cos(angle) * radiusVariation
        val y = centerY + sin(angle) * radiusVariation
        points.add(Point(x, y))
    }

    val lines = mutableListOf<Line>()
    val visited = mutableSetOf<Int>()
    var currentIndex = 0

    visited.add(currentIndex)

    // Create overlapping path using the specified algorithm
    while (visited.size < numPoints) {
        val shouldJump = random.nextFloat() < jumpProbability
        val nextIndex = if (shouldJump) {
            // Jump over center: choose a point on the opposite side
            chooseOppositePoint(currentIndex, numPoints, visited, random)
        } else {
            // Choose nearby point
            chooseNearbyPoint(currentIndex, numPoints, nearbyRange, visited, random)
        }

        if (nextIndex != -1) {
            lines.add(Line(points[currentIndex], points[nextIndex]))
            visited.add(nextIndex)
            currentIndex = nextIndex
        } else {
            // If no valid nearby point, find any unvisited point
            val unvisited = (0 until numPoints).filter { it !in visited }
            if (unvisited.isNotEmpty()) {
                val randomUnvisited = unvisited[random.nextInt(unvisited.size)]
                lines.add(Line(points[currentIndex], points[randomUnvisited]))
                visited.add(randomUnvisited)
                currentIndex = randomUnvisited
            }
        }
    }

    // Close the path back to start
    lines.add(Line(points[currentIndex], points[0]))

    return lines
}

/**
 * Chooses a point on the opposite side of the circle (jump over center)
 */
private fun chooseOppositePoint(
    currentIndex: Int,
    numPoints: Int,
    visited: Set<Int>,
    random: Random
): Int {
    val oppositeIndex = (currentIndex + numPoints / 2) % numPoints
    val oppositeRange = maxOf(1, numPoints / 6) // Range around the opposite point

    val candidates = mutableListOf<Int>()
    for (i in -oppositeRange..oppositeRange) {
        val candidateIndex = (oppositeIndex + i + numPoints) % numPoints
        if (candidateIndex !in visited) {
            candidates.add(candidateIndex)
        }
    }

    return if (candidates.isNotEmpty()) {
        candidates[random.nextInt(candidates.size)]
    } else {
        -1 // No valid opposite point
    }
}

/**
 * Chooses a nearby point within the specified range
 */
private fun chooseNearbyPoint(
    currentIndex: Int,
    numPoints: Int,
    nearbyRange: Int,
    visited: Set<Int>,
    random: Random
): Int {
    val candidates = mutableListOf<Int>()

    // Check both directions from current point
    for (distance in 1..nearbyRange) {
        val leftIndex = (currentIndex - distance + numPoints) % numPoints
        val rightIndex = (currentIndex + distance) % numPoints

        if (leftIndex !in visited) candidates.add(leftIndex)
        if (rightIndex !in visited) candidates.add(rightIndex)
    }

    return if (candidates.isNotEmpty()) {
        candidates[random.nextInt(candidates.size)]
    } else {
        -1 // No valid nearby point
    }
}

/**
 * Alternative version that creates more aggressive overlapping by allowing revisits
 */
fun generateChaoticOverlappingPath(
    numPoints: Int,
    radius: Float = 100f,
    centerX: Float = 0f,
    centerY: Float = 0f,
    pathLength: Int = numPoints * 2, // How many line segments to draw
    jumpProbability: Float = 0.5f,
    randomSeed: Long = System.currentTimeMillis()
): Path {
    require(numPoints >= 3) { "Need at least 3 points to create a closed path" }
    require(jumpProbability in 0f..1f) { "Jump probability must be between 0.0 and 1.0" }

    val random = Random(randomSeed)
    val points = mutableListOf<Point>()

    // Generate points with more variation for chaotic effect
    for (i in 0 until numPoints) {
        val angle = (2 * PI * i / numPoints).toFloat() + random.nextFloat() * 0.3f - 0.15f
        val radiusVariation = radius * (0.4f + random.nextFloat() * 0.6f)
        val x = centerX + cos(angle) * radiusVariation
        val y = centerY + sin(angle) * radiusVariation
        points.add(Point(x, y))
    }

    val path = PathBuilder()
    var currentIndex = random.nextInt(numPoints)

    // Start the path
    path.moveTo(points[currentIndex].x, points[currentIndex].y)

    // Create overlapping path allowing revisits for more chaos
    repeat(pathLength) {
        val shouldJump = random.nextFloat() < jumpProbability
        val nextIndex = if (shouldJump) {
            // Jump over center or to random distant point
            if (random.nextFloat() < 0.7f) {
                (currentIndex + numPoints / 2 + random.nextInt(numPoints / 4) - numPoints / 8 + numPoints) % numPoints
            } else {
                random.nextInt(numPoints)
            }
        } else {
            // Choose nearby point (allowing revisits)
            val nearbyRange = 1 + random.nextInt(3)
            val direction = if (random.nextBoolean()) 1 else -1
            val distance = 1 + random.nextInt(nearbyRange)
            (currentIndex + direction * distance + numPoints) % numPoints
        }

        path.lineTo(points[nextIndex].x, points[nextIndex].y)
        currentIndex = nextIndex
    }

    // Close the path
    path.closePath()

    return path.detach()
}

package dev.oblac.gart.pixels.halftone

import kotlin.math.cos
import kotlin.math.sin

/**
 * Maps a value from one range to another range.
 * todo use existing library function
 */
fun map(value: Double, minA: Double, maxA: Double, minB: Double, maxB: Double): Double {
    return ((value - minA) / (maxA - minA)) * (maxB - minB) + minB
}

/**
 * Rotates a point around another position by the given angle.
 */
internal fun rotatePointAroundPosition(point: Pair<Double, Double>, rotationCenter: Pair<Double, Double>, angle: Double): Pair<Double, Double> {
    val (x, y) = point
    val (rotX, rotY) = rotationCenter
    
    val cosAngle = cos(angle)
    val sinAngle = sin(angle)
    
    val newX = (x - rotX) * cosAngle - (y - rotY) * sinAngle + rotX
    val newY = (x - rotX) * sinAngle + (y - rotY) * cosAngle + rotY
    
    return newX to newY
}

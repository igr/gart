package dev.obac.gart.box2d

import de.pirckheimer_gymnasium.jbox2d.dynamics.World
import org.jetbrains.skia.Point

/**
 * Returns all the particles in the world.
 */
fun World.particles(): List<Point> {
    val list = mutableListOf<Point>()
    val particleCount = this.particleCount
    val positions = this.particlePositionBuffer
    for (i in 0 until particleCount) {
        val x = (positions[i].x)
        val y = (positions[i].y)
        list.add(Point(x, y))
    }
    return list
}

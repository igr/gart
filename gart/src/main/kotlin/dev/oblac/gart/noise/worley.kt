package dev.oblac.gart.noise

import dev.oblac.gart.math.hash01
import dev.oblac.gart.vector.Vec3
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Worley (cellular) noise in 3D: the distance from [p] to the nearest feature point. Each unit
 * cell has one point, at a spot hashed from the cell and [seed]. The value is 0 on a point and
 * never more than √3, the diagonal of a cell. Scale [p] to set the size of the cells.
 */
fun worley(p: Vec3, seed: Int): Float {
    val xi = floor(p.x).toInt()
    val yi = floor(p.y).toInt()
    val zi = floor(p.z).toInt()
    var best = 9f
    for (k in zi - 1..zi + 1) for (j in yi - 1..yi + 1) for (i in xi - 1..xi + 1) {
        val d = Vec3(i + hash01(i, j, k, seed), j + hash01(i, j, k, seed + 1), k + hash01(i, j, k, seed + 2)) - p
        best = min(best, d.dot(d))
    }
    return sqrt(best)
}

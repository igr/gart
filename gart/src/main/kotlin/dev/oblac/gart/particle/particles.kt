package dev.oblac.gart.particle

import org.jetbrains.skia.Point

data class Particle(
    val point: Point,
    val vx: Double,
    val vy: Double,
    val yD: Int
)

data class Row(
    val yD: Int,
    val particles: List<Particle>
)

class ParticleSystem(
    val r: Float,
    val d: Float = r * 2
) {
    private var particles: MutableList<Particle> = mutableListOf()

    fun x() {
        val sorted = particles
            .map { it.copy(yD = (it.point.y / d).toInt()) }
            .sortedBy { it.yD }
            .groupByTo(mutableMapOf()) { it.yD }
            .entries
            .map { Row(it.key, it.value.sortedBy { p -> p.point.x }) }
    }
}

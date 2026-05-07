package dev.oblac.gart.walker

import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WalkerTest {

    @Test
    fun walkRandomStaysWithinDisc() {
        var prev = Point(0f, 0f)
        repeat(100) {
            val next = walkRandom(prev, 2f)
            val dx = next.x - prev.x; val dy = next.y - prev.y
            assertTrue(sqrt(dx * dx + dy * dy) <= 2f + 1e-4f)
            prev = next
        }
    }

    @Test
    fun walkMomentumProducesMotion() {
        var m = Momentum(Point(0f, 0f))
        repeat(20) { m = walkMomentum(m, accel = 0.2f, damping = 0.99f) }
        assertNotEquals(Point(0f, 0f), m.pos)
    }

    @Test
    fun walkRandom3DStaysWithinBall() {
        var prev = Vec3.ZERO
        repeat(100) {
            val next = walkRandom3D(prev, 1.5f)
            val dx = next.x - prev.x; val dy = next.y - prev.y; val dz = next.z - prev.z
            assertTrue(sqrt(dx * dx + dy * dy + dz * dz) <= 1.5f + 1e-4f)
            prev = next
        }
    }

    @Test
    fun walkMomentum3DProducesMotion() {
        var m = Momentum3D(Vec3.ZERO)
        repeat(30) { m = walkMomentum3D(m, accel = 0.1f) }
        assertNotEquals(Vec3.ZERO, m.pos)
    }
}

package dev.oblac.gart.flow2

import org.jetbrains.skia.Point
import org.jetbrains.skia.Rect
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TraceTest {

    // unit tangents of circles round the origin, clockwise on screen
    private val whirl = VectorField.vortex(0f, 0f)

    private fun oneStep(rk: Integrator) = whirl.trace(Point(100f, 0f), steps = 1, step = 10f, rk = rk)[1]

    @Test
    fun eulerStepsAlongTheTangentAtTheStart() {
        val p = oneStep(Integrator.EULER)
        assertEquals(100f, p.x, 1e-4f)
        assertEquals(10f, p.y, 1e-4f)
    }

    @Test
    fun rk2StepsAlongTheTangentAtTheMidpoint() {
        // the midpoint is (100, 5), its tangent (-5, 100) / 100.1249, times 10
        val p = oneStep(Integrator.RK2)
        assertEquals(99.50062f, p.x, 1e-4f)
        assertEquals(9.98752f, p.y, 1e-4f)
    }

    @Test
    fun rk4StaysOnTheCircle() {
        // an arc of 10 on a radius of 100 is 0.1 rad: (100 cos 0.1, 100 sin 0.1)
        val p = oneStep(Integrator.RK4)
        assertEquals(99.50042f, p.x, 1e-4f)
        assertEquals(9.98334f, p.y, 1e-4f)
    }

    @Test
    fun theStepIsTheStepNotTheVectorLength() {
        val fast = VectorField { _, _, out -> out.set(5f, 0f) }
        assertEquals(listOf(0f, 2f, 4f, 6f), fast.trace(Point(0f, 0f), steps = 3, step = 2f).map { it.x })
    }

    @Test
    fun aNegativeStepWalksAgainstTheFlow() {
        val right = VectorField { _, _, out -> out.set(1f, 0f) }
        assertEquals(listOf(3f, 2f, 1f), right.trace(Point(3f, 0f), steps = 2, step = -1f).map { it.x })
    }

    @Test
    fun theDefaultIntegratorHoldsACircle() {
        // 5000 px of arc on a radius of 100 is 50 rad: (100 cos 50, 100 sin 50)
        val path = whirl.trace(Point(100f, 0f), steps = 5000)
        assertEquals(5001, path.size)
        assertEquals(96.4966f, path.last().x, 0.05f)
        assertEquals(-26.2375f, path.last().y, 0.05f)
    }

    @Test
    fun aZeroVectorEndsThePath() {
        val wall = VectorField { x, _, out -> if (x < 5f) out.set(1f, 0f) else out.zero() }
        assertEquals(listOf(0f, 1f, 2f, 3f, 4f, 5f), wall.trace(Point(0f, 0f), steps = 100).map { it.x })
    }

    @Test
    fun aVectorThatIsNotANumberEndsThePath() {
        val broken = VectorField { _, _, out -> out.set(Float.NaN, 0f) }
        assertEquals(listOf(Point(1f, 2f)), broken.trace(Point(1f, 2f), steps = 10))
    }

    @Test
    fun withVelocityAStepIsTheVectorTimesTheStep() {
        val fast = VectorField { _, _, out -> out.set(5f, 0f) }
        assertEquals(listOf(0f, 2.5f, 5f), fast.trace(Point(0f, 0f), steps = 2, step = 0.5f, velocity = true).map { it.x })
    }

    @Test
    fun withVelocityAPathGoesFasterWhereTheVectorIsLonger() {
        val road = VectorField { x, _, out -> out.set(if (x < 10f) 1f else 3f, 0f) }
        val xs = road.trace(Point(0f, 0f), steps = 12, rk = Integrator.EULER, velocity = true).map { it.x }
        assertEquals(listOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 13f, 16f), xs)
    }

    @Test
    fun withVelocityRk4TurnsWithTheRotation() {
        // v = (-y, x) turns everything round the origin at 1 rad per unit of time, so 10 steps of 0.1 are 1 rad
        val spin = VectorField { x, y, out -> out.set(-y, x) }
        val p = spin.trace(Point(100f, 0f), steps = 10, step = 0.1f, rk = Integrator.RK4, velocity = true).last()
        assertEquals(54.0302f, p.x, 0.01f)
        assertEquals(84.1471f, p.y, 0.01f)
    }

    @Test
    fun rk4StopsWhereItsSamplesCancel() {
        // straight into a sink: within half a step of it the four samples point both ways and sum to nothing
        val sink = VectorField.vortex(50f, 50f, spin = 0f, pull = 1f)
        val path = sink.trace(Point(60.3f, 50f), steps = 100, rk = Integrator.RK4)
        assertEquals(11, path.size)
        assertEquals(50.3f, path.last().x, 1e-3f)
    }

    @Test
    fun aHugeVectorStillHasADirection() {
        // 1e20 squared is past the largest float
        val huge = VectorField { _, _, out -> out.set(1e20f, 0f) }
        assertEquals(listOf(0f, 1f, 2f), huge.trace(Point(0f, 0f), steps = 2).map { it.x })
    }

    @Test
    fun anInfiniteVectorEndsThePath() {
        val infinite = VectorField { _, _, out -> out.set(Float.POSITIVE_INFINITY, 0f) }
        assertEquals(listOf(Point(1f, 2f)), infinite.trace(Point(1f, 2f), steps = 5))
        assertEquals(listOf(Point(1f, 2f)), infinite.trace(Point(1f, 2f), steps = 5, velocity = true))
    }

    @Test
    fun withVelocityASlowStepIsStillAStep() {
        // rk2 from 0 takes the speed at the midpoint 0.5: e^-10, a legal and very small step
        val fading = VectorField { x, _, out -> out.set(kotlin.math.exp(-20f * x), 0f) }
        val path = fading.trace(Point(0f, 0f), steps = 1, velocity = true)
        assertEquals(2, path.size)
        assertEquals(4.54e-5f, path[1].x, 1e-7f)
    }

    @Test
    fun withVelocityRk4SumsItsSamplesWithoutOverflow() {
        // the four samples add up to 6e38 before the step scales them down, past the largest float
        val huge = VectorField { _, _, out -> out.set(1e38f, 0f) }
        val path = huge.trace(Point(0f, 0f), steps = 1, step = 1e-38f, rk = Integrator.RK4, velocity = true)
        assertEquals(2, path.size)
        assertEquals(1f, path[1].x, 1e-3f)
    }

    @Test
    fun withVelocityAStepThatGoesNowhereEndsThePath() {
        // unit speed into a sink: within half a step of it the four rk4 samples cancel exactly
        val sink = VectorField.vortex(50f, 50f, spin = 0f, pull = 1f)
        assertEquals(listOf(Point(50.3f, 50f)), sink.trace(Point(50.3f, 50f), steps = 100, rk = Integrator.RK4, velocity = true))
    }

    @Test
    fun thePathEndsBeforeItLeavesTheBounds() {
        val right = VectorField { _, _, out -> out.set(1f, 0f) }
        val path = right.trace(Point(0f, 5f), steps = 100, bounds = Rect(0f, 0f, 10f, 10f))
        assertEquals((0..9).map { it.toFloat() }, path.map { it.x })
    }
}

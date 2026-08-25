package dev.oblac.gart.colonization

import org.jetbrains.skia.Point
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpaceColonizationTest {

    private fun food(step: Float, w: Float, h: Float) = buildList {
        var y = step / 2
        while (y < h) {
            var x = step / 2
            while (x < w) {
                add(Point(x, y))
                x += step
            }
            y += step
        }
    }

    private fun sc(venation: Venation = Venation.OPEN, seed: Int = 7, jitter: Float = 0.1f, allowed: (Point) -> Boolean = { true }) =
        SpaceColonization(
            attractionDistance = 40f, killDistance = 6f, segmentLength = 4f,
            venation = venation, jitter = jitter, allowed = allowed, rnd = Random(seed),
        )

    @Test
    fun openVenationEatsEverythingAndGrowsATree() {
        val sc = sc()
        sc.addAttractors(food(20f, 200f, 200f))
        sc.addRoot(100f, 100f)
        sc.grow()
        assertTrue(sc.done)
        assertEquals(0, sc.attractorsLeft)
        assertTrue(sc.nodes.size > 50)
        sc.nodes.forEachIndexed { i, n ->
            assertEquals(i, n.index)
            n.parent?.let { assertTrue(it.index < i, "parent after child") }
        }
    }

    @Test
    fun closedVenationConvergesToo() {
        val sc = sc(venation = Venation.CLOSED)
        sc.addAttractors(food(25f, 160f, 160f))
        sc.addRoot(80f, 80f)
        sc.grow(maxSteps = 3_000)
        assertTrue(sc.done)
        assertEquals(0, sc.attractorsLeft)
        assertTrue(sc.nodes.size > 20)
    }

    @Test
    fun sameSeedSameTree() {
        fun run(seed: Int): List<Point> {
            val sc = sc(seed = seed)
            sc.addAttractors(food(20f, 150f, 150f))
            sc.addRoot(10f, 10f)
            sc.grow()
            return sc.nodes.map { it.position }
        }
        assertEquals(run(42), run(42))
        assertTrue(run(42) != run(43))
    }

    @Test
    fun foodNextToTheRootIsEatenWithoutGrowth() {
        val sc = sc()
        sc.addAttractor(3f, 0f) // inside kill distance already
        sc.addRoot(0f, 0f)
        sc.grow()
        assertEquals(0, sc.attractorsLeft)
        assertEquals(1, sc.nodes.size)
    }

    @Test
    fun closedVenationEatsFoodBornInsideTheKillZone() {
        val sc = sc(venation = Venation.CLOSED)
        sc.addAttractor(3f, 0f) // inside kill distance from the start
        sc.addRoot(0f, 0f)
        sc.grow()
        assertTrue(sc.done)
        assertEquals(0, sc.attractorsLeft)
        assertEquals(1, sc.nodes.size)
    }

    @Test
    fun longSegmentsDontPingPongOverASmallKillRadius() {
        // segment way bigger than kill: naive growth overshoots the food back and
        // forth forever. the step clamp lands on it instead
        val sc = SpaceColonization(
            attractionDistance = 40f, killDistance = 2f, segmentLength = 10f,
            jitter = 0f, rnd = Random(1),
        )
        sc.addAttractor(26f, 0f)
        sc.addRoot(0f, 0f)
        sc.grow()
        assertEquals(0, sc.attractorsLeft)
        assertTrue(sc.nodes.size < 10, "grew ${sc.nodes.size} nodes chasing one attractor")
    }

    @Test
    fun unreachableFoodStallsInsteadOfSpinning() {
        val sc = sc()
        sc.addAttractor(1000f, 1000f) // way out of the attraction radius
        sc.addRoot(0f, 0f)
        val steps = sc.grow()
        assertTrue(sc.done)
        assertTrue(steps <= 1)
        assertEquals(1, sc.attractorsLeft)
        assertEquals(1, sc.nodes.size)
    }

    @Test
    fun allowedPredicateFencesTheGrowth() {
        val sc = sc(allowed = { it.x <= 100f })
        sc.addAttractors(food(20f, 200f, 100f)) // food on both sides of the fence
        sc.addRoot(10f, 50f)
        sc.grow()
        sc.nodes.forEach { assertTrue(it.position.x <= 100f) }
        assertTrue(sc.attractorsLeft > 0, "food behind the fence stays uneaten")
    }

    @Test
    fun trunkEndsUpThickerThanTips() {
        val sc = sc()
        sc.addAttractors(food(20f, 200f, 200f))
        val root = sc.addRoot(100f, 100f)
        sc.grow()
        sc.canalize()
        val tip = sc.nodes.last { it.isTip }
        assertTrue(root.thickness > tip.thickness)
        assertEquals(0f, tip.thickness)
    }

    @Test
    fun jitterAloneCantEndTheRunAtAFence() {
        // narrow corridor + jitter that dwarfs the pull: most wiggled samples land
        // outside, the unjittered pull never does. the run must still reach the food
        val sc = SpaceColonization(
            attractionDistance = 200f, killDistance = 3f, segmentLength = 2f,
            jitter = 10f, allowed = { it.y in 48f..52f }, rnd = Random(5),
        )
        sc.addAttractor(150f, 50f)
        sc.addRoot(10f, 50f)
        sc.grow()
        assertEquals(0, sc.attractorsLeft)
    }

    @Test
    fun tipCountsAccumulateToTheRoot() {
        val sc = sc()
        sc.addAttractors(food(20f, 200f, 200f))
        sc.addRoot(100f, 100f)
        sc.grow()
        val counts = sc.tipCounts()
        assertEquals(sc.nodes.count { it.isTip }, counts[0])
        sc.nodes.forEachIndexed { i, n -> if (n.isTip) assertEquals(1, counts[i]) }
    }
}

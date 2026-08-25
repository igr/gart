package dev.oblac.gart.colonization

import dev.oblac.gart.math.hypotFast
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import kotlin.math.floor
import kotlin.random.Random

/**
 * Space colonization: a branching network grown by scattered attractor points.
 *
 * Scatter attractors (food) over the canvas, drop one or more roots, call [grow].
 * Every step each attractor tugs nearby branch nodes one [segmentLength] toward
 * it; once a branch arrives within [killDistance] the attractor is eaten and gone.
 * Forks, twigs and the whole tree shape fall out of that one rule.
 *
 * Two venation modes:
 *  - [Venation.OPEN]   an attractor pulls only its single nearest node. Sparse,
 *                      tree-like: veins, lightning, roots.
 *  - [Venation.CLOSED] an attractor pulls all nodes of its relative neighborhood,
 *                      so separate branches grow toward the same food and meet.
 *                      Denser, loopier, leaf-blade look.
 *
 * Attractor seeding is the caller's job (poisson, halton, noise-thinned grids,
 * points sampled from an image...) - thats where most of the art is.
 */
class SpaceColonization(
    val attractionDistance: Float = 30f, // radius within which food is seen by branches
    val killDistance: Float = 5f,        // closer than this = eaten. keep it under attractionDistance
    val segmentLength: Float = 5f,       // growth step. smaller = smoother, more nodes
    val venation: Venation = Venation.OPEN,
    val jitter: Float = 0.1f,            // random wiggle mixed into each step direction
    val maxNodes: Int = 500_000,
    // bounds + obstacles in one predicate. checked at new node positions only, the
    // segment between them is not sampled - keep obstacles fatter than segmentLength.
    // roots are not checked either, placing those is your call
    val allowed: (Point) -> Boolean = { true },
    val rnd: Random = Random.Default,    // pass a seeded one for reproducible pieces
) {
    private val _nodes = ArrayList<Node>()

    /** grown so far, append-only; a parent always sits before its children */
    val nodes: List<Node> get() = _nodes

    // attractors as parallel lists
    private val ax = ArrayList<Float>()
    private val ay = ArrayList<Float>()
    private val alive = ArrayList<Boolean>()

    var attractorsLeft = 0
        private set
    var steps = 0
        private set
    var done = false
        private set

    // uniform grid over node positions, cell == attraction radius -> 3x3 search
    private val cell = attractionDistance
    private val grid = HashMap<Long, ArrayList<Int>>()

    private val touched = ArrayList<Int>() // nodes influenced this step

    // scratch for closed-mode neighborhood queries, reused across steps
    private var near = IntArray(64)
    private var nearD2 = FloatArray(64)

    fun addAttractor(x: Float, y: Float) {
        ax.add(x)
        ay.add(y)
        alive.add(true)
        attractorsLeft++
        done = false
    }

    fun addAttractor(p: Point) = addAttractor(p.x, p.y)

    fun addAttractors(points: Iterable<Point>) {
        for (p in points) addAttractor(p.x, p.y)
    }

    fun addRoot(x: Float, y: Float): Node {
        val root = Node(_nodes.size, Point(x, y), null, steps)
        insert(root)
        done = false
        return root
    }

    fun addRoot(p: Point): Node = addRoot(p.x, p.y)

    /** attractors still uneaten - handy for debug overlays */
    fun attractors(): List<Point> = ax.indices.filter { alive[it] }.map { Point(ax[it], ay[it]) }

    /** run until nothing moves anymore. returns steps taken this call */
    fun grow(maxSteps: Int = 10_000): Int {
        var s = 0
        while (s < maxSteps && step()) s++
        return s
    }

    /**
     * One growth step. Returns false once the network is finished - all food
     * eaten, or whats left is out of reach / walled off by [allowed].
     */
    fun step(): Boolean {
        if (done) return false
        steps++
        var activity = false

        // attractors pick the nodes they feed
        for (a in ax.indices) {
            if (!alive[a]) continue
            if (when (venation) {
                    Venation.OPEN -> feedNearest(a)
                    Venation.CLOSED -> feedNeighborhood(a)
                }
            ) activity = true
        }

        // influenced nodes sprout one segment along the averaged pull
        for (ti in touched) {
            val node = _nodes[ti]
            val rawX = node.infX
            val rawY = node.infY
            // never step past the nearest food. a long segment overshooting a small kill
            // radius would ping-pong across it, spawning junk from the same node forever
            val stepLen = if (node.infMin < segmentLength) node.infMin else segmentLength
            node.infX = 0f
            node.infY = 0f
            node.infMin = Float.MAX_VALUE
            node.influenced = false
            if (_nodes.size >= maxNodes) continue
            var p = spot(node, rawX + rnd.rndf(-jitter, jitter), rawY + rnd.rndf(-jitter, jitter), stepLen)
            // a fence rejection must come from the pull itself, not from an unlucky
            // wiggle - one bad sample would otherwise finish the whole run for good
            if (p == null && jitter > 0f) p = spot(node, rawX, rawY, stepLen)
            if (p == null) continue
            node.isTip = false
            insert(Node(_nodes.size, p, node, steps))
            activity = true
        }
        touched.clear()

        if (!activity || attractorsLeft == 0) done = true
        return activity
    }

    /**
     * Fill in [Node.thickness]: every link adds [thicken] walking tip -> root, so a
     * node ends up [thicken] x the height of its subtree - twigs thin, trunk fat.
     * One O(n) pass; call it after [grow], or each frame when animating.
     */
    fun canalize(thicken: Float = 0.03f) {
        for (n in _nodes) n.thickness = 0f
        for (i in _nodes.indices.reversed()) {
            val parent = _nodes[i].parent ?: continue
            val t = _nodes[i].thickness + thicken
            if (parent.thickness < t) parent.thickness = t
        }
    }

    /** how many tips each node feeds, index-aligned with [nodes]. trunk = big, tip = 1 */
    fun tipCounts(): IntArray {
        val counts = IntArray(_nodes.size)
        for (i in _nodes.indices) if (_nodes[i].isTip) counts[i] = 1
        // parent index < child index, so one reverse pass accumulates subtrees
        for (i in _nodes.indices.reversed()) {
            val parent = _nodes[i].parent ?: continue
            counts[parent.index] += counts[i]
        }
        return counts
    }

    // the machinery ---------

    /** open venation: pull the one nearest node, eat when its close enough */
    private fun feedNearest(a: Int): Boolean {
        val x = ax[a]
        val y = ay[a]
        val ni = nearestNode(x, y)
        if (ni < 0) return false
        val node = _nodes[ni]
        val dx = x - node.position.x
        val dy = y - node.position.y
        if (dx * dx + dy * dy <= killDistance * killDistance) {
            eat(a)
            return true
        }
        influence(node, dx, dy)
        return false
    }

    /**
     * closed venation: pull every relative neighbor (no third node both closer to
     * the food and closer to you). Eaten only when the whole neighborhood arrived,
     * so branches keep converging on the food from all sides and meet there.
     */
    private fun feedNeighborhood(a: Int): Boolean {
        val x = ax[a]
        val y = ay[a]
        val n = collectNear(x, y)
        if (n == 0) return false
        val kill2 = killDistance * killDistance
        var any = false
        var allArrived = true
        for (i in 0 until n) {
            val di = nearD2[i]
            val pi = _nodes[near[i]]
            var neighbor = true
            for (j in 0 until n) {
                if (i == j || nearD2[j] >= di) continue
                val qx = pi.position.x - _nodes[near[j]].position.x
                val qy = pi.position.y - _nodes[near[j]].position.y
                if (qx * qx + qy * qy < di) { // someone closer to both of us
                    neighbor = false
                    break
                }
            }
            if (!neighbor) continue
            any = true
            if (di > kill2) {
                influence(pi, x - pi.position.x, y - pi.position.y)
                allArrived = false
            }
        }
        if (!any || !allArrived) return false
        eat(a)
        return true
    }

    private fun eat(a: Int) {
        alive[a] = false
        attractorsLeft--
    }

    private fun influence(node: Node, dx: Float, dy: Float) {
        val d = hypotFast(dx, dy)
        node.infX += dx / d
        node.infY += dy / d
        if (d < node.infMin) node.infMin = d
        if (!node.influenced) {
            node.influenced = true
            touched.add(node.index)
        }
    }

    /** direction + step -> landing point, or null when [allowed] fences it off */
    private fun spot(node: Node, dx: Float, dy: Float, stepLen: Float): Point? {
        val len = hypotFast(dx, dy)
        if (len < 1e-4f) return null // opposing pulls cancelled out
        val p = Point(node.position.x + dx / len * stepLen, node.position.y + dy / len * stepLen)
        return if (allowed(p)) p else null
    }

    private fun insert(node: Node) {
        _nodes.add(node)
        grid.getOrPut(keyOf(node.position.x, node.position.y)) { ArrayList(4) }.add(node.index)
    }

    private fun nearestNode(x: Float, y: Float): Int {
        val gx = floor(x / cell).toInt()
        val gy = floor(y / cell).toInt()
        var best = -1
        var bestD2 = attractionDistance * attractionDistance
        for (iy in gy - 1..gy + 1) {
            for (ix in gx - 1..gx + 1) {
                val bucket = grid[packKey(ix, iy)] ?: continue
                for (i in bucket) {
                    val dx = x - _nodes[i].position.x
                    val dy = y - _nodes[i].position.y
                    val d2 = dx * dx + dy * dy
                    if (d2 < bestD2) {
                        bestD2 = d2
                        best = i
                    }
                }
            }
        }
        return best
    }

    /** all nodes within attractionDistance -> near/nearD2 scratch, returns count */
    private fun collectNear(x: Float, y: Float): Int {
        val gx = floor(x / cell).toInt()
        val gy = floor(y / cell).toInt()
        val r2 = attractionDistance * attractionDistance
        var n = 0
        for (iy in gy - 1..gy + 1) {
            for (ix in gx - 1..gx + 1) {
                val bucket = grid[packKey(ix, iy)] ?: continue
                for (i in bucket) {
                    val dx = x - _nodes[i].position.x
                    val dy = y - _nodes[i].position.y
                    val d2 = dx * dx + dy * dy
                    if (d2 >= r2) continue
                    if (n == near.size) {
                        near = near.copyOf(n * 2)
                        nearD2 = nearD2.copyOf(n * 2)
                    }
                    near[n] = i
                    nearD2[n] = d2
                    n++
                }
            }
        }
        return n
    }

    private fun keyOf(x: Float, y: Float): Long = packKey(floor(x / cell).toInt(), floor(y / cell).toInt())

    private fun packKey(ix: Int, iy: Int): Long = (ix.toLong() shl 32) or (iy.toLong() and 0xFFFFFFFFL)
}

enum class Venation { OPEN, CLOSED }

/** one grown segment end. draw the network as node -> parent lines */
class Node internal constructor(
    val index: Int,
    val position: Point,
    val parent: Node?,
    val birth: Int, // step number this node appeared on - age gradients live here
) {
    /** vein width, filled by [SpaceColonization.canalize] - zero until you call it */
    var thickness = 0f
        internal set

    /** still a growing end (no children yet) */
    var isTip = true
        internal set

    // per-step influence accumulator: sum of unit pulls from my attractors
    internal var infX = 0f
    internal var infY = 0f
    internal var infMin = Float.MAX_VALUE // distance to the nearest one, caps the step
    internal var influenced = false
}

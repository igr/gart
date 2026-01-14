package dev.oblac.gart.nbody

/**
 * Flat-array Barnes-Hut quadtree for 2D gravity simulation.
 * Uses a node pool instead of pointer-heavy objects for cache efficiency.
 *
 * Each node stores:
 * - Bounding box (cx, cy, halfSize)
 * - Aggregate mass and center of mass
 * - Children indices (or -1 if leaf/empty)
 * - Particle index if leaf with single particle (-1 if internal or multi-particle)
 *
 * @param initialCapacity Initial node pool capacity
 * @param maxDepth Maximum tree depth to prevent infinite recursion on coincident particles
 */
class QuadTree(
    initialCapacity: Int = 4096,
    private val maxDepth: Int = 50
) {

    // Node pool - flat arrays for each node property
    private var capacity = initialCapacity

    // Bounding box: center and half-size
    private var nodeCx = FloatArray(capacity)
    private var nodeCy = FloatArray(capacity)
    private var nodeHalfSize = FloatArray(capacity)

    // Aggregate properties
    private var nodeMass = FloatArray(capacity)      // Total mass in subtree
    private var nodeComX = FloatArray(capacity)      // Center of mass X
    private var nodeComY = FloatArray(capacity)      // Center of mass Y

    // Children indices: NW=0, NE=1, SW=2, SE=3; -1 means no child
    private var childNW = IntArray(capacity) { -1 }
    private var childNE = IntArray(capacity) { -1 }
    private var childSW = IntArray(capacity) { -1 }
    private var childSE = IntArray(capacity) { -1 }

    // Particle index if this is a leaf with exactly one particle; -1 otherwise
    // For multi-particle leaves (at max depth), we store first particle and accumulate mass
    private var nodeParticle = IntArray(capacity) { -1 }

    // Number of particles in this node (for max-depth leaves with multiple particles)
    private var nodeParticleCount = IntArray(capacity) { 0 }

    // Number of nodes currently in use
    private var nodeCount = 0

    // Root node index (always 0 after build)
    val root: Int get() = if (nodeCount > 0) 0 else -1

    /**
     * Build the quadtree from particles.
     *
     * @param particles The particle container
     * @param bounds Bounding square (should be square, use BoundingBox.toSquare())
     */
    fun build(particles: GravityParticles, bounds: BoundingBox) {
        // Reset
        nodeCount = 0
        this.particles = particles

        if (particles.count == 0) return

        // Create root node
        val squareBounds = bounds.toSquare().expand(0.01f) // Small margin
        val rootIdx = allocateNode(
            squareBounds.centerX,
            squareBounds.centerY,
            squareBounds.width / 2f
        )

        // Insert all particles
        for (i in 0 until particles.count) {
            insert(rootIdx, i, particles.x[i], particles.y[i], particles.mass[i], particles, 0)
        }

        // Compute aggregates (post-order traversal)
        computeAggregates(rootIdx, particles)
    }

    /**
     * Insert a particle into the tree.
     */
    private fun insert(
        nodeIdx: Int,
        particleIdx: Int,
        px: Float, py: Float, pm: Float,
        particles: GravityParticles,
        depth: Int
    ) {
        val cx = nodeCx[nodeIdx]
        val cy = nodeCy[nodeIdx]
        val halfSize = nodeHalfSize[nodeIdx]

        // Check if this node is empty (no particle and no children)
        if (nodeParticle[nodeIdx] == -1 && !hasChildren(nodeIdx) && nodeParticleCount[nodeIdx] == 0) {
            // Empty leaf - just store the particle here
            nodeParticle[nodeIdx] = particleIdx
            nodeParticleCount[nodeIdx] = 1
            return
        }

        // At max depth, just accumulate into this node (don't subdivide further)
        if (depth >= maxDepth) {
            // If there's an existing single particle, initialize mass/COM from it first
            if (nodeParticle[nodeIdx] != -1) {
                val existingP = nodeParticle[nodeIdx]
                nodeMass[nodeIdx] = particles.mass[existingP]
                nodeComX[nodeIdx] = particles.x[existingP]
                nodeComY[nodeIdx] = particles.y[existingP]
                nodeParticle[nodeIdx] = -1  // No longer a single-particle leaf
            }

            // Accumulate mass and update center of mass
            val oldMass = nodeMass[nodeIdx]
            val newMass = oldMass + pm
            if (newMass > 0f) {
                nodeComX[nodeIdx] = (nodeComX[nodeIdx] * oldMass + px * pm) / newMass
                nodeComY[nodeIdx] = (nodeComY[nodeIdx] * oldMass + py * pm) / newMass
                nodeMass[nodeIdx] = newMass
            }
            nodeParticleCount[nodeIdx]++
            return
        }

        // If this is a leaf with a particle, we need to subdivide
        if (nodeParticle[nodeIdx] != -1) {
            val existingParticle = nodeParticle[nodeIdx]
            nodeParticle[nodeIdx] = -1 // No longer a single-particle leaf
            nodeParticleCount[nodeIdx] = 0

            // Re-insert the existing particle
            val epx = particles.x[existingParticle]
            val epy = particles.y[existingParticle]
            val epm = particles.mass[existingParticle]
            insertIntoQuadrant(nodeIdx, existingParticle, epx, epy, epm, cx, cy, halfSize, particles, depth)
        }

        // Insert the new particle into appropriate quadrant
        insertIntoQuadrant(nodeIdx, particleIdx, px, py, pm, cx, cy, halfSize, particles, depth)
    }

    private fun insertIntoQuadrant(
        nodeIdx: Int,
        particleIdx: Int,
        px: Float, py: Float, pm: Float,
        cx: Float, cy: Float, halfSize: Float,
        particles: GravityParticles,
        depth: Int
    ) {
        val quarterSize = halfSize / 2f
        val childIdx: Int

        // Note: we must capture allocateNode result directly because grow() may replace arrays
        when (quadrant(px, py, cx, cy)) {
            Quadrant.NW -> {
                childIdx = if (childNW[nodeIdx] == -1) {
                    val newIdx = allocateNode(cx - quarterSize, cy + quarterSize, quarterSize)
                    childNW[nodeIdx] = newIdx
                    newIdx
                } else {
                    childNW[nodeIdx]
                }
            }

            Quadrant.NE -> {
                childIdx = if (childNE[nodeIdx] == -1) {
                    val newIdx = allocateNode(cx + quarterSize, cy + quarterSize, quarterSize)
                    childNE[nodeIdx] = newIdx
                    newIdx
                } else {
                    childNE[nodeIdx]
                }
            }

            Quadrant.SW -> {
                childIdx = if (childSW[nodeIdx] == -1) {
                    val newIdx = allocateNode(cx - quarterSize, cy - quarterSize, quarterSize)
                    childSW[nodeIdx] = newIdx
                    newIdx
                } else {
                    childSW[nodeIdx]
                }
            }

            Quadrant.SE -> {
                childIdx = if (childSE[nodeIdx] == -1) {
                    val newIdx = allocateNode(cx + quarterSize, cy - quarterSize, quarterSize)
                    childSE[nodeIdx] = newIdx
                    newIdx
                } else {
                    childSE[nodeIdx]
                }
            }
        }

        insert(childIdx, particleIdx, px, py, pm, particles, depth + 1)
    }

    /**
     * Compute aggregate mass and center of mass for all nodes (post-order).
     */
    private fun computeAggregates(nodeIdx: Int, particles: GravityParticles) {
        if (nodeIdx == -1) return

        // If leaf with single particle
        if (nodeParticle[nodeIdx] != -1) {
            val p = nodeParticle[nodeIdx]
            nodeMass[nodeIdx] = particles.mass[p]
            nodeComX[nodeIdx] = particles.x[p]
            nodeComY[nodeIdx] = particles.y[p]
            return
        }

        // If this is a max-depth leaf with multiple particles, mass/COM already computed during insert
        if (nodeParticleCount[nodeIdx] > 0 && !hasChildren(nodeIdx)) {
            return
        }

        // Recurse to children first
        computeAggregates(childNW[nodeIdx], particles)
        computeAggregates(childNE[nodeIdx], particles)
        computeAggregates(childSW[nodeIdx], particles)
        computeAggregates(childSE[nodeIdx], particles)

        // Aggregate from children
        var totalMass = 0f
        var comX = 0f
        var comY = 0f

        for (child in intArrayOf(childNW[nodeIdx], childNE[nodeIdx], childSW[nodeIdx], childSE[nodeIdx])) {
            if (child != -1 && nodeMass[child] > 0f) {
                val cm = nodeMass[child]
                comX += nodeComX[child] * cm
                comY += nodeComY[child] * cm
                totalMass += cm
            }
        }

        nodeMass[nodeIdx] = totalMass
        if (totalMass > 0f) {
            nodeComX[nodeIdx] = comX / totalMass
            nodeComY[nodeIdx] = comY / totalMass
        } else {
            nodeComX[nodeIdx] = nodeCx[nodeIdx]
            nodeComY[nodeIdx] = nodeCy[nodeIdx]
        }
    }

    /**
     * Compute gravitational acceleration on a particle using Barnes-Hut approximation.
     *
     * @param px Particle X position
     * @param py Particle Y position
     * @param particleIdx Index of the particle (to skip self-interaction)
     * @param theta Opening angle threshold (0.5-1.0, smaller = more accurate)
     * @param G Gravitational constant
     * @param softening Softening parameter to avoid singularities
     * @return Pair of (ax, ay) acceleration
     */
    fun computeAcceleration(
        px: Float, py: Float,
        particleIdx: Int,
        theta: Float,
        G: Float,
        softening: Float
    ): Pair<Float, Float> {
        if (nodeCount == 0) return Pair(0f, 0f)

        var ax = 0f
        var ay = 0f

        // Stack-based traversal to avoid recursion overhead
        val stack = IntArray(maxDepth * 4 + 16) // Enough for max depth
        var stackPtr = 0
        stack[stackPtr++] = 0 // Start with root

        val softeningSq = softening * softening

        while (stackPtr > 0) {
            val nodeIdx = stack[--stackPtr]

            // Skip empty nodes
            if (nodeMass[nodeIdx] <= 0f) continue

            // Skip self-interaction for single-particle leaves
            if (nodeParticle[nodeIdx] == particleIdx) continue

            val dx = nodeComX[nodeIdx] - px
            val dy = nodeComY[nodeIdx] - py
            val distSq = dx * dx + dy * dy + softeningSq
            val dist = kotlin.math.sqrt(distSq)

            val nodeSize = nodeHalfSize[nodeIdx] * 2f

            // Barnes-Hut criterion: s/d < theta
            // Single-particle leaf: nodeParticle holds the particle index
            val isSingleParticleLeaf = nodeParticle[nodeIdx] != -1
            // Multi-particle max-depth leaf: nodeParticle == -1 (cleared when 2nd particle added), count > 0
            val isMultiParticleLeaf = nodeParticle[nodeIdx] == -1 && nodeParticleCount[nodeIdx] > 0 && !hasChildren(nodeIdx)
            val isEmptyLeaf = !hasChildren(nodeIdx) && nodeParticle[nodeIdx] == -1 && nodeParticleCount[nodeIdx] == 0
            val isLeaf = isSingleParticleLeaf || isMultiParticleLeaf || isEmptyLeaf
            val isFarEnough = nodeSize / dist < theta

            // Check if particle is inside this node's bounding box (potential self-interaction)
            val nodeHalf = nodeHalfSize[nodeIdx]
            val nodeCenterX = nodeCx[nodeIdx]
            val nodeCenterY = nodeCy[nodeIdx]
            val particleInNode = px >= nodeCenterX - nodeHalf && px <= nodeCenterX + nodeHalf &&
                py >= nodeCenterY - nodeHalf && py <= nodeCenterY + nodeHalf

            if (isLeaf) {
                if (isMultiParticleLeaf && particleInNode) {
                    // Multi-particle leaf containing this particle - subtract self-mass from aggregate
                    val selfMass = particles?.mass?.get(particleIdx) ?: 0f
                    val effectiveMass = nodeMass[nodeIdx] - selfMass
                    if (effectiveMass > 0f) {
                        // Recompute center of mass excluding self (approximation: use node's COM)
                        // For better accuracy, would need to store particle list per node
                        val forceMag = G * effectiveMass / (distSq * dist)
                        ax += forceMag * dx
                        ay += forceMag * dy
                    }
                } else {
                    // Use this node's aggregate
                    val forceMag = G * nodeMass[nodeIdx] / (distSq * dist)
                    ax += forceMag * dx
                    ay += forceMag * dy
                }
            } else if (isFarEnough && !particleInNode) {
                // Far enough AND particle not inside - safe to use aggregate
                val forceMag = G * nodeMass[nodeIdx] / (distSq * dist)
                ax += forceMag * dx
                ay += forceMag * dy
            } else {
                // Need to descend - push children onto stack
                if (childNW[nodeIdx] != -1) stack[stackPtr++] = childNW[nodeIdx]
                if (childNE[nodeIdx] != -1) stack[stackPtr++] = childNE[nodeIdx]
                if (childSW[nodeIdx] != -1) stack[stackPtr++] = childSW[nodeIdx]
                if (childSE[nodeIdx] != -1) stack[stackPtr++] = childSE[nodeIdx]
            }
        }

        return Pair(ax, ay)
    }

    // Reference to particles for self-mass lookup (set during build)
    private var particles: GravityParticles? = null

    private fun allocateNode(cx: Float, cy: Float, halfSize: Float): Int {
        if (nodeCount >= capacity) {
            grow()
        }
        val idx = nodeCount++
        nodeCx[idx] = cx
        nodeCy[idx] = cy
        nodeHalfSize[idx] = halfSize
        nodeMass[idx] = 0f
        nodeComX[idx] = cx
        nodeComY[idx] = cy
        childNW[idx] = -1
        childNE[idx] = -1
        childSW[idx] = -1
        childSE[idx] = -1
        nodeParticle[idx] = -1
        nodeParticleCount[idx] = 0
        return idx
    }

    private fun hasChildren(nodeIdx: Int): Boolean {
        return childNW[nodeIdx] != -1 || childNE[nodeIdx] != -1 ||
            childSW[nodeIdx] != -1 || childSE[nodeIdx] != -1
    }

    private fun grow() {
        val newCapacity = capacity * 2
        nodeCx = nodeCx.copyOf(newCapacity)
        nodeCy = nodeCy.copyOf(newCapacity)
        nodeHalfSize = nodeHalfSize.copyOf(newCapacity)
        nodeMass = nodeMass.copyOf(newCapacity)
        nodeComX = nodeComX.copyOf(newCapacity)
        nodeComY = nodeComY.copyOf(newCapacity)
        childNW = childNW.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = -1 }
        childNE = childNE.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = -1 }
        childSW = childSW.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = -1 }
        childSE = childSE.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = -1 }
        nodeParticle = nodeParticle.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = -1 }
        nodeParticleCount = nodeParticleCount.copyOf(newCapacity).also { for (i in capacity until newCapacity) it[i] = 0 }
        capacity = newCapacity
    }

    private enum class Quadrant { NW, NE, SW, SE }

    private fun quadrant(px: Float, py: Float, cx: Float, cy: Float): Quadrant {
        return if (py >= cy) {
            if (px < cx) Quadrant.NW else Quadrant.NE
        } else {
            if (px < cx) Quadrant.SW else Quadrant.SE
        }
    }

    /** Number of nodes in the tree */
    val size: Int get() = nodeCount

    /**
     * Get node info for debugging/visualization.
     */
    fun getNodeInfo(nodeIdx: Int): NodeInfo? {
        if (nodeIdx < 0 || nodeIdx >= nodeCount) return null
        return NodeInfo(
            cx = nodeCx[nodeIdx],
            cy = nodeCy[nodeIdx],
            halfSize = nodeHalfSize[nodeIdx],
            mass = nodeMass[nodeIdx],
            comX = nodeComX[nodeIdx],
            comY = nodeComY[nodeIdx],
            isLeaf = nodeParticle[nodeIdx] != -1 || nodeParticleCount[nodeIdx] > 0 || !hasChildren(nodeIdx),
            particleCount = nodeParticleCount[nodeIdx]
        )
    }

    data class NodeInfo(
        val cx: Float,
        val cy: Float,
        val halfSize: Float,
        val mass: Float,
        val comX: Float,
        val comY: Float,
        val isLeaf: Boolean,
        val particleCount: Int = 0
    )
}

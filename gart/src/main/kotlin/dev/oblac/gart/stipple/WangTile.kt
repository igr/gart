package dev.oblac.gart.stipple

import dev.oblac.gart.stipple.util.*
import dev.oblac.gart.vector.Vec2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

internal enum class WangTileSide {
    NORTH, SOUTH, EAST, WEST;

    companion object {
        const val NUM = 4
    }
}

/**
 * A merge candidate tracks which source distribution a sample came from.
 */
internal data class MergeCandidate(
    val sample: PoissonSample,
    val sourceDist: Int
)

/**
 * A source tile is a Poisson-disk distribution used as the seed for a Wang Tile edge.
 */
internal data class SourceTile(
    val distribution: List<PoissonSample>,
    val color: Int
)

/**
 * Wang Tile with colored edges and a Poisson-disk distribution of sample points.
 *
 * Based on: "Recursive Wang Tiles for Real-Time Blue Noise" by Kopf et al.
 */
internal class WangTile(
    baseDistribution: List<PoissonSample>,
    neighbors: List<SourceTile>
) {
    val edgeColors = IntArray(WangTileSide.NUM)
    var distribution: List<PoissonSample>

    init {
        var current = baseDistribution.toMutableList()
        for (i in neighbors.indices) {
            edgeColors[i] = neighbors[i].color
            current = mergeTiles(current, neighbors[i].distribution, WangTileSide.entries[i])
        }
        distribution = makeProgressive(current)
    }

    companion object {
        private const val SCALE = 1000f

        /**
         * Merge two Poisson distributions along a seam found via Delaunay/Voronoi/Dijkstra.
         */
        private fun mergeTiles(
            source: List<PoissonSample>,
            toMerge: List<PoissonSample>,
            side: WangTileSide
        ): MutableList<PoissonSample> {
            val candidates = mutableListOf<MergeCandidate>()
            for (s in source) candidates.add(MergeCandidate(s, 0))
            for (s in toMerge) candidates.add(MergeCandidate(s, 1))

            // Scale points for numerical stability in Delaunay
            val vertices = candidates.map { Vec2(it.sample.x * SCALE, it.sample.y * SCALE) }

            val adjacency = Adjacency()
            delaunayTriangulate(vertices, false, adjacency) ?: return source.toMutableList()

            // Unscale vertices
            val unscaled = mutableListOf<Vec2>()
            for (v in adjacency.vertices) {
                unscaled.add(Vec2(v.x / SCALE, v.y / SCALE))
            }
            adjacency.vertices = unscaled

            val voronoi = VoronoiDiagram.fromDelaunay(adjacency)
            val graph = SeamGraph(voronoi, adjacency, candidates)

            // Clip all sides except target first, then target last
            for (i in 0 until WangTileSide.NUM) {
                if (i != side.ordinal) graph.clip(WangTileSide.entries[i])
            }
            val pathEndpoints = graph.clip(side)

            val path = graph.shortestPath(pathEndpoints.first, pathEndpoints.second)
                ?: return source.toMutableList()
            val shape = graph.generateShape(path)

            // Scale shape points for polygon test
            val scaledShape = shape.map { Vec2(it.x * SCALE, it.y * SCALE) }

            val result = mutableListOf<PoissonSample>()
            for (c in candidates) {
                val insideSeam = pointInPolygon(
                    Vec2(c.sample.x * SCALE, c.sample.y * SCALE),
                    scaledShape
                )
                if ((insideSeam && c.sourceDist == 1) || (!insideSeam && c.sourceDist == 0)) {
                    result.add(c.sample)
                }
            }
            return result
        }

        /**
         * Make progressive ordering: sort by ranking, fix equal-ranking,
         * then reorder to maximize minimum distance to earlier samples.
         */
        private fun makeProgressive(distribution: List<PoissonSample>): List<PoissonSample> {
            if (distribution.size < 2) return distribution

            val sorted = distribution.sortedBy { it.ranking }.toMutableList()
            val random = Random(sorted.size)

            // Fix equal-ranking samples by random interleaving
            var i = 0
            while (i < sorted.size) {
                val rankingFixStart = i
                val ranking = sorted[i].ranking
                while (i + 1 < sorted.size && sorted[i + 1].ranking == ranking) i++

                if (rankingFixStart < i) {
                    val fixLength = i - rankingFixStart + 1
                    val shuffle = IntArray(fixLength) { rankingFixStart + it }
                    for (j in 0 until fixLength) {
                        val a = random.nextInt(fixLength)
                        val b = random.nextInt(fixLength)
                        if (a != b) {
                            val tmp = shuffle[a]
                            shuffle[a] = shuffle[b]
                            shuffle[b] = tmp
                        }
                    }
                    val interleaved = Array(fixLength) { sorted[shuffle[it]] }
                    for (j in 0 until fixLength) sorted[rankingFixStart + j] = interleaved[j]
                }
                i++
            }

            // Update rankings
            for (idx in sorted.indices) {
                sorted[idx] = sorted[idx].copy(ranking = idx.toFloat() / sorted.size)
            }

            // Fix seam-neighbors: maximize minimum distance to earlier samples
            val alpha = 0.5f
            for (idx in 2 until sorted.size - 1) {
                var j = idx
                val threshold = alpha * sqrt(idx.toFloat())
                while (j < sorted.size - 1) {
                    var minDist = Float.MAX_VALUE
                    for (k in 0 until idx) {
                        val dist = sorted[j].distSquared(sorted[k])
                        if (dist < minDist) minDist = dist
                    }
                    if (sqrt(minDist) > threshold) break
                    j++
                }
                if (j != idx) {
                    val tmp = sorted[idx]
                    sorted[idx] = sorted[j]
                    sorted[j] = tmp
                }
            }

            return sorted
        }
    }
}

/**
 * A complete set of Wang Tiles covering all edge color permutations.
 */
class WangTileSet internal constructor(
    internal val tiles: List<WangTile>,
    internal val numColors: Int,
    internal val subdivisions: List<Array<IntArray>>,
    internal val splitsPerDimension: Int
) {
    //private val random = Random(tiles.size)

    // Lookup tables for fast tile selection by edge constraints
    private val tilesSortedByWest: Array<List<Int>>
    private val tilesSortedByWestNorth: Array<Array<List<Int>>>

    init {
        tilesSortedByWest = Array(numColors) { west ->
            tiles.indices.filter { tiles[it].edgeColors[WangTileSide.WEST.ordinal] == west }
        }
        tilesSortedByWestNorth = Array(numColors) { west ->
            Array(numColors) { north ->
                tilesSortedByWest[west].filter { tiles[it].edgeColors[WangTileSide.NORTH.ordinal] == north }
            }
        }
    }

    internal fun getSubdivisions(tile: Int): Pair<Array<IntArray>, Int> {
        return subdivisions[tile] to splitsPerDimension
    }

    companion object {
        /**
         * Generate a complete Wang Tile set.
         *
         * @param numColors Number of edge colors. Total tiles = numColors^4.
         * @param samplesPerTile Number of Poisson samples per tile.
         * @param seed Random seed for reproducibility.
         */
        fun generate(
            numColors: Int = 2,
            samplesPerTile: Int = 1000,
            seed: Int = 42
        ): WangTileSet {
            val random = Random(seed)
            val numEdges = WangTileSide.NUM
            val numTiles = numColors.toDouble().pow(numEdges).toInt()

            // Generate Poisson distributions for each tile
            val distributions = Array(numTiles) { i ->
                generatePoissonDisk(
                    desiredSamples = samplesPerTile,
                    seed = seed + i
                )
            }

            // Generate seam tiles (one per edge color)
            val seamTiles = (0 until numColors).map { color ->
                val dist = generatePoissonDisk(
                    desiredSamples = samplesPerTile,
                    seed = seed + numTiles + color
                )
                SourceTile(dist, color)
            }

            // Generate all edge color permutations
            val edgeCol = Array(numTiles) { IntArray(numEdges) }
            for (i in 1 until numTiles) {
                for (j in 0 until numEdges) {
                    edgeCol[i][j] = (edgeCol[i - 1][j] +
                        (if (i % numColors.toDouble().pow(j).toInt() == 0) 1 else 0)) % numColors
                }
            }

            // Generate Wang Tiles
            val tiles = Array(numTiles) { t ->
                val neighbors = if (numColors > 1) {
                    (0 until numEdges).map { j -> seamTiles[edgeCol[t][j]] }
                } else {
                    emptyList()
                }
                WangTile(distributions[t], neighbors)
            }

            // Generate subdivision rules
            val tileList = tiles.toList()
            val subdivisions = makeRecursive(tileList, numColors, random)

            return WangTileSet(tileList, numColors, subdivisions, 4)
        }

        private fun makeRecursive(
            tiles: List<WangTile>,
            numColors: Int,
            random: Random
        ): List<Array<IntArray>> {
            val numSubdivisions = 4
            val combinatory = numColors.toDouble().pow(numSubdivisions).toInt()

            // Create subdivision rules for each color
            val rules = Array(numColors) { color ->
                val rule = IntArray(numSubdivisions)
                var ruleIndex = (color + 1) * combinatory / (numColors + 1)
                for (j in 0 until numSubdivisions) {
                    val power = numColors.toDouble().pow(numSubdivisions - j - 1).toInt()
                    rule[j] = ruleIndex / power
                    ruleIndex -= rule[j] * power
                }
                rule
            }

            // Subdivide each tile
            return tiles.map { tile ->
                subdivide(tile, numSubdivisions, rules, tiles, random)
            }
        }

        private fun subdivide(
            tile: WangTile,
            numSub: Int,
            rules: Array<IntArray>,
            allTiles: List<WangTile>,
            random: Random
        ): Array<IntArray> {
            val result = Array(numSub) { IntArray(numSub) }
            val upperRow = IntArray(numSub)

            for (i in 0 until numSub) {
                for (j in 0 until numSub) {
                    val restrictions = IntArray(WangTileSide.NUM) { -1 }

                    if (i == 0) restrictions[WangTileSide.NORTH.ordinal] =
                        rules[tile.edgeColors[WangTileSide.NORTH.ordinal]][j]
                    if (i == numSub - 1) restrictions[WangTileSide.SOUTH.ordinal] =
                        rules[tile.edgeColors[WangTileSide.SOUTH.ordinal]][j]
                    if (j == 0) restrictions[WangTileSide.WEST.ordinal] =
                        rules[tile.edgeColors[WangTileSide.WEST.ordinal]][i]
                    if (j == numSub - 1) restrictions[WangTileSide.EAST.ordinal] =
                        rules[tile.edgeColors[WangTileSide.EAST.ordinal]][i]

                    if (i > 0) restrictions[WangTileSide.NORTH.ordinal] =
                        allTiles[upperRow[j]].edgeColors[WangTileSide.SOUTH.ordinal]
                    if (j > 0) restrictions[WangTileSide.WEST.ordinal] =
                        allTiles[result[i][j - 1]].edgeColors[WangTileSide.EAST.ordinal]

                    result[i][j] = findTile(restrictions, allTiles, random)
                }
                for (j in 0 until numSub) upperRow[j] = result[i][j]
            }
            return result
        }

        private fun findTile(
            colors: IntArray,
            tiles: List<WangTile>,
            random: Random
        ): Int {
            val candidates = mutableListOf<Int>()
            for (i in tiles.indices) {
                var match = true
                for (j in 0 until WangTileSide.NUM) {
                    if (colors[j] >= 0 && tiles[i].edgeColors[j] != colors[j]) {
                        match = false
                        break
                    }
                }
                if (match) candidates.add(i)
            }
            return if (candidates.isEmpty()) 0 else candidates[random.nextInt(candidates.size)]
        }
    }
}

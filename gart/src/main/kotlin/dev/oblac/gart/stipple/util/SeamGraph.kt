package dev.oblac.gart.stipple.util

import dev.oblac.gart.stipple.MergeCandidate
import dev.oblac.gart.stipple.WangTileSide
import dev.oblac.gart.vector.Vec2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Graph built from a Voronoi diagram for finding minimum-cost seam paths.
 * Used in Wang Tile merging to find optimal boundaries between tile distributions.
 */
internal class SeamGraphEdge(val v0: Int, val v1: Int, var cost: Float)

internal class SeamGraph(
	voronoi: VoronoiDiagram,
	adjacency: Adjacency,
	val candidates: List<MergeCandidate>
) {
	val vertices = voronoi.vertices
	val edges = mutableListOf<SeamGraphEdge>()

	init {
		var maxCellDist = Float.MIN_VALUE

		for (ve in voronoi.edges) {
			// Check for duplicate edges
			val found = edges.any { e ->
				(e.v0 == ve.v0 && e.v1 == ve.v1) || (e.v0 == ve.v1 && e.v1 == ve.v0)
			}
			if (!found) {
				val cellA = if (ve.cellA < adjacency.vertices.size) adjacency.vertices[ve.cellA] else null
				val cellB = if (ve.cellB < adjacency.vertices.size) adjacency.vertices[ve.cellB] else null
				val cellDist = if (cellA != null && cellB != null) (cellA - cellB).length() else Float.MAX_VALUE
				if (cellDist < Float.MAX_VALUE) maxCellDist = max(maxCellDist, cellDist)
				edges.add(SeamGraphEdge(ve.v0, ve.v1, cellDist))
			}
		}

		// Convert distances to costs
		for (e in edges) {
			e.cost = if (e.cost < Float.MAX_VALUE) {
				(1f - e.cost / maxCellDist).pow(100f)
			} else {
				INFINITY_COST
			}
		}
	}

	/**
	 * Clips edges beyond the given boundary and adds a boundary edge.
	 * Returns the two boundary vertex indices.
	 */
	fun clip(side: WangTileSide): Pair<Int, Int> {
		val epsilon = 1e-3f
		val deletedVertices = mutableSetOf<Int>()
		val v0: Int
		val v1: Int

		when (side) {
			WangTileSide.NORTH -> {
				v0 = addVertex(Vec2(0f, 1f))
				v1 = addVertex(Vec2(1f, 1f))
				addEdge(v0, v1, 1000f)
				for (i in vertices.indices) {
					if (vertices[i].y > 1f + epsilon) deletedVertices.add(i)
				}
			}
			WangTileSide.SOUTH -> {
				v0 = addVertex(Vec2(0f, 0f))
				v1 = addVertex(Vec2(1f, 0f))
				addEdge(v0, v1, 1000f)
				for (i in vertices.indices) {
					if (vertices[i].y < -epsilon) deletedVertices.add(i)
				}
			}
			WangTileSide.EAST -> {
				v0 = addVertex(Vec2(1f, 0f))
				v1 = addVertex(Vec2(1f, 1f))
				addEdge(v0, v1, 1000f)
				for (i in vertices.indices) {
					if (vertices[i].x > 1f + epsilon) deletedVertices.add(i)
				}
			}
			WangTileSide.WEST -> {
				v0 = addVertex(Vec2(0f, 0f))
				v1 = addVertex(Vec2(0f, 1f))
				addEdge(v0, v1, 1000f)
				for (i in vertices.indices) {
					if (vertices[i].x < -epsilon) deletedVertices.add(i)
				}
			}
		}

		edges.removeAll { e ->
			deletedVertices.contains(e.v0) || deletedVertices.contains(e.v1)
		}

		return v0 to v1
	}

	/**
	 * Dijkstra's shortest path algorithm.
	 */
	fun shortestPath(start: Int, end: Int): List<Int>? {
		val cost = FloatArray(vertices.size) { INFINITY_COST }
		val previous = IntArray(vertices.size) { -1 }
		val visited = BooleanArray(vertices.size)

		// Build adjacency lists
		val neighbors = Array(vertices.size) { mutableListOf<Pair<Int, Float>>() }
		for (e in edges) {
			neighbors[e.v0].add(e.v1 to e.cost)
			neighbors[e.v1].add(e.v0 to e.cost)
		}

		var current = start
		cost[current] = 0f
		var unvisited = vertices.size

		while (unvisited > 0) {
			for ((neighbor, edgeCost) in neighbors[current]) {
				if (visited[neighbor]) continue
				val tentative = cost[current] + edgeCost
				if (tentative < cost[neighbor]) {
					cost[neighbor] = tentative
					previous[neighbor] = current
				}
			}
			visited[current] = true
			unvisited--

			if (current == end) break

			var minCost = Float.MAX_VALUE
			var next = current
			for (i in vertices.indices) {
				if (!visited[i] && cost[i] < minCost) {
					minCost = cost[i]
					next = i
				}
			}
			current = next
		}

		if (current != end) return null

		val path = mutableListOf<Int>()
		var c = current
		while (c != start) {
			path.add(c)
			c = previous[c]
			if (c < 0) return null
		}
		path.add(start)
		return path
	}

	fun generateShape(path: List<Int>): List<Vec2> {
		if (path.isEmpty()) return emptyList()
		val shape = path.map { vertices[it] }.toMutableList()
		shape.add(vertices[path[0]]) // close the shape
		return shape
	}

	private fun addVertex(v: Vec2): Int {
		val tolerance = 1e-3f
		for (i in vertices.indices) {
			if (abs(vertices[i].x - v.x) <= tolerance && abs(vertices[i].y - v.y) <= tolerance) {
				return i
			}
		}
		vertices.add(v)
		return vertices.size - 1
	}

	private fun addEdge(v0: Int, v1: Int, cost: Float) {
		val candidates = ArrayDeque<SeamGraphEdge>()
		candidates.addLast(SeamGraphEdge(v0, v1, cost))

		while (candidates.isNotEmpty()) {
			val cEdge = candidates.removeLast()
			if ((vertices[cEdge.v0] - vertices[cEdge.v1]).length() <= 1e-3f) continue

			var intersected = false
			for (e in edges.toList()) {
				if (e.v0 == cEdge.v0 || e.v0 == cEdge.v1 || e.v1 == cEdge.v0 || e.v1 == cEdge.v1) continue

				val result = segmentIntersect(vertices[e.v0], vertices[e.v1], vertices[cEdge.v0], vertices[cEdge.v1])
				if (result.intersects) {
					vertices.add(result.point)
					val isectIdx = vertices.size - 1

					edges.remove(e)
					edges.add(SeamGraphEdge(e.v0, isectIdx, e.cost))
					edges.add(SeamGraphEdge(isectIdx, e.v1, e.cost))

					candidates.addLast(SeamGraphEdge(cEdge.v0, isectIdx, cost))
					candidates.addLast(SeamGraphEdge(isectIdx, cEdge.v1, cost))
					intersected = true
					break
				}
			}
			if (!intersected) {
				edges.add(cEdge)
			}
		}
	}

	companion object {
		private const val INFINITY_COST = Float.MAX_VALUE / 2
	}
}

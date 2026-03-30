package dev.oblac.gart.stipple.util

import dev.oblac.gart.vector.Vec2
import kotlin.math.*

/**
 * Delaunay triangulation using Lawson's incremental algorithm with edge flipping.
 */

internal class DelaunayEdge {
	val vertices = intArrayOf(-1, -1)
	val triangles = intArrayOf(-1, -1)

	fun links(v1: Int, v2: Int): Boolean {
		return (vertices[0] == v1 && vertices[1] == v2) || (vertices[0] == v2 && vertices[1] == v1)
	}
}

internal class DelaunayTriangle {
	val vertices = intArrayOf(-1, -1, -1)
	val edges = intArrayOf(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
	var valid = false

	fun contains(v: Int) = vertices[0] == v || vertices[1] == v || vertices[2] == v

	fun localEdgeIndex(globalEdgeIndex: Int): Int {
		for (i in 0..2) {
			if (abs(edges[i]) - 1 == globalEdgeIndex) return i
		}
		return -1
	}

	fun insideCircumcircle(p: Vec2, verts: List<Vec2>): Float {
		return inCircle(verts[vertices[0]], verts[vertices[1]], verts[vertices[2]], p)
	}

	fun circumCircle(verts: List<Vec2>): Pair<Vec2, Float>? {
		val p0 = verts[vertices[0]]
		val p1 = verts[vertices[1]]
		val p2 = verts[vertices[2]]

		val a = det3x3(
			p0.x, p0.y, 1f,
			p1.x, p1.y, 1f,
			p2.x, p2.y, 1f
		)
		if (abs(a) < 1e-5f) return null

		val d = -det3x3(
			p0.x * p0.x + p0.y * p0.y, p0.y, 1f,
			p1.x * p1.x + p1.y * p1.y, p1.y, 1f,
			p2.x * p2.x + p2.y * p2.y, p2.y, 1f
		)
		val e = det3x3(
			p0.x * p0.x + p0.y * p0.y, p0.x, 1f,
			p1.x * p1.x + p1.y * p1.y, p1.x, 1f,
			p2.x * p2.x + p2.y * p2.y, p2.x, 1f
		)
		val f = -det3x3(
			p0.x * p0.x + p0.y * p0.y, p0.x, p0.y,
			p1.x * p1.x + p1.y * p1.y, p1.x, p1.y,
			p2.x * p2.x + p2.y * p2.y, p2.x, p2.y
		)

		val center = Vec2(-d / (2 * a), -e / (2 * a))
		val radius = sqrt((d * d + e * e) / (4 * a * a) - f / a)
		return center to radius
	}
}

internal class Adjacency {
	val edges = mutableListOf<DelaunayEdge>()
	val triangles = mutableListOf<DelaunayTriangle>()
	var vertices = mutableListOf<Vec2>()
	val invalidTriangles = mutableListOf<Int>()

	fun createTriangle(a: Int, b: Int, c: Int): Int {
		val triangleIndex = triangles.size
		val tri = DelaunayTriangle()
		tri.vertices[0] = a
		tri.vertices[1] = b
		tri.vertices[2] = c
		triangles.add(tri)

		for (i in 0..2) {
			val v1 = tri.vertices[i]
			val v2 = tri.vertices[(i + 1) % 3]
			val e = findEdge(v1, v2)
			if (e >= 0) {
				val edge = edges[e]
				val edgeTriIdx = if (edge.vertices[0] == v1) 0 else 1
				edge.triangles[edgeTriIdx] = triangleIndex
				tri.edges[i] = if (edgeTriIdx == 0) e + 1 else -(e + 1)
			} else {
				val ne = createEdge(v1, v2)
				edges[ne].triangles[0] = triangleIndex
				tri.edges[i] = ne + 1
			}
		}
		tri.valid = true
		return triangleIndex
	}

	fun removeTriangle(t: Int) {
		val tri = triangles[t]
		for (i in 0..2) {
			val edgeIndex = tri.edges[i]
			val edge = edges[abs(edgeIndex) - 1]
			if (edgeIndex < 0) {
				edge.triangles[1] = -1
			} else {
				edge.triangles[0] = -1
			}
		}
		tri.valid = false
		invalidTriangles.add(t)
	}

	fun pointInTriangle(p: Vec2): Int {
		for (i in triangles.indices) {
			val t = triangles[i]
			if (!t.valid) continue

			val v0 = vertices[t.vertices[0]]
			val v1 = vertices[t.vertices[1]]
			val v2 = vertices[t.vertices[2]]

			val b0 = (v1.x - v0.x) * (v2.y - v0.y) - (v2.x - v0.x) * (v1.y - v0.y)
			if (b0 != 0f) {
				val b1 = ((v1.x - p.x) * (v2.y - p.y) - (v2.x - p.x) * (v1.y - p.y)) / b0
				val b2 = ((v2.x - p.x) * (v0.y - p.y) - (v0.x - p.x) * (v2.y - p.y)) / b0
				val b3 = ((v0.x - p.x) * (v1.y - p.y) - (v1.x - p.x) * (v0.y - p.y)) / b0
				if (b1 >= 0 && b2 >= 0 && b3 >= 0) return i
			}
		}
		return -1
	}

	fun pointInTriangleEdge(p: Vec2, t: Int): Int {
		val tri = triangles[t]
		var closestEdge = -1
		var closestDistance = Float.MAX_VALUE

		for (i in 0..2) {
			val edgeIdx = abs(tri.edges[i]) - 1
			val edge = edges[edgeIdx]
			val dist = pointOnSegment(vertices[edge.vertices[0]], vertices[edge.vertices[1]], p)
			if (dist != null && dist < closestDistance) {
				closestDistance = dist
				closestEdge = edgeIdx
			}
		}
		return closestEdge
	}

	fun vertexOutOfTriEdge(triIndex: Int, edgeIndex: Int): Int {
		val tri = triangles[triIndex]
		val globalIdx = abs(tri.edges[edgeIndex]) - 1
		val edge = edges[globalIdx]
		return when {
			edge.links(tri.vertices[0], tri.vertices[1]) -> tri.vertices[2]
			edge.links(tri.vertices[0], tri.vertices[2]) -> tri.vertices[1]
			else -> tri.vertices[0]
		}
	}

	fun adjacentTriangle(triIndex: Int, edgeIndex: Int): Int {
		val tri = triangles[triIndex]
		val edgeGlobalIdx = abs(tri.edges[edgeIndex]) - 1
		val edge = edges[edgeGlobalIdx]
		return if (tri.edges[edgeIndex] < 0) edge.triangles[0] else edge.triangles[1]
	}

	fun flipTriangles(tri1: Int, tri2: Int): IntArray? {
		val t1 = triangles[tri1]
		val t2 = triangles[tri2]

		val commonEdgeIdx = commonEdge(t1, t2) ?: return null
		val localEdgeT1 = t1.localEdgeIndex(commonEdgeIdx)
		val localEdgeT2 = t2.localEdgeIndex(commonEdgeIdx)

		val a = vertexOutOfTriEdge(tri1, localEdgeT1)
		val d = vertexOutOfTriEdge(tri2, localEdgeT2)

		val commonEdge = edges[commonEdgeIdx]
		val b = if (commonEdge.triangles[0] == tri1) commonEdge.vertices[0] else commonEdge.vertices[1]
		val c = if (b == commonEdge.vertices[0]) commonEdge.vertices[1] else commonEdge.vertices[0]

		if (!segmentsIntersect(vertices[a], vertices[d], vertices[b], vertices[c])) return null
		if (!isWorthFlipping(a, b, c, d)) return null

		removeTriangle(tri1)
		removeTriangle(tri2)

		return intArrayOf(
			createTriangle(a, b, d),
			createTriangle(a, d, c)
		)
	}

	fun splitEdge(edgeIndex: Int, p: Vec2): IntArray {
		val edge = edges[edgeIndex]
		val triIdx1 = edge.triangles[0]
		val triIdx2 = edge.triangles[1]

		val tri1edge = if (triIdx1 >= 0) triangles[triIdx1].localEdgeIndex(edgeIndex) else -1
		val tri2edge = if (triIdx2 >= 0) triangles[triIdx2].localEdgeIndex(edgeIndex) else -1
		val a = if (triIdx1 >= 0 && tri1edge >= 0) vertexOutOfTriEdge(triIdx1, tri1edge) else -1
		val d = if (triIdx2 >= 0 && tri2edge >= 0) vertexOutOfTriEdge(triIdx2, tri2edge) else -1

		if (a < 0 || d < 0) return intArrayOf(-1, -1, -1, -1)

		val b = edge.vertices[0]
		val c = edge.vertices[1]

		if (triIdx1 >= 0) removeTriangle(triIdx1)
		if (triIdx2 >= 0) removeTriangle(triIdx2)

		vertices.add(p)
		val newVertex = vertices.size - 1

		val result = intArrayOf(-1, -1, -1, -1)
		var idx = 0
		if (a >= 0 && b >= 0) result[idx++] = createTriangle(a, b, newVertex)
		if (d >= 0 && c >= 0) result[idx++] = createTriangle(d, c, newVertex)
		if (a >= 0 && c >= 0) result[idx++] = createTriangle(a, newVertex, c)
		if (b >= 0 && d >= 0) result[idx++] = createTriangle(b, d, newVertex)
		return result
	}

	fun splitTriangle(triIdx: Int, p: Vec2): IntArray {
		val newVertex = vertices.size
		vertices.add(p)

		val oldTri = triangles[triIdx]
		val a = oldTri.vertices[0]
		val b = oldTri.vertices[1]
		val c = oldTri.vertices[2]

		removeTriangle(triIdx)

		return intArrayOf(
			createTriangle(a, b, newVertex),
			createTriangle(b, c, newVertex),
			createTriangle(c, a, newVertex)
		)
	}

	// ---- Private helpers ----

	private fun findEdge(v1: Int, v2: Int): Int {
		for (i in edges.indices) {
			if (edges[i].links(v1, v2)) return i
		}
		return -1
	}

	private fun createEdge(v1: Int, v2: Int): Int {
		val e = DelaunayEdge()
		e.vertices[0] = v1
		e.vertices[1] = v2
		edges.add(e)
		return edges.size - 1
	}

	private fun commonEdge(t1: DelaunayTriangle, t2: DelaunayTriangle): Int? {
		for (i in 0..2) {
			val e1 = abs(t1.edges[i]) - 1
			for (j in 0..2) {
				if (e1 == abs(t2.edges[j]) - 1) return e1
			}
		}
		return null
	}

	private fun isWorthFlipping(a: Int, b: Int, c: Int, d: Int): Boolean {
		val before = min(closestAngleOnTri(a, b, c), closestAngleOnTri(b, d, c))
		val after = min(closestAngleOnTri(a, b, d), closestAngleOnTri(a, d, c))
		return before < after
	}

	private fun closestAngleOnTri(vA: Int, vB: Int, vC: Int): Float {
		val indices = intArrayOf(vA, vB, vC)
		var closestAngle = PI.toFloat()
		for (i in 0..2) {
			val nAB = (vertices[indices[(i + 1) % 3]] - vertices[indices[i]]).normalize()
			val nAC = (vertices[indices[(i + 2) % 3]] - vertices[indices[i]]).normalize()
			val dot = nAB.dot(nAC).coerceIn(-1f, 1f)
			val angle = acos(dot)
			if (angle < closestAngle) closestAngle = angle
		}
		return closestAngle
	}

	companion object {
		private const val POINT_ON_SEGMENT_DISTANCE_EPSILON = 1e-4f
		private const val POINT_ON_SEGMENT_PARAMETRIC_EPSILON = 1e-5f

		fun pointOnSegment(a: Vec2, b: Vec2, p: Vec2): Float? {
			val segLen = (a - b).length()
			if (segLen < 1e-8f) return null
			val abDir = (b - a).normalize()
			val apDir = (p - a)
			val apLen = apDir.length()
			if (apLen < 1e-8f) return null
			val tangentDist = abDir.dot(apDir.normalize()) * apLen
			val u = tangentDist / segLen
			if (u < POINT_ON_SEGMENT_PARAMETRIC_EPSILON || u > 1f - POINT_ON_SEGMENT_PARAMETRIC_EPSILON) return null

			val isect = a + (b - a) * u
			val dist = (p - isect).length()
			return if (dist > POINT_ON_SEGMENT_DISTANCE_EPSILON * segLen) null else dist
		}
	}
}

// ---- Delaunay triangulation ----

private const val INSIDE_CIRCUMCIRCLE_EPSILON = 1e-2f
private const val COINCIDENT_POINTS_DISTANCE_EPSILON = 1e-1f

internal fun delaunayTriangulate(
	points: List<Vec2>,
	removeSurroundingTriangle: Boolean,
	adjacency: Adjacency
): List<Int>? {
	if (points.size < 3) return emptyList()

	// Create super triangle
	val bounds = Bounds2f()
	for (p in points) bounds.addPoint(p)
	val (center, radius) = bounds.boundingCircle()
	val deg2rad = PI.toFloat() / 180f

	val st1 = Vec2(
		center.x + 2f * radius * cos(0f * deg2rad),
		center.y + 2f * radius * sin(0f * deg2rad)
	)
	val st2 = Vec2(
		center.x + 2f * radius * cos(120f * deg2rad),
		center.y + 2f * radius * sin(120f * deg2rad)
	)
	val st3 = Vec2(
		center.x + 2f * radius * cos(240f * deg2rad),
		center.y + 2f * radius * sin(240f * deg2rad)
	)

	adjacency.vertices.add(st1)
	adjacency.vertices.add(st2)
	adjacency.vertices.add(st3)
	val stIdx1 = adjacency.vertices.size - 3
	val stIdx2 = adjacency.vertices.size - 2
	val stIdx3 = adjacency.vertices.size - 1
	adjacency.createTriangle(stIdx1, stIdx2, stIdx3)

	// Insert points
	val toCheck = mutableSetOf<Int>()
	for (i in points.indices) {
		val vi = points[i]

		// Skip coincident points
		var skip = false
		for (j in adjacency.vertices.indices) {
			if ((vi - adjacency.vertices[j]).length() <= COINCIDENT_POINTS_DISTANCE_EPSILON) {
				skip = true
				break
			}
		}
		if (skip) continue

		val tri = adjacency.pointInTriangle(vi)
		if (tri < 0) return null

		val edgeIdx = adjacency.pointInTriangleEdge(vi, tri)
		if (edgeIdx >= 0) {
			val result = adjacency.splitEdge(edgeIdx, vi)
			for (r in result) {
				if (r >= 0) toCheck.add(r)
			}
		} else {
			val result = adjacency.splitTriangle(tri, vi)
			for (r in result) {
				toCheck.add(r)
			}
		}

		// Check Delaunay condition and flip
		while (toCheck.isNotEmpty()) {
			val t = toCheck.last()
			toCheck.remove(t)
			val triangle = adjacency.triangles[t]
			if (!triangle.valid) continue

			for (e in 0..2) {
				if (!adjacency.triangles[t].valid) continue
				val adjacentIdx = adjacency.adjacentTriangle(t, e)
				if (adjacentIdx < 0) continue
				val adjacent = adjacency.triangles[adjacentIdx]
				if (!adjacent.valid) continue

				val globalEdgeIndex = abs(triangle.edges[e]) - 1
				val edgeFromAdjacent = adjacent.localEdgeIndex(globalEdgeIndex)
				if (edgeFromAdjacent < 0) continue
				val v = adjacency.vertexOutOfTriEdge(adjacentIdx, edgeFromAdjacent)
				if (v < 0 || triangle.contains(v)) continue

				if (triangle.insideCircumcircle(adjacency.vertices[v], adjacency.vertices) > INSIDE_CIRCUMCIRCLE_EPSILON) {
					val result = adjacency.flipTriangles(t, adjacentIdx)
					if (result != null) {
						toCheck.add(result[0])
						toCheck.add(result[1])
					}
				}
			}
		}
	}

	// Handle super triangle
	val outTriangles = mutableListOf<Int>()

	if (removeSurroundingTriangle) {
		for (i in adjacency.triangles.indices) {
			val tri = adjacency.triangles[i]
			if (!tri.valid) continue
			if (tri.contains(stIdx1) || tri.contains(stIdx2) || tri.contains(stIdx3)) {
				adjacency.removeTriangle(i)
			} else {
				for (j in 0..2) outTriangles.add(tri.vertices[j])
			}
		}
		adjacency.invalidTriangles.clear()
		// Remap: remove super triangle vertices (first 3)
		for (i in outTriangles.indices) outTriangles[i] -= 3
		for (e in adjacency.edges) {
			e.vertices[0] -= 3
			e.vertices[1] -= 3
		}
		for (tri in adjacency.triangles) {
			if (tri.valid) {
				tri.vertices[0] -= 3
				tri.vertices[1] -= 3
				tri.vertices[2] -= 3
			}
		}
	} else {
		// Move super triangle vertices to end
		for (e in adjacency.edges) {
			for (i in 0..1) {
				e.vertices[i] -= 3
				if (e.vertices[i] < 0) e.vertices[i] += adjacency.vertices.size
			}
		}
		for (tri in adjacency.triangles) {
			for (i in 0..2) {
				tri.vertices[i] -= 3
				if (tri.vertices[i] < 0) tri.vertices[i] += adjacency.vertices.size
			}
		}
		// Move first 3 vertices to end
		val st = listOf(adjacency.vertices[0], adjacency.vertices[1], adjacency.vertices[2])
		adjacency.vertices.removeAt(0)
		adjacency.vertices.removeAt(0)
		adjacency.vertices.removeAt(0)
		adjacency.vertices.addAll(st)

		for (i in adjacency.triangles.indices) {
			if (adjacency.triangles[i].valid) {
				outTriangles.add(adjacency.triangles[i].vertices[0])
				outTriangles.add(adjacency.triangles[i].vertices[1])
				outTriangles.add(adjacency.triangles[i].vertices[2])
			}
		}
	}

	return outTriangles
}

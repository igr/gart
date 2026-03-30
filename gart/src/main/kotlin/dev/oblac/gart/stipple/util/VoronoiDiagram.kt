package dev.oblac.gart.stipple.util

import dev.oblac.gart.vector.Vec2
import kotlin.math.abs

/**
 * Voronoi diagram generated from the dual of a Delaunay triangulation.
 */
internal class VoronoiEdge(
	val v0: Int,
	val v1: Int,
	val cellA: Int,
	val cellB: Int
)

internal class VoronoiDiagram(
	val vertices: MutableList<Vec2>,
	val edges: MutableList<VoronoiEdge>
) {
	companion object {
		fun fromDelaunay(adjacency: Adjacency): VoronoiDiagram {
			val vertices = mutableListOf<Vec2>()
			val edges = mutableListOf<VoronoiEdge>()

			val triangleRemapping = IntArray(adjacency.triangles.size) { -1 }

			for (i in adjacency.triangles.indices) {
				val t = adjacency.triangles[i]
				if (!t.valid) continue
				val cc = t.circumCircle(adjacency.vertices) ?: continue
				triangleRemapping[i] = vertices.size
				vertices.add(cc.first)
			}

			for (i in adjacency.triangles.indices) {
				val t = adjacency.triangles[i]
				if (!t.valid) continue

				for (e in 0..2) {
					val adjT = adjacency.adjacentTriangle(i, e)
					if (adjT < 0 || !adjacency.triangles[adjT].valid) continue
					if (triangleRemapping[adjT] < 0) continue

					val voronoiV0 = triangleRemapping[i]
					val voronoiV1 = triangleRemapping[adjT]
					if (voronoiV0 < 0 || voronoiV1 < 0) continue

					val edgeGlobalIdx = abs(t.edges[e]) - 1
					val cellA = adjacency.edges[edgeGlobalIdx].vertices[0]
					val cellB = adjacency.edges[edgeGlobalIdx].vertices[1]

					edges.add(VoronoiEdge(voronoiV0, voronoiV1, cellA, cellB))
				}
				triangleRemapping[i] = -1
			}

			return VoronoiDiagram(vertices, edges)
		}
	}
}

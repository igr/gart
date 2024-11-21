package dev.oblac.gart.triangulation

import dev.oblac.gart.gfx.Line
import dev.oblac.gart.gfx.Triangle
import org.jetbrains.skia.Point

data class VoronoiCell(val site: Point, val edges: List<Line>) {
    /**
     * Converts the Voronoi cell to a list of points that represent the cell's path.
     */
    @Suppress("t")
    fun toPathPoints(): List<Point> {
        if (edges.isEmpty()) {
            return emptyList()
        }

        // Start with the first edge
        val path = mutableListOf<Point>()
        val remainingEdges = edges.toMutableList()

        // Add the first edge's start and end points
        val firstEdge = remainingEdges.removeAt(0)
        path.add(firstEdge.a)
        path.add(firstEdge.b)

        // Sequentially find the next edge that connects to the current path
        while (remainingEdges.isNotEmpty()) {
            // try last
            val lastPoint = path.last()
            val nextEdgeIndexForLast = remainingEdges.indexOfFirst { edge ->
                (edge.a == lastPoint || edge.b == lastPoint)
            }
            if (nextEdgeIndexForLast != -1) {
                // Get the next edge and add the new point to the path
                val nextEdge = remainingEdges.removeAt(nextEdgeIndexForLast)
                val nextPoint = if (nextEdge.a == lastPoint) nextEdge.b else nextEdge.a
                path.add(nextPoint)
                continue
            }

            // try first point
            val firstPoint = path.first()
            val nextEdgeIndexForFirst = remainingEdges.indexOfFirst { edge ->
                (edge.a == firstPoint || edge.b == firstPoint)
            }
            if (nextEdgeIndexForFirst != -1) {
                // Get the next edge and add the new point to the path
                val nextEdge = remainingEdges.removeAt(nextEdgeIndexForFirst)
                val nextPoint = if (nextEdge.a == firstPoint) nextEdge.b else nextEdge.a
                path.add(0, nextPoint)
                continue
            }
            throw IllegalStateException("Could not find a next edge for the current path")
        }

        return path
    }

}

fun delaunayToVoronoi(triangles: List<Triangle>): List<VoronoiCell> {
    // Map each site to its Voronoi cell edges
    val voronoiEdges = mutableMapOf<Point, MutableList<Line>>()

    // Cache circumcenters for all triangles
    val circumcenters = mutableMapOf<Triangle, Point>()
    for (triangle in triangles) {
        circumcenters[triangle] = triangle.calculateCircumcircle().center
    }

    // Find neighboring triangles for each edge
    val edgeToTriangles = mutableMapOf<Pair<Point, Point>, MutableList<Triangle>>()
    for (triangle in triangles) {
        for (edge in listOf(
            Pair(triangle.a, triangle.b),
            Pair(triangle.b, triangle.c),
            Pair(triangle.c, triangle.a)
        )) {
            val normalizedEdge = normalizeEdge(edge)
            edgeToTriangles.getOrPut(normalizedEdge) { mutableListOf() }.add(triangle)
        }
    }

    // Create Voronoi edges from Delaunay edges
    for ((edge, edgeTriangles) in edgeToTriangles) {
        if (edgeTriangles.size == 2) {
            // Shared edge: Create a Voronoi edge between circumcenters
            val circumcenter1 = circumcenters[edgeTriangles[0]]!!
            val circumcenter2 = circumcenters[edgeTriangles[1]]!!
            val voronoiEdge = Line(circumcenter1, circumcenter2)

            // Assign edge to both sites of the Delaunay edge
            voronoiEdges.getOrPut(edge.first) { mutableListOf() }.add(voronoiEdge)
            voronoiEdges.getOrPut(edge.second) { mutableListOf() }.add(voronoiEdge)
        }
    }

    // Build Voronoi cells
    return voronoiEdges.map { (site, edges) ->
        VoronoiCell(site, edges)
    }
}

// Helper to normalize edges to a consistent order
fun normalizeEdge(edge: Pair<Point, Point>): Pair<Point, Point> {
    return if (edge.first.x < edge.second.x || (edge.first.x == edge.second.x && edge.first.y < edge.second.y)) {
        edge
    } else {
        edge.second to edge.first
    }
}

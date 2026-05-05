package dev.oblac.gart.dynagraph

import dev.oblac.gart.painter.SprayPainter
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder

/**
 * Returns a Skia [Path] containing every edge of [group] as an independent
 * `moveTo`+`lineTo` pair. Works for arbitrary topology (branched, multiple
 * components). For chain/loop-only groups prefer [groupAsPath].
 */
fun DynaGraph.toPath(group: GroupId = DynaGraph.MAIN): Path {
    val pb = PathBuilder()
    val g = groupOrNull(group) ?: return pb.detach()
    for (e in g.edges()) {
        pb.moveTo(x(e.a), y(e.a))
        pb.lineTo(x(e.b), y(e.b))
    }
    return pb.detach()
}

/** Strokes every edge of [group] with [paint]. */
fun Canvas.drawDynaGraphEdges(graph: DynaGraph, group: GroupId = DynaGraph.MAIN, paint: Paint) {
    val g = graph.groupOrNull(group) ?: return
    for (e in g.edges()) {
        drawLine(graph.x(e.a), graph.y(e.a), graph.x(e.b), graph.y(e.b), paint)
    }
}

/** Draws a small filled circle at every vertex in [group]. */
fun Canvas.drawDynaGraphVertices(graph: DynaGraph, group: GroupId = DynaGraph.MAIN, paint: Paint, radius: Float = 1f) {
    val g = graph.groupOrNull(group) ?: return
    for (v in g.vertices()) {
        drawCircle(graph.x(v), graph.y(v), radius, paint)
    }
}

/**
 * Strokes [group] as a single Path when it forms a chain or loop (via
 * [groupAsPath]); otherwise falls back to per-edge stroking.
 */
fun Canvas.drawDynaGraph(graph: DynaGraph, group: GroupId = DynaGraph.MAIN, paint: Paint) {
    val pathOrNull = graph.groupAsPath(group)
    if (pathOrNull != null) {
        drawPath(pathOrNull, paint)
    } else {
        drawDynaGraphEdges(graph, group, paint)
    }
}

/**
 * For each edge in [group], paints [samplesPerEdge] random points along the
 * edge with the spray painter's current foreground.
 */
fun SprayPainter.drawDynaGraphEdges(graph: DynaGraph, group: GroupId = DynaGraph.MAIN, samplesPerEdge: Int = 50) {
    val g = graph.groupOrNull(group) ?: return
    for (e in g.edges()) {
        stroke(graph.pos(e.a), graph.pos(e.b), samplesPerEdge)
    }
}

/** Paints one pixel per vertex in [group] with the current foreground. */
fun SprayPainter.drawDynaGraphVertices(graph: DynaGraph, group: GroupId = DynaGraph.MAIN) {
    val g = graph.groupOrNull(group) ?: return
    for (v in g.vertices()) pixel(graph.x(v), graph.y(v))
}

/**
 * Paints [samples] random disc points around every vertex in [group]
 * (radius [radius]).
 */
fun SprayPainter.drawDynaGraphCircle(graph: DynaGraph, group: GroupId = DynaGraph.MAIN, radius: Float, samples: Int = 30) {
    val g = graph.groupOrNull(group) ?: return
    for (v in g.vertices()) circle(graph.x(v), graph.y(v), radius, samples)
}

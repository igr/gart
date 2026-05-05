package dev.oblac.gart

import dev.oblac.gart.color.CssColors
import dev.oblac.gart.dynagraph.DynaGraph
import dev.oblac.gart.dynagraph.drawDynaGraphEdges
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.math.TWO_PIf
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.Point
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hyphae-style branching growth, exercising the features
 * that justify DynaGraph over a plain polyline:
 *   - **arbitrary topology**: a branching tree (not a chain or loop)
 *   - **`appendEdgeSegX(mustIntersect = false)`**: only grows a new edge
 *     when it doesn't cross any existing edge in the group; this is what
 *     keeps the dendrite from self-intersecting
 *   - **stable vertex IDs across mutations**: `tips` holds Int IDs that
 *     remain valid even as new vertices are appended around them
 */
fun main() {
    val gart = Gart.of("dynagraph-hyphae", 1024, 1024, fps = 30)
    val w = gart.window()
    val d = gart.d

    val graph = DynaGraph()
    val tips = ArrayList<Int>()

    // Seed: 5 points on a small circle, each with a 1-edge stub so the tip
    // has a definable heading from the start.
    val seeds = 5
    for (i in 0 until seeds) {
        val a = i * TWO_PIf / seeds
        val anchor = graph.addVert(d.cx + cos(a) * 20f, d.cy + sin(a) * 20f).newVert!!
        val tip = graph.addVert(d.cx + cos(a) * 40f, d.cy + sin(a) * 40f).newVert!!
        graph.addEdge(anchor, tip)
        tips.add(tip)
    }

    val maxVerts = 5000
    val trunk = strokeOf(CssColors.darkSlateGray, 1.3f)
    val tipFill = fillOf(CssColors.crimson)

    w.show { c, _, f ->
        if (f.new && graph.numVerts < maxVerts) {
            growTips(graph, tips)
        }
        c.clear(0xFFFAF8F2.toInt())
        c.drawDynaGraphEdges(graph, paint = trunk)
        for (t in tips) c.drawCircle(graph.x(t), graph.y(t), 1.8f, tipFill)
    }
}

private const val STEP = 5f
private const val JITTER = 0.35f
private const val BRANCH_PROB = 0.1f

private fun growTips(graph: DynaGraph, tips: ArrayList<Int>) {
    val next = ArrayList<Int>(tips.size + 4)
    for (tip in tips) {
        val a = heading(graph, tip) + rndf(-JITTER, JITTER)
        val grown = graph.appendEdgeSegX(
            tip, Point(cos(a) * STEP, sin(a) * STEP), mustIntersect = false,
        ).newVert

        if (grown != null) {
            next.add(grown)
            if (rndf(0f, 1f) < BRANCH_PROB) {
                val sgn = if (rndf(0f, 1f) < 0.5f) 1f else -1f
                val ab = a + sgn * (0.7f + rndf(0f, 0.3f))
                val branch = graph.appendEdgeSegX(
                    tip, Point(cos(ab) * STEP, sin(ab) * STEP), mustIntersect = false,
                ).newVert
                if (branch != null) next.add(branch)
            }
        }
        // dead tips (blocked by an existing edge) just drop out
    }
    tips.clear(); tips.addAll(next)
}

private fun heading(graph: DynaGraph, v: Int): Float {
    val ns = graph.group().neighbors(v)
    if (ns.isEmpty()) return rndf(0f, TWO_PIf)
    val prev = ns.first()
    return atan2(graph.y(v) - graph.y(prev), graph.x(v) - graph.x(prev))
}

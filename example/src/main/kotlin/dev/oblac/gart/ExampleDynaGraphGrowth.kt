package dev.oblac.gart

import dev.oblac.gart.dynagraph.DynaGraph
import dev.oblac.gart.dynagraph.addCircle
import dev.oblac.gart.dynagraph.drawDynaGraphEdges
import dev.oblac.gart.math.rndf
import dev.oblac.gart.math.rndsgn
import dev.oblac.gart.painter.SprayPainter
import org.jetbrains.skia.Point

/**
 * ExampleDynaGraphGrowth: animated demonstration of batched mutations.
 * Each frame:
 *   - generates one split-edge mutation per existing edge whose length is
 *     above a threshold (densifying the curve)
 *   - generates a small Brownian MoveVert for each vertex (organic wobble)
 * Applies the mutations, then renders via SprayPainter.
 */
fun main() {
    val size = 1024
    val gart = Gart.of("dynagraph-growth", size, size, fps = 30)
    val w = gart.window()
    val d = gart.d

    val dyg = DynaGraph()
    dyg.addCircle(Point(d.cx, d.cy), 220f, 36)

    val splitThreshold = 28f
    val wobble = 1.6f

    val sp = SprayPainter(size, size, bg = 0x00FFFFFF, fg = 0x20101820)

    w.show { c, _, f ->
        if (f.new) {
            val splits = dyg.group().edges()
                .filter { dyg.edgeLength(it) > splitThreshold }
                .toList()
            for (e in splits) dyg.splitEdge(e.a, e.b)

            val vs = dyg.group().vertices()
            for (v in vs) {
                dyg.moveVert(
                    v,
                    Point(rndsgn() * rndf(0f, wobble), rndsgn() * rndf(0f, wobble)),
                )
            }
        }

        c.clear(0xFFFAF8F2.toInt())
        sp.clear()
        sp.drawDynaGraphEdges(dyg, samplesPerEdge = 60)
        sp.drawTo(c)
    }
}

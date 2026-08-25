package dev.oblac.gart

import dev.oblac.gart.colonization.SpaceColonization
import dev.oblac.gart.colonization.Venation
import dev.oblac.gart.gfx.strokeOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.math.rndf
import org.jetbrains.skia.PaintStrokeCap
import kotlin.random.Random

/**
 * ExampleSpaceColonization: the same food field grown twice.
 * Left, open venation - every attractor pulls only its nearest branch: a tree.
 * Right, closed venation - attractors pull their whole relative neighborhood,
 * so branches converge on the food and meet. Width comes from canalization.
 */
fun main(args: Array<String>) {
    val gart = Gart.of("space-colonization", 1600, 800)
    val g = gart.gartvas()
    val c = g.canvas
    val d = gart.d

    c.clear(0xFFF6F2E9.toInt())

    fun grow(venation: Venation, x0: Float) {
        val rnd = Random(11)
        val sc = SpaceColonization(
            attractionDistance = 42f,
            killDistance = 8f,
            segmentLength = 5f,
            venation = venation,
            rnd = rnd,
            allowed = { it.x >= x0 + 30f && it.x <= x0 + 770f && it.y in 30f..(d.hf - 30f) },
        )

        // jittered grid of food, randomly thinned
        var y = 60f
        while (y < d.hf - 60f) {
            var x = x0 + 60f
            while (x < x0 + 740f) {
                if (rnd.rndf() < 0.8f) sc.addAttractor(x + rnd.rndf(-14f, 14f), y + rnd.rndf(-14f, 14f))
                x += 16f
            }
            y += 16f
        }
        sc.addRoot(x0 + 400f, d.hf - 40f)
        sc.grow()
        sc.canalize()

        for (n in sc.nodes) {
            val p = n.parent ?: continue
            c.drawLine(
                p.position.x, p.position.y, n.position.x, n.position.y,
                strokeOf(0xFF2B2118.toInt(), 0.6f + n.thickness).apply { strokeCap = PaintStrokeCap.ROUND }
            )
        }
    }

    grow(Venation.OPEN, 0f)
    grow(Venation.CLOSED, 800f)

    gart.saveImage(g)
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

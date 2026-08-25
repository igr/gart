package sixsix.two

import dev.oblac.gart.Gartvas
import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.*
import dev.oblac.gart.math.GOLDEN_RATIOf
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Point
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import kotlin.math.cos
import kotlin.math.sin

internal val cells = listOf(
    ::cell1,
    ::cell2,
    ::cell3,
    ::cell4,
    ::cell5,
    ::cell6,
    ::cell7,
    ::cell8,
    ::cell9,
    ::cell10,
    ::cell11,
    ::cell12,
    ::cell13,
    ::cell14,
    ::cell15,
    ::cell16,
    ::cell17,
    ::cell18,
    ::cell19,
    ::cell20,
    ::cell21,
    ::cell22,
    ::cell23,
    ::cell24,
    ::cell25,
    ::cell26,
    ::cell27,
    ::cell28,
    ::cell29,
    ::cell30,
    ::cell31,
    ::cell32,
    ::cell33,
    ::cell34,
    ::cell35,
    ::cell36,
)

private const val RATIO = 1f / GOLDEN_RATIOf
private const val LW = 20f  // line width

private fun cell1(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // arch rising from the bottom
    val r = d.wf * 0.28f
    c.drawCircle(d.cx, d.cy, r, fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.cx - r, d.cy, r * 2, d.cy), fillOf(p[1]))
    c.drawCircle(d.wf * 0.82f, d.hf * 0.18f, d.wf * 0.08f, fillOf(p[2]))
}

private fun cell2(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // tunnel of arches
    c.drawCircle(d.cx, d.hf, d.cx * 0.9f, fillOf(p[1]))
    c.drawCircle(d.cx, d.hf, d.cx * 0.6f, fillOf(p[2]))
    c.drawCircle(d.cx, d.hf, d.cx * 0.3f, fillOf(p[3]))
}

private fun cell3(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val t = d.hf * 0.25f
    val k1 = d.hf * 0.1f
    listOf(
        Point(0f, k1),
        Point(0f, k1 + t),
        Point(d.wf - k1 - t, d.hf),
        Point(d.wf - k1, d.hf),
    ).toClosedPath().let { c.drawPath(it, fillOf(p[1])) }

    val k2 = d.wf * 0.15f
    listOf(
        Point(k2, 0f),
        Point(k2 + t, 0f),
        Point(d.wf, d.wf - k2 - t),
        Point(d.wf, d.wf - k2),
    ).toClosedPath().let { c.drawPath(it, fillOf(p[2])) }
}

private fun cell4(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // zigzag band across the middle
    val zh = d.hf * 0.12f
    val bt = d.hf * 0.22f
    val step = d.wf / 4f
    val path = PathBuilder()
    path.moveTo(0f, d.cy - zh)
    for (i in 1..4) {
        path.lineTo(i * step, d.cy + (if (i % 2 == 1) zh else -zh))
    }
    for (i in 4 downTo 0) {
        path.lineTo(i * step, d.cy + (if (i % 2 == 1) zh else -zh) + bt)
    }
    path.closePath()
    c.drawPath(path.detach(), fillOf(p[1]))

    c.drawCircle(d.wf * 0.82f, d.hf * 0.16f, d.wf * 0.09f, fillOf(p[2]))
}

private fun cell5(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // two circles, lens where they meet
    val r = d.cx * 0.62f
    val c1 = Point(d.cx - r * 0.45f, d.cy)
    val c2 = Point(d.cx + r * 0.45f, d.cy)
    c.drawCircle(c1, r, fillOf(p[1]))
    c.drawCircle(c2, r, fillOf(p[2]))
    c.save()
    c.clipPath(Circle.of(c1, r).toPath())
    c.drawCircle(c2, r, fillOf(p[3]))
    c.restore()
}

private fun cell6(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // petals curling in from the edges
    val r = d.cx * 0.55f
    c.drawCircle(Point(d.cx, 0f), r, fillOf(p[1]))
    c.drawCircle(Point(d.wf, d.cy), r, fillOf(p[2]))
    c.drawCircle(Point(d.cx, d.hf), r, fillOf(p[1]))
    c.drawCircle(Point(0f, d.cy), r, fillOf(p[2]))
    c.drawCircle(d.center, d.cx * 0.25f, fillOf(p[3]))
}

private fun cell7(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val r = d.cx * 0.62f
    c.drawCircle(d.center, r, fillOf(p[1]))
    c.drawCircle(d.cx + r * 0.35f, d.cy - r * 0.25f, r, fillOf(p[0]))
    c.drawCircle(d.wf * 0.2f, d.hf * 0.78f, d.wf * 0.07f, fillOf(p[2]))
}

private fun cell8(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // off center target
    c.drawCircle(d.w3, d.h3x2, d.wf * 0.42f, fillOf(p[1]))
    c.drawCircle(d.w3, d.h3x2, d.wf * 0.28f, fillOf(p[2]))
    c.drawCircle(d.w3, d.h3x2, d.wf * 0.14f, fillOf(p[3]))
}

private fun cell9(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val bw = d.wf * 0.3f
    c.drawRect(Rect.makeXYWH(d.cx - bw / 2, 0f, bw, d.hf), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(0f, d.cy - bw / 2, d.wf, bw), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.cx - bw / 2, d.cy - bw / 2, bw, bw), fillOf(p[2]))
}

private fun cell10(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    fun diamond(s: Float) = listOf(
        Point(d.cx, d.cy - s),
        Point(d.cx + s, d.cy),
        Point(d.cx, d.cy + s),
        Point(d.cx - s, d.cy),
    ).toClosedPath()

    c.drawPath(diamond(d.cx), fillOf(p[1]))
    c.drawPath(diamond(d.cx * 0.55f), fillOf(p[2]))
    c.drawPath(diamond(d.cx * 0.2f), fillOf(p[3]))
}

private fun cell11(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // bowtie
    val s = d.hf * 0.35f
    c.drawTriangle(Triangle(Point(0f, d.cy - s), Point(0f, d.cy + s), d.center), fillOf(p[1]))
    c.drawTriangle(Triangle(Point(d.wf, d.cy - s), Point(d.wf, d.cy + s), d.center), fillOf(p[2]))
    c.drawCircle(d.center, d.wf * 0.08f, fillOf(p[3]))
}

private fun cell12(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // rainbow rings in the corner
    c.drawCircle(d.leftTop, d.wf * 0.95f, fillOf(p[1]))
    c.drawCircle(d.leftTop, d.wf * 0.68f, fillOf(p[0]))
    c.drawCircle(d.leftTop, d.wf * 0.42f, fillOf(p[2]))
    c.drawCircle(d.leftTop, d.wf * 0.18f, fillOf(p[3]))
}

private fun cell13(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // downtown
    c.drawRect(Rect.makeXYWH(d.wf * 0.12f, d.hf * 0.45f, d.wf * 0.18f, d.hf * 0.55f), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.wf * 0.42f, d.hf * 0.2f, d.wf * 0.18f, d.hf * 0.8f), fillOf(p[2]))
    c.drawRect(Rect.makeXYWH(d.wf * 0.72f, d.hf * 0.65f, d.wf * 0.18f, d.hf * 0.35f), fillOf(p[3]))
}

private fun cell14(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // scallop wave
    val top = d.hf * 0.55f
    val r = d.wf / 6f
    c.drawRect(Rect.makeXYWH(0f, top, d.wf, d.hf - top), fillOf(p[1]))
    c.drawCircle(r, top, r, fillOf(p[1]))
    c.drawCircle(d.cx, top, r, fillOf(p[1]))
    c.drawCircle(d.wf - r, top, r, fillOf(p[1]))
    c.drawCircle(d.wf * 0.8f, d.hf * 0.2f, d.wf * 0.09f, fillOf(p[2]))
}

private fun cell15(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawRect(Rect.makeXYWH(0f, 0f, d.cx, d.cy), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.cx, d.cy, d.cx, d.cy), fillOf(p[1]))
    c.drawCircle(d.center, d.wf * 0.22f, fillOf(p[2]))
}

private fun cell16(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val r = d.wf * 0.11f
    for (i in 0 until 3) {
        for (j in 0 until 3) {
            val col = if (i == 1 && j == 1) p[2] else p[1]
            c.drawCircle((i + 0.5f) * d.wf / 3f, (j + 0.5f) * d.hf / 3f, r, fillOf(col))
        }
    }
}

private fun cell17(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // fan opening from the corner
    val r = d.wf * 0.85f
    val rect = Rect.makeXYWH(-r, d.hf - r, r * 2, r * 2)
    c.drawArc(rect, -90f, 90f, true, fillOf(p[1]))
    c.drawArc(rect, -90f, 45f, true, fillOf(p[2]))
}

private fun cell18(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // the eye
    val r = d.wf * 0.75f
    val off = d.wf * 0.55f
    val ca = Point(d.cx, d.cy - off)
    val cb = Point(d.cx, d.cy + off)
    c.save()
    c.clipPath(Circle.of(ca, r).toPath())
    c.drawCircle(cb, r, fillOf(p[1]))
    c.restore()
    c.drawCircle(d.center, d.wf * 0.16f, fillOf(p[2]))
    c.drawCircle(d.cx + d.wf * 0.05f, d.cy - d.hf * 0.05f, d.wf * 0.05f, fillOf(p[0]))
}

private fun cell19(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawRect(Rect.makeXYWH(d.wf * 0.15f, d.hf * 0.15f, d.wf * 0.25f, d.hf * 0.7f), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.wf * 0.15f, d.hf * 0.6f, d.wf * 0.7f, d.hf * 0.25f), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.wf * 0.52f, d.hf * 0.22f, d.wf * 0.26f, d.hf * 0.26f), fillOf(p[2]))
}

private fun cell20(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // half solid, half ring
    val r = d.cx * 0.62f
    c.save()
    c.clipRect(Rect.makeXYWH(0f, 0f, d.cx, d.hf))
    c.drawCircle(d.center, r, fillOf(p[1]))
    c.restore()
    c.save()
    c.clipRect(Rect.makeXYWH(d.cx, 0f, d.cx, d.hf))
    c.drawCircle(d.center, r, fillOf(p[2]))
    c.drawCircle(d.center, r * 0.55f, fillOf(p[0]))
    c.restore()
}

private fun cell21(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawCircle(d.wf * 0.2f, d.hf * 0.2f, d.wf * 0.1f, fillOf(p[3]))
    c.drawTriangle(Triangle(Point(d.wf * 0.35f, d.hf), Point(d.wf * 0.75f, d.hf * 0.15f), Point(d.wf * 1.15f, d.hf)), fillOf(p[2]))
    c.drawTriangle(Triangle(Point(0f, d.hf), Point(d.wf * 0.45f, d.hf * 0.35f), Point(d.wf * 0.9f, d.hf)), fillOf(p[1]))
}

private fun cell22(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // squares tucked in the corner
    fun sq(s: Float, col: Int) = c.drawRect(Rect.makeXYWH(d.wf - s, d.hf - s, s, s), fillOf(col))
    sq(d.wf * 0.8f, p[1])
    sq(d.wf * 0.52f, p[2])
    sq(d.wf * 0.26f, p[3])
}

private fun cell23(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val r = d.wf * 0.38f
    val rect = Rect.makeXYWH(d.cx - r, d.cy - r, r * 2, r * 2)
    c.drawArc(rect, 30f, 300f, true, fillOf(p[1]))
    c.drawCircle(d.cx + r * 0.1f, d.cy - r * 0.5f, d.wf * 0.06f, fillOf(p[2]))
}

private fun cell24(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // orbit with a satellite
    val r = d.cx * RATIO
    c.drawCircle(d.center, r, strokeOf(p[1], LW))
    val a = Math.toRadians(-45.0).toFloat()
    c.drawCircle(d.cx + r * cos(a), d.cy + r * sin(a), d.wf * 0.14f, fillOf(p[2]))
}

private fun cell25(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // sun going down, stripes of water
    c.save()
    c.clipRect(Rect.makeXYWH(0f, 0f, d.wf, d.cy))
    c.drawCircle(d.center, d.wf * 0.32f, fillOf(p[1]))
    c.restore()
    c.drawRect(Rect.makeXYWH(0f, d.hf * 0.58f, d.wf, d.hf * 0.07f), fillOf(p[2]))
    c.drawRect(Rect.makeXYWH(0f, d.hf * 0.72f, d.wf, d.hf * 0.07f), fillOf(p[2]))
    c.drawRect(Rect.makeXYWH(0f, d.hf * 0.86f, d.wf, d.hf * 0.07f), fillOf(p[2]))
}

private fun cell26(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawTriangle(Triangle(d.leftTop, d.rightTop, d.leftBottom), fillOf(p[1]))
    c.drawCircle(d.wf * 0.68f, d.hf * 0.68f, d.wf * 0.15f, fillOf(p[2]))
    c.drawRect(Rect.makeXYWH(d.wf * 0.15f, d.hf * 0.15f, d.wf * 0.18f, d.hf * 0.18f), fillOf(p[3]))
}

private fun cell27(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // four panes, one lit
    val s = d.wf * 0.32f
    val rr = d.wf * 0.06f
    fun pane(x: Float, y: Float, col: Int) = c.drawRRect(RRect.makeXYWH(x, y, s, s, rr), fillOf(col))
    pane(d.wf * 0.1f, d.hf * 0.1f, p[1])
    pane(d.wf * 0.58f, d.hf * 0.1f, p[1])
    pane(d.wf * 0.1f, d.hf * 0.58f, p[1])
    pane(d.wf * 0.58f, d.hf * 0.58f, p[2])
}

private fun cell28(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // bullseye on the edge
    c.drawCircle(Point(0f, d.cy), d.cx * 0.85f, fillOf(p[1]))
    c.drawCircle(Point(0f, d.cy), d.cx * 0.55f, fillOf(p[0]))
    c.drawCircle(Point(0f, d.cy), d.cx * 0.28f, fillOf(p[2]))
}

private fun cell29(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // pennant
    c.drawRect(Rect.makeXYWH(d.wf * 0.14f, d.hf * 0.1f, d.wf * 0.05f, d.hf * 0.8f), fillOf(p[2]))
    c.drawTriangle(
        Triangle(
            Point(d.wf * 0.19f, d.hf * 0.12f),
            Point(d.wf * 0.78f, d.hf * 0.28f),
            Point(d.wf * 0.19f, d.hf * 0.44f)
        ),
        fillOf(p[1])
    )
}

private fun cell30(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    val t = d.hf * 0.2f
    var k = d.hf * 0.25f
    for (i in 1..3) {
        listOf(
            Point(0f, k),
            Point(0f, k + t),
            Point(k + t, 0f),
            Point(k, 0f),
        ).toClosedPath().let { c.drawPath(it, fillOf(p[i])) }
        k += d.hf * 0.3f
    }
}

private fun cell31(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // sunrise
    c.save()
    c.clipRect(Rect.makeXYWH(0f, 0f, d.wf, d.cy))
    c.drawCircle(d.center, d.wf * 0.5f, fillOf(p[1]))
    c.restore()
    c.drawRect(Rect.makeXYWH(0f, d.cy, d.wf, d.hf * 0.05f), fillOf(p[2]))
    c.drawCircle(d.cx, d.hf * 0.24f, d.wf * 0.07f, fillOf(p[3]))
}

private fun cell32(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawCircle(d.leftTop, d.wf * 0.55f, fillOf(p[1]))
    c.drawCircle(d.rightBottom, d.wf * 0.55f, fillOf(p[1]))
    c.drawCircle(d.center, d.wf * 0.3f, strokeOf(p[2], LW))
}

private fun cell33(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // domino
    val inset = d.wf * 0.12f
    c.drawRRect(
        RRect.makeXYWH(inset, inset, d.wf - 2 * inset, d.hf - 2 * inset, d.wf * 0.08f),
        strokeOf(p[1], LW * 0.6f)
    )
    c.drawCircle(d.wf * 0.34f, d.hf * 0.34f, d.wf * 0.13f, fillOf(p[2]))
    c.drawCircle(d.wf * 0.66f, d.hf * 0.66f, d.wf * 0.13f, fillOf(p[3]))
}

private fun cell34(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    c.drawRect(Rect.makeXYWH(0f, 0f, d.w3, d.hf), fillOf(p[1]))
    c.drawRect(Rect.makeXYWH(d.w3x2, 0f, d.w3, d.hf), fillOf(p[2]))
    c.drawTriangle(
        Triangle(
            Point(d.cx - d.wf * 0.12f, d.hf),
            Point(d.cx + d.wf * 0.12f, d.hf),
            Point(d.cx, d.hf * 0.76f)
        ),
        fillOf(p[3])
    )
}

private fun cell35(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // unwinding
    fun arcRect(r: Float) = Rect.makeXYWH(d.cx - r, d.cy - r, r * 2, r * 2)
    c.drawArc(arcRect(d.cx * 0.8f), -90f, 270f, false, strokeOf(p[1], LW))
    c.drawArc(arcRect(d.cx * 0.52f), 90f, 200f, false, strokeOf(p[2], LW))
    c.drawArc(arcRect(d.cx * 0.26f), -90f, 130f, false, strokeOf(p[3], LW))
}

private fun cell36(g: Gartvas, p: Palette) {
    val d = g.d
    val c = g.canvas

    c.clear(p[0])

    // umbrella
    c.save()
    c.clipRect(Rect.makeXYWH(0f, 0f, d.wf, d.hf * 0.55f))
    c.drawCircle(d.cx, d.hf * 0.55f, d.wf * 0.45f, fillOf(p[1]))
    c.restore()
    c.drawRect(Rect.makeXYWH(d.cx - d.wf * 0.03f, d.hf * 0.55f, d.wf * 0.06f, d.hf * 0.33f), fillOf(p[2]))
    c.drawCircle(d.cx + d.wf * 0.09f, d.hf * 0.88f, d.wf * 0.06f, fillOf(p[3]))
}

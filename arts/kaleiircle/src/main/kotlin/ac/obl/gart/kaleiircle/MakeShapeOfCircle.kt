package ac.obl.gart.kaleiircle

import ac.obl.gart.Box
import ac.obl.gart.Shape
import ac.obl.gart.gfx.Colors
import ac.obl.gart.gfx.fillOf
import ac.obl.gart.math.cosd
import ac.obl.gart.math.sind
import ac.obl.gart.skia.*

class MakeShapeOfCircle(private val box: Box) {
    operator fun invoke(circle: DHCircle): Shape {
        val alfa = circle.innerAngle / 2

        val cx = box.w / 2f
        val cy = box.h / 2f
        val r = circle.radius
        val r2 = r + circle.width
        val r2prim = r2 + 10
        val angle = circle.angle

        val rect = Rect(cx-r, cy-r, cx+r, cy+r)
        val rect2 = Rect(cx-r2, cy-r2, cx+r2, cy+r2)
        val arc1 = Path()
            .addArc(rect, 180f - angle, 180f)
            .lineTo(cx + r2 * cosd(-angle), cy + r2 * sind(-angle))
            .addArc(rect2, -angle, -180f)
            .lineTo(cx - r * cosd(-angle), cy - r * sind(-angle))

        val triangle1 = Path()
            .moveTo(cx, cy)
            .lineTo(cx + r2prim * cosd(-angle - alfa), cy + r2prim * sind(-angle - alfa))
            .lineTo(cx + r2prim * cosd(-angle + alfa), cy + r2prim * sind(-angle + alfa))
            .closePath()
        val triangle1_2 = Path()
            .moveTo(cx, cy)
            .lineTo(cx + r2prim * cosd(-angle), cy + r2prim * sind(-angle))
            .lineTo(cx + r2prim * cosd(-angle + alfa), cy + r2prim * sind(-angle + alfa))
            .closePath()
        val arc2 = Path()
            .addArc(rect, -angle, 180f)
            .lineTo(cx - r2 * cosd(-angle), cy - r2 * sind(-angle))
            .addArc(rect2, 180f - angle, -180f)
            .lineTo(cx + r * cosd(-angle), cy + r * sind(-angle))
        val triangle2 = Path()
            .moveTo(cx, cy)
            .lineTo(cx - r2prim * cosd(-angle - alfa), cy - r2prim * sind(-angle - alfa))
            .lineTo(cx - r2prim * cosd(-angle + alfa), cy - r2prim * sind(-angle + alfa))
            .closePath()
        val triangle2_2 = Path()
            .moveTo(cx, cy)
            .lineTo(cx - r2prim * cosd(-angle), cy - r2prim * sind(-angle))
            .lineTo(cx - r2prim * cosd(-angle + alfa), cy - r2prim * sind(-angle + alfa))
            .closePath()
        val arc1Color = fillOf(circle.colors.first)
            .apply {
                imageFilter = ImageFilter.makeDropShadow(
                    0f, 0f,
                    10f, 10f,
                    Colors.black.toColor()
                )
            }
        val arc2Color = fillOf(circle.colors.second)
            .apply {
                imageFilter = ImageFilter.makeDropShadow(
                    0f, 0f,
                    10f, 10f,
                    Colors.black.toColor()
                )
            }
        val triangle1ColorShadow = fillOf(circle.colors.first).apply {
            imageFilter = ImageFilter.makeDropShadow(
                0f, 0f,
                10f, 0f,
                Colors.black.toColor()
            )
        }
        val triangle1Color = fillOf(circle.colors.first)
        val triangle2ColorShadow = fillOf(circle.colors.second).apply {
            imageFilter = ImageFilter.makeDropShadow(
                0f, 0f,
                10f, 0f,
                Colors.black.toColor()
            )
        }
        val triangle2Color = fillOf(circle.colors.second)

        val waves = MakeWaves(box).invoke(angle * 3)

        val drawCircle = circle.type == DHType.CIRCLE || circle.type == DHType.FULL
        val drawTriangle = circle.type == DHType.TRIANGLE || circle.type == DHType.FULL

        return object : Shape {
            override fun draw(canvas: Canvas) {
                if (drawCircle) {
                    canvas.drawPath(arc1, arc1Color)
                    canvas.drawPath(arc2, arc2Color)
                }
                if (drawTriangle) {
                    canvas.drawPath(triangle1_2, triangle1ColorShadow)
                    canvas.drawPath(triangle1, triangle1Color)
                    canvas.drawPath(triangle2_2, triangle2ColorShadow)
                    canvas.drawPath(triangle2, triangle2Color)
                }
                if (drawCircle) {
                    canvas.save()
                    canvas.clipPath(arc1, ClipMode.INTERSECT)
                    waves.draw(canvas)
                    canvas.restore()
                    canvas.save()
                    canvas.clipPath(arc2, ClipMode.INTERSECT)
                    waves.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }
}


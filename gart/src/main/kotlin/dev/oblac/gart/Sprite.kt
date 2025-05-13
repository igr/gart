package dev.oblac.gart

import dev.oblac.gart.angles.Angle
import dev.oblac.gart.angles.Degrees
import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.Triangle
import org.jetbrains.skia.*

class Sprite(surface: Surface) {
    val image = surface.makeImageSnapshot()

    val d = Dimension(image.width, image.height)

    fun cropRect(x: Float, y: Float, width: Float, height: Float): Sprite {
        val w = width.toInt()
        val h = height.toInt()
        val g = Gartvas(Dimension(w, h))

        val target = g.canvas
        target.save()
        target.clear(Colors.transparent)
        target.translate(-x, -y)
        target.clipRect(Rect.makeXYWH(x, y, width, height))

        target.drawImage(image, 0f, 0f)
        target.restore()
        return of(g)
    }

    fun cropTriangle(p: Point, size: Float, angle: Angle = Degrees.ZERO): Sprite {
        val radius = size / 2
        val triangle = Triangle.equilateral(p, radius, angle)

        val w = size.toInt()
        val h = size.toInt()
        val sprite = Gartvas(Dimension(w, h))

        val target = sprite.canvas
        target.save()
        target.clear(Colors.transparent)
        target.translate(-p.x + radius, -p.y + radius)
        target.rotate(-angle.degrees() + 30f, p.x, p.y) // to keep the triangle upright
        target.clipPath(triangle.path)

        // Draw the source image onto the target canvas
        target.drawImage(image, 0f, 0f)
        target.restore()

        return of(sprite)
    }


    fun draw() = SpriteTransformations(this)

    companion object {
        fun of(gartvas: Gartvas): Sprite {
            return Sprite(gartvas.surface)
        }
    }
}

fun Canvas.drawSprite(sprite: Sprite, fn: (SpriteTransformations) -> SpriteTransformations) {
    fn(SpriteTransformations(sprite)).draw(this)
}


data class SpriteTransformations(
    private val sprite: Sprite,
) {
    private val c = sprite.d.center     // sprite center, all transformations are relative to this point
    private var x = 0f
    private var y = 0f
    private val transformations: MutableList<(Canvas) -> Unit> = mutableListOf()

    fun rotate(degrees: Number) = rotate(degrees, 0, 0)
    fun rotateRB(degrees: Number) = rotate(degrees, sprite.d.cx, sprite.d.cy)
    fun rotateLB(degrees: Number) = rotate(degrees, -sprite.d.cx, sprite.d.cy)

    fun translate(x: Number, y: Number): SpriteTransformations {
        transformations.add {
            it.translate(x.toFloat(), y.toFloat())
        }
        return this
    }

    fun right(x: Number) = translate(x, 0)
    fun left(x: Number) = translate(-x.toFloat(), 0)
    fun down(y: Number) = translate(0, y)

    fun rotate(degrees: Number, x: Number, y: Number): SpriteTransformations {
        transformations.add {
            it.rotate(degrees.toFloat(), c.x + x.toFloat(), c.y + y.toFloat())
        }
        return this
    }

    /**
     * Flips the sprite around the Y axis.
     */
    fun flipHorizontal(): SpriteTransformations {
        transformations.add {
            it.translate(c.x, sprite.d.hf)
            it.scale(-1f, 1f)
            it.translate(-c.x, -sprite.d.hf)
        }
        return this
    }

    fun flipVertical(): SpriteTransformations {
        transformations.add {
            it.translate(sprite.d.wf, c.y)
            it.scale(1f, -1f)
            it.translate(-sprite.d.wf, -c.y)
        }
        return this
    }

    fun scaleX(scale: Number): SpriteTransformations {
        transformations.add {
            it.scale(scale.toFloat(), 1f)
        }
        return this
    }

    fun at(x: Number, y: Number): SpriteTransformations {
        this.x = x.toFloat()
        this.y = y.toFloat()
        return this
    }


    fun draw(canvas: Canvas) {
        canvas.save()

        canvas.translate(x - c.x, y - c.y)

        transformations.reversed().forEach { it(canvas) }

        canvas.drawImage(sprite.image, 0f, 0f, Paint().apply {
            this.isAntiAlias = true
            // need to blur due to Skiko issue https://youtrack.jetbrains.com/issue/SKIKO-1023
            this.imageFilter = ImageFilter.makeBlur(0.1f, 0.1f, FilterTileMode.CLAMP)
        })

        canvas.restore()
    }
}



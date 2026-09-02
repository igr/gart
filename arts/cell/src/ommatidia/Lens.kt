package ommatidia

import dev.oblac.gart.color.colorLift
import dev.oblac.gart.color.colorScale
import dev.oblac.gart.color.gradientOf
import dev.oblac.gart.gfx.paint
import dev.oblac.gart.math.hash01
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.Shader
import kotlin.math.pow
import kotlin.math.sqrt

private const val TREMOR_SEED = 0x4F4D4D41

/** unit light direction. screen y points down, so a light from above has ly negative. */
internal fun lightOf(p: Params): FloatArray {
    val l = sqrt(p.lightX * p.lightX + p.lightY * p.lightY + p.lightZ * p.lightZ)
    return floatArrayOf(p.lightX / l, p.lightY / l, p.lightZ / l)
}

/**
 * draws one lens. the cell is filled with a radial gradient offset toward the light! which
 * is what makes a flat polygon read as convex
 */
internal class LensPainter(private val p: Params, colors: Colors) : AutoCloseable {
    private val pb = PathBuilder()
    private val fill = paint()

    // additive, and deliberately the only thing in the piece that goes brighter than the
    // base colour. the specular peak sits right on top of the diffuse peak, so if the
    // diffuse is allowed to blow out to white as well the glint has nowhere to show -
    // keep ambient + diffuse at or under 1 and this is what you see instead.
    private val glint = paint().apply {
        blendMode = BlendMode.PLUS
    }

    private val lx: Float
    private val ly: Float
    private val lz: Float
    private val hx: Float
    private val hy: Float
    private val hz: Float
    private val glintRgb = colors.glint and 0xFFFFFF

    init {
        val l = lightOf(p)
        lx = l[0]; ly = l[1]; lz = l[2]
        // half vector against a viewer straight on at (0,0,1)
        val vx = lx
        val vy = ly
        val vz = lz + 1f
        val vl = sqrt(vx * vx + vy * vy + vz * vz)
        hx = vx / vl; hy = vy / vl; hz = vz / vl
    }

    fun draw(c: Canvas, f: Facet, base: Int) {
        var nx = f.nx
        var ny = f.ny
        var nz = f.nz
        if (p.tremor > 0f) {
            // every lens is set a hair off true. this is where the glint speckle comes from
            nx += (hash01(f.id, 1, TREMOR_SEED) - 0.5f) * p.tremor
            ny += (hash01(f.id, 2, TREMOR_SEED) - 0.5f) * p.tremor
            val l = sqrt(nx * nx + ny * ny + nz * nz)
            nx /= l; ny /= l; nz /= l
        }

        // colorLift, never lighten - see its docs. the dome centre is the middle of every
        // single facet, so anything that desaturates there desaturates the whole frame
        val lam = (nx * lx + ny * ly + nz * lz).coerceAtLeast(0f)
        val col = colorLift(base, p.ambient + p.diffuse * lam)

        val poly = f.poly
        pb.moveTo(poly[0], poly[1])
        var i = 2
        while (i < poly.size) {
            pb.lineTo(poly[i], poly[i + 1])
            i += 2
        }
        // detach resets the builder, so the same one carries every facet
        val path = pb.closePath().detach()

        val gx = f.x + lx * p.domeOff * f.r
        val gy = f.y + ly * p.domeOff * f.r
        val shader = Shader.makeRadialGradient(
            gx, gy, f.r * p.domeR,
            gradientOf(
                intArrayOf(colorLift(col, 1f + p.domeLift), col, colorScale(col, 1f - p.domeEdge)),
                floatArrayOf(0f, 0.55f, 1f),
            ),
        )
        fill.shader = shader
        c.drawPath(path, fill)
        shader.close()
        path.close()

        val spec = (nx * hx + ny * hy + nz * hz).coerceAtLeast(0f).pow(p.shine)
        if (spec > p.specMin) {
            val a = ((spec - p.specMin) / (1f - p.specMin) * p.specA * 255f).toInt().coerceIn(0, 255)
            glint.color = (a shl 24) or glintRgb
            c.drawCircle(gx, gy, f.r * p.specSize, glint)
        }
    }

    override fun close() {
        pb.close()
        fill.close()
        glint.close()
    }
}

package dev.oblac.gart.march

import dev.oblac.gart.math.degToRad
import dev.oblac.gart.vector.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A pinhole camera that makes rays for [march].
 *
 * It sits at [eye] and looks along +z, turned by [pitch] degrees about x (negative looks down).
 * [fov] is the vertical field of view in degrees. Screen y points down, world y points up.
 * [ss] is the number of rays per pixel side, it only changes [spread].
 */
class RayCamera(val eye: Vec3, pitch: Float, fov: Float, val w: Int, val h: Int, ss: Int = 1) {

    private val tanF = tan(degToRad(fov) / 2f)
    private val cp = cos(degToRad(pitch))
    private val sp = sin(degToRad(pitch))

    /**
     * The angle between two neighboring rays, in radians.
     */
    val spread = 2f * tanF / h / ss

    /**
     * The unit direction through the screen point ([px], [py]), in pixels. It is small enough for
     * the JIT to inline, so in a render loop the vector costs nothing (measured on salum).
     */
    fun ray(px: Float, py: Float): Vec3 {
        val sx = (px - w / 2f) / (h / 2f) * tanF
        val sy = (h / 2f - py) / (h / 2f) * tanF
        val il = 1f / sqrt(sx * sx + sy * sy + 1f)
        return Vec3(sx * il, (sy * cp + sp) * il, (-sy * sp + cp) * il)
    }
}

package dev.oblac.gart.walker

import dev.oblac.gart.gfx.plus
import dev.oblac.gart.gfx.times
import dev.oblac.gart.math.rndInBall
import dev.oblac.gart.math.rndInDisc
import dev.oblac.gart.vector.Vec3
import org.jetbrains.skia.Point

data class Momentum(val pos: Point, val vel: Point = Point(0f, 0f))
data class Momentum3D(val pos: Vec3, val vel: Vec3 = Vec3.ZERO)

fun walkRandom(pos: Point, step: Float = 1f): Point =
    pos + rndInDisc(step)

fun walkMomentum(m: Momentum, accel: Float = 0.05f, damping: Float = 0.95f): Momentum {
    val vel = (m.vel + rndInDisc(accel)) * damping
    return Momentum(m.pos + vel, vel)
}

fun walkRandom3D(pos: Vec3, step: Float = 1f): Vec3 =
    pos + rndInBall(step)

fun walkMomentum3D(m: Momentum3D, accel: Float = 0.05f, damping: Float = 0.95f): Momentum3D {
    val vel = (m.vel + rndInBall(accel)) * damping
    return Momentum3D(m.pos + vel, vel)
}


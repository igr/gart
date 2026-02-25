package dev.oblac.gart.tri3d

import dev.oblac.gart.vector.Vec3
import kotlin.math.cos
import kotlin.math.sin

fun rotateX(v: Vec3, angle: Float): Vec3 {
    val c = cos(angle)
    val s = sin(angle)
    return Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
}

fun rotateY(v: Vec3, angle: Float): Vec3 {
    val c = cos(angle)
    val s = sin(angle)
    return Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
}

fun rotateZ(v: Vec3, angle: Float): Vec3 {
    val c = cos(angle)
    val s = sin(angle)
    return Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z)
}

fun Face.rotateX(angle: Float) = Face(rotateX(a, angle), rotateX(b, angle), rotateX(c, angle), color)
fun Face.rotateY(angle: Float) = Face(rotateY(a, angle), rotateY(b, angle), rotateY(c, angle), color)
fun Face.rotateZ(angle: Float) = Face(rotateZ(a, angle), rotateZ(b, angle), rotateZ(c, angle), color)

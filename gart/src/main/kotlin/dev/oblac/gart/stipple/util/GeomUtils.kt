package dev.oblac.gart.stipple.util

import dev.oblac.gart.vector.Vec2
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ---- Matrix determinants ----

internal fun det3x3(
	m00: Float, m01: Float, m02: Float,
	m10: Float, m11: Float, m12: Float,
	m20: Float, m21: Float, m22: Float
): Float {
	val det01 = m10 * m21 - m11 * m20
	val det02 = m10 * m22 - m12 * m20
	val det12 = m11 * m22 - m12 * m21
	return m00 * det12 - m01 * det02 + m02 * det01
}

internal fun det4x4(
	m00: Float, m01: Float, m02: Float, m03: Float,
	m10: Float, m11: Float, m12: Float, m13: Float,
	m20: Float, m21: Float, m22: Float, m23: Float,
	m30: Float, m31: Float, m32: Float, m33: Float
): Float {
	val d01_01 = m00 * m11 - m01 * m10
	val d01_02 = m00 * m12 - m02 * m10
	val d01_03 = m00 * m13 - m03 * m10
	val d01_12 = m01 * m12 - m02 * m11
	val d01_13 = m01 * m13 - m03 * m11
	val d01_23 = m02 * m13 - m03 * m12

	val d201_012 = m20 * d01_12 - m21 * d01_02 + m22 * d01_01
	val d201_013 = m20 * d01_13 - m21 * d01_03 + m23 * d01_01
	val d201_023 = m20 * d01_23 - m22 * d01_03 + m23 * d01_02
	val d201_123 = m21 * d01_23 - m22 * d01_13 + m23 * d01_12

	return -d201_123 * m30 + d201_023 * m31 - d201_013 * m32 + d201_012 * m33
}

// ---- Geometric predicates ----

internal fun orient2d(a: Vec2, b: Vec2, c: Vec2): Float {
	return det3x3(
		a.x, a.y, 1f,
		b.x, b.y, 1f,
		c.x, c.y, 1f
	)
}

internal fun inCircle(a: Vec2, b: Vec2, c: Vec2, p: Vec2): Float {
	val det = det4x4(
		a.x, a.y, a.x * a.x + a.y * a.y, 1f,
		b.x, b.y, b.x * b.x + b.y * b.y, 1f,
		c.x, c.y, c.x * c.x + c.y * c.y, 1f,
		p.x, p.y, p.x * p.x + p.y * p.y, 1f
	)
	return if (orient2d(a, b, c) > 0f) det else -det
}

// ---- Segment intersection ----

private fun signed2DTriangleArea(a: Vec2, b: Vec2, c: Vec2): Float {
	return (a.x - c.x) * (b.y - c.y) - (a.y - c.y) * (b.x - c.x)
}

internal data class IntersectionResult(val point: Vec2, val intersects: Boolean)

internal fun segmentIntersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): IntersectionResult {
	val a1 = signed2DTriangleArea(a, b, d)
	val a2 = signed2DTriangleArea(a, b, c)

	if (abs(a1) > 1e-3f && abs(a2) > 1e-3f && a1 * a2 < 0f) {
		val a3 = signed2DTriangleArea(c, d, a)
		val a4 = a3 + a2 - a1
		if (a3 * a4 < 0f) {
			val t = a3 / (a3 - a4)
			val p = a + (b - a) * t
			return IntersectionResult(p, true)
		}
	}
	return IntersectionResult(Vec2(-1f, -1f), false)
}

internal fun segmentsIntersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Boolean {
	return segmentIntersect(a, b, c, d).intersects
}

// ---- Point-in-polygon ----

internal fun pointInPolygon(p: Vec2, polygon: List<Vec2>): Boolean {
	if (polygon.size < 4) return false
	// Polygon must be closed (first == last)

	val ys = (0 until polygon.size - 1).map { polygon[it].y }.sorted()
	if (p.y <= ys.first() || p.y >= ys.last()) return false

	val index = ys.binarySearch(p.y).let { if (it < 0) it.inv() else it }
	if (index <= 0 || index >= ys.size) return false

	val approxY = (ys[index - 1] + ys[index]) * 0.5f
	val rectEndPoint = Vec2(p.x + 10000f, approxY)

	var intersections = 0
	for (i in 0 until polygon.size - 1) {
		if (segmentsIntersect(polygon[i], polygon[i + 1], p, rectEndPoint)) {
			intersections++
		}
	}
	return intersections > 0 && intersections % 2 != 0
}

// ---- Bounding box ----

internal class Bounds2f {
	var minX = Float.MAX_VALUE
	var minY = Float.MAX_VALUE
	var maxX = -Float.MAX_VALUE
	var maxY = -Float.MAX_VALUE

	fun addPoint(p: Vec2) {
		minX = min(minX, p.x)
		maxX = max(maxX, p.x)
		minY = min(minY, p.y)
		maxY = max(maxY, p.y)
	}

	fun boundingCircle(): Pair<Vec2, Float> {
		val center = Vec2((maxX + minX) * 0.5f, (maxY + minY) * 0.5f)
		val radius = (center - Vec2(minX, minY)).length()
		return center to radius
	}
}

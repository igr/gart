package dev.oblac.gart.stipple

import dev.oblac.gart.Pixels
import dev.oblac.gart.color.space.luminance
import dev.oblac.gart.color.space.of
import org.jetbrains.skia.Color4f
import kotlin.math.*

/**
 * Stippling using Wang Tiles.
 *
 * Recursively subdivides the image into Wang Tiles and places dots
 * where the local image density exceeds a threshold based on the
 * tile's Poisson disk distribution.
 *
 * Based on: "Recursive Wang Tiles for Real-Time Blue Noise" by Kopf et al.
 *
 * @param tileSet Pre-generated Wang Tile set. Generate via [WangTileSet.generate].
 * @param tonalRange Controls density threshold and subdivision depth.
 *                   Higher = more subdivision = denser stippling. Sensible range: 10000-1000000.
 * @param maxDepth Maximum recursion depth for tile subdivision.
 * @param minSize Minimum tile size in pixels before stopping subdivision.
 * @param foreground Stipple dot color (ARGB).
 * @param background Background color (ARGB).
 */
fun stippleWangTile(
	bitmap: Pixels,
	tileSet: WangTileSet,
	tonalRange: Int = 100_000,
	maxDepth: Int = 5,
	minSize: Int = 8,
	foreground: Int = 0xFF000000.toInt(),
	background: Int = 0xFFFFFFFF.toInt()
) {
	val w = bitmap.d.w
	val h = bitmap.d.h

	// Convert to density map (darkness 0-1)
	val density = FloatArray(w * h)
	for (y in 0 until h) {
		for (x in 0 until w) {
			density[y * w + x] = 1f - Color4f.of(bitmap[x, y]).luminance
		}
	}

	bitmap.fill(background)

	// Start with a random root tile
	val rootTile = abs(tileSet.tiles.size.hashCode()) % tileSet.tiles.size

	refine(
		tileSet, rootTile,
		0, 0, w, h,
		density, w, h,
		minSize, 0, maxDepth, tonalRange,
		bitmap, foreground
	)
}

private fun areaDensity(
	density: FloatArray, imgW: Int,
	rx: Int, ry: Int, rw: Int, rh: Int
): Float {
	var sum = 0f
	for (j in ry until ry + rh) {
		for (i in rx until rx + rw) {
			sum += density[j * imgW + i]
		}
	}
	return sum
}

private fun diskDensity(
	density: FloatArray, imgW: Int, imgH: Int,
	cx: Int, cy: Int, radius: Int
): Float {
	var sum = 0f
	val r2 = radius * radius
	for (j in max(0, cy - radius) until min(imgH, cy + radius)) {
		for (i in max(0, cx - radius) until min(imgW, cx + radius)) {
			val d2 = (i - cx) * (i - cx) + (j - cy) * (j - cy)
			if (d2 > r2) continue
			sum += density[j * imgW + i]
		}
	}
	return sum
}

private fun refine(
	tileSet: WangTileSet,
	tileIndex: Int,
	rx: Int, ry: Int, rw: Int, rh: Int,
	density: FloatArray, imgW: Int, imgH: Int,
	minSize: Int, depth: Int, maxDepth: Int, toneScale: Int,
	bitmap: Pixels, foreground: Int
) {
	val tile = tileSet.tiles[tileIndex]
	val distribution = tile.distribution
	val tileMaxDensity = distribution.size.toFloat()

	// Place stipple dots from the current tile's distribution
	for (i in distribution.indices) {
		val stippleX = rx + (rw * distribution[i].x).toInt()
		val stippleY = ry + (rh * distribution[i].y).toInt()
		if (stippleX < 0 || stippleX >= imgW || stippleY < 0 || stippleY >= imgH) continue

		val r = 1
		val area = (r * r * PI).toFloat()
		val dd = diskDensity(density, imgW, imgH, stippleX, stippleY, r)
		val diskAvgDensity = dd / area

		val factor = (0.1f / 1.0.pow(-2.0).toFloat() * 4.0.pow(2.0 * depth).toFloat() / toneScale)
		if (diskAvgDensity < i * factor) continue
		bitmap[stippleX, stippleY] = foreground
	}

	// Check whether we need to subdivide
	if (rw <= minSize || rh <= minSize || depth == maxDepth) return

	if (0.1.pow(-2.0) / 4.0.pow(2.0 * depth) * toneScale - tileMaxDensity > 16 * tileMaxDensity) {
		val (subd, splitsPerDim) = tileSet.getSubdivisions(tileIndex)
		val childW = rw / splitsPerDim
		val childH = rh / splitsPerDim

		for (j in 0 until splitsPerDim) {
			for (i in 0 until splitsPerDim) {
				val cx = rx + i * childW
				val cy = ry + j * childH
				val cw = if (i == splitsPerDim - 1) rx + rw - cx else childW
				val ch = if (j == splitsPerDim - 1) ry + rh - cy else childH

				refine(
					tileSet, subd[j][i],
					cx, cy, cw, ch,
					density, imgW, imgH,
					minSize, depth + 1, maxDepth, toneScale,
					bitmap, foreground
				)
			}
		}
	}
}

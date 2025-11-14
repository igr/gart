package dev.oblac.gart.pixader

import dev.oblac.gart.Gartmap
import dev.oblac.gart.color.argb
import dev.oblac.gart.color.blendColors
import dev.oblac.gart.math.doubleLoopSequence
import dev.oblac.gart.vector.Vec2
import dev.oblac.gart.vector.Vec4
import kotlinx.coroutines.*

// Pixader is a shader variant for the CPU.
// The whole idea is to have shader-like functionality, where each pixel color is computed
// based on its coordinates and some global parameters (like time, resolution, etc).

typealias PixelFn = (fragCoord: Vec2, iResolution: Vec2, iTime: Float) -> Vec4

/**
 * Draw pixels on the [bmp] using the provided [pixelFunction].
 */
@OptIn(ExperimentalCoroutinesApi::class)
context(bmp: Gartmap)
suspend fun pixdraw(iResolution: Vec2, iTime: Float, maxConcurrency: Int? = null, pixelFunction: PixelFn) = coroutineScope {
    val width = iResolution.x.toInt()
    val height = iResolution.y.toInt()

    val dispatcher = maxConcurrency?.let { Dispatchers.Default.limitedParallelism(it) } ?: Dispatchers.Default

    doubleLoopSequence(width, height).map { (x, y) ->
        async(dispatcher) {
            val fragCoord = Vec2(x.toFloat(), y.toFloat())
            val color = pixelFunction(fragCoord, iResolution, iTime)

            bmp[x, y] = blendColors(
                argb(color.w, color.x, color.y, color.z),
                bmp[x, y]
            )
        }
    }.toList().awaitAll()
}

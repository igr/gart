package dev.oblac.gart.shader

import dev.oblac.gart.skia.Paint
import dev.oblac.gart.skia.RuntimeEffect
import dev.oblac.gart.skia.RuntimeShaderBuilder
import dev.oblac.gart.skia.Shader

fun Shader.toPaint() = Paint().apply {
    isAntiAlias = true
    shader = this@toPaint
}

/**
 * Creates a shader builder from the given [sksl] string.
 */
fun String.sksl() = RuntimeShaderBuilder(RuntimeEffect.makeForShader(this))

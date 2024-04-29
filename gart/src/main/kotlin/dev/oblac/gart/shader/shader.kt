package dev.oblac.gart.shader

import org.jetbrains.skia.Paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

fun Shader.toPaint() = Paint().apply {
    isAntiAlias = true
    shader = this@toPaint
}

/**
 * Creates a shader builder from the given [sksl] string.
 */
fun String.sksl() = RuntimeShaderBuilder(RuntimeEffect.makeForShader(this))

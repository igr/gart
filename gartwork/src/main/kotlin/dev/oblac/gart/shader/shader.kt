package dev.oblac.gart.shader

import dev.oblac.gart.skia.RuntimeShaderBuilder
import dev.oblac.gart.skia.Shader
import org.jetbrains.skia.RuntimeEffect

fun Shader.toPaint() = dev.oblac.gart.skia.Paint().apply {
    isAntiAlias = true
    shader = this@toPaint
}

/**
 * Creates a shader builder from the given [sksl] string.
 */
fun String.sksl() = RuntimeShaderBuilder(RuntimeEffect.makeForShader(this))

package dev.oblac.gart.shader

import dev.oblac.gart.gfx.paint
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader

fun Shader.toPaint() = paint().apply {
    shader = this@toPaint
}

/**
 * Creates a shader builder from the given [sksl] string.
 */
fun String.sksl() = RuntimeShaderBuilder(RuntimeEffect.makeForShader(this))

package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.shader.*
import org.jetbrains.skia.*
import org.jetbrains.skia.Shader.Companion.makeFractalNoise
import org.jetbrains.skia.Shader.Companion.makeLinearGradient
import org.jetbrains.skia.Shader.Companion.makeRadialGradient
import org.jetbrains.skia.Shader.Companion.makeSweepGradient
import org.jetbrains.skia.Shader.Companion.makeTwoPointConicalGradient

/**
 * Shaders are assigned to the entire canvas background.
 * Shader creates image.
 * Filter modifies the image.
 */
fun main() {
    val gart = Gart.of("ExampleShader", 800, 800, 60)
    println(gart.name)

    val w = gart.window()

    var filter: ImageFilter? = null
    var fill = fillOfRed()
    var shader: (Float) -> Shader = { tick -> createNeuroShader(tick, 0.1f) }

    var tick = 0f
    w.show { c, _, f ->
        val p = shader(tick).toPaint().also {
            it.imageFilter = filter
        }
        c.drawPaint(p)
        c.drawCircle(200f, 200f, 100f, fill)

        if (f.new) {
            tick += 0.01f
        }
    }.onKey {
        when (it) {
            Key.KEY_1 -> {
                fill = fillOfRed()
                filter = null
            }

            Key.KEY_2 -> {
                fill = Paint().apply {
                    this.isAntiAlias = true
                    this.color = Colors.red
                    this.imageFilter = ImageFilter.makeBlur(20f, 20f, FilterTileMode.DECAL)
                }
                filter = null
            }

            Key.KEY_3 -> {
                fill = Paint().apply {
                    this.isAntiAlias = true
                    this.color = Colors.blue
                    this.imageFilter = ImageFilter.makeDilate(20f, 20f, input = null, crop = null)
                }
                filter = null
            }

            Key.KEY_4 -> {
                fill = Paint().apply {
                    this.isAntiAlias = true
                    this.color = Colors.darkGoldenrod
                    this.imageFilter = ImageFilter.makeDropShadow(10f, 20f, 10f, 20f, Colors.black)
                }
                filter = null
            }

            // <editor-fold desc="Filters">

            Key.KEY_A -> {
                fill = fillOfRed()
                filter = createNoiseGrainFilter(-0.2f, gart.d)
            }
            Key.KEY_S -> {
                fill = fillOfRed()
                filter = createNoiseGrain2Filter(0.2f, gart.d)
            }
            Key.KEY_D -> {
                fill = fillOfRed()
                filter = createRisographFilter(0.1f, d = gart.d)
            }
            Key.KEY_F -> {
                fill = fillOfRed()
                filter = createMarbledFilter(0.1f, gart.d)
            }
            Key.KEY_G -> {
                fill = fillOfRed()
                filter = createSketchingPaperFilter(1.2f, 0.2f, 0.15f, gart.d)
            }

            // </editor-fold>

            // <editor-fold desc="Shaders">

            Key.KEY_Z -> {
                shader = { tick ->
                    createNeuroShader(tick, 0.1f)
                }
            }

            Key.KEY_X -> {
                shader = {
                    makeLinearGradient(
                        0f, 0f, gart.d.w.toFloat(), gart.d.h.toFloat(),
                        intArrayOf(Colors.red, Colors.yellow, Colors.green, Colors.cyan, Colors.blue, Colors.magenta),
                        floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f),
                        GradientStyle.DEFAULT
                    )
                }
            }

            Key.KEY_C -> {
                shader = {
                    makeRadialGradient(
                        gart.d.wf / 2,
                        gart.d.hf / 2,
                        gart.d.wf / 2,
                        intArrayOf(Colors.white, Colors.blue, Colors.black),
                        floatArrayOf(0f, 0.5f, 1f),
                        GradientStyle.DEFAULT
                    )
                }
            }

            Key.KEY_V -> {
                shader = { tick ->
                    makeTwoPointConicalGradient(
                        gart.d.wf / 2,
                        gart.d.hf / 2,
                        50f + 30f * kotlin.math.sin(tick),
                        gart.d.wf / 2,
                        gart.d.hf / 2,
                        gart.d.wf / 2,
                        intArrayOf(Colors.yellow, Colors.red, Colors.magenta, Colors.blue, Colors.cyan, Colors.green, Colors.yellow),
                        floatArrayOf(0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f, 1f),
                        GradientStyle.DEFAULT
                    )
                }
            }

            Key.KEY_B -> {
                shader = {
                    makeSweepGradient(
                        gart.d.wf / 2,
                        gart.d.hf / 2,
                        intArrayOf(Colors.red, Colors.yellow, Colors.green, Colors.cyan, Colors.blue, Colors.magenta, Colors.red),
                        floatArrayOf(0f, 0.16f, 0.33f, 0.5f, 0.66f, 0.83f, 1f),
                        GradientStyle.DEFAULT
                    )
                }
            }

            Key.KEY_N -> {
                shader = {
                    makeFractalNoise(
                        0.1f,
                        0.1f,
                        6,
                        0.5f
                    )
                }
            }
            // </editor-fold>

            else -> {}
        }
    }
}

package dev.oblac.gart

import dev.oblac.gart.color.Colors
import dev.oblac.gart.gfx.fillOfRed
import dev.oblac.gart.shader.*
import dev.oblac.gart.skia.FilterTileMode
import dev.oblac.gart.skia.ImageFilter
import dev.oblac.gart.skia.Paint


fun main() {
    val gart = Gart.of("ExampleShader", 400, 400, 60)
    println(gart.name)

    val w = gart.window()

    var filter: ImageFilter? = null
    var fill = fillOfRed()

    var tick = 0f
    w.show { c, _, f ->
        val p = createNeuroShader(tick, 0.1f).toPaint().also {
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
            else -> {}
        }
    }
}

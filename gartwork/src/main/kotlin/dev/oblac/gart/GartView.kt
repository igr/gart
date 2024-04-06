package dev.oblac.gart

import dev.oblac.gart.skia.Canvas
import dev.oblac.gart.skia.SkiaSwingLayer
import dev.oblac.gart.skia.SkikoView
import javax.swing.JFrame

class GartView(
    private val d: Dimension,
    private val drawFrame: DrawFrame,
    fps: Int,
    printFps: Boolean,
) : SkikoView {

    // github says to use SkiaLayer if swing inter-op is needed
    // however, with SkiaSwingLayer I am getting significantly better performance
    private val skiaLayer = SkiaSwingLayer(this)
    internal val fpsGuard = FpsGuard(fps, printFps)

    init {
        //skiaLayer.addView(this)
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        fpsGuard.withFps(nanoTime)
        drawFrame(canvas, d, fpsGuard.frames)
        repaint()
    }

    fun repaint() {
        skiaLayer.repaint()
    }

    internal fun attachTo(frame: JFrame) {
        frame.contentPane.add(skiaLayer)
    }
}

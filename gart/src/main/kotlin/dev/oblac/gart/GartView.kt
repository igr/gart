package dev.oblac.gart

import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkikoRenderDelegate
import org.jetbrains.skiko.swing.SkiaSwingLayer
import javax.swing.JFrame

class GartView(
    private val d: Dimension,
    private var drawFrame: DrawFrame,
    fps: Int,
    printFps: Boolean
) : SkikoRenderDelegate {

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

    /**
     * Replaces the view with new DrawFrame.
     */
    fun replace(drawFrame: DrawFrame): GartView {
        this.drawFrame = drawFrame
        return this
    }

    fun drawFrame() = drawFrame
}

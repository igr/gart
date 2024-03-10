package dev.oblac.gart

import dev.oblac.gart.skia.*
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

class Window(private val animation: Animation) {

    init {
        JFrame.setDefaultLookAndFeelDecorated(true)
        //setupSkikoLoggerFactory { DefaultConsoleLogger.fromLevel(System.getProperty("skiko.log.level", "INFO")) }
    }

    /**
     * Shows the windows and starts the animation.
     */
    fun show() = SwingUtilities.invokeLater {
        val g = animation.g

        val frame = JFrame()
        frame.title = "gȧrt!"
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isResizable = false
        frame.isVisible = true

        // skia
        val skiaLayer = SkiaLayer()
        val view = GartView(g, animation.printFps)
        skiaLayer.addView(GenericSkikoView(skiaLayer, view))
        skiaLayer.attachTo(frame.contentPane)

        // animation
        animation.onPaint {
            view.update(it)
            //SwingUtilities.invokeLater { skiaLayer.needRedraw() }
        }
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent) {
                animation.stop()
            }
        })

        // frame sizing
        frame.preferredSize = java.awt.Dimension(g.d.w, g.d.h + frame.insets.top)   // add title bar height
        skiaLayer.needRedraw()
        frame.pack()

        // windows positioning
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        frame.setLocation((screenSize.width - g.d.w) / 2, (screenSize.height - g.d.h) / 2)
    }
}

internal class GartView(g: Gartvas, private val printFps: Boolean) : SkikoView {

    private var lastBufferedImage = g.snapshot()
    private val fpsCounter = FPSCounter()
    private val animationTicker = AnimationTicker()

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        canvas.drawImage(lastBufferedImage, 0f, 0f)
        if (printFps) {
            fpsCounter.tick()
            print("fps = ${fpsCounter.average} ${animationTicker.str()}\r")
        }
    }

    /**
     * Updates image to be drawn.
     */
    fun update(image: Image) {
        lastBufferedImage = image
        //skiaLayer.needRedraw()
    }
}

private class AnimationTicker {
    private val chars = charArrayOf('|', '/', '-', '\\')
    private var index = 0
    fun str(): Char {
        val c = chars[index]
        index = (index + 1) % chars.size
        return c
    }
}

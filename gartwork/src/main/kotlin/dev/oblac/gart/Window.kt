package dev.oblac.gart

import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

open class Window(val d: Dimension, val fps: Int, internal val printFps: Boolean) {

    init {
        JFrame.setDefaultLookAndFeelDecorated(true)
        //setupSkikoLoggerFactory { DefaultConsoleLogger.fromLevel(System.getProperty("skiko.log.level", "INFO")) }
    }

    /**
     * Shows the windows.
     */
    open fun show(drawFrame: DrawFrame) = SwingUtilities.invokeLater {
        val frame = JFrame()
        frame.title = "gȧrt!"
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isResizable = false
        frame.isVisible = true
        val density = frame.graphicsConfiguration.defaultTransform.scaleX
        println("Window fps: $fps • density: $density")
        val d = Dimension((d.w / density).toInt(), (d.h / density).toInt())

        // skia
        val view = GartView(d, drawFrame, fps, printFps)
        view.attachTo(frame)

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent) {
                onClose()
            }
        })

        // frame sizing
        frame.preferredSize = java.awt.Dimension(d.w, d.h + frame.insets.top)   // add title bar height
        view.repaint()
        frame.pack()

        // windows positioning
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        frame.setLocation((screenSize.width - d.w) / 2, (screenSize.height - d.h) / 2)
    }

    protected open fun onClose() = println("Window closing")
}

package dev.oblac.gart

import java.awt.Toolkit
import java.awt.event.*
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
    open fun show(drawFrame: DrawFrame): WindowView {
        val view = GartView(d, drawFrame, fps, printFps)
        val windowView = WindowView(this, view)
        SwingUtilities.invokeLater {
            val frame = JFrame()
            frame.title = "gȧrt!"
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.isResizable = false
            frame.isVisible = true
            val density = frame.graphicsConfiguration.defaultTransform.scaleX
            println("Window fps: $fps • density: $density")
            val d = Dimension((d.w / density).toInt(), (d.h / density).toInt())

            // skia
            view.attachTo(frame)

            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(windowEvent: WindowEvent) {
                    onClose()
                }
            })

            // add keyboard listener
            frame.addKeyListener(windowView.keyListener)
            frame.addMouseListener(windowView.mouseListener)

            // frame sizing
            frame.preferredSize = java.awt.Dimension(d.w, d.h + frame.insets.top)   // add title bar height
            view.repaint()
            frame.pack()

            // windows positioning
            val screenSize = Toolkit.getDefaultToolkit().screenSize
            frame.setLocation((screenSize.width - d.w) / 2, (screenSize.height - d.h) / 2)
        }
        return windowView
    }

    /**
     * Called when the window is closing.
     */
    protected open fun onClose() = println("Window closing")
}

class WindowView(w: Window, private val v: GartView) {

    private var keyboardHandler: (Key) -> Unit = {}
    /**
     * Defines a keyboard handler.
     */
    fun keyboardHandler(keyboardHandler: (Key) -> Unit): WindowView {
        this.keyboardHandler = keyboardHandler
        return this
    }

    internal val keyListener = object : KeyListener {
        override fun keyTyped(e: KeyEvent) {
        }

        override fun keyPressed(e: KeyEvent) {
            keyboardHandler(Key.valueOf(e.keyCode))
        }

        override fun keyReleased(e: KeyEvent) {
        }
    }

    private val mouseHandler: (MouseEvent) -> Unit = {
        println("(${it.x},${it.y})")
    }

    internal val mouseListener = object : MouseListener {
        override fun mouseClicked(e: MouseEvent) {
            mouseHandler(e)
        }

        override fun mousePressed(e: MouseEvent) {
        }

        override fun mouseReleased(e: MouseEvent) {
        }

        override fun mouseEntered(e: MouseEvent) {
        }

        override fun mouseExited(e: MouseEvent) {
        }
    }

    fun fastForwardTo(frame: Long) {
        v.fpsGuard.frames.set(frame)
    }

}

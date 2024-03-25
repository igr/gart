package dev.oblac.gart

import dev.oblac.gart.skia.SkikoKeyboardEvent
import org.jetbrains.skiko.toSkikoEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
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
            frame.addKeyListener(object : KeyListener {
                override fun keyTyped(e: KeyEvent) {
                }

                override fun keyPressed(e: KeyEvent) {
                    view.onKeyboardEvent(toSkikoEvent(e))
                }

                override fun keyReleased(e: KeyEvent) {
                    view.onKeyboardEvent(toSkikoEvent(e))
                }
            })
            frame.addMouseListener(object : java.awt.event.MouseListener {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    view.onPointerEvent(toSkikoEvent(e))
                }

                override fun mousePressed(e: java.awt.event.MouseEvent) {
                    view.onPointerEvent(toSkikoEvent(e))
                }

                override fun mouseReleased(e: java.awt.event.MouseEvent) {
                    view.onPointerEvent(toSkikoEvent(e))
                }

                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    view.onPointerEvent(toSkikoEvent(e))
                }

                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    view.onPointerEvent(toSkikoEvent(e))
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
        return windowView
    }

    /**
     * Called when the window is closing.
     */
    protected open fun onClose() = println("Window closing")
}

class WindowView(w: Window, private val v: GartView) {
    /**
     * Defines a keyboard handler.
     */
    fun keyboardHandler(keyboardHandler: (SkikoKeyboardEvent) -> Unit): WindowView {
        v.keyboardHandler = keyboardHandler
        return this
    }

    fun fastForwardTo(frame: Long) {
        v.fpsGuard.frames.set(frame)
    }

}

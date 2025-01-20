package dev.oblac.gart

import org.jetbrains.skia.Image
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
     * Shows the gartvas image.
     * Usually used for static images already generated.
     */
    fun showImage(gartvas: Gartvas): WindowView {
        val snapshot = gartvas.snapshot()
        return show { c, _, _ ->
            c.drawImage(snapshot, 0f, 0f)
        }
    }

    fun showImage(image: Image): WindowView {
        return show { c, _, _ ->
            c.drawImage(image, 0f, 0f)
        }
    }

    /**
     * Shows the windows.
     * There is no guarantee that the drawFrame will be called at the exact fps rate.
     * The drawFrame is called on each repaint, that may happen multiple times per frame.
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

    internal var onCloseHandler: () -> Unit = {
        println("Window closing")
    }

    /**
     * Called when the window is closing.
     */
    protected open fun onClose() {
        onCloseHandler()
    }
}

class WindowView(private val w: Window, private val v: GartView) {

    private var keyboardHandler: (Key) -> Unit = {}
    /**
     * Defines a keyboard handler.
     */
    fun onKey(keyboardHandler: (Key) -> Unit): WindowView {
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

    /**
     * Skips time to the given frame.
     */
    fun skipTo(frame: Long) {
        v.fpsGuard.frames.set(frame)
    }

    fun onClose(onClose: () -> Unit) {
        w.onCloseHandler = onClose
    }

}

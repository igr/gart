package dev.oblac.gart

import dev.oblac.gart.hotreload.DrawFrameReloader
import org.jetbrains.skia.Image
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.event.*
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.SwingUtilities

open class Window(
    val d: Dimension,
    val fps: Int,
    internal val printFps: Boolean,
    internal val fullScreen: Boolean = false
) {

    init {
        JFrame.setDefaultLookAndFeelDecorated(true)
        //setupSkikoLoggerFactory { DefaultConsoleLogger.fromLevel(System.getProperty("skiko.log.level", "INFO")) }
    }

    /**
     * Shows the Gartvas image.
     * Usually used for static images that are already generated.
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

    fun showImage(image: () -> Image): WindowView {
        return show { c, _, _ ->
            c.drawImage(image(), 0f, 0f)
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
        if (java.awt.Taskbar.isTaskbarSupported()) {
            // set taskbar icon
            val taskbar = java.awt.Taskbar.getTaskbar()
            taskbar.iconImage = ImageIcon(object {}.javaClass.getResource("/g.png")).image
        }
        SwingUtilities.invokeLater {
            val frame = JFrame()
            this.frame = frame
            frame.title = "gȧrt!"
            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            frame.isResizable = false
            frame.isVisible = true
            frame.isAlwaysOnTop = true
            val density = frame.graphicsConfiguration.defaultTransform.scaleX
            println("Window fps: $fps • density: $density")
            val d = Dimension((d.w / density).toInt(), (d.h / density).toInt())

            if (fullScreen) {
                frame.extendedState = frame.extendedState or JFrame.MAXIMIZED_BOTH
                //frame.isUndecorated = true  // hide title bar (not working)

                // exclusive full screen mode
                val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                ge.defaultScreenDevice.setFullScreenWindow(frame)
            }

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
            frame.addMouseMotionListener(windowView.mouseMotionListener)

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

    protected var frame: JFrame? = null

    internal val onCloseHandlers: MutableList<() -> Unit> = mutableListOf({
        println("Window closing")
    })

    /**
     * Called when the window is closing.
     * Invokes all close handlers in reversed order.
     */
    protected open fun onClose() {
        onCloseHandlers.reversed().forEach { it() }
    }

    /**
     * Explicitly closes the window.
     * This is useful when you want to close the window from a keyboard handler or a mouse handler.
     */
    fun close() {
        SwingUtilities.invokeLater {
            frame?.dispatchEvent(WindowEvent(frame, WindowEvent.WINDOW_CLOSING))
        }
    }
}

class WindowView(private val w: Window, private val v: GartView) {

    private val keyboardHandlers: MutableList<(Key) -> Unit> = mutableListOf()
    /**
     * Defines a keyboard handler.
     */
    fun onKey(keyboardHandler: (Key) -> Unit): WindowView {
        keyboardHandlers.add(keyboardHandler)
        return this
    }

    internal val keyListener = object : KeyListener {
        override fun keyTyped(e: KeyEvent) {
        }

        override fun keyPressed(e: KeyEvent) {
            keyboardHandlers.forEach { it(Key.valueOf(e.keyCode)) }
        }

        override fun keyReleased(e: KeyEvent) {
        }
    }

    private val mouseHandlers: MutableList<(MouseEvent) -> Unit> = mutableListOf({
        println("(${it.x},${it.y})")
    })

    /**
     * Defines a mouse handler.
     */
    fun onMouse(mouseHandler: (MouseEvent) -> Unit): WindowView {
        mouseHandlers.add(mouseHandler)
        return this
    }

    internal val mouseListener = object : MouseListener {
        override fun mouseClicked(e: MouseEvent) {
            mouseHandlers.forEach { it(e) }
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

    private val mouseMotionHandlers: MutableList<(MouseEvent) -> Unit> = mutableListOf()

    /**
     * Defines a mouse motion handler.
     */
    fun onMouseMotion(mouseMotionHandler: (MouseEvent) -> Unit): WindowView {
        mouseMotionHandlers.add(mouseMotionHandler)
        return this
    }

    internal val mouseMotionListener = object : MouseMotionListener {
        override fun mouseDragged(e: MouseEvent) {
            mouseMotionHandlers.forEach { it(e) }
        }

        override fun mouseMoved(e: MouseEvent) {
            mouseMotionHandlers.forEach { it(e) }
        }
    }

    /**
     * Skips time to the given frame.
     */
    fun skipTo(frame: Long) {
        v.fpsGuard.frames.set(frame)
    }

    fun onClose(onClose: () -> Unit) {
        w.onCloseHandlers.add(onClose)
    }

    /**
     * Reloads the view with new DrawFrame.
     */
    internal fun reload(drawFrame: DrawFrame) {
        v.replace(drawFrame)
        v.repaint()
    }

    /**
     * Returns the current draw frame.
     */
    internal fun drawFrame(): DrawFrame {
        return v.drawFrame()
    }

    /**
     * Enables hot reload for the window.
     * This is experimental feature.
     */
    fun hotReload(g: Gartvas, projectRoot: String = System.getProperty("user.dir")) {
        val hotReloadWindowsView = DrawFrameReloader(g, this, projectRoot)
        this.onClose {
            hotReloadWindowsView.shutdown()
        }
    }
}

object KeyHandlers {
    /**
     * Prints the key to the console.
     */
    val showKey: (Key) -> Unit = { println("Key: $it") }
}

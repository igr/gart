package ac.obl.gart

import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel

class Window(private val g: Gartvas, private val frames: Int = 25) {

	init {
		JFrame.setDefaultLookAndFeelDecorated(true)
	}

	fun show(): Painter {
		val frame = JFrame()
		frame.title = "gȧrt"
		frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
		frame.isResizable = false
		frame.isVisible = true
		frame.setSize(g.box.w, g.box.h)

		val panel = GartPanel(g)
		frame.contentPane.add(panel)
		frame.pack()

		val painter = Painter(g, frames) { panel.paint(it) }
		frame.addWindowListener(object : WindowAdapter() {
			override fun windowClosing(windowEvent: WindowEvent) {
				painter.running = false
			}
		})

		val screenSize = Toolkit.getDefaultToolkit().screenSize
		frame.setLocation((screenSize.width - g.box.w) / 2, (screenSize.height - g.box.h) / 2)

		return painter
	}
}

class GartPanel(private val g: Gartvas) : JPanel(true) {
	private var lastBufferedImage = g.snapshot().toBufferedImage()

	override fun getPreferredSize(): Dimension? {
		return if (isPreferredSizeSet) {
			super.getPreferredSize()
		} else {
			Dimension(g.box.w, g.box.h)
		}
	}

	override fun paintComponent(graphics: Graphics) {
		val g2 = graphics as Graphics2D
		g2.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON
		)
		g2.drawImage(lastBufferedImage, 0, 0, null)
	}

	fun paint(bufferedImage: BufferedImage) {
		lastBufferedImage = bufferedImage
		this.repaint()
	}
}

class Painter(
	private val g: Gartvas,
	frames: Int,
	private val paintCallback: (BufferedImage) -> Unit,
) {

    private val frameDurationInMillis = 1000 / frames

    // initial time is 1 second in the past, so we can kick painting right away
    private var lastPaintTimestamp = System.currentTimeMillis() - 1000
    internal var running = true
    private val framesCount = FramesCount(frames)
    val frames: Frames = framesCount

    private fun draw(paintFrame: (Frames) -> Boolean) {
        val currentTimeStamp = System.currentTimeMillis()
        val elapsedSinceLastPaint = currentTimeStamp - lastPaintTimestamp
        val remainingSleepTime = frameDurationInMillis - elapsedSinceLastPaint

        if (remainingSleepTime < 0) {
            this.running = paintFrame(frames)
            paintCallback(g.snapshot().toBufferedImage())
            lastPaintTimestamp = currentTimeStamp
            framesCount.tick()
        }
    }

    /**
     * Paints a frame while window is up.
     */
    fun paint2(paintFrame: (Frames) -> Boolean) {
        while (this.running) {
            this.draw(paintFrame)
        }
    }

    fun paint(paintFrame: (Frames) -> Unit) {
        while (this.running) {
            this.draw {
                paintFrame(it)
                return@draw true
            }
        }
    }
}

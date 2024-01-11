package dev.oblac.gart

import dev.oblac.gart.util.toBufferedImage
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel

class Window(private val animation: Animation) {

    init {
        JFrame.setDefaultLookAndFeelDecorated(true)
    }

    /**
     * Shows the windows and starts the animation.
     */
    fun show(): Animation.AnimationRunner {
        val g = animation.g

        val frame = JFrame()
        frame.title = "gȧrt!"
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isResizable = false
        frame.isVisible = true
        frame.setSize(g.d.w, g.d.h)

        val panel = GartPanel(g)
        frame.contentPane.add(panel)
        frame.pack()

        val anim = animation.start { panel.paint(it.toBufferedImage()) }

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowEvent: WindowEvent) {
                anim.stop()
            }
        })

        val screenSize = Toolkit.getDefaultToolkit().screenSize
        frame.setLocation((screenSize.width - g.d.w) / 2, (screenSize.height - g.d.h) / 2)

        return anim
    }
}

class GartPanel(private val g: Gartvas) : JPanel(true) {
    private var lastBufferedImage = g.snapshot().toBufferedImage()

    override fun getPreferredSize(): java.awt.Dimension? {
        return if (isPreferredSizeSet) {
            super.getPreferredSize()
        } else {
            java.awt.Dimension(g.d.w, g.d.h)
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

package dev.oblac.gart

object Screen {

    /**
     * Returns the screen resolution, no matter the density.
     */
    fun resolution(): Dimension {
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        return Dimension(screenSize.width, screenSize.height)
    }

    /**
     * Returns the screen dimension, taking into account the density.
     */
    fun dimension(): Dimension {
        val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
        val density = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX
        return Dimension((screenSize.width * density).toInt(), (screenSize.height * density).toInt())
    }
}

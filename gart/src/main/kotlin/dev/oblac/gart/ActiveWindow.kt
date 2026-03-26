package dev.oblac.gart

import javax.swing.JFrame

/**
 * Holds reference to the active window for classloader-based hot reload.
 * When the launcher restarts main(), Window.show() checks this holder
 * to reuse the existing JFrame and SkiaSwingLayer instead of creating new ones.
 */
internal object ActiveWindow {
    @Volatile
    var frame: JFrame? = null

    @Volatile
    var view: GartView? = null

    @Volatile
    var dimension: Dimension? = null

    fun canReuse(d: Dimension): Boolean {
        val f = frame ?: return false
        view ?: return false
        return f.isDisplayable && dimension == d
    }

    fun cleanupListeners() {
        val f = frame ?: return
        f.windowListeners.forEach { f.removeWindowListener(it) }
        f.keyListeners.forEach { f.removeKeyListener(it) }
        f.mouseListeners.forEach { f.removeMouseListener(it) }
        f.mouseMotionListeners.forEach { f.removeMouseMotionListener(it) }
    }
}

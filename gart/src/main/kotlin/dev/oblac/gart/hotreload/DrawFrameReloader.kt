package dev.oblac.gart.hotreload

import dev.oblac.gart.DrawFrame
import dev.oblac.gart.Gartvas
import dev.oblac.gart.WindowView
import java.nio.file.Path
import java.time.LocalDateTime
import javax.swing.SwingUtilities

class DrawFrameReloader(
    private val gartvas: Gartvas,
    private val wv: WindowView,
    projectRoot: String
) {

    private val fileWatcher: FileWatcher
    private var hotReloadClassLoader: HotReloadClassLoader
    private val drawFrameClassName: String
    private val watchPaths: List<String>

    init {
        val drawFrame = wv.drawFrame()
        drawFrameClassName = drawFrame.javaClass.name

        // todo check if class name is loadable!!!
        println("🔥DrawFrame: $drawFrameClassName")

        watchPaths = ProjectClassPaths.forGartProject(projectRoot)

        // Create new class loader
        hotReloadClassLoader = HotReloadClassLoader(watchPaths)

        // Start file watcher
        fileWatcher = FileWatcher(
            watchPaths,
            { it: Path -> it.toString().endsWith(".class") }) {
            reloadClasses()
        }
        fileWatcher.start()
    }

    private fun reloadClasses() {
        SwingUtilities.invokeLater {
            generateSequence(0) { it + 1 }
                .take(10)   // max number of attempts
                .firstOrNull { attempt ->
                    try {
                        reloadClasses(drawFrameClassName)
                        true // Success, stop trying
                    } catch (e: ClassToReloadException) {
                        println("Class needs reload: ${e.className}")
                        false // Continue trying
                    } catch (e: Exception) {
                        println("❌ Failed to reload classes: ${e.message}")
                        e.printStackTrace()
                        true // Fatal error, stop trying
                    }
                }
        }
    }

    private fun reloadClasses(drawFrameClassName: String) {
        val changedClasses = hotReloadClassLoader.changedClasses()
        if (changedClasses.isEmpty()) {
            return
        }

        println("🔥 Reloading changed classes: ${changedClasses.size}")
        // for debugging purposes
        //println("Changed classes:\n${changedClasses.joinToString("\n")}")

        // Create a NEW classloader for hot reload.
        // We can't redefine classes in the same classloader!
        hotReloadClassLoader = HotReloadClassLoader(watchPaths)

        // Set the current thread's context classloader temporarily
        val originalClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = hotReloadClassLoader

            // Try to reload the DrawFrame with the new classloader
            // We MUST load it with the new classloader even if it hasn't changed,
            // so it can access updated dependencies
            val drawFrameClass = try {
                hotReloadClassLoader.loadClass(drawFrameClassName)
            } catch (e: Exception) {
                println("❌ Could not load DrawFrame class $drawFrameClassName with new classloader: ${e.message}")
                null
            }

            if (drawFrameClass != null && DrawFrame::class.java.isAssignableFrom(drawFrameClass)) {
                try {
                    // Try to create new instance with single arg constructor
                    val constructor = drawFrameClass.getDeclaredConstructor(Gartvas::class.java)
                    constructor.isAccessible = true
                    val newDrawFrame = constructor.newInstance(gartvas) as DrawFrame

                    wv.reload(newDrawFrame)

                    println("✅ Successfully hot-reloaded: $drawFrameClassName at ${LocalDateTime.now()}")
                } catch (e: Exception) {
                    println("❌ Could not create new instance of $drawFrameClassName: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                println("⚠️ No compatible DrawFrame found: $drawFrameClass")
            }
        } finally {
            Thread.currentThread().contextClassLoader = originalClassLoader
        }
    }

    /**
     * Cleanup resources when shutting down hot reload
     */
    fun shutdown() {
        fileWatcher.stop()
        hotReloadClassLoader.clearCache()
        println("🔥 Hot reload stopped")
    }

}

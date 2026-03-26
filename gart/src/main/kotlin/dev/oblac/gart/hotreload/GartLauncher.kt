package dev.oblac.gart.hotreload

import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * Launches a gart art project's main() in an isolated classloader.
 * Watches for .class file changes and restarts the app by disposing
 * all Swing windows and re-invoking main() with a fresh classloader.
 *
 * Usage: GartLauncherKt <classes-dir> <main-class>
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("Usage: GartLauncher <classes-dir> <main-class>")
        exitProcess(1)
    }
    GartLauncher(args[0], args[1]).start()
}

class GartLauncher(
    private val classesDir: String,
    private val mainClassName: String
) {
    @Volatile
    private var currentClassLoader: URLClassLoader? = null

    fun start() {
        println("🚀 GartLauncher: $mainClassName")
        println("   Classes: $classesDir")

        launchApp()

        val fileWatcher = FileWatcher(
            listOf(classesDir),
            { path: Path -> path.toString().endsWith(".class") }
        ) {
            restart()
        }

        Runtime.getRuntime().addShutdownHook(Thread {
            fileWatcher.stop()
            currentClassLoader?.close()
        })

        fileWatcher.start()

        // Block the launcher thread forever (JVM stays alive)
        Thread.currentThread().join()
    }

    private fun launchApp() {
        val url = Paths.get(classesDir).toUri().toURL()
        val classLoader = URLClassLoader(arrayOf(url), javaClass.classLoader)
        currentClassLoader = classLoader

        try {
            val mainClass = classLoader.loadClass(mainClassName)
            val mainMethod = mainClass.getMethod("main", Array<String>::class.java)

            val thread = Thread({
                Thread.currentThread().contextClassLoader = classLoader
                try {
                    mainMethod.invoke(null, emptyArray<String>())
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    System.err.println("❌ ${cause.javaClass.simpleName}: ${cause.message}")
                }
            }, "gart-app")
            thread.isDaemon = true
            thread.start()
        } catch (e: ClassNotFoundException) {
            System.err.println("❌ Class not found: $mainClassName")
        } catch (e: NoSuchMethodException) {
            System.err.println("❌ No main() method in $mainClassName")
        }
    }

    private fun restart() {
        println("🔄 Reloading...")

        // Dispose all Swing windows on the EDT
        SwingUtilities.invokeAndWait {
            java.awt.Window.getWindows().forEach { it.dispose() }
        }

        // Release old classloader
        currentClassLoader?.close()
        currentClassLoader = null

        // Brief pause for Skia/AWT resources to settle
        Thread.sleep(300)

        launchApp()

        println("✅ Reloaded")
    }
}

package dev.oblac.gart.hotreload

import java.nio.file.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * File watcher that monitors changes and triggers reload callbacks.
 */
class FileWatcher(
    private val watchPaths: List<String>,
    private val pathChangeFilter: (Path) -> Boolean,
    private val onChange: () -> Unit
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private var watchThread: Thread = Thread.currentThread()
    private var isRunning = false

    fun start() {
        if (isRunning) return

        try {
            isRunning = true

            // register directories for watching
            watchPaths.forEach {
                val path = Paths.get(it)
                if (Files.exists(path) && Files.isDirectory(path)) {
                    registerDirectory(path)
                }
            }

            watchThread = thread(name = "FileWatcher") {
                watchLoop()
            }

            println("FileWatcher started, monitoring: ${watchPaths.count()} paths")
        } catch (e: Exception) {
            println("Failed to start FileWatcher: ${e.message}")
            stop()
        }
    }

    private fun registerDirectory(dir: Path) {
        dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        // Recursively register subdirectories:
        Files.walk(dir).use { paths ->
            paths.filter { Files.isDirectory(it) && it != dir }
                .forEach { subDir ->
                    try {
                        subDir.register(
                            watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                        )
                    } catch (e: Exception) {
                        println("Failed to register directory $subDir: ${e.message}")
                    }
                }
        }
    }

    private fun watchLoop() {
        while (isRunning) {
            try {
                val key = watchService.poll(100, TimeUnit.MILLISECONDS)
                if (key != null) {
                    processKey(key)
                }
            } catch (_: InterruptedException) {
                break
            } catch (e: Exception) {
                println("Error in watch loop: ${e.message}")
            }
        }
    }

    private fun processKey(key: WatchKey) {
        var shouldReload = false

        for (event in key.pollEvents()) {
            val kind = event.kind()

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue
            }

            val filename = event.context() as Path
            val filepath = (key.watchable() as Path).resolve(filename)

            // Only react to filtered files
            if (pathChangeFilter(filepath)) {
                //println("Detected file change: $filepath")
                shouldReload = true
            }
        }

        key.reset()

        if (shouldReload) {
            // Small delay to avoid multiple rapid changes
            Thread.sleep(500)
            onChange()
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        watchThread.interrupt()
        watchService.close()
        println("FileWatcher stopped")
    }

}

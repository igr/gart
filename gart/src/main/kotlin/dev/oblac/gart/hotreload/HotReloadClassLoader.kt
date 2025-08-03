package dev.oblac.gart.hotreload

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * Custom ClassLoader that can reload classes from .class files.
 * This allows for true hot reloading by creating new instances of modified classes.
 */
class HotReloadClassLoader(
    private val classPaths: List<String>,
    parent: ClassLoader = Thread.currentThread().contextClassLoader
) : URLClassLoader(emptyArray<URL>(), parent) {

    private val classFileCache = ConcurrentHashMap<String, Long>()
    private val loadedClasses = ConcurrentHashMap<String, Class<*>>()

    init {
        classPaths
            .asSequence()
            .map(::File)
            .filter { it.exists() }
            .map { it.toURI().toURL() }
            .forEach(::addURL)
    }

    @Synchronized
    override fun loadClass(name: String): Class<*> {
        return loadClass(name, false)
    }

    @Synchronized
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // First check if we've already loaded this class
        var clazz = findLoadedClass(name)
        if (clazz != null) {
            if (resolve) resolveClass(clazz)
            return clazz
        }

        // Parent-Last Strategy: Try to load from our paths FIRST
        if (!shouldDelegateToParent(name)) {

            // Check if we have a cached version and if the class file has changed
            val classFile = findClassFile(name)
            if (classFile != null && hasClassChanged(name, classFile)) {
                // For hot reload: we need to create a new classloader instance
                // because we can't redefine classes in the same classloader
                throw ClassToReloadException(name)
            }

            // Try to load from cached classes
            loadedClasses[name]?.let {
                if (resolve) resolveClass(it)
                return it
            }

            // Try to load/define the class from our custom paths
            try {
                clazz = findAndDefineClass(name)
                if (clazz != null) {
                    loadedClasses[name] = clazz
                    if (resolve) resolveClass(clazz)
                    return clazz
                }
            } catch (e: Exception) {
                // continue to parent delegation
                println("Failed to load class $name from custom paths: ${e.message}")
            }
        }

        // finally, delegate to parent if we haven't found the class
        try {
            clazz = parent.loadClass(name)
            if (resolve) resolveClass(clazz)
            return clazz
        } catch (_: ClassNotFoundException) {
            // Will throw below
        }

        throw ClassNotFoundException(name)
    }

    /**
     * Determines whether a class should be delegated to parent classloader first.
     * System classes and certain framework classes should always be loaded by parent.
     */
    private fun shouldDelegateToParent(className: String): Boolean {
        return className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("kotlin.") ||
            className.startsWith("kotlinx.") ||
            className.startsWith("org.jetbrains.") ||
            // Add other system packages as needed
            className.startsWith("org.slf4j.") ||
            className.startsWith("ch.qos.logback.")
    }

    private fun findClassFile(className: String): Path? {
        val classFileName = className.replace('.', '/') + ".class"

        return classPaths
            .asSequence()
            .map { classPath -> Paths.get(classPath, classFileName) }
            .firstOrNull { Files.exists(it) }
    }

    private fun hasClassChanged(className: String, classFile: Path): Boolean {
        val lastModified = Files.getLastModifiedTime(classFile).toMillis()
        val cachedTime = classFileCache[className]

        return if (cachedTime == null) {
            // First time seeing this class - initialize cache but don't treat as changed
            classFileCache[className] = lastModified
            false
        } else if (cachedTime < lastModified) {
            // Class has actually changed since we last saw it
            classFileCache[className] = lastModified
            true
        } else {
            false
        }
    }

    private fun findAndDefineClass(name: String): Class<*>? {
        val classFile = findClassFile(name) ?: return null

        return try {
            val bytes = Files.readAllBytes(classFile)
            defineClass(name, bytes, 0, bytes.size)
        } catch (e: Exception) {
            println("Failed to define class $name: ${e.message}")
            null
        }
    }

    /**
     * Forces reload of a specific class by removing it from cache
     */
    fun forceReload(className: String) {
        loadedClasses.remove(className)
        classFileCache.remove(className)
    }

    /**
     * Clears all cached classes to force complete reload
     */
    fun clearCache() {
        loadedClasses.clear()
        classFileCache.clear()
    }

    /**
     * Returns all class files that have changed since last check.
     */
    fun changedClasses(): Set<String> {
        return classPaths
            .asSequence()
            .map(::File)
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> findAllClassFiles(dir, "") }
            .mapNotNull { className ->
                findClassFile(className)?.let { classFile ->
                    className.takeIf { hasClassChanged(className, classFile) }
                }
            }
            .toSet()
    }

    private fun findAllClassFiles(dir: File, packageName: String): List<String> {
        val classes = mutableListOf<String>()

        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    val subPackage = if (packageName.isEmpty()) file.name else "$packageName.${file.name}"
                    classes.addAll(findAllClassFiles(file, subPackage))
                }

                file.name.endsWith(".class") -> {
                    val className = if (packageName.isEmpty()) {
                        file.name.removeSuffix(".class")
                    } else {
                        "$packageName.${file.name.removeSuffix(".class")}"
                    }
                    classes.add(className)
                }
            }
        }

        return classes
    }

}

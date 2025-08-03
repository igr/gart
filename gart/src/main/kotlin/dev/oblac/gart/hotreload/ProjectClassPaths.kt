package dev.oblac.gart.hotreload

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object ProjectClassPaths {

    /**
     * Returns all classpaths for the Gart project.
     * Includes all arts and the main project.
     */
    fun forGartProject(projectRoot: String): List<String> {

        val artsDirs = Files.walk(Paths.get(projectRoot, "arts")).use { paths ->
            paths
                .filter { Files.isDirectory(it) }
                .toList()
        }.toMutableList()

        artsDirs.add(Paths.get(projectRoot))

        return artsDirs
            .flatMap {
                listOf(
                    "$it/build/classes/kotlin/main/",
                    "$it/build/classes/java/main/",
                )
            }
            .filter { File(it).exists() }
            .toList()
    }
}

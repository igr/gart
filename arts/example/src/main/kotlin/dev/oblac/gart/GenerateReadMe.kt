package dev.oblac.gart

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*

fun main() {
    val currentDir = Paths.get(System.getProperty("user.dir"))

    val artsDirectory = findArtsDirectory(currentDir)

    if (artsDirectory == null || !artsDirectory.exists() || !artsDirectory.isDirectory()) {
        println("Arts directory not found. Current directory: $currentDir")
        return
    }

    val thumbnailsByFolder = findAllThumbnails(artsDirectory)
    val markdown = generateMarkdown(thumbnailsByFolder)

    //println(markdown)

    // Update README.md file
    updateReadmeFile(artsDirectory.parent, markdown)
}

/**
 * Finds the arts directory by walking up the directory tree
 */
private fun findArtsDirectory(startPath: Path): Path? {
    var current = startPath.toAbsolutePath()

    while (current != null) {
        val artsDir = current.resolve("arts")
        if (artsDir.exists() && artsDir.isDirectory()) {
            return artsDir
        }
        current = current.parent
    }

    return null
}

/**
 * Scans all subdirectories in the arts folder and finds thumbnail files matching "*_thumb.*" pattern
 */
private fun findAllThumbnails(artsDirectory: Path): Map<String, List<ThumbnailInfo>> {
    return try {
        Files.list(artsDirectory).use { stream ->
            stream
                .filter { it.isDirectory() }
                .filter { !it.fileName.toString().startsWith(".") }
                .sorted()
                .toList()
                .associate { folder ->
                    val folderName = folder.fileName.toString()
                    val thumbnails = findThumbnailsInFolder(folder)
                    folderName to thumbnails
                }
                .filterValues { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        println("Error scanning arts directory: ${e.message}")
        emptyMap()
    }
}

/**
 * Finds all thumbnail files in a specific folder
 */
private fun findThumbnailsInFolder(folder: Path): List<ThumbnailInfo> {
    return try {
        Files.walk(folder, 1).use { stream ->
            stream
                .filter { it.isRegularFile() }
                .filter { it.fileName.toString().contains("_thumb.") }
                .map { thumbnailPath ->
                    val relativePath = "arts/${folder.fileName}/${thumbnailPath.fileName}"
                    ThumbnailInfo(
                        name = thumbnailPath.nameWithoutExtension,
                        relativePath = relativePath,
                        displayName = generateDisplayName(thumbnailPath.nameWithoutExtension)
                    )
                }
                .sorted()
                .toList()
        }
    } catch (e: Exception) {
        println("Error scanning folder $folder: ${e.message}")
        emptyList()
    }
}

/**
 * Generates a clean display name from the thumbnail filename
 */
private fun generateDisplayName(fileName: String): String {
    return fileName
        .removeSuffix("_thumb")
        .replace("-", " ")
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

/**
 * Generates the complete markdown content
 */
private fun generateMarkdown(thumbnailsByFolder: Map<String, List<ThumbnailInfo>>): String {
    if (thumbnailsByFolder.isEmpty()) {
        return "# Art Gallery\n\nNo thumbnail images found in the arts directory."
    }

    return buildString {
        appendLine()
        appendLine("A collection of generative art pieces (ordered by name).")
        appendLine()

        thumbnailsByFolder.forEach { (folderName, thumbnails) ->
            appendLine("## ${generateDisplayName(folderName)}")
            appendLine()

            // Create HTML img list
            appendLine("<p align=\"left\">")
            thumbnails.forEach { thumbnail ->
                appendLine("    <img src=\"${thumbnail.relativePath}\" hspace=\"10\" align=\"left\">")
            }
            appendLine("</p>")
            appendLine("<br clear=\"both\">")

            appendLine()
        }

        // Summary statistics
        val totalThumbnails = thumbnailsByFolder.values.sumOf { it.size }
        val totalFolders = thumbnailsByFolder.size

        appendLine("---")
        appendLine()
        appendLine("**Total: $totalThumbnails thumbnails across $totalFolders art collections**")
    }
}

/**
 * Updates the README.md file by finding the "Gȧlléry" section and replacing everything below it
 */
private fun updateReadmeFile(projectRoot: Path, generatedMarkdown: String) {
    val readmeFile = projectRoot.resolve("README.md")

    if (!readmeFile.exists()) {
        println("README.md not found at: $readmeFile")
        return
    }

    try {
        val existingContent = readmeFile.readText()
        val lines = existingContent.lines().toMutableList()

        // Find the line containing "Gȧlléry"
        val galleryLineIndex = lines.indexOfFirst { it.contains("Gȧlléry") }

        if (galleryLineIndex == -1) {
            println("Gallery section not found in README.md")
            return
        }

        // Keep everything up to and including the line after "Gȧlléry"
        val keepLines = lines.take(galleryLineIndex + 2)

        // Create new content with the generated markdown
        val newContent = buildString {
            keepLines.forEach { appendLine(it) }
            appendLine()
            append(generatedMarkdown)
        }

        readmeFile.writeText(newContent)
        println("README.md updated successfully!")

    } catch (e: Exception) {
        println("Error updating README.md: ${e.message}")
    }
}

/**
 * Data class representing thumbnail information
 */
private data class ThumbnailInfo(
    val name: String,
    val relativePath: String,
    val displayName: String
) : Comparable<ThumbnailInfo> {
    override fun compareTo(other: ThumbnailInfo): Int = name.compareTo(other.name)
}

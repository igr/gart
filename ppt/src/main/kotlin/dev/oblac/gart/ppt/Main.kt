package dev.oblac.gart.ppt

import dev.oblac.gart.Gart
import dev.oblac.gart.Key
import dev.oblac.gart.color.CssColors
import dev.oblac.gart.color.toFillPaint
import dev.oblac.gart.text.HorizontalAlign
import dev.oblac.gart.text.VerticalAlign
import dev.oblac.gart.text.drawMultilineStringInRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import java.io.File

private var currentSlide = 0
private var codeSnippet: Int? = null

fun main() {
    val gart = Gart.of("presentation", screen)
    println(gart)

    val w = gart.fullScreenWindow()

    w.show { canvas, draw, f ->
        if (f.new) {
            if (f.frame % 100 == 0L) {
                println("Frame: ${f.frame}")
            }
        }
        val snippet = codeSnippet
        if (snippet != null && readCodeSnippet(currentSlide, snippet) != null) {
            canvas.drawCode(currentSlide, snippet)
        } else {
            slides[currentSlide](canvas, draw, f)
        }
    }.onKey {
        when (it) {
            Key.KEY_ESCAPE -> w.close()
            Key.KEY_SPACE, Key.KEY_RIGHT -> {
                codeSnippet = null
                if (currentSlide < slides.size - 1) {
                    currentSlide++
                }
            }

            Key.KEY_LEFT, Key.KEY_BACKSPACE -> {
                codeSnippet = null
                if (currentSlide > 0) {
                    currentSlide--
                }
            }

            Key.KEY_1 -> codeSnippet = if (codeSnippet == 1) null else 1
            Key.KEY_2 -> codeSnippet = if (codeSnippet == 2) null else 2
            Key.KEY_3 -> codeSnippet = if (codeSnippet == 3) null else 3
            Key.KEY_4 -> codeSnippet = if (codeSnippet == 4) null else 4
            Key.KEY_5 -> codeSnippet = if (codeSnippet == 5) null else 5
            Key.KEY_6 -> codeSnippet = if (codeSnippet == 6) null else 6
            Key.KEY_7 -> codeSnippet = if (codeSnippet == 7) null else 7
            Key.KEY_8 -> codeSnippet = if (codeSnippet == 8) null else 8
            Key.KEY_9 -> codeSnippet = if (codeSnippet == 9) null else 9

            else -> {}
        }
    }
}


fun Canvas.drawCode(slideNumber: Int, snippetNumber: Int) {
    clear(0xFF2B2B2B.toInt())
    val snippet = readCodeSnippet(slideNumber, snippetNumber)
    if (snippet == null) {
        drawMultilineStringInRect(
            "No code snippet #$snippetNumber",
            activeRect,
            codeFont,
            codePaint,
            HorizontalAlign.LEFT,
            VerticalAlign.CENTER
        )
        return
    }
    if (snippet.title != null) {
        drawTitle(snippet.title)
    }
    val codeRect = if (snippet.title != null) contentBox else activeRect
    val lines = snippet.code.split('\n')
    val metrics = codeFont.metrics
    val lineHeight = metrics.descent - metrics.ascent + metrics.leading
    val totalHeight = lines.size * lineHeight
    val startY = codeRect.top + (codeRect.height - totalHeight) / 2 - metrics.ascent

    for ((i, line) in lines.withIndex()) {
        val y = startY + i * lineHeight
        var x = codeRect.left
        for (token in tokenizeKotlin(line)) {
            val paint = syntaxPaints.getValue(token.type)
            drawString(token.text, x, y, codeFont, paint)
            x += codeFont.measureTextWidth(token.text)
        }
    }
}

// --- Kotlin syntax highlighting ---

private enum class TokenType {
    KEYWORD, STRING, NUMBER, COMMENT, ANNOTATION, FUNCTION_CALL, PUNCTUATION, PLAIN
}

private val syntaxPaints: Map<TokenType, Paint> = mapOf(
    TokenType.KEYWORD to 0xFFCC7832.toInt().toFillPaint(),      // orange
    TokenType.STRING to 0xFF6A8759.toInt().toFillPaint(),       // green
    TokenType.NUMBER to 0xFF6897BB.toInt().toFillPaint(),       // blue
    TokenType.COMMENT to 0xFF808080.toInt().toFillPaint(),      // gray
    TokenType.ANNOTATION to 0xFFBBB529.toInt().toFillPaint(),   // yellow
    TokenType.FUNCTION_CALL to 0xFFFFC66D.toInt().toFillPaint(),// light yellow
    TokenType.PUNCTUATION to 0xFFA9B7C6.toInt().toFillPaint(), // light gray
    TokenType.PLAIN to CssColors.white.toFillPaint(),
)

private data class Token(val text: String, val type: TokenType)

private val kotlinKeywords = setOf(
    "fun", "val", "var", "if", "else", "when", "for", "while", "do",
    "return", "class", "object", "interface", "package", "import",
    "is", "as", "in", "null", "true", "false", "this", "super",
    "override", "open", "abstract", "sealed", "data", "enum",
    "companion", "private", "protected", "internal", "public",
    "inline", "suspend", "lateinit", "by", "get", "set",
    "try", "catch", "finally", "throw", "break", "continue",
    "typealias", "typeof", "const", "it", "apply", "also", "let", "run", "with",
)

private val punctuation = setOf(
    '(', ')', '{', '}', '[', ']', '.', ',', ';', ':', '=', '+', '-', '*', '/', '<', '>', '!', '&', '|', '?',
)

private fun tokenizeKotlin(line: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0

    while (i < line.length) {
        // line comment
        if (i + 1 < line.length && line[i] == '/' && line[i + 1] == '/') {
            tokens.add(Token(line.substring(i), TokenType.COMMENT))
            break
        }
        // string literal
        if (line[i] == '"') {
            val sb = StringBuilder("\"")
            i++
            while (i < line.length && line[i] != '"') {
                if (line[i] == '\\' && i + 1 < line.length) {
                    sb.append(line[i]); i++
                }
                sb.append(line[i]); i++
            }
            if (i < line.length) {
                sb.append('"'); i++
            }
            tokens.add(Token(sb.toString(), TokenType.STRING))
            continue
        }
        // annotation
        if (line[i] == '@') {
            val sb = StringBuilder("@")
            i++
            while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                sb.append(line[i]); i++
            }
            tokens.add(Token(sb.toString(), TokenType.ANNOTATION))
            continue
        }
        // number
        if (line[i].isDigit() || (line[i] == '.' && i + 1 < line.length && line[i + 1].isDigit())) {
            val sb = StringBuilder()
            while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == '_')) {
                sb.append(line[i]); i++
            }
            // trailing f/L/etc already captured by isLetterOrDigit
            tokens.add(Token(sb.toString(), TokenType.NUMBER))
            continue
        }
        // word (identifier or keyword)
        if (line[i].isLetter() || line[i] == '_') {
            val sb = StringBuilder()
            while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) {
                sb.append(line[i]); i++
            }
            val word = sb.toString()
            val type = when {
                word in kotlinKeywords -> TokenType.KEYWORD
                i < line.length && line[i] == '(' -> TokenType.FUNCTION_CALL
                else -> TokenType.PLAIN
            }
            tokens.add(Token(word, type))
            continue
        }
        // punctuation
        if (line[i] in punctuation) {
            tokens.add(Token(line[i].toString(), TokenType.PUNCTUATION))
            i++
            continue
        }
        // whitespace and anything else
        val sb = StringBuilder()
        while (i < line.length && !line[i].isLetterOrDigit() && line[i] != '_' && line[i] != '"' && line[i] != '@' && line[i] != '/' && line[i] !in punctuation) {
            sb.append(line[i]); i++
        }
        if (sb.isNotEmpty()) {
            tokens.add(Token(sb.toString(), TokenType.PLAIN))
        }
    }
    return tokens
}

private val sourceDir = File("ppt/src/main/kotlin/dev/oblac/gart/ppt")

data class CodeSnippet(val code: String, val title: String?)

private val snippetCache = mutableMapOf<Pair<Int, Int>, CodeSnippet?>()

private fun readCodeSnippet(slideNumber: Int, snippetNumber: Int): CodeSnippet? {
    return snippetCache.getOrPut(slideNumber to snippetNumber) {
        parseCodeSnippet(slideNumber, snippetNumber)
    }
}

private fun parseCodeSnippet(slideNumber: Int, snippetNumber: Int): CodeSnippet? {
    val fileName = "slide%02d.kt".format(slideNumber)
    val file = sourceDir.resolve(fileName)
    if (!file.exists()) return null

    val lines = file.readLines()
    val startMarker = "//--- src: $snippetNumber"
    val endMarker = "//--- crs: $snippetNumber"

    val startIndex = lines.indexOfFirst { it.trim().startsWith(startMarker) }
    val endIndex = lines.indexOfFirst { it.trim() == endMarker }

    if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) return null

    val title = lines[startIndex].trim().removePrefix(startMarker).trim().ifEmpty { null }
    val code = lines.subList(startIndex + 1, endIndex)
        .joinToString("\n")
        .trimIndent()

    return CodeSnippet(code, title)
}

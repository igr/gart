package work.sweeper

import dev.oblac.gart.font.FontFamily
import dev.oblac.gart.font.font
import dev.oblac.gart.saveImageToFile
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

// Sweeper - brute-force any work/ art over a grid of inputs (defined in a .sweep config), render
// headless, dump pngs + params + contact sheets. full docs in docs/sweeper.md.

private const val MAX = 500          // above this many renders we make you pass --yes

private data class Axis(val key: String, val values: List<String>, val swept: Boolean)
private data class Branch(val name: String, val axes: List<Axis>)
private data class Task(val branch: String, val name: String, val combo: List<Pair<String, String>>, val label: String)
private data class Result(val branch: String, val name: String, val ok: Boolean, val msg: String, val png: File?, val label: String)
private data class Opts(val par: Int, val name: String?, val sheet: Boolean, val thumb: Int, val sample: Int, val limit: Int, val yes: Boolean, val dry: Boolean, val timeout: Long)

fun main(args: Array<String>) {
    // one arg only: the .sweep config file. every setting lives in the file, no flags.
    if (args.size != 1 || args[0].startsWith("-")) {
        System.err.println("usage: Sweeper <config.sweep>  (or: just sweep <name>) - see docs/sweeper.md"); return
    }
    val configFile = File(args[0])
    if (!configFile.isFile) { System.err.println("config file not found: ${args[0]}"); return }

    try {
        val cfg = parseConfig(configFile)
        val outDir = File(cfg.out).absoluteFile
        val opts = Opts(
            par = cfg.par ?: (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 8),
            name = cfg.name,
            sheet = cfg.sheet,
            thumb = cfg.thumb ?: -1,
            sample = cfg.sample ?: -1,
            limit = cfg.limit ?: -1,
            yes = cfg.yes,
            dry = cfg.dry,
            timeout = cfg.timeout ?: 180,
        )
        val contDir = cfg.continueDir
        if (contDir != null) {
            val (art, branches) = continueBranches(File(contDir), cfg.vary, cfg.spread ?: 0.15, cfg.steps ?: 3)
            run(resolveMainClass(art), branches, outDir, opts)
        } else {
            val art = cfg.art ?: error("config needs an 'art = ...' line (or 'continue = <dir>')")
            run(resolveMainClass(art), cfg.branches, outDir, opts)
        }
    } catch (e: Exception) {
        System.err.println("error: ${e.message}")
    }
}

// RUN

private fun run(mainClass: String, branches: List<Branch>, outDir: File, o: Opts) {
    val prefix = o.name ?: mainClass.substringAfterLast('.').removeSuffix("Kt").lowercase()

    val tasks = ArrayList<Task>()
    for (b in branches) {
        val swept = b.axes.filter { it.swept }.map { it.key }.toSet()
        val combos = cartesian(b.axes)
        val pad = combos.size.toString().length.coerceAtLeast(3)
        combos.forEachIndexed { idx, combo ->
            val label = combo.filter { it.first in swept }.joinToString(" ") { "${it.first}=${it.second}" }
            tasks += Task(b.name, nameFor(prefix, b.name, idx + 1, pad, combo, swept), combo, label)
        }
    }

    var picked: List<Task> = tasks
    val full = picked.size
    if (o.sample in 1 until picked.size) picked = picked.shuffled(kotlin.random.Random(0L)).take(o.sample)
    if (o.limit in 1 until picked.size) picked = picked.take(o.limit)

    println("art:      $mainClass")
    println("branches: ${branches.size} ${branches.map { it.name.ifEmpty { "(main)" } }}")
    println("renders:  $full${if (picked.size != full) " -> ${picked.size} after sample/limit" else ""}   par=${o.par}")
    println("out:      ${outDir.path}")

    if (o.dry) {
        println("\n-- dry run, sample names --")
        picked.take(8).forEach { println("  ${it.name}   [${it.label}]") }
        if (picked.size > 8) println("  ... and ${picked.size - 8} more")
        return
    }
    if (picked.size > MAX && !o.yes) { println("\nthat's ${picked.size} renders (> $MAX). re-run with --yes, or trim with --sample N / --limit N."); return }
    if (picked.isEmpty()) { println("nothing to render."); return }

    outDir.mkdirs()
    val cp = System.getProperty("java.class.path")
    val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
    val done = AtomicInteger()
    val pool = Executors.newFixedThreadPool(o.par)
    val started = System.currentTimeMillis()

    val results = picked.map { t ->
        pool.submit(Callable {
            val r = renderOne(t, mainClass, outDir, cp, javaBin, o.timeout)
            val k = done.incrementAndGet()
            println("[$k/${picked.size}] ${if (r.ok) "ok  " else "FAIL"} ${r.name}${if (r.ok) "" else "  (${r.msg})"}")
            r
        })
    }.map { it.get() }
    pool.shutdown()

    val ok = results.count { it.ok }
    println("\ndone: $ok/${results.size} ok in ${"%.1f".format(Locale.US, (System.currentTimeMillis() - started) / 1000.0)}s")
    results.filter { !it.ok }.take(8).forEach { println("  failed: ${it.name} (${it.msg})") }

    if (o.sheet) results.filter { it.ok }.groupBy { it.branch }.forEach { (branch, rs) ->
        val sheet = File(outDir, "_sheet${if (branch.isEmpty()) "" else "_$branch"}.png")
        runCatching { buildSheet(sheet, "$prefix $branch".trim(), rs, o.thumb); println("sheet: ${sheet.path}  (${rs.size} imgs)") }
            .onFailure { System.err.println("sheet failed for '$branch': ${it.message}") }
    }
    println("-> ${outDir.path}")
}

/** run one combo as a headless subprocess; save <name>.png + <name>.txt (full resolved params). */
private fun renderOne(t: Task, mainClass: String, outDir: File, cp: String, javaBin: String, timeoutSec: Long): Result {
    val target = File(outDir, "${t.name}.png")
    val tmp = Files.createTempDirectory("sweep_").toFile()
    val log = File(tmp, "_log.txt")
    val dump = File(tmp, "_params.txt")
    try {
        val cmd = ArrayList<String>()
        cmd += javaBin; cmd += "-cp"; cmd += cp
        cmd += "-Dgart.headless=true"; cmd += "-Dparams.out=${dump.absolutePath}"
        for ((k, v) in t.combo) cmd += "-D$k=$v"
        cmd += "-Dout=${File(outDir, t.name).absolutePath}"
        cmd += mainClass; cmd += "--render"

        val proc = ProcessBuilder(cmd).directory(tmp).redirectErrorStream(true).redirectOutput(log).start()
        if (!proc.waitFor(timeoutSec, TimeUnit.SECONDS)) { proc.destroyForcibly(); return Result(t.branch, t.name, false, "timeout", null, t.label) }
        if (proc.exitValue() != 0) return Result(t.branch, t.name, false, "exit ${proc.exitValue()}: ${log.readText().trim().takeLast(140)}", null, t.label)
        if (!target.exists()) tmp.listFiles { f -> f.extension.equals("png", true) }?.firstOrNull()?.copyTo(target, overwrite = true)
        if (!target.exists()) return Result(t.branch, t.name, false, "no png produced", null, t.label)

        // prefer the art's full resolved dump (defaults included); fall back to what we passed
        val fullParams = if (dump.exists()) readParams(dump).filterKeys { it != "out" } else t.combo.toMap()
        File(outDir, "${t.name}.txt").writeText(buildTxt(mainClass, fullParams, t.combo))
        return Result(t.branch, t.name, true, "", target, t.label)
    } catch (e: Exception) {
        return Result(t.branch, t.name, false, e.message ?: e.javaClass.simpleName, null, t.label)
    } finally {
        tmp.deleteRecursively()
    }
}

private fun buildTxt(mainClass: String, params: Map<String, String>, repro: List<Pair<String, String>>): String = buildString {
    appendLine("# sweeper render")
    appendLine("# art: $mainClass")
    appendLine()
    for ((k, v) in params) appendLine("$k=$v")
    appendLine()
    appendLine("# reproduce:")
    appendLine("# java @work/build/classpath.txt " + repro.joinToString(" ") { "-D${it.first}=${it.second}" } + " $mainClass --render")
}

private fun readParams(f: File): Map<String, String> = f.readLines().mapNotNull { line ->
    val s = line.trim()
    if (s.isEmpty() || s.startsWith("#") || !s.contains('=')) null else s.substringBefore('=').trim() to s.substringAfter('=').trim()
}.toMap(LinkedHashMap())

// CONTACT SHEET (the big table) - pure skia, tile the pngs into a labelled grid

private fun buildSheet(out: File, title: String, results: List<Result>, thumbOverride: Int) {
    val n = results.size
    val cols = min(if (n > 64) 10 else if (n > 24) 8 else 6, n).coerceAtLeast(1)
    val rows = ceil(n / cols.toDouble()).toInt()
    val thumb = if (thumbOverride > 0) thumbOverride else if (n > 64) 150 else if (n > 24) 200 else 260
    val labelH = 26; val pad = 8; val headerH = 38
    val cellW = thumb + pad; val cellH = thumb + labelH + pad
    val w = cols * cellW + pad; val h = headerH + rows * cellH + pad

    val surface = Surface.makeRasterN32Premul(w, h)
    val c = surface.canvas
    c.clear(0xFF0B0E14.toInt())
    val titlePaint = Paint().apply { color = 0xFFD4EDF3.toInt() }
    val labelPaint = Paint().apply { color = 0xFF9FB3C8.toInt() }
    c.drawString("$title   ·   $n images", pad.toFloat(), 25f, font(FontFamily.SpaceMonoBold, 18f), titlePaint)
    val labelFont = font(FontFamily.SpaceMono, 12f)

    results.forEachIndexed { idx, r ->
        val cx = pad + (idx % cols) * cellW
        val cy = headerH + (idx / cols) * cellH
        r.png?.let { f ->
            runCatching {
                val img = Image.makeFromEncoded(f.readBytes())
                c.drawImageRect(img, Rect.makeXYWH(cx.toFloat(), cy.toFloat(), thumb.toFloat(), thumb.toFloat()))
                img.close()
            }
        }
        val text = (r.label.ifEmpty { r.name }).let { if (it.length > thumb / 7) it.take(thumb / 7 - 1) + "…" else it }
        c.drawString(text, cx.toFloat(), (cy + thumb + 16).toFloat(), labelFont, labelPaint)
    }
    saveImageToFile(surface.makeImageSnapshot(), out.absolutePath)
}

// CONTINUE - branch a tight neighbourhood around each survivor's params

private fun continueBranches(keepDir: File, vary: String?, spread: Double, steps: Int): Pair<String, List<Branch>> {
    require(keepDir.isDirectory) { "${keepDir.path} is not a folder" }
    val txts = keepDir.walkTopDown().filter { it.isFile && it.extension == "txt" }.toList()
    require(txts.isNotEmpty()) { "no .txt param files under ${keepDir.path} - keep some renders (with their .txt) there first" }
    val varyKeys = vary?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()

    var art: String? = null
    val branches = ArrayList<Branch>()
    txts.sortedBy { it.name }.forEachIndexed { si, txt ->
        txt.readLines().firstOrNull { it.trim().startsWith("# art:") }?.let { art = art ?: it.substringAfter("# art:").trim() }
        val params = readParams(txt).filterKeys { it != "out" }
        val toVary = (varyKeys ?: params.filter { it.key != "seed" && it.value.toDoubleOrNull() != null }.keys)
            .filter { params[it]?.toDoubleOrNull() != null }
        val axes = params.map { (k, vv) ->
            if (k in toVary) Axis(k, neighbourhood(vv, spread, steps), true) else Axis(k, listOf(vv), false)
        }
        branches += Branch("s${(si + 1).toString().padStart(2, '0')}", axes)
    }
    return (art ?: error("no '# art:' line in the kept .txt files")) to branches
}

/** values around [baseStr] - steps points spanning +-spread (relative, abs fallback near 0).
 *  always floats: continue is for continuous knobs (pull/curl/agew/...), not int counts/seed. */
private fun neighbourhood(baseStr: String, spread: Double, steps: Int): List<String> {
    val base = baseStr.toDouble()
    if (steps <= 1) return listOf(fmtNum(base))
    val delta = spread * (if (abs(base) < 1e-9) 1.0 else abs(base))
    val lo = base - delta; val hi = base + delta
    return (0 until steps).map { k -> fmtNum(lo + (hi - lo) * k / (steps - 1)) }.distinct()
}

// CONFIG FILE (.sweep)

private class Config {
    var art: String? = null
    var out: String = "output"
    var par: Int? = null
    var name: String? = null
    var sheet: Boolean = true
    var thumb: Int? = null
    var sample: Int? = null
    var limit: Int? = null
    var timeout: Long? = null
    var yes: Boolean = false
    var dry: Boolean = false
    var continueDir: String? = null
    var vary: String? = null
    var spread: Double? = null
    var steps: Int? = null
    var branches: List<Branch> = emptyList()
}

private fun parseConfig(file: File): Config {
    val cfg = Config()
    val fixed = LinkedHashMap<String, Axis>()
    val globals = LinkedHashMap<String, Axis>()
    val branchAxes = LinkedHashMap<String, LinkedHashMap<String, Axis>>()
    var cur: String? = null

    for (raw in file.readLines()) {
        val line = raw.substringBefore('#').trim()
        if (line.isEmpty()) continue
        if (line.startsWith("[") && line.endsWith("]")) {
            cur = line.removePrefix("[").removeSuffix("]").removePrefix("branch").trim()
            branchAxes.getOrPut(cur) { LinkedHashMap() }
            continue
        }
        val key = line.substringBefore('=').trim()
        val value = line.substringAfter('=').trim()
        if (cur == null) when (key) {
            "art" -> cfg.art = value
            "out" -> cfg.out = value
            "par" -> cfg.par = value.toInt()
            "name" -> cfg.name = value
            "thumb" -> cfg.thumb = value.toInt()
            "sheet" -> cfg.sheet = value.lowercase() in setOf("on", "true", "yes", "1")
            "sample" -> cfg.sample = value.toInt()
            "limit" -> cfg.limit = value.toInt()
            "timeout" -> cfg.timeout = value.toLong()
            "yes" -> cfg.yes = value.lowercase() in setOf("on", "true", "yes", "1")
            "dry" -> cfg.dry = value.lowercase() in setOf("on", "true", "yes", "1")
            "continue" -> cfg.continueDir = value
            "vary" -> cfg.vary = value
            "spread" -> cfg.spread = value.toDouble()
            "steps" -> cfg.steps = value.toInt()
            "fixed" -> value.split(Regex("\\s+")).filter { it.contains('=') }.forEach { val a = parseSpec(it); fixed[a.key] = a }
            else -> { val a = parseSpec("$key=$value"); globals[a.key] = a }
        } else {
            val a = parseSpec("$key=$value"); branchAxes[cur]!![a.key] = a
        }
    }

    cfg.branches = if (branchAxes.isEmpty()) listOf(Branch("", mergeAxes(fixed, globals)))
    else branchAxes.map { (n, ax) -> Branch(n, mergeAxes(fixed, globals, ax)) }
    return cfg
}

private fun mergeAxes(vararg maps: Map<String, Axis>): List<Axis> {
    val out = LinkedHashMap<String, Axis>()
    for (m in maps) for ((k, v) in m) out[k] = v
    return out.values.toList()
}

// SPEC PARSING + HELPERS

private fun parseSpec(spec: String): Axis {
    val eq = spec.indexOf('=')
    require(eq > 0) { "bad spec '$spec' (expected key=...)" }
    val key = spec.substring(0, eq)
    val rest = spec.substring(eq + 1)
    return when {
        rest.contains(':') -> {
            val p = rest.split(':')
            require(p.size == 2 || p.size == 3) { "bad range '$spec' (from:to or from:to:step)" }
            val vals = rangeValues(p[0], p[1], p.getOrNull(2))
            Axis(key, vals, vals.size > 1)
        }
        rest.contains(',') -> {
            val vals = rest.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            Axis(key, vals, vals.size > 1)
        }
        else -> Axis(key, listOf(rest), false)
    }
}

private fun rangeValues(from: String, to: String, step: String?): List<String> {
    val ints = !from.contains('.') && !to.contains('.') && (step == null || !step.contains('.'))
    val f = from.toDouble(); val t = to.toDouble()
    val s = step?.toDouble() ?: 1.0
    require(s > 0.0) { "step must be > 0" }
    val n = round(abs(t - f) / s).toInt()
    val dir = if (t >= f) 1.0 else -1.0
    return (0..n).map { k -> val x = f + dir * k * s; if (ints) x.toLong().toString() else fmtNum(x) }
}

private fun fmtNum(d: Double): String {
    if (d == floor(d) && !d.isInfinite()) return d.toLong().toString()
    return String.format(Locale.US, "%.4f", d).trimEnd('0').trimEnd('.')
}

private fun cartesian(axes: List<Axis>): List<List<Pair<String, String>>> {
    var acc = listOf(emptyList<Pair<String, String>>())
    for (ax in axes) {
        val nx = ArrayList<List<Pair<String, String>>>(acc.size * ax.values.size)
        for (combo in acc) for (v in ax.values) nx.add(combo + (ax.key to v))
        acc = nx
    }
    return acc
}

private fun resolveMainClass(arg: String): String =
    if (arg.contains('.')) arg else "work.$arg.${arg.replaceFirstChar { it.uppercase() }}Kt"

private fun nameFor(prefix: String, branch: String, index: Int, pad: Int, combo: List<Pair<String, String>>, swept: Set<String>): String {
    val base = if (branch.isEmpty()) "${prefix}_${index.toString().padStart(pad, '0')}"
    else "${prefix}_${branch}_${index.toString().padStart(pad, '0')}"
    val tail = combo.filter { it.first in swept }.joinToString("") { "_${it.first}-${sanitize(it.second)}" }
    val fullName = base + tail
    return if (fullName.length > 120) base else fullName
}

private fun sanitize(s: String) = buildString { for (ch in s) append(if (ch.isLetterOrDigit() || ch == '.' || ch == '-') ch else '_') }

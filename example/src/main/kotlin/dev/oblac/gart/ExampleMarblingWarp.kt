package dev.oblac.gart

import dev.oblac.gart.angle.Degrees
import dev.oblac.gart.brush.Brushes
import dev.oblac.gart.brush.drawBrush
import dev.oblac.gart.color.Palette
import dev.oblac.gart.gfx.drawBlackText
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.io.detectHeadlessFlags
import dev.oblac.gart.marbling.Ink
import dev.oblac.gart.marbling.Marbling
import dev.oblac.gart.marbling.contours
import dev.oblac.gart.math.between
import dev.oblac.gart.math.lerp
import dev.oblac.gart.util.Stopwatch
import org.jetbrains.skia.Rect
import java.io.File
import kotlin.random.Random

/**
 * ExampleMarblingWarp: the same engine three ways. Left, a drawn picture is the bath, so the
 * combs and drops rake the picture (Ink.image). Middle, a recipe's drops come back as vector
 * contours and are filled as paths. Right, the same contours stroked with a pen - the line art
 * of a marbling. Run with `-Dheadless=1` to just write `output/exampleMarblingWarp.png`.
 */
fun main(args: Array<String>) {
    val s = 600
    val sf = s.toFloat()
    val gap = 30f
    val top = gap + 20f
    val gart = Gart.of("exampleMarblingWarp", (3 * sf + 4 * gap).toInt(), (sf + top + gap).toInt())
    val g = gart.gartvas()
    val c = g.canvas
    c.clear(0xFFFFFFFF.toInt())
    val rnd = Random(7)
    val sw = Stopwatch()

    val paper = 0xFFF1E9D6.toInt()
    val inks = Palette(0xFF24364F, 0xFFA0342C, 0xFFD9A441, 0xFF6F8F6A, 0xFF2B2B2B, 0xFFF6F1E6)

    // 1. a picture drawn the ordinary way - bands, a disc, a stripe - then treated as the bath
    val pic = Gartvas(Dimension(s, s))
    val bands = Palette(0xFF264653, 0xFF2A9D8F, 0xFFE9C46A, 0xFFF4A261, 0xFFE76F51)
    for (i in 0 until 15) pic.canvas.drawRect(Rect.makeXYWH(0f, i * sf / 15, sf, sf / 15), fillOf(bands.safe(i)))
    pic.canvas.drawRect(Rect.makeXYWH(sf * 0.44f, 0f, sf * 0.12f, sf), fillOf(0xFFF6F1E6))
    pic.canvas.drawCircle(sf * 0.5f, sf * 0.5f, sf * 0.24f, fillOf(0xFF111111))
    pic.canvas.drawCircle(sf * 0.5f, sf * 0.5f, sf * 0.13f, fillOf(0xFFF6F1E6))

    val raked = Marbling(Ink.image(Gartmap(pic)))
    val sp = sf / 9
    raked.comb(sf / 2, sf / 2, Degrees(0f), z = sf * 0.14f, c = sp / 3, tines = 13, spacing = sp)
    raked.comb(sf / 2, sf / 2 + sp / 2, Degrees(180f), z = sf * 0.14f, c = sp / 3, tines = 13, spacing = sp)
    raked.comb(sf / 2, sf / 2, Degrees(90f), z = sf * 0.16f, c = 8f, tines = 20, spacing = sf / 14, amplitude = 10f, wavelength = sf / 3)
    raked.shift(0f, -sf * 0.18f)
    raked.vortex(sf * 0.66f, sf * 0.36f, z = sf * 0.22f, c = sf * 0.09f)
    for (i in 0 until 10) raked.drop(rnd.between(0.1f, 0.9f) * sf, rnd.between(0.1f, 0.9f) * sf, rnd.between(0.025f, 0.055f) * sf, inks[i % inks.size])
    Gartmap(Dimension(s, s)).use { bmp ->
        raked.render(bmp, aa = 3)
        c.drawImage(bmp.image(), gap, top)
    }
    c.drawBlackText("a picture raked  (${raked.ops.size} ops)", gap, top - 6f)
    println("raked: ${sw.lap()} ms")

    // 2. a recipe as vector contours: stone bed, gelgit, two whirls - every drop one closed path
    val vec = Marbling(paper)
    for (i in 0 until 110) {
        val r = sf * lerp(0.12f, 0.02f, i / 109f) * rnd.between(0.7f, 1.3f)
        vec.drop(rnd.between(-0.2f, 1.2f) * sf, rnd.between(-0.2f, 1.2f) * sf, r, inks[i % inks.size])
    }
    vec.comb(sf / 2, sf / 2, Degrees(0f), z = sf * 0.15f, c = sp / 3, tines = 17, spacing = sp)
    vec.comb(sf / 2, sf / 2 + sp / 2, Degrees(180f), z = sf * 0.15f, c = sp / 3, tines = 17, spacing = sp)
    vec.whirl(sf * 0.3f, sf * 0.35f, sf * 0.09f, z = sf * 0.3f, c = sf * 0.05f)
    vec.whirl(sf * 0.7f, sf * 0.68f, sf * 0.07f, z = sf * 0.25f, c = sf * 0.04f)
    val cs = vec.contours(step = 1.5f)
    println("contours: ${cs.size} paths, ${cs.sumOf { it.points.size }} points, ${sw.lap()} ms")

    c.save()
    c.translate(2 * gap + sf, top)
    c.clipRect(Rect(0f, 0f, sf, sf))
    c.drawRect(Rect(0f, 0f, sf, sf), fillOf(paper))
    for (ct in cs) c.drawPath(ct.path, fillOf(ct.color))
    c.restore()
    c.drawBlackText("the same as filled paths  (${cs.size} contours)", 2 * gap + sf, top - 6f)
    println("filled: ${sw.lap()} ms")

    // 3. the contours as pen lines, the drawing of a marbling
    c.save()
    c.translate(3 * gap + 2 * sf, top)
    c.clipRect(Rect(0f, 0f, sf, sf))
    c.drawRect(Rect(0f, 0f, sf, sf), fillOf(paper))
    for (ct in cs) c.drawBrush(ct.path, Brushes.pen, 0xFF2B2B2B.toInt(), 1.2f, rnd)
    c.restore()
    c.drawBlackText("and as pen lines", 3 * gap + 2 * sf, top - 6f)
    println("pen: ${sw.lap()} ms")

    File("output").mkdirs()
    gart.saveImage(g, "output/exampleMarblingWarp.png")
    if (!detectHeadlessFlags(args)) gart.window().showImage(g)
}

package dev.oblac.gart

import dev.oblac.gart.cellular.rule.generateRuleCellularAutomaton
import dev.oblac.gart.color.Colors
import dev.oblac.gart.color.Palettes
import dev.oblac.gart.gfx.dimension
import dev.oblac.gart.gfx.fillOf
import dev.oblac.gart.gfx.strokeOfBlack
import dev.oblac.gart.gfx.strokeOfWhite
import dev.oblac.gart.math.f
import dev.oblac.gart.math.rndi
import dev.oblac.gart.util.forSequence
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect

fun main() {
    val gart = Gart.of("rulez", 1024, 1024)
    println(gart)

    val w = gart.window()
    val g = gart.gartvas()

    val draw = MyDraw3(g)

    // save image
    g.draw(draw)
    gart.saveImage(g)

    w.show(draw).hotReload(g)
}

/**
 * This version draws static image.
 */
private class MyDraw3(g: Gartvas) : Drawing(g) {
    init {
        draw(g.canvas, g.d)
    }
}

private val rule9 = { generateRuleCellularAutomaton(rule = 9, neighborsAside = 1) }
private val rule13 = { generateRuleCellularAutomaton(rule = 13, neighborsAside = 1) }
private val rule45 = { generateRuleCellularAutomaton(rule = 45, neighborsAside = 1) }
private val rule30 = { generateRuleCellularAutomaton(rule = 30, neighborsAside = 1) }
private val rule57 = { generateRuleCellularAutomaton(rule = 57, neighborsAside = 1) }
private val rule61 = { generateRuleCellularAutomaton(rule = 61, neighborsAside = 1) }
private val rule73 = { generateRuleCellularAutomaton(rule = 73, neighborsAside = 1) }
private val rule94 = { generateRuleCellularAutomaton(rule = 94, neighborsAside = 1) }
private val rule133 = { generateRuleCellularAutomaton(rule = 133, neighborsAside = 1) }

private val rule838 = { generateRuleCellularAutomaton(rule = 838, neighborsAside = 2) }
private val rule209218 = { generateRuleCellularAutomaton(rule = 209218, neighborsAside = 2) }
private val rule774857 = { generateRuleCellularAutomaton(rule = 774857, neighborsAside = 2) }
private val rule316715877 = { generateRuleCellularAutomaton(rule = 316715877, neighborsAside = 2) }
private val rule37788005 = { generateRuleCellularAutomaton(rule = 37788005, neighborsAside = 2) }
private val rule22047073 = { generateRuleCellularAutomaton(rule = 22047073, neighborsAside = 2) }
private val rule1069090987 = { generateRuleCellularAutomaton(rule = 1069090987, neighborsAside = 2) }

private val allRules = listOf(
    rule9, rule13, rule30, rule45, rule57, rule61, rule73, rule94, rule133,
    rule838, rule209218, rule774857, rule316715877, rule37788005, rule22047073,
    rule1069090987
).shuffled()

private val pal = Palettes.colormap072

private fun draw(c: Canvas, d: Dimension) {
    c.clear(Colors.white)

    forSequence(0, 3)
        .fold(listOf(d.rect)) { acc, _ ->
            divideRects(acc, 100f)
        }
        .zip(allRules)
        .forEach { (r, rule) ->
            val whiteColor = pal.random()
            val blackColor = pal.randomExclude(whiteColor)
            val i = generateImage(
                rule,
                r.dimension(),
                falseColor = whiteColor,
                trueColor = blackColor
            )
            c.drawImage(i, r.left, r.top)
        }
}

private fun generateImage(
    rule: () -> List<List<Boolean>>,
    d: Dimension,
    falseColor: Int = Colors.white,
    trueColor: Int = Colors.black
): Image {
    val gartvas = Gartvas.of(1024, 1024)
    val c = gartvas.canvas
    c.clear(falseColor)
    rule().let {
        it.forEachIndexed { y, row ->
            row.forEachIndexed { x, cell ->
                if (cell) {
//                    c.drawCircle(x.toFloat() * 4, y.toFloat() * 4, 2f, fillOfBlack())
                    val pixel = Rect.makeXYWH(x.f() * 4, y.f() * 4, 4f, 4f)
                    c.drawRect(pixel, fillOf(trueColor))
                }
            }
        }
    }
    val rect = Rect.makeXYWH(512 - d.w / 2f, 0f, d.wf, d.hf)
    c.drawRect(rect, strokeOfWhite(20f))
    c.drawRect(rect, strokeOfBlack(6f))
    return gartvas.snapshot(rect)!!
}


private fun divideRects(startingRect: List<Rect>, gap: Float): List<Rect> =
    startingRect.flatMap { rect ->
        if (rect.width < gap * 4 || rect.height < gap * 4) {
            listOf(rect)
        } else {
            val divideWidth = rndi(1, (rect.width / gap).toInt()) * gap
            val divideHeight = rndi(1, (rect.height / gap).toInt()) * gap

            listOf(
                Rect(rect.left, rect.top, rect.left + divideWidth, rect.top + divideHeight),     // top left
                Rect(rect.left + divideWidth, rect.top, rect.right, rect.top + divideHeight),    // top right
                Rect(rect.left, rect.top + divideHeight, rect.left + divideWidth, rect.bottom),  // bottom left
                Rect(rect.left + divideWidth, rect.top + divideHeight, rect.right, rect.bottom)  // bottom right
            )
        }
    }
